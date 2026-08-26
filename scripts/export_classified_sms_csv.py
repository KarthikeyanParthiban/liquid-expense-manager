#!/usr/bin/env python3
"""
Export all SMS transactions from connected ADB device into a clean CSV file
with extracted Amount in a dedicated column, along with 3-Tier classification,
merchant, bank account, and raw body.
"""

import os
import csv
import json
import re
import math
import sqlite3
import subprocess
from datetime import datetime

# 1. Pull fresh database from ADB device
def pull_device_database():
    dump_dir = "/tmp/adb_db_dump"
    os.makedirs(dump_dir, exist_ok=True)
    adb_path = "/home/karthikeyan/android-dev/sdk/platform-tools/adb"

    for file_name in ["expense_manager.db", "expense_manager.db-wal", "expense_manager.db-shm"]:
        target_path = os.path.join(dump_dir, file_name)
        with open(target_path, "wb") as f:
            subprocess.run([adb_path, "exec-out", "run-as", "com.expensemanager.app", "cat", f"databases/{file_name}"], stdout=f, check=False)

    return os.path.join(dump_dir, "expense_manager.db")

# 2. Load ML weights for Tier 3 classification
def load_ml_model():
    weights_path = "app/src/main/assets/models/merchant_classifier_weights.json"
    if not os.path.exists(weights_path):
        return None
    with open(weights_path, "r", encoding="utf-8") as f:
        return json.load(f)

def clean_text(raw):
    t = raw.lower()
    t = re.sub(r"[^a-z0-9\s]", " ", t)
    return re.sub(r"\s+", " ", t).strip()

def predict_ml(text, model):
    if not model or not text:
        return None
    cleaned = clean_text(text)
    if not cleaned:
        return None

    word_vocab = model["word_vocab"]
    word_idf = model["word_idf"]
    char_vocab = model["char_vocab"]
    char_idf = model["char_idf"]
    weights = model["weights"]
    intercept = model["intercept"]
    categories = model["categories"]

    word_offset = 0
    char_offset = len(word_vocab)

    term_counts = {}
    tokens = [t for t in cleaned.split() if t]

    # Word n-grams (1-2)
    for i, tok in enumerate(tokens):
        if tok in word_vocab:
            fid = word_offset + word_vocab[tok]
            term_counts[fid] = term_counts.get(fid, 0) + 1
        if i + 1 < len(tokens):
            bg = f"{tokens[i]} {tokens[i+1]}"
            if bg in word_vocab:
                fid = word_offset + word_vocab[bg]
                term_counts[fid] = term_counts.get(fid, 0) + 1

    # Char n-grams (3-4)
    for tok in tokens:
        padded = f" {tok} "
        L = len(padded)
        for i in range(L - 2):
            g3 = padded[i:i+3]
            if g3 in char_vocab:
                fid = char_offset + char_vocab[g3]
                term_counts[fid] = term_counts.get(fid, 0) + 1
        for i in range(L - 3):
            g4 = padded[i:i+4]
            if g4 in char_vocab:
                fid = char_offset + char_vocab[g4]
                term_counts[fid] = term_counts.get(fid, 0) + 1

    if not term_counts:
        return None

    # TF-IDF & L2 Normalization
    raw_feats = []
    sum_sq = 0.0
    for fid, count in term_counts.items():
        idf = word_idf[fid] if fid < char_offset else char_idf[fid - char_offset]
        tf = 1.0 + math.log(float(count))
        val = tf * idf
        raw_feats.append((fid, val))
        sum_sq += val * val

    norm = max(math.sqrt(sum_sq), 1e-6)
    norm_feats = [(fid, val / norm) for fid, val in raw_feats]

    # Softmax Logits
    n_classes = min(len(categories), min(len(intercept), len(weights)))
    logits = [intercept[c] for c in range(n_classes)]
    for c in range(n_classes):
        w_row = weights[c]
        for fid, fval in norm_feats:
            if fid < len(w_row):
                logits[c] += w_row[fid] * fval

    max_logit = max(logits)
    exp_logits = [math.exp(l - max_logit) for l in logits]
    sum_exp = sum(exp_logits)
    probs = [e / sum_exp for e in exp_logits]

    best_idx = int(np_argmax(probs)) if "np_argmax" in globals() else probs.index(max(probs))
    best_prob = probs[best_idx]
    best_cat = categories[best_idx]

    return best_cat, best_prob

# 3. Comprehensive Dictionary Matcher (Ported from CategoryClassifier.kt)
KEYWORD_MAP = {
    "FOOD": [
        "swiggy", "zomato", "dominos", "domino", "mcdonalds", "mcdonald", "mcd", "kfc", "starbucks", "burger king",
        "pizza hut", "subway", "taco bell", "wendy", "wow momo", "wow china", "behrouz", "faasos", "ovenstory",
        "eatclub", "box8", "freshmenu", "rebel foods", "mojo pizza", "chaayos", "chai point", "chai", "coffee",
        "kaapi", "blue tokai", "third wave", "theobroma", "naturals ice cream", "baskin robbins", "barbeque nation",
        "haldiram", "bikanervala", "bikaner", "a2b", "saravana bhavan", "bawarchi", "paradise biryani",
        "meghana foods", "meghana biryani", "sweets", "mithai", "dining", "restaurant", "eatery", "bakery",
        "cake", "dhaba", "kitchen", "canteen", "cafe", "bistro", "tiffin", "thali", "dosa", "biryani",
        "hotel olympic", "fast food", "snacks", "bites", "sweet house", "hotel"
    ],
    "GROCERIES": [
        "blinkit", "zepto", "instamart", "bigbasket", "bbdaily", "bb daily", "bbinstant", "bb instant", "dunzo",
        "milkbasket", "country delight", "dmart", "d-mart", "reliance fresh", "smart bazaar", "spencers",
        "more retail", "nature basket", "ratnadeep", "patanjali", "licious", "freshtohome", "meat", "fish",
        "chicken", "vegetable", "fruits", "mandi", "kirana", "supermarket", "grocery", "provision", "dairy", "milk",
        "organic", "super market", "stores", "general store"
    ],
    "SHOPPING": [
        "flipkart", "myntra", "ajio", "nykaa", "purplle", "meesho", "tata cliq", "snapdeal", "amazon",
        "zara", "h&m", "uniqlo", "lifestyle", "shoppers stop", "westside", "pantaloons", "max fashion",
        "zudio", "decathlon", "trends", "fabindia", "manyavar", "kalyan jewellers", "tanishq", "malabar gold",
        "croma", "reliance digital", "vijay sales", "poorvika", "sangeetha", "titan", "lenskart", "hamleys",
        "firstcry", "ikea", "pepperfry", "urban ladder", "clothing", "apparel", "jewellers", "electronics",
        "furniture", "flowers", "textiles", "sarees", "hardware", "stationery", "opticals", "footwear"
    ],
    "TRANSPORT": [
        "uber", "ola", "rapido", "namma yatri", "blusmart", "quick ride", "indianoil", "indian oil", "iocl",
        "bharat petroleum", "bpcl", "hpcl", "hindustan petroleum", "shell", "nayara", "petrol", "fuel", "diesel",
        "cng", "fastag", "toll", "nhai", "ihmcl", "irctc", "makemytrip", "goibibo", "easemytrip", "cleartrip",
        "redbus", "abhibus", "indigo", "air india", "vistara", "akasa air", "spicejet", "metro", "parking",
        "taxi", "cabs", "travels", "auto ride"
    ],
    "BILLS_UTILITIES": [
        "electricity", "power", "bescom", "tangedco", "msedcl", "cesc", "uppcl", "dhbvn", "wbsetcl", "tata power",
        "adani electricity", "torrent power", "gas", "indane", "bharat gas", "hp gas", "igl", "mgl", "water",
        "broadband", "fiber", "fibernet", "act fibernet", "airtel", "jio", "vi", "vodafone", "idea", "bsnl",
        "tataplay", "tata play", "dth", "dish tv", "sun direct", "recharge", "billdesk", "bbps", "prepaid rechar", "rechar"
    ],
    "ENTERTAINMENT": [
        "pvr", "inox", "cinepolis", "miraj", "carnival", "wave cinemas", "bookmyshow", "insider", "paytm insider",
        "netflix", "amazon prime", "prime video", "disney", "hotstar", "zee5", "sonyliv", "spotify", "wynk",
        "gaana", "apple music", "youtube premium", "gaming", "steam", "playstation", "xbox", "wonderla", "smaaash"
    ],
    "HEALTHCARE": [
        "apollo", "medplus", "netmeds", "tata 1mg", "1mg", "pharmeasy", "practo", "dr lal", "lal pathlabs",
        "max healthcare", "metropolis", "fortis", "manipal", "narayana", "hospital", "clinic", "pharmacy",
        "chemist", "druggist", "healthcare", "diagnostics", "lab", "dental", "medico"
    ],
    "INVESTMENT": [
        "zerodha", "groww", "upstox", "angel one", "angel broking", "5paisa", "icici direct", "hdfc securities",
        "kotak securities", "motilal oswal", "sharekhan", "paytm money", "indmoney", "kuvera", "smallcase",
        "mutual fund", "sip", "securities", "broking", "stocks", "nps", "etf", "sovereign gold", "invest", "investment"
    ],
    "EDUCATION": [
        "school", "college", "university", "academy", "institute", "vidyalaya", "tuition", "coaching",
        "allen", "aakash", "fiitjee", "resonance", "byju", "unacademy", "physics wallah", "vedantu",
        "udemy", "coursera", "skillshare", "edx", "fees"
    ],
    "PERSONAL": [
        "urban company", "urbanclap", "yes madam", "enrich", "geetanjali", "javed habib", "looks salon",
        "vlcc", "kaya", "bodycraft", "cult.fit", "cultfit", "gold gym", "anytime fitness", "salon", "parlour",
        "spa", "massage", "hairdressing", "barber", "grooming", "gym", "fitness"
    ]
}

def is_spam_or_non_financial(body):
    b = (body or "").lower()
    spam_patterns = [
        "otp", "verification code", "personal loan", "loan offer", "instant loan", "pre-qualified", "pre-approved",
        "pre approved", "pre qualified", "flexi emi", "split your", "into emi", "convert now", "khushkhabri",
        "win up to", "cashback offer", "congratulations", "scratch card", "link par click", "click here",
        "business loan", "home loan", "loan activate", "activate ho gaya", "loan against", "disbursement"
    ]
    return any(k in b for k in spam_patterns)

def classify_transaction(merchant, body, txn_type, model):
    if is_spam_or_non_financial(body):
        return "OTHERS", "Filtered: Loan Offer / Promotional Spam / EMI Proposal", 0.00, txn_type

    b_lower = (body or "").lower().strip()
    m_lower = (merchant or "").lower().strip()
    combined = f"{m_lower} {b_lower}"

    # 0. Credit Card Bill Settlements (Inter-Account Transfers)
    if ("received towards your" in b_lower and "card" in b_lower) or \
       "payment towards your credit card" in b_lower or \
       "towards your card ending" in b_lower or \
       ("payment of" in b_lower and "received towards" in b_lower):
        return "TRANSFERS", "Credit Card Bill Settlement / Transfer", 0.99, "CARD_SETTLEMENT"

    # 1. Contextual Disambiguation
    if "amazon fresh" in combined or "amazon pantry" in combined or "amzn fresh" in combined:
        return "GROCERIES", "Contextual: Amazon Fresh/Pantry -> GROCERIES", 0.99, txn_type
    if "amazon prime" in combined or "prime video" in combined:
        return "ENTERTAINMENT", "Contextual: Amazon Prime -> ENTERTAINMENT", 0.99, txn_type
    if "swiggy instamart" in combined or "instamart" in combined:
        return "GROCERIES", "Contextual: Swiggy Instamart -> GROCERIES", 0.99, txn_type
    if "blinkit" in combined or "hyperpure" in combined:
        return "GROCERIES", "Contextual: Blinkit Grocery -> GROCERIES", 0.99, txn_type
    if "jio fiber" in combined or "jiofiber" in combined:
        return "BILLS_UTILITIES", "Contextual: Jio Fiber -> BILLS_UTILITIES", 0.99, txn_type
    if "tata 1mg" in combined:
        return "HEALTHCARE", "Contextual: Tata 1mg -> HEALTHCARE", 0.99, txn_type
    if "tata play" in combined or "tata power" in combined:
        return "BILLS_UTILITIES", "Contextual: Tata Play/Power -> BILLS_UTILITIES", 0.99, txn_type

    # 2. UPI VPA Token Matching
    if "@" in m_lower:
        vpa_handle = m_lower.split("@")[0]
        vpa_handle = re.sub(r"\.(rzp|payu|billdesk|pos|retail|corp|merchant|upi)", "", vpa_handle)
        tokens = re.split(r"[.\-_]", vpa_handle)
        for tok in tokens:
            if len(tok) >= 3:
                for cat, kws in KEYWORD_MAP.items():
                    for kw in kws:
                        if kw in tok:
                            return cat, f"Tier 2: VPA token '{tok}' matched '{kw}'", 0.95, txn_type

    # 3. Direct Dictionary Keyword Matching
    for cat, kws in KEYWORD_MAP.items():
        for kw in kws:
            escaped = re.escape(kw)
            if re.search(rf"\b{escaped}\b", combined):
                return cat, f"Tier 2: Dictionary keyword '{kw}'", 0.95, txn_type

    # 4. HDFC Bank UPI / YES Bank P2P Format: "Sent Rs... To [Name]" or "From HDFC Bank A/C ... To [Name]"
    p2p_hdfc = re.search(r'sent\s+(?:rs\.?|inr)\s*[0-9,.]+\s+(?:from\s+[\s\S]*?to\s+|to\s+)([A-Za-z\s.]+?)(?:\s+on|\s+ref|\n|$)', b_lower, re.I)
    if p2p_hdfc or \
       "sent via upi to" in b_lower or "transferred to" in b_lower or "upi/p2a/" in b_lower or "self transfer" in b_lower or "sent to vpa" in b_lower:
        recipient = p2p_hdfc.group(1).strip().title() if p2p_hdfc else (merchant or "Personal Transfer")
        return "TRANSFERS", f"Tier 2: P2P Transfer to '{recipient}'", 0.92, "TRANSFER"

    # 5. YES Bank Card spent at @UPI_[Name] with human name format
    if "@upi_" in b_lower or (merchant and merchant.startswith("UPI_")):
        clean_m = (merchant or "").replace("UPI_", "").strip()
        # If it has a space and looks like a name (e.g. KAVITHA P, DIVYA G)
        if re.match(r"^[A-Za-z\s.]+$", clean_m) and len(clean_m.split()) in [2, 3]:
            return "TRANSFERS", f"Tier 2: P2P Transfer to '{clean_m}'", 0.85, "TRANSFER"

    # 6. Tier 3 ML Fallback
    ml_res = predict_ml(merchant if merchant else body, model)
    if ml_res:
        ml_cat, ml_conf = ml_res
        if ml_cat != "OTHERS" and ml_conf >= 0.40:
            return ml_cat, f"Tier 3: On-Device ML Model ({int(ml_conf*100)}% conf)", ml_conf, txn_type

    return "OTHERS", "Tier 3: Default Fallback", 0.50, txn_type


def main():
    print("Pulling transactions from connected ADB hardware device...")
    db_path = pull_device_database()
    ml_model = load_ml_model()

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    cursor.execute("""
        SELECT 
            id,
            timestamp,
            sender,
            amount,
            currency,
            type,
            status,
            category as db_category,
            merchantName,
            bankName,
            accountMask,
            referenceId,
            balanceAfter,
            rawBody
        FROM transactions 
        ORDER BY timestamp DESC;
    """)
    rows = cursor.fetchall()
    print(f"Loaded {len(rows)} raw transactions from device SQLite database.")

    output_csv = "device_sms_classified_export.csv"

    fieldnames = [
        "Transaction_ID",
        "Date_Time",
        "Sender",
        "Amount",
        "Currency",
        "Transaction_Type",
        "Status",
        "Classified_Category",
        "Merchant_Name",
        "Bank_Name",
        "Account_Mask",
        "Reference_UTR_ID",
        "Balance_After",
        "Classification_Reason",
        "Confidence_Score",
        "Raw_SMS_Body"
    ]

    category_counts = {}
    with open(output_csv, "w", newline="", encoding="utf-8") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for r in rows:
            txn_id, ts, sender, amount, currency, t_type, status, db_cat, merchant, bank, mask, ref_id, balance, body = r

            # Format Date
            dt_str = datetime.fromtimestamp(ts / 1000.0).strftime("%Y-%m-%d %H:%M:%S") if ts else ""

            # Run 3-Tier Classification
            cat, reason, conf, final_type = classify_transaction(merchant, body, t_type, ml_model)
            category_counts[cat] = category_counts.get(cat, 0) + 1

            writer.writerow({
                "Transaction_ID": txn_id or "",
                "Date_Time": dt_str,
                "Sender": sender or "",
                "Amount": f"{amount:.2f}" if amount is not None else "0.00",
                "Currency": currency or "INR",
                "Transaction_Type": final_type or "DEBIT",
                "Status": status or "COMPLETED",
                "Classified_Category": cat,
                "Merchant_Name": merchant or "",
                "Bank_Name": bank or "",
                "Account_Mask": mask or "",
                "Reference_UTR_ID": ref_id or "",
                "Balance_After": f"{balance:.2f}" if balance is not None else "",
                "Classification_Reason": reason,
                "Confidence_Score": f"{conf:.2f}",
                "Raw_SMS_Body": (body or "").replace("\n", " ").strip()
            })

    print(f"\nSuccessfully generated CSV export: {output_csv}")
    print("\n--- Category Breakdown ---")
    for cat, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True):
        pct = (count / len(rows)) * 100 if rows else 0
        print(f"  {cat:16s}: {count:4d} transactions ({pct:.1f}%)")

if __name__ == "__main__":
    main()
