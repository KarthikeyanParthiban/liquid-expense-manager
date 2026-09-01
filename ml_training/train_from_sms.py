#!/usr/bin/env python3
"""
train_from_sms.py — Pull SMS from device, build training data, retrain model.

Reads the raw ADB content dump from raw_sms.txt, parses it into (sender, body, date)
triples, applies the keyword-based weak labeller to filter financial SMS and assign
category labels, then trains the TF-IDF + logistic regression classifier and writes
the new model JSON as a drop-in replacement for OnDeviceMerchantClassifier.

Usage:
  python3 ml_training/train_from_sms.py
  python3 ml_training/train_from_sms.py --sms ml_training/raw_sms.txt
  python3 ml_training/train_from_sms.py --dry-run   # show stats, no write
  python3 ml_training/train_from_sms.py --export-csv ml_training/training_data.csv
"""

import argparse
import collections
import json
import math
import os
import re
import sys
import warnings
warnings.filterwarnings("ignore")
from pathlib import Path

try:
    import numpy as np
    from sklearn.linear_model import LogisticRegression
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.model_selection import StratifiedKFold, cross_val_score
    from sklearn.metrics import classification_report
    from sklearn.utils.class_weight import compute_class_weight
    from sklearn.preprocessing import LabelEncoder
    import scipy.sparse as sp
except ImportError as e:
    print(f"[ERROR] {e}\nInstall with: pip install scikit-learn numpy scipy")
    sys.exit(1)

PROJECT_ROOT  = Path(__file__).resolve().parent.parent
SMS_DUMP      = PROJECT_ROOT / "ml_training" / "raw_sms.txt"
OUTPUT_JSON   = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "models" / "merchant_classifier_weights.json"

CATEGORIES = [
    "FOOD", "GROCERIES", "SHOPPING", "TRANSPORT", "BILLS_UTILITIES",
    "ENTERTAINMENT", "HEALTHCARE", "INVESTMENT", "SALARY_INCOME",
    "TRANSFERS", "FEES_CHARGES", "EDUCATION", "PERSONAL", "OTHERS"
]
SKIP_CATEGORIES = {"TRANSFERS", "SALARY_INCOME", "OTHERS"}

WORD_MAX   = 3000
CHAR_MAX   = 4000
LR_C       = 2.0
MIN_CONF   = 0.60   # minimum keyword-match confidence to include sample

# Per-category synthetic sample floor — lower values let real SMS data dominate
# for well-represented categories, higher values anchor thin classes
SYNTHETIC_MIN = {
    "FOOD": 40, "GROCERIES": 30, "SHOPPING": 30, "TRANSPORT": 35,
    "BILLS_UTILITIES": 35, "ENTERTAINMENT": 30, "HEALTHCARE": 30,
    "INVESTMENT": 30, "FEES_CHARGES": 25, "EDUCATION": 25, "PERSONAL": 25,
}

# ─────────────────────────────────────────────────────────────────────────────
# SMS dump parser
# Each "Row: N " block in ADB content output may span multiple lines (body has \n).
# Format: "Row: N address=..., body=..., date=..."
# ─────────────────────────────────────────────────────────────────────────────

_ROW_START = re.compile(r'^Row:\s*\d+\s+address=', re.MULTILINE)

def parse_sms_dump(path: str) -> list[dict]:
    """Parse the raw ADB content dump into list of {address, body, date} dicts."""
    with open(path, encoding='utf-8', errors='replace') as f:
        raw = f.read()

    # Split on row boundaries
    positions = [m.start() for m in _ROW_START.finditer(raw)]
    positions.append(len(raw))

    records = []
    for i in range(len(positions) - 1):
        chunk = raw[positions[i]:positions[i+1]].strip()

        # Extract address (always on first line after "Row: N ")
        addr_m = re.search(r'address=([^,]+),', chunk)
        address = addr_m.group(1).strip() if addr_m else ""

        # Extract date (at end, format: ", date=DIGITS")
        date_m = re.search(r',\s*date=(\d+)\s*$', chunk, re.MULTILINE)
        date_ms = int(date_m.group(1)) if date_m else 0

        # Body is everything between "body=" and ", date="
        body_m = re.search(r'body=(.*?),\s*date=\d+\s*$', chunk, re.DOTALL)
        body = body_m.group(1).strip() if body_m else ""

        if body:
            records.append({"address": address, "body": body, "date": date_ms})

    print(f"[SMS] Parsed {len(records)} messages from {path}")
    return records

# ─────────────────────────────────────────────────────────────────────────────
# Financial SMS filter — mirrors SmsClassifier.kt logic in Python
# ─────────────────────────────────────────────────────────────────────────────

# Reject patterns (mirrors SmsClassifier.kt)
_REJECT = [
    re.compile(p, re.IGNORECASE) for p in [
        r'\bwill\s+be\s+(?:auto-?debited|debited|deducted|credited|refunded)\b',
        r'\b(?:is\s+due\s+on|payment\s+is\s+due|statement\s+generated|bill\s+generated|minimum\s+amount\s+due|total\s+amount\s+due)\b',
        r'\b(?:bill\s+amount|amount\s+payable|pay\s+before\s+due\s+date|amount\s+due|min\s*due)\b',
        r'\b(?:pre-?approved|instant\s+loan|loan\s+offer|personal\s+loan|home\s+loan|apply\s+now)\b',
        r'\b(?:loan\s+against|loan.*approved|sanctioned)\b',
        r'\b(?:%\s*off|biggest\s+deals|use\s+coupon|use\s+promo|gift\s+voucher|congratulations|you\s+have\s+won)\b',
        r'\b(?:traded\s+value|bse\s+trade|nse\s+trade|fno\s+value|cdsl:|pledge\s+accepted)\b',
        r'\b(?:mandate\s+created|mandate\s+registration|autopay\s+activation|has\s+requested\s+money)\b',
        r'\b(?:unsuccessful|payment\s+declined|txn\s+failed|insufficient\s+funds)\b',
        r'\b(?:data\s+quota|data\s+used|validity\s+expir|recharge\s+credited\s+to\s+your|talktime\s+credited)\b',
        r'\b(?:otp|one\s*time\s*password|verification\s*code)\b',
        r'\b(?:passbook\s+balance|member\s+passbook|uan\b|epfo)\b',
    ]
]

_DEBIT  = re.compile(r'\b(?:debited|debit|spent|paid|purchase|deducted|sent|charged?|withdrawn|wdl)\b', re.IGNORECASE)
_CREDIT = re.compile(r'\b(?:credited|received|deposited|refund|cashback|reversal)\b', re.IGNORECASE)
_MONEY  = re.compile(r'(?:rs\.?|inr|₹|\$)', re.IGNORECASE)

def is_financial(body: str) -> bool:
    b = body
    if any(p.search(b) for p in _REJECT):
        return False
    # neutralise "credit card" before testing credit keyword
    clean = re.sub(r'\bcredit\s+(?:card|limit|score|line|bureau)\b', 'card_token', b, flags=re.IGNORECASE)
    return bool(_MONEY.search(b)) and bool(_DEBIT.search(b) or _CREDIT.search(clean))

# ─────────────────────────────────────────────────────────────────────────────
# Merchant extractor — simplified mirror of SmsParser.extractMerchant()
# ─────────────────────────────────────────────────────────────────────────────

_MERCHANT_PATS = [
    re.compile(r'(?:from\s+VPA|to\s+VPA|VPA)\s+([A-Za-z0-9.\-_]+@[A-Za-z0-9.\-_]+)', re.IGNORECASE),
    re.compile(r'(?:PHP\*|PG\*|POS\*|ECOM\*|BIL\*|IN\*)\s*([A-Za-z0-9\s&.\-_]+?)(?:\s+Avl|\s+Lmt|\n|$)', re.IGNORECASE),
    re.compile(r'(?:paid\s+to|sent\s+to|spent\s+at|at)\s+([A-Za-z][A-Za-z0-9\s&.\-_]{2,30}?)(?:\s+on|\s+ref|\s+avl|\n|$)', re.IGNORECASE),
    re.compile(r'towards\s+([A-Za-z][A-Za-z0-9\s&.\-_]{2,25}?)(?:\s+on|\s+ref|\s+vide|\n|$)', re.IGNORECASE),
]

_NOISE_WORDS = {"account","acct","bank","bal","balance","card","rs","inr","otp","ref","txn","avl","available","limit","lmt"}

def extract_merchant(body: str) -> str:
    body_clean = re.sub(r'(?:SMS\s+BLKCC|Not\s+You\?|Call\s+1800).*$', '', body, flags=re.IGNORECASE|re.DOTALL).strip()
    for pat in _MERCHANT_PATS:
        m = pat.search(body_clean)
        if m:
            raw = m.group(1).strip()
            # clean
            raw = re.sub(r'(?i)^(?:vpa|info:|towards|to|at|@)\s*', '', raw)
            raw = re.sub(r'[^\w\s&.\-@]', ' ', raw).strip()
            words = [w for w in raw.split() if w.lower() not in _NOISE_WORDS]
            cleaned = ' '.join(words)[:40]
            if cleaned and len(cleaned) >= 2 and not cleaned.isdigit():
                return cleaned
    return ""

# ─────────────────────────────────────────────────────────────────────────────
# MerchantNormalizer (mirrors MerchantNormalizer.kt)
# ─────────────────────────────────────────────────────────────────────────────

_GW_PREFIX    = re.compile(r'^(?:razorpay|payu|billdesk|ccavenue|cashfree|[A-Z]{2,6})\s*[*_\-|/]\s*', re.IGNORECASE)
_POS_PREFIX   = re.compile(r'^(?:PHP|PG|POS|ECOM|BIL|IN|SQ|NP)\s*[*_\-]\s*', re.IGNORECASE)
_TRAIL_NOISE  = re.compile(r'[_\-\s/]*(?:ORDER|TXN|BILL|PAY|SERVICES?|SVCS?|TECH|DIGITAL|ONLINE|INDIA|PVT|LTD)\s*[_\-\s/]*[A-Z0-9]{3,}$', re.IGNORECASE)
_NUM_SUFFIX   = re.compile(r'[0-9]{4,}$')

def normalize(raw: str) -> str:
    if not raw:
        return ""
    s = raw.strip()
    s = _GW_PREFIX.sub("", s)
    s = _POS_PREFIX.sub("", s)
    s = _TRAIL_NOISE.sub("", s).strip()
    s = _NUM_SUFFIX.sub("", s).strip()
    s = re.sub(r'[_\-|]+', ' ', s).strip()
    s = re.sub(r'\s{2,}', ' ', s).strip()
    return s

# ─────────────────────────────────────────────────────────────────────────────
# Amount extractor
# ─────────────────────────────────────────────────────────────────────────────

_AMT_PAT = re.compile(r'(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)', re.IGNORECASE)

def extract_amount(body: str) -> float:
    m = _AMT_PAT.search(body)
    if m:
        try:
            return float(m.group(1).replace(',', ''))
        except ValueError:
            pass
    return 0.0

# ─────────────────────────────────────────────────────────────────────────────
# Keyword labeller — comprehensive India-focused map
# ─────────────────────────────────────────────────────────────────────────────

KEYWORD_MAP = {
    "FOOD": [
        "sharma sweet house", "sweet house", "sweets house", "amma canteen",
        "amma canteen meals", "theobroma patisserie", "patisserie bakery",
        "swiggy", "swiggy food", "swiggy delivery", "swiggy order",
        "zomato", "zomato food", "dominos", "domino", "mcdonalds", "kfc", "starbucks",
        "burger king", "pizza hut", "subway", "faasos", "box8", "behrouz",
        "wow momo", "dunkin", "chaayos", "chai point", "costa coffee", "barista",
        "theobroma", "havmor", "barbeque nation", "haldiram", "bikanervala",
        "saravana bhavan", "saravana", "thalappakatti", "paradise biryani",
        "bawarchi", "pista house", "biryani by kilo", "mad over donuts",
        "sweet house", "sweets house", "sweet shop", "sweet stall",
        "naturals ice cream", "baskin robbins", "belgian waffle", "keventers",
        "farzi cafe", "mainland china", "hard rock cafe", "smoke house deli",
        "empire restaurant", "meghana biryani", "nagarjuna", "anjappar",
        "cafe coffee day", "ccd", "blue tokai", "third wave coffee",
        "restaurant", "dining", "food", "bakery", "cafe", "canteen", "tiffin",
        "kitchen", "dhaba", "bistro", "dessert", "snacks", "pizza", "burger",
        "momos", "brewery", "pub", "biryani", "waffle", "pastry",
        "eazydiner", "dineout", "magicpin", "sweets", "mithai",
    ],
    "GROCERIES": [
        "zepto", "blinkit", "instamart", "bigbasket", "bb daily", "dmart",
        "jiomart", "milkbasket", "country delight", "akshayakalpa", "grofers",
        "licious", "freshtohome", "meatigo", "tendercuts",
        "reliance smart", "spencers", "more supermarket", "spar", "star bazaar",
        "supermarket", "hypermarket", "kirana", "grocery", "groceries",
        "dairy", "milk", "vegetables", "fruits", "organic", "mandi",
        "fresh mart", "daily mart",
    ],
    "SHOPPING": [
        "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho",
        "snapdeal", "purplle", "firstcry", "tata cliq", "tata neu",
        "zara", "h&m", "shoppers stop", "lifestyle", "pantaloons",
        "max fashion", "zudio", "fabindia", "decathlon", "bata", "woodland",
        "metro shoes", "puma", "nike", "adidas", "skechers", "reebok",
        "croma", "reliance digital", "vijay sales", "poorvika", "samsung",
        "lenskart", "tanishq", "caratlane", "malabar gold", "kalyan jewellers",
        "joyalukkas", "senco gold", "giva", "ikea", "pepperfry", "urban ladder",
        "wakefit", "hamleys", "crossword", "olx", "jewellers", "jewellery",
        "diamond", "silver",
    ],
    "TRANSPORT": [
        "uber", "ola", "rapido", "blusmart", "namma yatri", "indrive",
        "irctc", "indian railways", "railyatri", "confirmtkt",
        "metro", "dmrc", "bmtc", "ksrtc", "msrtc", "tsrtc", "apsrtc",
        "redbus", "abhibus", "zingbus",
        "makemytrip", "mmt", "goibibo", "cleartrip", "yatra", "easemytrip", "ixigo",
        "booking.com", "agoda", "airbnb", "oyo", "treebo", "fabhotels",
        "indigo", "air india", "vistara", "spicejet", "akasa air",
        "flight", "airline", "airfare", "airport",
        "fastag", "nhai", "toll", "parking",
        "indian oil", "iocl", "bharat petroleum", "bpcl", "hindustan petroleum",
        "hpcl", "shell", "nayara", "jio-bp", "petrol", "diesel", "cng", "fuel",
        "statiq", "ather", "chargezone", "ola electric", "taxi", "cab", "auto",
        "cabs", "cabs travels", "travels", "petrol bunk", "petrol pump",
        "ev charging", "ev charging station", "charging station", "fuel station",
        "ather ev", "ather grid",
    ],
    "BILLS_UTILITIES": [
        "airtel", "reliance jio", "vodafone", "vi mobile", "bsnl", "mtnl",
        "tata play", "tata sky", "dish tv", "sun direct", "d2h",
        "act fibernet", "hathway", "jiofiber", "excitel", "broadband",
        "bescom", "tata power", "adani electricity", "msedcl", "torrent power",
        "tneb", "tangedco", "bses rajdhani", "bses yamuna", "cesc", "kseb",
        "electricity", "mahanagar gas", "igl", "indane", "bharat gas", "lpg",
        "nobroker", "nobrokerhood", "mygate", "apnacomplex",
        "maintenance", "rent", "house rent",
        "recharge", "postpaid", "prepaid", "dth",
        "google cloud", "github", "openai", "deepseek", "chatgpt", "cursor",
        "bbps", "billdesk",
    ],
    "ENTERTAINMENT": [
        "netflix", "spotify", "amazon prime", "prime video", "disney",
        "hotstar", "jiohotstar", "jiocinema", "apple tv", "apple music",
        "sonyliv", "zee5", "voot", "aha", "sun nxt", "discovery+",
        "youtube premium", "youtube music", "google play",
        "audible", "kuku fm", "pocket fm",
        "bookmyshow", "pvr", "inox", "pvr cinemas", "cinepolis", "cinema", "cinemas",
        "movie", "movies", "movie ticket", "theatre", "theater",
        "steam", "playstation", "xbox", "gaming", "bgmi", "krafton", "nintendo",
        "smaaash", "imagicaa", "wonderla", "timezone", "bowling", "arcade",
        "bowling arcade", "amusement", "game zone", "gaming arcade",
    ],
    "HEALTHCARE": [
        "apollo pharmacy", "apollo 24/7", "1mg", "tata 1mg", "pharmeasy",
        "medplus", "netmeds", "practo", "truemeds", "wellness forever",
        "dr lal", "lal pathlabs", "srl diagnostics", "metropolis", "thyrocare",
        "redcliffe", "healthians", "agilus",
        "apollo hospital", "fortis", "manipal", "narayana health",
        "cloudnine", "motherhood", "medanta", "kokilaben", "kims",
        "clove dental", "mydentist",
        "hospital", "clinic", "pharmacy", "chemist", "medical", "medicine",
        "doctor", "diagnostic", "nursing home", "pharma", "dental",
    ],
    "INVESTMENT": [
        "groww", "zerodha", "upstox", "angel one", "5paisa", "kuvera",
        "smallcase", "scripbox", "et money", "paytm money", "dezerv",
        "wint wealth", "grip invest", "goldenpi", "vested", "coindcx", "wazirx",
        "camsonline", "cams", "kfintech", "mutual fund", "sip",
        "lic", "lic premium", "lic policy", "life insurance corporation",
        "hdfc life", "icici prudential life", "max life", "sbi life",
        "bajaj allianz", "tata aia", "star health", "care health",
        "niva bupa", "hdfc ergo", "icici lombard", "digit insurance", "acko",
        "policybazaar", "insurance", "insurance premium", "policy premium",
        "gold bond", "sgb", "stock", "equity", "demat", "nps", "ppf",
    ],
    "FEES_CHARGES": [
        "annual fee", "late fee", "penalty", "forex markup", "gst on charges",
        "interest charge", "amc", "annual maintenance", "processing fee",
        "service charge", "bounce charges", "overlimit fee", "penal charges",
        "debit card fee", "minimum balance", "non maintenance", "surcharge",
        "bank charge", "transaction fee", "card fee", "renewal fee",
    ],
    "EDUCATION": [
        "coursera", "udemy", "edureka", "simplilearn", "upgrad", "scaler",
        "unacademy", "byju", "vedantu", "physicswallah", "allen", "aakash",
        "fiitjee", "duolingo", "british council", "linkedin learning",
        "school fees", "college fees", "university fees", "tuition", "tuition fees",
        "semester fees", "semester tuition", "exam fee", "ielts", "toefl", "gre", "gmat",
        "school", "college", "university", "vidyalaya", "kendriya vidyalaya",
        "coaching", "coaching center", "coaching centre", "neet", "jee",
        "academy", "institute", "education", "student fees", "admission fees",
    ],
    "PERSONAL": [
        "cultfit", "cult.fit", "gold's gym", "anytime fitness",
        "snap fitness", "talwalkars", "gym", "yoga", "pilates", "zumba", "crossfit",
        "urban company", "urbanclap", "naturals salon", "lakme salon",
        "toni & guy", "jawed habib", "enrich salon", "bblunt",
        "vlcc", "kaya skin", "bodycraft", "truefitt",
        "salon", "spa", "parlour", "barber", "haircut", "grooming", "massage",
        "fnp", "ferns n petals", "fern n petals", "floweraura", "interflora",
        "florist", "flowers", "fitness class", "dermatologist",
    ],
}

# Sort keywords by length descending for greedy matching
_KW_INDEX: list[tuple[str, str]] = sorted(
    [(kw.lower(), cat) for cat, kws in KEYWORD_MAP.items() for kw in kws],
    key=lambda x: len(x[0]),
    reverse=True
)

def keyword_label(text: str) -> tuple[str | None, float]:
    """Return (category, confidence) or (None, 0.0)."""
    t = text.lower()
    for kw, cat in _KW_INDEX:
        try:
            if re.search(r'\b' + re.escape(kw) + r'\b', t):
                # Confidence: longer keyword = more specific = higher confidence
                conf = min(0.55 + len(kw) * 0.008, 0.95)
                return cat, conf
        except re.error:
            pass
    return None, 0.0

# ─────────────────────────────────────────────────────────────────────────────
# Build training data from SMS records
# ─────────────────────────────────────────────────────────────────────────────

def build_training(records: list[dict]) -> list[tuple[str, str, float]]:
    training = []
    stats = collections.Counter()

    for rec in records:
        body   = rec["body"]
        sender = rec["address"]

        # Filter non-financial
        if not is_financial(body):
            stats["non_financial"] += 1
            continue

        amount   = extract_amount(body)
        merchant = normalize(extract_merchant(body))

        # Try to label by merchant first, then full body
        text_for_label = (merchant + " " + body[:120]).strip() if merchant else body[:120]
        cat, conf = keyword_label(text_for_label)

        if cat is None or conf < MIN_CONF:
            stats["no_label"] += 1
            continue

        if cat in SKIP_CATEGORIES:
            stats["skipped_cat"] += 1
            continue

        # Input text for the model: normalized merchant name if available, else body prefix
        # The on-device model is primarily called with merchant names
        input_text = merchant if len(merchant) >= 3 else body[:80]
        if not input_text.strip():
            stats["empty_text"] += 1
            continue

        training.append((input_text, cat, conf))
        stats[f"cat_{cat}"] += 1

    total_financial = sum(1 for r in records if is_financial(r["body"]))
    print(f"\n[DATA] SMS breakdown:")
    print(f"  Total messages    : {len(records)}")
    print(f"  Financial SMS     : {total_financial}")
    print(f"  Non-financial     : {stats['non_financial']}")
    print(f"  No keyword label  : {stats['no_label']}")
    print(f"  Skipped (transfer): {stats['skipped_cat']}")
    print(f"  Training samples  : {len(training)}")
    print(f"\n[DATA] Category breakdown:")
    for cat in CATEGORIES:
        n = stats.get(f"cat_{cat}", 0)
        bar = "█" * min(n // 3, 60)
        print(f"  {cat:<22} {n:4d}  {bar}")

    return training

# ─────────────────────────────────────────────────────────────────────────────
# Also add keyword-synthetic samples for thin classes
# ─────────────────────────────────────────────────────────────────────────────

def add_synthetic(training: list, min_per_class=30):
    """
    For categories with fewer than min_per_class samples, inject the keyword
    strings themselves as synthetic training examples (weight 0.40).
    Use per-category floor from SYNTHETIC_MIN if available.
    Real SMS samples are weighted higher so they dominate when abundant.
    """
    counter = collections.Counter(cat for _, cat, _ in training)
    added = 0
    for cat, kws in KEYWORD_MAP.items():
        if cat in SKIP_CATEGORIES:
            continue
        floor = SYNTHETIC_MIN.get(cat, min_per_class)
        deficit = max(0, floor - counter.get(cat, 0))
        if deficit == 0:
            continue
        # Add all keywords for this category regardless of deficit
        # so char n-grams are well-anchored even when real samples are few.
        # Multi-word keywords are added with extra weight + duplicated so the model
        # learns the compound phrasing (e.g. "sharma sweet house") strongly enough
        # to clear the ML confidence threshold.
        for kw in kws:
            if len(kw) >= 4:
                is_compound = " " in kw
                weight = 0.60 if is_compound else 0.45
                training.append((kw, cat, weight))
                added += 1
                if is_compound:
                    # duplicate compound anchors to reinforce phrase-level char n-grams
                    training.append((kw, cat, weight))
                    added += 1
    if added:
        print(f"[DATA] Added {added} synthetic keyword samples to anchor thin classes")
    return training

# ─────────────────────────────────────────────────────────────────────────────
# Vectoriser + training
# ─────────────────────────────────────────────────────────────────────────────

def clean(text: str) -> str:
    t = text.lower()
    t = re.sub(r'[^a-z0-9\s]', ' ', t)
    t = re.sub(r'\s+', ' ', t)
    return t.strip()

def train_model(training: list[tuple[str, str, float]]):
    texts   = [t for t, _, _ in training]
    labels  = [l for _, l, _ in training]
    weights = np.array([w for _, _, w in training])

    print(f"\n[TRAIN] Fitting vectorisers on {len(texts)} samples...")

    vec_w = TfidfVectorizer(
        analyzer='word', ngram_range=(1, 2),
        max_features=WORD_MAX, sublinear_tf=True, min_df=1, norm='l2'
    )
    vec_c = TfidfVectorizer(
        analyzer='char_wb', ngram_range=(3, 4),
        max_features=CHAR_MAX, sublinear_tf=True, min_df=1, norm='l2'
    )
    cleaned = [clean(t) for t in texts]
    X = sp.hstack([vec_w.fit_transform(cleaned), vec_c.fit_transform(cleaned)], format='csr')

    le = LabelEncoder()
    le.fit(CATEGORIES)
    y = le.transform(labels)

    present = sorted(set(y))
    cw_vals = compute_class_weight('balanced', classes=np.array(present), y=y)
    cw_dict = dict(zip(present, cw_vals))

    print(f"[TRAIN] Feature matrix: {X.shape[0]} × {X.shape[1]}")
    print(f"[TRAIN] Training logistic regression (C={LR_C})...")

    clf = LogisticRegression(C=LR_C, max_iter=3000, solver='lbfgs',
                             class_weight=cw_dict)
    clf.fit(X, y, sample_weight=weights)

    # Cross-validation
    present_labels = sorted(set(labels))
    min_count = min(collections.Counter(labels).values())
    n_folds = min(5, min_count)
    if n_folds >= 2:
        print(f"[EVAL] {n_folds}-fold cross-validation...")
        skf = StratifiedKFold(n_splits=n_folds, shuffle=True, random_state=42)
        scores = cross_val_score(clf, X, y, cv=skf, scoring='f1_weighted')
        print(f"[EVAL] Weighted F1: {scores.mean():.3f} ± {scores.std():.3f}")

    # Training report
    y_pred = clf.predict(X)
    present_idx = sorted(set(y))
    present_names = [le.inverse_transform([i])[0] for i in present_idx]
    print("\n[EVAL] Training-set classification report:")
    print(classification_report(y, y_pred, labels=present_idx,
                                target_names=present_names, zero_division=0))

    return clf, vec_w, vec_c, le

# ─────────────────────────────────────────────────────────────────────────────
# Serialiser — exact schema for OnDeviceMerchantClassifier.kt
# ─────────────────────────────────────────────────────────────────────────────

def save_model(clf, vec_w, vec_c, le, output_path: str):
    print(f"\n[SAVE] Writing model to {output_path} ...")

    W = len(vec_w.vocabulary_)
    C = len(vec_c.vocabulary_)
    total = W + C

    coef      = clf.coef_
    intercept = clf.intercept_
    trained   = list(clf.classes_)  # encoded ints of classes present in training

    weights_out   = []
    intercept_out = []

    for cat in CATEGORIES:
        if cat in le.classes_:
            enc = int(le.transform([cat])[0])
            if enc in trained:
                row_idx = trained.index(enc)
                row = coef[row_idx].tolist()
                row = (row + [0.0] * total)[:total]
                weights_out.append(row)
                intercept_out.append(float(intercept[row_idx]))
                continue
        weights_out.append([0.0] * total)
        intercept_out.append(0.0)

    model = {
        "categories": CATEGORIES,
        "word_vocab":  {t: int(i) for t, i in vec_w.vocabulary_.items()},
        "word_idf":    vec_w.idf_.tolist(),
        "char_vocab":  {t: int(i) for t, i in vec_c.vocabulary_.items()},
        "char_idf":    vec_c.idf_.tolist(),
        "weights":     weights_out,
        "intercept":   intercept_out,
    }

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(model, f, ensure_ascii=False, separators=(',', ':'))

    size_kb = os.path.getsize(output_path) / 1024
    print(f"[SAVE] Done — {size_kb:.0f} KB, {len(CATEGORIES)} classes × {total} features ({W}w + {C}c)")

# ─────────────────────────────────────────────────────────────────────────────
# Sanity check
# ─────────────────────────────────────────────────────────────────────────────

SANITY = [
    ("Swiggy",                  "FOOD"),
    ("Zomato",                  "FOOD"),
    ("Blinkit",                 "GROCERIES"),
    ("BigBasket",               "GROCERIES"),
    ("Amazon",                  "SHOPPING"),
    ("Uber",                    "TRANSPORT"),
    ("IRCTC",                   "TRANSPORT"),
    ("Indian Oil Petrol Pump",  "TRANSPORT"),
    ("Netflix",                 "ENTERTAINMENT"),
    ("BookMyShow",              "ENTERTAINMENT"),
    ("Apollo Pharmacy",         "HEALTHCARE"),
    ("Lal PathLabs",            "HEALTHCARE"),
    ("Groww",                   "INVESTMENT"),
    ("LIC Premium",             "INVESTMENT"),
    ("Airtel Postpaid",         "BILLS_UTILITIES"),
    ("BESCOM electricity",      "BILLS_UTILITIES"),
    ("Udemy",                   "EDUCATION"),
    ("CultFit",                 "PERSONAL"),
    ("Urban Company",           "PERSONAL"),
    ("ECOM*SWIGGY38291",        "FOOD"),
    ("PHP*HALDIRAMS_NOIDA",     "FOOD"),
    ("RAZORPAY*NETFLIX",        "ENTERTAINMENT"),
]

def run_sanity(clf, vec_w, vec_c, le):
    print("\n[CHECK] Sanity-check predictions:")
    passed = 0
    for merchant, expected in SANITY:
        c = clean(normalize(merchant))
        X = sp.hstack([vec_w.transform([c]), vec_c.transform([c])], format='csr')
        pred_enc  = clf.predict(X)[0]
        pred_cat  = le.inverse_transform([pred_enc])[0]
        conf      = clf.predict_proba(X)[0].max()
        ok = pred_cat == expected
        if ok:
            passed += 1
        mark = "✓" if ok else "✗"
        flag = "" if ok else f"  ← expected {expected}"
        print(f"  {mark} {merchant:<30} → {pred_cat:<22} ({conf:.2f}){flag}")
    print(f"\n[CHECK] {passed}/{len(SANITY)} passed")
    return passed

# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sms",        default=str(SMS_DUMP))
    ap.add_argument("--output",     default=str(OUTPUT_JSON))
    ap.add_argument("--dry-run",    action="store_true")
    ap.add_argument("--export-csv", metavar="PATH")
    args = ap.parse_args()

    print("=" * 60)
    print("  LQD — SMS-driven Merchant Classifier Retraining")
    print("=" * 60)

    records  = parse_sms_dump(args.sms)
    training = build_training(records)
    training = add_synthetic(training, min_per_class=30)

    if args.export_csv:
        import csv
        with open(args.export_csv, 'w', newline='', encoding='utf-8') as f:
            w = csv.writer(f)
            w.writerow(["text", "category", "weight"])
            for t, c, wt in sorted(training, key=lambda x: x[1]):
                w.writerow([t, c, f"{wt:.2f}"])
        print(f"[CSV] Exported {len(training)} rows to {args.export_csv}")

    if len(training) < 100:
        print(f"[ERROR] Only {len(training)} samples — not enough to train. Check SMS dump.")
        sys.exit(1)

    clf, vec_w, vec_c, le = train_model(training)
    run_sanity(clf, vec_w, vec_c, le)

    if args.dry_run:
        print("\n[DRY-RUN] Model not written.")
    else:
        save_model(clf, vec_w, vec_c, le, args.output)
        print("\n[DONE] Rebuild the app to deploy new weights:")
        print("       ./gradlew assembleDebug")

if __name__ == "__main__":
    main()
