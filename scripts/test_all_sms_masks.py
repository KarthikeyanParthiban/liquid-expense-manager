#!/usr/bin/env python3
import csv
import re

with open("/mnt/newvolume/liquid-expense-manager/device_sms_classified_export.csv", mode="r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    rows = list(reader)

account_patterns = [
    re.compile(r"(?i)(?:A/c|Acct|Account|Acc|A/C)\s*(?:no\.?|number)?\s*[*Xx-]*([0-9]{3,4})"),
    re.compile(r"(?i)(?:Card|Credit Card|Debit Card|CC)\s*(?:no\.?)?\s*[*Xx-]*([0-9]{4})"),
    re.compile(r"(?i)(?:Card\s*X([0-9]{4}))"),
    re.compile(r"(?i)(?:ending\s*(?:with|in)?\s*[*Xx-]*([0-9]{3,4}))"),
    re.compile(r"(?i)(?:UAN|Member\s*ID|PF\s*A/c)\s*[*Xx-]*([0-9]{4})"),
    re.compile(r"(?i)(?:[*Xx]{1,}[-]?[*Xx]*([0-9]{3,4}))"),
    re.compile(r"(?i)(?:SMS\s+BLKCC|SMS\s+BLOCK\s+CC|SMS\s+BLOCK)\s*([0-9]{4})")
]

def extract_mask(body):
    for pat in account_patterns:
        m = pat.search(body)
        if m:
            digits = m.group(1).strip()
            if digits:
                return f"XX{digits}"
    return None

results = {}
for r in rows:
    mask = extract_mask(r["Raw_SMS_Body"])
    bank = r["Bank_Name"]
    key = f"{bank} ({mask if mask else 'NO_MASK'})"
    results[key] = results.get(key, 0) + 1

print("=== PARSED ACCOUNT MASK RESULTS ON 2,414 USER SMS ===")
for k, count in sorted(results.items(), key=lambda x: x[1], reverse=True):
    print(f"  {k:35s}: {count:5d} transactions")
