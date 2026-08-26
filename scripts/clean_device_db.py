#!/usr/bin/env python3
"""
Clean non-transactional marketing messages (loan offers, EMI promos, OTPs)
from the connected ADB device SQLite database.
"""

import os
import sqlite3
import subprocess

def clean_device_db():
    adb_path = "/home/karthikeyan/android-dev/sdk/platform-tools/adb"
    dump_dir = "/tmp/adb_db_clean"
    os.makedirs(dump_dir, exist_ok=True)
    db_file = os.path.join(dump_dir, "expense_manager.db")

    print("Pulling current database from device...")
    with open(db_file, "wb") as f:
        subprocess.run([adb_path, "exec-out", "run-as", "com.expensemanager.app", "cat", "databases/expense_manager.db"], stdout=f, check=True)

    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()

    # Identify spam rows to delete
    cursor.execute("""
        SELECT id, sender, amount, rawBody 
        FROM transactions 
        WHERE (rawBody LIKE '%pre-qualified%' OR rawBody LIKE '%pre qualified%' OR rawBody LIKE '%khushkhabri%')
           OR (rawBody LIKE '%flexi emi%' AND rawBody LIKE '%split your%')
           OR (rawBody LIKE '%convert now%')
           OR (rawBody LIKE '%link par click%')
           OR (rawBody LIKE '%activate ho gaya%');
    """)
    to_delete = cursor.fetchall()
    print(f"Found {len(to_delete)} spam/marketing non-transaction records:")
    for r in to_delete:
        print(f"  Deleting ID: {r[0]} | Sender: {r[1]} | Body: {r[3][:80]}...")

    if to_delete:
        delete_ids = [r[0] for r in to_delete]
        placeholders = ",".join(["?"] * len(delete_ids))
        cursor.execute(f"DELETE FROM transactions WHERE id IN ({placeholders})", delete_ids)
        conn.commit()
        print(f"Deleted {len(delete_ids)} records from local copy.")

    conn.close()

    # Push cleaned database back to device
    if to_delete:
        print("Pushing cleaned database back to device...")
        # Copy to /data/local/tmp first then run-as
        subprocess.run([adb_path, "push", db_file, "/data/local/tmp/expense_manager_clean.db"], check=True)
        cmd = "run-as com.expensemanager.app cp /data/local/tmp/expense_manager_clean.db databases/expense_manager.db && rm /data/local/tmp/expense_manager_clean.db"
        subprocess.run([adb_path, "shell", cmd], check=True)
        print("Device database successfully updated!")

if __name__ == "__main__":
    clean_device_db()
