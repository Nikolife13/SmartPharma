# -*- coding: utf-8 -*-
"""
Evaluates the forecasting model the way the project proposal specifies: hold
out the most recent 30 days of each product's real (seeded) sales history,
forecast that window using only the days before it, and compare the model's
RMSE against a naive baseline ("order the same as last month" - i.e. repeat
the daily pattern from the 30 days immediately before the holdout window).

Success criterion from the proposal: the model's RMSE should be at least 15%
lower than the baseline's RMSE.

Run from ml-service-python/ as: python -m scripts.backtest
"""
import os
from collections import defaultdict
from datetime import timedelta

import numpy as np
import pymysql

from app.model import predict_daily_series
from app.schemas import DailySale

HOLDOUT_DAYS = 30


def db_connection():
    return pymysql.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        port=int(os.environ.get("DB_PORT", "3306")),
        user=os.environ.get("DB_USER", "root"),
        password=os.environ.get("DB_PASSWORD", "changeme"),
        database=os.environ.get("DB_NAME", "smartpharma_java"),
        cursorclass=pymysql.cursors.DictCursor,
    )


def rmse(actual, predicted):
    actual = np.array(actual, dtype=float)
    predicted = np.array(predicted, dtype=float)
    return float(np.sqrt(np.mean((actual - predicted) ** 2)))


def backtest_product(product, by_date):
    all_dates = sorted(by_date.keys())
    last_date = all_dates[-1]
    cutoff = last_date - timedelta(days=HOLDOUT_DAYS - 1)  # first day of the holdout window

    train_dates = [d for d in all_dates if d < cutoff]
    if len(train_dates) < 30:
        return None  # not enough history for a fair test

    holdout_dates = [cutoff + timedelta(days=i) for i in range(HOLDOUT_DAYS)]
    baseline_source_dates = [d - timedelta(days=HOLDOUT_DAYS) for d in holdout_dates]  # "last month"

    actual = [by_date.get(d, 0) for d in holdout_dates]
    baseline_pred = [by_date.get(d, 0) for d in baseline_source_dates]

    train_sales = [DailySale(date=d, quantity=by_date[d]) for d in train_dates]
    model_pred = predict_daily_series(train_sales, product["name"], HOLDOUT_DAYS).tolist()

    return {
        "name": product["name"],
        "rmse_model": rmse(actual, model_pred),
        "rmse_baseline": rmse(actual, baseline_pred),
        "actual_total": sum(actual),
    }


def main():
    conn = db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, name FROM products")
            products = cursor.fetchall()

            results = []
            for product in products:
                cursor.execute(
                    "SELECT transaction_date, quantity_change FROM inventory_transactions "
                    "WHERE product_id=%s AND reason='SALE' ORDER BY transaction_date",
                    (product["id"],),
                )
                rows = cursor.fetchall()
                if not rows:
                    continue

                by_date = defaultdict(int)
                for row in rows:
                    by_date[row["transaction_date"].date()] += abs(row["quantity_change"])

                result = backtest_product(product, by_date)
                if result:
                    results.append(result)
    finally:
        conn.close()

    if not results:
        print("No products had enough history to backtest.")
        return

    print(f"{'Product':25s} {'RMSE model':>11s} {'RMSE baseline':>14s} {'Improvement':>12s}")
    improvements = []
    for r in results:
        improvement = (r["rmse_baseline"] - r["rmse_model"]) / r["rmse_baseline"] * 100 if r["rmse_baseline"] else 0.0
        improvements.append(improvement)
        print(f"{r['name']:25s} {r['rmse_model']:11.2f} {r['rmse_baseline']:14.2f} {improvement:+11.1f}%")

    avg_model_rmse = sum(r["rmse_model"] for r in results) / len(results)
    avg_baseline_rmse = sum(r["rmse_baseline"] for r in results) / len(results)
    avg_improvement = sum(improvements) / len(improvements)

    print()
    print(f"Products tested:        {len(results)}")
    print(f"Average RMSE (model):    {avg_model_rmse:.2f}")
    print(f"Average RMSE (baseline): {avg_baseline_rmse:.2f}")
    print(f"Average improvement:     {avg_improvement:+.1f}%")
    print()
    target = 15.0
    if avg_improvement >= target:
        print(f"PASS - model beats the naive baseline by {avg_improvement:.1f}%, "
              f"meeting the proposal's {target:.0f}% success criterion.")
    else:
        print(f"FAIL - model beats the naive baseline by only {avg_improvement:.1f}%, "
              f"short of the proposal's {target:.0f}% success criterion.")


if __name__ == "__main__":
    main()
