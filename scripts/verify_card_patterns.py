#!/usr/bin/env python3
import csv
import re

with open("/mnt/newvolume/liquid-expense-manager/device_sms_classified_export.csv", mode="r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    rows = list(reader)

patterns = [
    (r"(?i)(?:A/c|Acct|Account|Acc|A/C)\s*(?:no\.?|number)?\s*[*Xx]*([0-9]{3,4})", "Acct pattern"),
    (r"(?i)(?:Card|Credit Card|Debit Card)\s*(?:no\.?)?\s*[*Xx]*([0-9]{4})", "Card pattern 1"),
    (r"(?i)(?:Card\s*X([0-9]{4}))", "Card pattern 2 (X1006)"),
    (r"(?i)(?:ending\s*(?:with|in)?\s*[*Xx]*([0-9]{3,4}))", "Ending with"),
    (r"(?i)(?:[*Xx]{1,}([0-9]{3,4}))", "Mask X/star digits"),
    (r"(?i)(?:SMS\s+BLKCC|SMS\s+BLOCK\s+CC|SMS\s+BLOCK)\s*([0-9]{4})", "Block CC digits")
]

card_rows = [r for r in rows if "card" in r["Raw_SMS_Body"].lower() or "cc" in r["Raw_SMS_Body"].lower()]
print(f"Total card-mentioning rows: {len(card_rows)}")

missing_mask_cards = []
for r in card_rows:
    body = r["Raw_SMS_Body"]
    matched = False
    for pat, name in patterns:
        m = re.search(pat, body)
        if m:
            matched = True
            break
    if not matched:
        missing_mask_cards.append(r)

print(f"Card rows without detected mask: {len(missing_mask_cards)}")
for i, r in enumerate(missing_mask_cards[:10]):
    sender = r['Sender']
    bank = r['Bank_Name']
    amt = r['Amount']
    body = r['Raw_SMS_Body']
    print(f"[{i+1}] Sender: {sender} | Bank: {bank} | Amt: {amt}")
    print(f"    Body: {body}\n")
