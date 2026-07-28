# -*- coding: utf-8 -*-
"""
Seeds a realistic small-pharmacy product catalog (if missing) and synthetic daily
SALE transactions for every product in the database.

Product catalog: PHARMACY_CATALOG below adds ~15 common community-pharmacy drugs
alongside whatever products already exist. Insertion is idempotent (matched by
name) so re-running the script never duplicates rows.

Sales history: this does NOT invent real sales history - the pharmacy has none yet. Instead it
generates a plausible daily sales pattern for each existing product, shaped by:

  1. A base daily rate derived from the product's own min_threshold (a rough proxy
     for how fast it normally moves - no real basis beyond that).
  2. A relative demand weight from real HSE PCRS national prescribing data
     (data/pcrs_national_trend.csv) when the product name matches a row there.
     Products PCRS doesn't cover (e.g. mostly-OTC items) get weight 1.0 - PCRS only
     captures GMS-scheme prescriptions, not over-the-counter sales.
  3. A generic seasonal multiplier (winter uplift / summer dip) for month-to-month
     variation. This part is an assumption for demo purposes, NOT sourced from PCRS.
  4. Poisson noise on top, so the series isn't a perfectly smooth curve.

products.current_quantity is left untouched - only historical inventory_transactions
rows are inserted, dated in the past. Re-running the script is safe: it skips any
product that already has synthetic SALE history in the target window.
"""
import csv
import os
from datetime import date, datetime, timedelta
from pathlib import Path

import numpy as np
import pymysql

HISTORY_DAYS = 180
DATA_DIR = Path(__file__).resolve().parent.parent / "data"
PCRS_CSV = DATA_DIR / "pcrs_national_trend.csv"

# Generic seasonal assumption for a temperate-climate OTC/GMS pharmacy mix - not from PCRS.
SEASONAL_MULTIPLIER = {
    1: 1.15, 2: 1.15, 3: 1.05, 4: 0.95, 5: 0.9, 6: 0.85,
    7: 0.85, 8: 0.9, 9: 1.0, 10: 1.05, 11: 1.15, 12: 1.2,
}

# A representative small community pharmacy shelf. Names match pcrs_national_trend.csv
# exactly where a real PCRS row exists, so the demand-weight lookup matches by name.
# Metformin has no PCRS row in our single captured snapshot - it falls back to weight 1.0.
# Paracetamol and Ibuprofen already exist in the database from earlier manual testing and
# are intentionally left out of this list so they aren't duplicated.
PHARMACY_CATALOG = [
    {"name": "Amoxicillin", "batch_number": "B701", "min_threshold": 15, "current_quantity": 42, "expiry_offset_days": 240},
    {"name": "Atorvastatin", "batch_number": "B702", "min_threshold": 20, "current_quantity": 85, "expiry_offset_days": 330},
    {"name": "Levothyroxine Sodium", "batch_number": "B703", "min_threshold": 8, "current_quantity": 32, "expiry_offset_days": 180},
    {"name": "Colecalciferol", "batch_number": "B704", "min_threshold": 15, "current_quantity": 60, "expiry_offset_days": 400},
    {"name": "Esomeprazole", "batch_number": "B705", "min_threshold": 12, "current_quantity": 18, "expiry_offset_days": 18},
    {"name": "Acetylsalicylic Acid", "batch_number": "B706", "min_threshold": 20, "current_quantity": 14, "expiry_offset_days": 260},
    {"name": "Bisoprolol", "batch_number": "B707", "min_threshold": 10, "current_quantity": 40, "expiry_offset_days": 210},
    {"name": "Rosuvastatin", "batch_number": "B708", "min_threshold": 15, "current_quantity": 55, "expiry_offset_days": 290},
    {"name": "Salbutamol (Inhaled)", "batch_number": "B709", "min_threshold": 8, "current_quantity": 22, "expiry_offset_days": 120},
    {"name": "Pantoprazole", "batch_number": "B710", "min_threshold": 12, "current_quantity": 9, "expiry_offset_days": 340},
    {"name": "Amlodipine", "batch_number": "B711", "min_threshold": 15, "current_quantity": 48, "expiry_offset_days": 380},
    {"name": "Folic Acid", "batch_number": "B712", "min_threshold": 10, "current_quantity": 30, "expiry_offset_days": 8},
    {"name": "Ramipril", "batch_number": "B713", "min_threshold": 12, "current_quantity": 38, "expiry_offset_days": 230},
    {"name": "Sertraline", "batch_number": "B714", "min_threshold": 10, "current_quantity": 25, "expiry_offset_days": 450},
    {"name": "Metformin", "batch_number": "B715", "min_threshold": 20, "current_quantity": 16, "expiry_offset_days": 250},
]


def seed_products(cursor):
    inserted = 0
    for item in PHARMACY_CATALOG:
        cursor.execute("SELECT id FROM products WHERE name = %s", (item["name"],))
        if cursor.fetchone():
            continue
        expiry_date = date.today() + timedelta(days=item["expiry_offset_days"])
        cursor.execute(
            "INSERT INTO products (name, batch_number, expiry_date, min_threshold, current_quantity, created_at, updated_at) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s)",
            (item["name"], item["batch_number"], expiry_date, item["min_threshold"],
             item["current_quantity"], date.today(), date.today()),
        )
        inserted += 1
        print(f"  seeded new product: {item['name']}")
    if inserted == 0:
        print("Product catalog already complete, nothing new to seed.")


def db_connection():
    return pymysql.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        port=int(os.environ.get("DB_PORT", "3306")),
        user=os.environ.get("DB_USER", "root"),
        password=os.environ.get("DB_PASSWORD", "changeme"),
        database=os.environ.get("DB_NAME", "smartpharma"),
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def load_pcrs_weights():
    if not PCRS_CSV.exists():
        return {}
    with open(PCRS_CSV, newline="", encoding="utf-8") as f:
        data_lines = (line for line in f if not line.startswith("#"))
        rows = list(csv.DictReader(data_lines))
    if not rows:
        return {}
    frequencies = {row["product_name"].lower(): int(row["prescribing_frequency"]) for row in rows}
    average = sum(frequencies.values()) / len(frequencies)
    return {name: freq / average for name, freq in frequencies.items()}


def seed_product(cursor, product, pcrs_weights, system_user_id):
    cursor.execute(
        "SELECT COUNT(*) AS cnt FROM inventory_transactions "
        "WHERE product_id = %s AND reason = 'SALE' AND transaction_date >= %s",
        (product["id"], datetime.now() - timedelta(days=HISTORY_DAYS)),
    )
    if cursor.fetchone()["cnt"] > 0:
        print(f"  {product['name']}: already has synthetic history, skipping")
        return

    base_rate = max(1.0, product["min_threshold"] / 10)
    weight = pcrs_weights.get(product["name"].lower(), 1.0)

    today = date.today()
    rows = []
    for offset in range(HISTORY_DAYS, 0, -1):
        day = today - timedelta(days=offset)
        seasonal = SEASONAL_MULTIPLIER[day.month]
        lam = max(0.1, base_rate * weight * seasonal)
        qty = int(np.random.poisson(lam=lam))
        if qty <= 0:
            continue
        rows.append((product["id"], system_user_id, "SALE", -qty, datetime.combine(day, datetime.min.time().replace(hour=12))))

    cursor.executemany(
        "INSERT INTO inventory_transactions (product_id, user_id, reason, quantity_change, transaction_date) "
        "VALUES (%s, %s, %s, %s, %s)",
        rows,
    )
    print(f"  {product['name']}: inserted {len(rows)} days of synthetic SALE history (weight={weight:.2f})")


def main():
    pcrs_weights = load_pcrs_weights()
    print(f"Loaded PCRS demand weights for: {list(pcrs_weights.keys()) or 'none'}")

    conn = db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT id FROM users ORDER BY id LIMIT 1")
            user_row = cursor.fetchone()
            if not user_row:
                print("No users found - register a user in the app first.")
                return
            system_user_id = user_row["id"]

            seed_products(cursor)

            cursor.execute("SELECT id, name, min_threshold FROM products")
            products = cursor.fetchall()
            if not products:
                print("No products found - add products in the app first.")
                return

            print(f"Seeding {HISTORY_DAYS} days of synthetic sales for {len(products)} product(s)...")
            for product in products:
                seed_product(cursor, product, pcrs_weights, system_user_id)

        conn.commit()
        print("Done.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
