#!/usr/bin/env python3
import csv
import re

with open("/mnt/newvolume/liquid-expense-manager/device_sms_classified_export.csv", mode="r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    rows = list(reader)

print("=== ACCOUNT / CARD BREAKDOWN IN USER SMS ===")
acc_map = {}
blank_mask_rows = []
for r in rows:
    bank = r["Bank_Name"]
    mask = r["Account_Mask"]
    m_str = mask if mask else "NO_MASK"
    key = f"{bank} ({m_str})"
    acc_map[key] = acc_map.get(key, 0) + 1
    if not mask:
        blank_mask_rows.append(r)

for k, count in sorted(acc_map.items(), key=lambda x: x[1], reverse=True):
    print(f"  {k:35s}: {count:5d} transactions")

print(f"\nTotal transactions without account mask: {len(blank_mask_rows)}")
print("\nSample messages with NO_MASK:")
for i, r in enumerate(blank_mask_rows[:25]):
    sender = r['Sender']
    bank = r['Bank_Name']
    amt = r['Amount']
    body = r['Raw_SMS_Body']
    print(f"[{i+1}] Sender: {sender} | Bank: {bank} | Amt: {amt}")
    print(f"    Body: {body}\n")
