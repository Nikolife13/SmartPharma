# -*- coding: utf-8 -*-
"""
Unit tests for app/model.py's forecast() - the trend x seasonal decomposition
that turns a product's sales history into a 30-day demand prediction.
"""
import sys
from datetime import date, timedelta
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.model import CONFIDENCE_CEILING, CONFIDENCE_FLOOR, FALLBACK_CONFIDENCE, forecast, predict_daily_series
from app.schemas import DailySale, ProductHistory


def history_with_daily_sales(quantities, name="unknown drug", min_threshold=10, current_quantity=5):
    start = date(2026, 1, 1)
    daily_sales = [
        DailySale(date=start + timedelta(days=i), quantity=q)
        for i, q in enumerate(quantities)
    ]
    return ProductHistory(
        productId=1,
        name=name,
        currentQuantity=current_quantity,
        minThreshold=min_threshold,
        dailySales=daily_sales,
    )


def test_fewer_than_seven_days_falls_back_to_min_threshold():
    history = history_with_daily_sales([1, 2, 3], min_threshold=20, current_quantity=5)

    result = forecast(history)

    assert result.forecastedDemand30d == 20
    assert result.confidenceScore == FALLBACK_CONFIDENCE
    assert result.suggestedOrderQty == max(0, 20 + 20 - 5)


def test_forecast_preserves_the_requested_product_id():
    history = history_with_daily_sales([2] * 10)
    history.productId = 42

    result = forecast(history)

    assert result.productId == 42


def test_forecast_and_confidence_are_never_negative():
    # A declining trend that would go negative if not clipped.
    quantities = list(range(20, 0, -1))  # 20, 19, ..., 1
    history = history_with_daily_sales(quantities)

    result = forecast(history)

    assert result.forecastedDemand30d >= 0
    assert result.suggestedOrderQty >= 0
    assert CONFIDENCE_FLOOR <= result.confidenceScore <= CONFIDENCE_CEILING


def test_confidence_is_high_when_history_is_a_near_perfect_trend():
    # Perfectly linear growth, no noise - the model should fit this almost exactly,
    # so weekly R^2 should be close to 1 and confidence close to the ceiling.
    quantities = [5 + i for i in range(42)]  # 6 full weeks, steadily increasing
    history = history_with_daily_sales(quantities)

    result = forecast(history)

    assert result.confidenceScore >= 90


def test_suggested_order_qty_accounts_for_current_stock_and_threshold():
    quantities = [3] * 10
    history = history_with_daily_sales(quantities, min_threshold=15, current_quantity=1000)

    result = forecast(history)

    # Plenty of stock already on hand -> shouldn't need to order more.
    assert result.suggestedOrderQty == 0


def test_unknown_drug_name_gets_no_seasonal_adjustment():
    # A name that can't match anything in the real PCRS dataset should behave as a
    # pure trend forecast (seasonal multiplier defaults to 1.0 for every month).
    quantities = [4] * 30
    history = history_with_daily_sales(quantities, name="totally-fictional-drug-xyz")

    result = forecast(history)

    # Flat history -> flat trend -> ~4/day x 30 days, give or take rounding.
    assert 100 <= result.forecastedDemand30d <= 140


def _rmse(actual, predicted):
    actual = np.array(actual, dtype=float)
    predicted = np.array(predicted, dtype=float)
    return float(np.sqrt(np.mean((actual - predicted) ** 2)))


def test_model_rmse_beats_naive_baseline_on_a_trending_series():
    """
    Matches the project proposal's evaluation methodology: the model's RMSE on
    a holdout window should be lower than a naive "order the same as last
    month" baseline. Uses a deterministic synthetic series (fixed seed) with a
    genuine upward trend - something only the regression can capture, since the
    naive baseline just repeats the previous 30 days unchanged.
    """
    rng = np.random.default_rng(seed=42)
    total_days = 90
    trend = [5 + i * 0.15 for i in range(total_days)]
    noise = rng.normal(0, 0.5, total_days)
    quantities = [max(0, round(t + n)) for t, n in zip(trend, noise)]

    start = date(2026, 1, 1)
    all_dates = [start + timedelta(days=i) for i in range(total_days)]

    train_sales = [
        DailySale(date=d, quantity=q) for d, q in zip(all_dates[:60], quantities[:60])
    ]
    holdout_actual = quantities[60:90]
    baseline_prediction = quantities[30:60]  # "same as last month"

    model_prediction = predict_daily_series(train_sales, "trend-test-drug", horizon_days=30)

    model_rmse = _rmse(holdout_actual, model_prediction)
    baseline_rmse = _rmse(holdout_actual, baseline_prediction)

    assert model_rmse < baseline_rmse
