#!/usr/bin/env python3
"""
LQD Merchant Classifier — Retraining Script
============================================
Trains a new on-device ML model (log-linear softmax / multinomial logistic regression)
using two data sources:

  1. WEAK LABELS from the app's keyword/rule engine — every transaction whose
     category was assigned at tier 1-5 (brand disambiguation, hard rules) is
     treated as a high-confidence training sample.  Tier 7-8 keyword matches
     are used as medium-confidence samples.  ML-assigned and OTHERS are skipped.

  2. USER CORRECTIONS from the live Room database — any transaction with
     isUserEdited=1 is treated as ground-truth (highest confidence), because
     these were manually reviewed by a human user.

Output: app/src/main/assets/models/merchant_classifier_weights.json
        (same schema as the existing model — drop-in replacement)

Usage
-----
  # From the project root:
  python3 ml_training/retrain.py

  # With a specific DB path:
  python3 ml_training/retrain.py --db /path/to/expense_manager.db

  # Dry-run (print stats, don't overwrite model):
  python3 ml_training/retrain.py --dry-run

  # Save training data CSV for inspection:
  python3 ml_training/retrain.py --export-csv training_data.csv

Requirements:
  pip install scikit-learn numpy
"""

import argparse
import json
import math
import os
import re
import sqlite3
import sys
import collections
from pathlib import Path

try:
    import numpy as np
    from sklearn.linear_model import LogisticRegression
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.model_selection import StratifiedKFold, cross_val_score
    from sklearn.metrics import classification_report
    from sklearn.utils.class_weight import compute_class_weight
    from sklearn.preprocessing import LabelEncoder
except ImportError as e:
    print(f"[ERROR] Missing dependency: {e}")
    print("Install with:  pip install scikit-learn numpy")
    sys.exit(1)

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DB = PROJECT_ROOT / "app" / "databases" / "expense_manager.db"
OUTPUT_JSON = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "models" / "merchant_classifier_weights.json"

CATEGORIES = [
    "FOOD", "GROCERIES", "SHOPPING", "TRANSPORT", "BILLS_UTILITIES",
    "ENTERTAINMENT", "HEALTHCARE", "INVESTMENT", "SALARY_INCOME",
    "TRANSFERS", "FEES_CHARGES", "EDUCATION", "PERSONAL", "OTHERS"
]

# Categories to EXCLUDE from training data (they're defined structurally, not by merchant name)
EXCLUDE_CATEGORIES = {"TRANSFERS", "SALARY_INCOME", "OTHERS"}

# Minimum samples per class to include in training; classes below this are over-sampled
MIN_SAMPLES_PER_CLASS = 20

# Cross-validation folds
CV_FOLDS = 5

# TF-IDF / model hyperparameters — tuned to match the existing on-device architecture
WORD_NGRAM_RANGE = (1, 2)      # unigrams + bigrams (matches OnDeviceMerchantClassifier)
CHAR_NGRAM_RANGE = (3, 4)      # char 3-grams + 4-grams (matches OnDeviceMerchantClassifier)
WORD_MAX_FEATURES = 2233       # match existing vocab size
CHAR_MAX_FEATURES = 3000       # match existing vocab size
LOGISTIC_C = 1.5               # regularisation (higher = less regularised than the default 1.0)
LOGISTIC_MAX_ITER = 2000

# ─────────────────────────────────────────────────────────────────────────────
# Keyword-based weak labeller
# Mirror of CategoryClassifier.kt's KEYWORD_CATEGORY_MAP (abbreviated for speed)
# ─────────────────────────────────────────────────────────────────────────────

KEYWORD_LABELS = {
    "FOOD": [
        "swiggy", "zomato", "dominos", "mcdonalds", "kfc", "starbucks", "burger king",
        "pizza hut", "subway", "faasos", "box8", "behrouz", "wow momo", "dunkin",
        "chaayos", "chai point", "costa coffee", "barista", "theobroma", "havmor",
        "barbeque nation", "mainland china", "farzi cafe", "hard rock cafe",
        "haldiram", "bikanervala", "saravana bhavan", "saravana", "thalappakatti",
        "paradise biryani", "bawarchi", "biryani", "restaurant", "dining", "food",
        "bakery", "cafe", "canteen", "tiffin", "kitchen", "dhaba", "bistro",
        "dessert", "snacks", "pizza", "burger", "momos", "brewery", "pub",
        "chaayos", "tea", "coffee", "juice", "waffle", "pastry",
    ],
    "GROCERIES": [
        "zepto", "blinkit", "instamart", "bigbasket", "dmart", "jiomart",
        "milkbasket", "country delight", "grofers", "licious", "freshtohome",
        "meatigo", "supermarket", "hypermarket", "kirana", "grocery", "groceries",
        "dairy", "milk", "vegetables", "fruits", "organic", "mandi", "reliance smart",
    ],
    "SHOPPING": [
        "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal",
        "zara", "h&m", "shoppers stop", "lifestyle", "pantaloons", "zudio",
        "fabindia", "decathlon", "bata", "woodland", "puma", "nike", "adidas",
        "croma", "reliance digital", "vijay sales", "lenskart", "tanishq",
        "caratlane", "malabar gold", "ikea", "pepperfry", "urban ladder",
        "firstcry", "crossword", "shopping", "retail",
    ],
    "TRANSPORT": [
        "uber", "ola", "rapido", "blusmart", "namma yatri", "irctc", "metro",
        "dmrc", "bmtc", "ksrtc", "redbus", "makemytrip", "goibibo", "cleartrip",
        "ixigo", "indigo", "air india", "spicejet", "fastag", "nhai", "toll",
        "parking", "petrol", "diesel", "iocl", "bpcl", "hpcl", "fuel",
        "indian oil", "bharat petroleum", "hindustan petroleum", "taxi", "cab",
        "auto", "rickshaw", "airfare", "flight", "airport", "oyo", "treebo",
        "statiq", "ather", "chargezone", "ola electric",
    ],
    "BILLS_UTILITIES": [
        "airtel", "reliance jio", "vodafone", "bsnl", "tata play", "dish tv",
        "act fibernet", "hathway", "jiofiber", "broadband", "bescom", "tata power",
        "adani electricity", "msedcl", "torrent power", "tneb", "electricity",
        "mahanagar gas", "igl", "indane", "bharat gas", "lpg", "nobroker",
        "mygate", "maintenance", "rent", "recharge", "postpaid", "prepaid",
        "google cloud", "github", "openai", "deepseek", "chatgpt",
    ],
    "ENTERTAINMENT": [
        "netflix", "spotify", "amazon prime", "disney", "hotstar", "jiohotstar",
        "jiocinema", "apple tv", "sonyliv", "zee5", "youtube premium", "gaana",
        "bookmyshow", "pvr", "inox", "cinepolis", "steam", "playstation",
        "xbox", "gaming", "theatre", "cinema", "movies",
    ],
    "HEALTHCARE": [
        "apollo pharmacy", "1mg", "tata 1mg", "pharmeasy", "medplus", "netmeds",
        "practo", "dr lal", "lal pathlabs", "metropolis", "thyrocare",
        "apollo hospital", "fortis", "manipal", "narayana health", "cloudnine",
        "clove dental", "hospital", "clinic", "pharmacy", "chemist", "medical",
        "medicine", "doctor", "diagnostic", "pharma", "dental",
    ],
    "INVESTMENT": [
        "groww", "zerodha", "upstox", "angel one", "5paisa", "kuvera",
        "smallcase", "scripbox", "paytm money", "et money", "wazirx", "coindcx",
        "mutual fund", "sip", "lic", "hdfc life", "icici prudential life",
        "max life", "sbi life", "bajaj allianz", "star health", "care health",
        "policybazaar", "insurance", "stock", "equity", "gold bond",
    ],
    "FEES_CHARGES": [
        "annual fee", "late fee", "penalty", "forex markup", "gst on charges",
        "interest charge", "amc", "annual maintenance", "processing fee",
        "service charge", "bounce charges", "overlimit fee", "penal charges",
        "debit card fee", "minimum balance", "non maintenance", "surcharge",
        "bank charge", "transaction fee", "card fee", "renewal fee",
    ],
    "EDUCATION": [
        "coursera", "udemy", "upgrad", "scaler", "unacademy", "byju",
        "vedantu", "physicswallah", "allen", "aakash", "duolingo",
        "school", "college", "university", "tuition", "exam fee",
    ],
    "PERSONAL": [
        "cultfit", "cult.fit", "gold's gym", "anytime fitness", "gym", "yoga",
        "urban company", "naturals salon", "lakme salon", "vlcc", "kaya skin",
        "salon", "spa", "barber", "haircut", "fnp", "ferns n petals", "fern n petals",
        "floweraura", "florist", "flowers", "fitness", "pilates", "zumba", "crossfit",
        "dermatologist", "grooming", "massage",
    ],
}

# Compile keyword sets for fast lookup
_KEYWORD_TO_CATEGORY = {}
for cat, keywords in KEYWORD_LABELS.items():
    for kw in keywords:
        _KEYWORD_TO_CATEGORY[kw.lower()] = cat


def keyword_label(text: str) -> str | None:
    """Return a category label if the text contains a known keyword, else None."""
    t = text.lower()
    # Longer keywords first for precision
    for kw in sorted(_KEYWORD_TO_CATEGORY.keys(), key=len, reverse=True):
        if re.search(r'\b' + re.escape(kw) + r'\b', t):
            return _KEYWORD_TO_CATEGORY[kw]
    return None


# ─────────────────────────────────────────────────────────────────────────────
# MerchantNormalizer (mirrors MerchantNormalizer.kt)
# ─────────────────────────────────────────────────────────────────────────────

_GATEWAY_PREFIX = re.compile(
    r'^(?:razorpay|payu|billdesk|ccavenue|cashfree|payumoney|freecharge|'
    r'mobikwik|worldline|ingenico|[A-Z]{2,6})\s*[*_\-|/]\s*', re.IGNORECASE
)
_POS_ECOM_PREFIX = re.compile(r'^(?:PHP|PG|POS|ECOM|BIL|IN|SQ|NP)\s*[*_\-]\s*', re.IGNORECASE)
_TRAILING_NOISE = re.compile(
    r'[_\-\s/]*(?:ORDER|TXN|BILL|PAY|PAYMENT|SERVICES?|SVCS?|TECH|'
    r'DIGITAL|ONLINE|INDIA|PVT|LTD)\s*[_\-\s/]*[A-Z0-9]{3,}$', re.IGNORECASE
)
_NUMERIC_SUFFIX = re.compile(r'[0-9]{4,}$')
_NOISE_TOKENS = {
    "upi", "neft", "imps", "rtgs", "nach", "mandate", "ecs",
    "credit card", "debit card", "bank account", "payment", "transfer",
}


def normalize_merchant(raw: str) -> str:
    """Clean a raw merchant string before feature extraction."""
    if not raw:
        return ""
    s = raw.strip()
    s = _GATEWAY_PREFIX.sub("", s)
    s = _POS_ECOM_PREFIX.sub("", s)
    s = _TRAILING_NOISE.sub("", s).strip()
    s = _NUMERIC_SUFFIX.sub("", s).strip()
    s = re.sub(r'[_\-|]+', ' ', s).strip()
    s = re.sub(r'\s{2,}', ' ', s).strip()
    if s.lower() in _NOISE_TOKENS:
        return ""
    return s


# ─────────────────────────────────────────────────────────────────────────────
# Data loading
# ─────────────────────────────────────────────────────────────────────────────

def load_from_db(db_path: str) -> list[dict]:
    """Load all transactions from the Room SQLite database."""
    if not os.path.exists(db_path):
        print(f"[WARN] Database not found at {db_path}")
        return []

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("""
        SELECT merchantName, category, isUserEdited, classificationReason, rawBody, amount
        FROM transactions
        WHERE category IS NOT NULL
          AND category != ''
    """)
    rows = cur.fetchall()
    conn.close()

    records = []
    for merchant_name, category, is_user_edited, reason, raw_body, amount in rows:
        records.append({
            "merchant": merchant_name or "",
            "category": category,
            "is_user_edited": bool(is_user_edited),
            "reason": reason or "",
            "raw_body": raw_body or "",
            "amount": float(amount) if amount else 0.0,
        })

    print(f"[DB] Loaded {len(records)} transactions from {db_path}")
    return records


def build_training_data(db_records: list[dict]) -> list[tuple[str, str, float]]:
    """
    Returns list of (text, category, confidence_weight) tuples.

    Confidence weights:
      1.0  — User-corrected (ground truth)
      0.9  — Brand disambiguation (tier 5 exact brand match, e.g. "Merchant: Swiggy Food")
      0.85 — MCC code match (tier 1b — card network ground truth)
      0.80 — User custom rule match (tier 1)
      0.75 — Keyword match on merchant name (tier 7)
      0.65 — Keyword match on body (tier 8)
      0.50 — Keyword-based weak label applied retroactively here
      skip — OTHERS, ML-assigned only, TRANSFERS, SALARY_INCOME
    """
    training = []
    skipped_others = 0
    skipped_ml_only = 0
    skipped_transfer = 0
    augmented_from_keywords = 0

    for rec in db_records:
        cat = rec["category"]
        reason = rec["reason"].lower()
        merchant = normalize_merchant(rec["merchant"])
        raw_body = rec["raw_body"]

        # Build the input text (merchant name is the primary signal; body is secondary)
        text = merchant if merchant else ""
        if not text and raw_body:
            # No merchant extracted — try to use the first 80 chars of the body
            text = raw_body[:80]
        if not text:
            continue

        # Skip excluded categories
        if cat in EXCLUDE_CATEGORIES:
            skipped_transfer += 1
            continue

        # Ground truth: user manually corrected this transaction
        if rec["is_user_edited"]:
            training.append((text, cat, 1.0))
            continue

        # High-confidence classifier signals
        if "mcc " in reason:
            training.append((text, cat, 0.85))
            continue
        if "user custom rule" in reason:
            training.append((text, cat, 0.80))
            continue
        if "context:" in reason or "merchant: " in reason:
            # Brand disambiguation tier (tier 5) — very reliable
            if cat not in EXCLUDE_CATEGORIES:
                training.append((text, cat, 0.90))
            continue
        if "merchant keyword matched" in reason or "normalized merchant" in reason:
            training.append((text, cat, 0.75))
            continue
        if "body keyword matched" in reason:
            training.append((text, cat, 0.65))
            continue
        if "upi vpa handle matched" in reason:
            training.append((text, cat, 0.72))
            continue

        # ML-assigned or OTHERS — skip, these are the least reliable
        if cat == "OTHERS" or "ml model" in reason or "default fallback" in reason:
            skipped_others += 1
            skipped_ml_only += 1
            continue

        # Remaining: some other classification reason not covered above
        # Apply weak keyword labelling as a check
        kw_cat = keyword_label(text) or keyword_label(raw_body[:120] if raw_body else "")
        if kw_cat and kw_cat == cat:
            training.append((text, cat, 0.50))
        elif kw_cat and kw_cat != cat:
            # Conflict between stored category and keyword — skip to avoid noise
            pass
        else:
            # No keyword match — skip (too uncertain)
            pass

    # ── Synthetic samples from keyword labels ──────────────────────────────
    # Build a set of ~50 canonical merchant name samples per category from the
    # keyword list itself. This ensures every category has at least MIN_SAMPLES_PER_CLASS
    # training examples even if the live DB is sparse.
    for cat, keywords in KEYWORD_LABELS.items():
        if cat in EXCLUDE_CATEGORIES:
            continue
        for kw in keywords:
            # Only add single-word / short keywords as synthetic samples
            if len(kw) >= 4:
                training.append((kw, cat, 0.45))
                augmented_from_keywords += 1

    print(f"[DATA] Training samples: {len(training)}")
    print(f"[DATA]   Skipped (OTHERS/ML-only): {skipped_others}")
    print(f"[DATA]   Skipped (TRANSFERS/SALARY): {skipped_transfer}")
    print(f"[DATA]   Synthetic keyword samples: {augmented_from_keywords}")

    return training


# ─────────────────────────────────────────────────────────────────────────────
# Feature engineering — mirrors OnDeviceMerchantClassifier.kt exactly
# ─────────────────────────────────────────────────────────────────────────────

def clean_text(raw: str) -> str:
    """Mirror of OnDeviceMerchantClassifier.cleanText()."""
    s = raw.lower()
    s = re.sub(r'[^a-z0-9\s]', ' ', s)
    s = re.sub(r'\s+', ' ', s)
    return s.strip()


def build_vectorizer(texts: list[str]):
    """
    Builds a combined word n-gram + char n-gram TF-IDF vectorizer.
    Returns (vectorizer_word, vectorizer_char, X_combined).

    We keep them separate to mirror the on-device architecture which stores
    word_vocab/word_idf and char_vocab/char_idf as separate arrays.
    """
    cleaned = [clean_text(t) for t in texts]

    vec_word = TfidfVectorizer(
        analyzer='word',
        ngram_range=WORD_NGRAM_RANGE,
        max_features=WORD_MAX_FEATURES,
        sublinear_tf=True,       # matches (1 + ln(tf)) in the Kotlin code
        min_df=1,
        norm='l2'
    )
    vec_char = TfidfVectorizer(
        analyzer='char_wb',      # char_wb adds word-boundary padding — matches " token "
        ngram_range=CHAR_NGRAM_RANGE,
        max_features=CHAR_MAX_FEATURES,
        sublinear_tf=True,
        min_df=1,
        norm='l2'
    )

    import scipy.sparse as sp
    X_word = vec_word.fit_transform(cleaned)
    X_char = vec_char.fit_transform(cleaned)
    X = sp.hstack([X_word, X_char], format='csr')

    return vec_word, vec_char, X, cleaned


# ─────────────────────────────────────────────────────────────────────────────
# Training
# ─────────────────────────────────────────────────────────────────────────────

def train(texts: list[str], labels: list[str], weights: list[float]):
    """Train a multinomial logistic regression model and return it + vectorizers."""
    print(f"\n[TRAIN] Training on {len(texts)} samples across {len(set(labels))} categories...")

    # Class distribution
    counter = collections.Counter(labels)
    print("[TRAIN] Class distribution:")
    for cat in CATEGORIES:
        n = counter.get(cat, 0)
        bar = "█" * min(n // 5, 60)
        print(f"  {cat:<20} {n:5d}  {bar}")

    # Check minimum samples
    for cat in CATEGORIES:
        if cat in EXCLUDE_CATEGORIES:
            continue
        n = counter.get(cat, 0)
        if n < MIN_SAMPLES_PER_CLASS:
            print(f"[WARN] Category {cat} has only {n} samples (< {MIN_SAMPLES_PER_CLASS} minimum) — consider collecting more data")

    vec_word, vec_char, X, _ = build_vectorizer(texts)

    le = LabelEncoder()
    le.fit(CATEGORIES)
    y = le.transform(labels)

    # Compute class weights to handle imbalance
    present_classes = sorted(set(y))
    cw = compute_class_weight('balanced', classes=np.array(present_classes), y=y)
    class_weight_dict = {cls: w for cls, w in zip(present_classes, cw)}

    # Sample weights (combine user-correction confidence with class balance)
    sample_weights = np.array(weights)

    clf = LogisticRegression(
        C=LOGISTIC_C,
        max_iter=LOGISTIC_MAX_ITER,
        solver='lbfgs',
        class_weight=class_weight_dict,
    )
    clf.fit(X, y, sample_weight=sample_weights)

    # ── Cross-validation ──────────────────────────────────────────────────────
    # Only run CV if we have enough samples in every present class
    min_class_count = min(counter.get(c, 0) for c in le.classes_ if c not in EXCLUDE_CATEGORIES)
    if min_class_count >= CV_FOLDS:
        print(f"\n[EVAL] Running {CV_FOLDS}-fold cross-validation...")
        skf = StratifiedKFold(n_splits=CV_FOLDS, shuffle=True, random_state=42)
        cv_scores = cross_val_score(clf, X, y, cv=skf, scoring='f1_weighted')
        print(f"[EVAL] Cross-val F1 (weighted): {cv_scores.mean():.3f} ± {cv_scores.std():.3f}")
    else:
        print(f"[EVAL] Skipping CV — insufficient samples (min class has {min_class_count})")

    # Full training set report — use only the classes actually present in training data
    y_pred = clf.predict(X)
    present_class_indices = sorted(set(y))
    present_target_names = [le.inverse_transform([i])[0] for i in present_class_indices]
    print("\n[EVAL] Training-set classification report:")
    print(classification_report(y, y_pred, labels=present_class_indices,
                                target_names=present_target_names, zero_division=0))

    return clf, vec_word, vec_char, le


# ─────────────────────────────────────────────────────────────────────────────
# Serialisation — exact schema expected by OnDeviceMerchantClassifier.kt
# ─────────────────────────────────────────────────────────────────────────────

def serialise_model(clf, vec_word, vec_char, label_encoder, output_path: str):
    """
    Writes the model to JSON in the schema expected by OnDeviceMerchantClassifier.kt:
    {
      "categories":  [str, ...]            # ordered class labels
      "word_vocab":  {token: index, ...}
      "word_idf":    [float, ...]
      "char_vocab":  {ngram: index, ...}
      "char_idf":    [float, ...]
      "weights":     [[float, ...], ...]   # shape: n_classes × n_features
      "intercept":   [float, ...]          # length n_classes
    }

    Feature layout: word features [0..W-1], char features [W..W+C-1]
    This matches the Kotlin extractFeatures() method which offsets char indices by wordVocab.size.
    """
    print(f"\n[SAVE] Serialising model to {output_path}")

    W = len(vec_word.vocabulary_)
    C = len(vec_char.vocabulary_)
    print(f"[SAVE] Word vocab: {W} features, Char vocab: {C} features, Total: {W+C}")

    # Word vocab: token → index (0-indexed within word space)
    word_vocab = {token: int(idx) for token, idx in vec_word.vocabulary_.items()}
    word_idf = vec_word.idf_.tolist()

    # Char vocab: ngram → index (0-indexed within char space; Kotlin adds W as offset)
    char_vocab = {ngram: int(idx) for ngram, idx in vec_char.vocabulary_.items()}
    char_idf = vec_char.idf_.tolist()

    # The LR coef_ shape is (n_classes, n_features_combined).
    # We need to ensure classes are ordered exactly as CATEGORIES.
    # label_encoder.classes_ gives the alphabetical order from fit() — remap to CATEGORIES order.
    coef = clf.coef_    # shape: (n_classes_present, W+C)
    intercept = clf.intercept_   # shape: (n_classes_present,)

    n_out = len(CATEGORIES)
    W_out = min(W, WORD_MAX_FEATURES)
    C_out = min(C, CHAR_MAX_FEATURES)
    total_features = W_out + C_out

    # Build full weight matrix in CATEGORIES order, zero-fill absent classes
    # clf.classes_ gives the actual class indices present during training (in le-encoded space)
    # We must map each CATEGORY back through label_encoder to find its clf row index.
    trained_classes_list = list(clf.classes_)  # encoded int indices present in training

    weights_out = []
    intercept_out = []
    for cat in CATEGORIES:
        if cat in label_encoder.classes_:
            encoded_idx = int(label_encoder.transform([cat])[0])
            if encoded_idx in trained_classes_list:
                clf_row = trained_classes_list.index(encoded_idx)
                row = coef[clf_row].tolist()
            else:
                # Category was in le but had no training samples
                row = [0.0] * total_features
                intercept_val = 0.0
                weights_out.append(row)
                intercept_out.append(intercept_val)
                continue
        else:
            row = [0.0] * total_features
            weights_out.append(row)
            intercept_out.append(0.0)
            continue

        # Pad or truncate to exact feature count
        if len(row) < total_features:
            row = row + [0.0] * (total_features - len(row))
        else:
            row = row[:total_features]
        weights_out.append(row)

        if cat in label_encoder.classes_ and encoded_idx in trained_classes_list:
            clf_row = trained_classes_list.index(encoded_idx)
            intercept_out.append(float(intercept[clf_row]))
        else:
            intercept_out.append(0.0)

    model_json = {
        "categories": CATEGORIES,
        "word_vocab": word_vocab,
        "word_idf": word_idf[:W_out],
        "char_vocab": char_vocab,
        "char_idf": char_idf[:C_out],
        "weights": weights_out,
        "intercept": intercept_out,
    }

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(model_json, f, ensure_ascii=False, separators=(',', ':'))

    size_kb = os.path.getsize(output_path) / 1024
    print(f"[SAVE] Written {output_path} ({size_kb:.1f} KB)")
    print(f"[SAVE] Model: {n_out} classes × {total_features} features ({W_out} word + {C_out} char)")


# ─────────────────────────────────────────────────────────────────────────────
# CSV export (for manual inspection)
# ─────────────────────────────────────────────────────────────────────────────

def export_csv(training_data: list[tuple[str, str, float]], path: str):
    import csv
    with open(path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(["text", "category", "confidence_weight"])
        for text, cat, weight in sorted(training_data, key=lambda x: x[1]):
            writer.writerow([text, cat, f"{weight:.2f}"])
    print(f"[CSV] Exported {len(training_data)} samples to {path}")


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="LQD Merchant Classifier Retraining Script")
    parser.add_argument("--db", default=str(DEFAULT_DB),
                        help=f"Path to expense_manager.db (default: {DEFAULT_DB})")
    parser.add_argument("--output", default=str(OUTPUT_JSON),
                        help=f"Output JSON path (default: {OUTPUT_JSON})")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print stats and eval but don't write the model")
    parser.add_argument("--export-csv", metavar="PATH",
                        help="Export training data to CSV for inspection")
    parser.add_argument("--no-db", action="store_true",
                        help="Skip DB loading — train only on keyword synthetic samples")
    args = parser.parse_args()

    print("=" * 60)
    print("  LQD Merchant Classifier — Retraining Pipeline")
    print("=" * 60)

    # ── Load data ─────────────────────────────────────────────────────────────
    db_records = [] if args.no_db else load_from_db(args.db)
    training_data = build_training_data(db_records)

    if args.export_csv:
        export_csv(training_data, args.export_csv)

    if len(training_data) < 50:
        print(f"[ERROR] Insufficient training data ({len(training_data)} samples). "
              "Run with --no-db first to verify keyword baseline, then connect the DB.")
        sys.exit(1)

    # ── Split into texts / labels / weights ───────────────────────────────────
    texts = [t for t, _, _ in training_data]
    labels = [l for _, l, _ in training_data]
    weights = [w for _, _, w in training_data]

    # ── Train ─────────────────────────────────────────────────────────────────
    clf, vec_word, vec_char, le = train(texts, labels, weights)

    # ── Quick sanity-check predictions ───────────────────────────────────────
    print("\n[CHECK] Sanity-check predictions on known merchants:")
    test_cases = [
        ("Swiggy",              "FOOD"),
        ("Zomato",              "FOOD"),
        ("Blinkit",             "GROCERIES"),
        ("BigBasket",           "GROCERIES"),
        ("Amazon",              "SHOPPING"),
        ("Uber",                "TRANSPORT"),
        ("IRCTC",               "TRANSPORT"),
        ("Netflix",             "ENTERTAINMENT"),
        ("Apollo Pharmacy",     "HEALTHCARE"),
        ("Groww",               "INVESTMENT"),
        ("Zerodha",             "INVESTMENT"),
        ("BESCOM",              "BILLS_UTILITIES"),
        ("Airtel",              "BILLS_UTILITIES"),
        ("Clove Dental",        "HEALTHCARE"),
        ("Udemy",               "EDUCATION"),
        ("CultFit",             "PERSONAL"),
        ("FNP",                 "PERSONAL"),
        ("ECOM*SWIGGY38291",    "FOOD"),   # Normalizer test
        ("PHP*HALDIRAMS_NOIDA", "FOOD"),   # POS prefix test
    ]

    import scipy.sparse as sp

    all_passed = True
    for merchant, expected in test_cases:
        clean = clean_text(normalize_merchant(merchant))
        X_test = sp.hstack([
            vec_word.transform([clean]),
            vec_char.transform([clean])
        ], format='csr')
        pred_idx = clf.predict(X_test)[0]
        pred_cat = le.inverse_transform([pred_idx])[0]
        proba = clf.predict_proba(X_test)[0].max()
        status = "✓" if pred_cat == expected else "✗"
        if pred_cat != expected:
            all_passed = False
        print(f"  {status} {merchant:<25} → {pred_cat:<20} (conf: {proba:.2f}, expected: {expected})")

    if all_passed:
        print("\n[CHECK] All sanity checks passed ✓")
    else:
        print("\n[CHECK] Some sanity checks failed — review training data and thresholds")

    # ── Save ──────────────────────────────────────────────────────────────────
    if args.dry_run:
        print("\n[DRY-RUN] Skipping model write (--dry-run flag set)")
    else:
        serialise_model(clf, vec_word, vec_char, le, args.output)
        print("\n[DONE] Model saved. Rebuild the Android app to pick up the new weights.")
        print(f"       File: {args.output}")


if __name__ == "__main__":
    main()
