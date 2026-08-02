from collections import defaultdict
from datetime import timedelta

import numpy as np
from sklearn.linear_model import LinearRegression

from app.pcrs import load_pcrs_data
from app.schemas import PredictionResult, ProductHistory

MIN_HISTORY_DAYS = 7
MIN_WEEKS_FOR_WEEKLY_R2 = 3
FORECAST_HORIZON_DAYS = 30
FALLBACK_CONFIDENCE = 50
CONFIDENCE_FLOOR = 50
CONFIDENCE_CEILING = 95

# Loaded once at import time from real HSE PCRS data (2016-2023) - see app/pcrs.py.
_, SEASONAL_INDEX = load_pcrs_data()


def _weekly_totals(dates, values):
    """Sums (date, value) pairs into ISO-week buckets, keyed by (year, week)."""
    buckets = defaultdict(float)
    for date, value in zip(dates, values):
        buckets[date.isocalendar()[:2]] += value
    return buckets


def _r_squared(actual, predicted):
    mean_actual = sum(actual) / len(actual)
    ss_tot = sum((a - mean_actual) ** 2 for a in actual)
    if ss_tot == 0:
        return 0.0
    ss_res = sum((a - p) ** 2 for a, p in zip(actual, predicted))
    return 1 - ss_res / ss_tot


def predict_daily_series(sales, drug_name, horizon_days=FORECAST_HORIZON_DAYS):
    """
    Fits a linear trend on `sales` (a list of DailySale, already sorted by date)
    and returns the clipped, seasonally-adjusted predicted quantity for each of
    the next `horizon_days` calendar days after the last historical day.

    Pulled out of forecast() so the same fit-and-predict logic can be reused by
    the offline backtest (scripts/backtest.py), which needs day-by-day
    predictions to compare against actual holdout data - not just the 30-day total.
    """
    day_index = np.arange(len(sales)).reshape(-1, 1)
    quantities = np.array([s.quantity for s in sales])

    model = LinearRegression()
    model.fit(day_index, quantities)

    # Trend (regression) x real seasonal index: the line captures overall growth,
    # then each forecasted day is scaled by how that calendar month actually
    # behaves for this specific drug nationally (flat 1.0 where PCRS has no data).
    drug_seasonal = SEASONAL_INDEX.get(drug_name.lower(), {})
    last_date = sales[-1].date

    future_days = np.arange(len(sales), len(sales) + horizon_days).reshape(-1, 1)
    trend_predicted = model.predict(future_days)
    seasonal_multipliers = np.array([
        drug_seasonal.get((last_date + timedelta(days=offset)).month, 1.0)
        for offset in range(1, horizon_days + 1)
    ])

    return np.clip(trend_predicted * seasonal_multipliers, a_min=0, a_max=None)


def forecast(history: ProductHistory) -> PredictionResult:
    """
    Predicts 30-day demand for one product: fits a linear trend on its daily
    sales, then scales each forecasted day by that drug's real seasonal index
    for the matching calendar month (trend x seasonal decomposition).
    """
    sales = sorted(history.dailySales, key=lambda s: s.date)

    if len(sales) < MIN_HISTORY_DAYS:
        # Not enough history to fit a trend line - fall back to a conservative
        # heuristic anchored on the product's own reorder threshold.
        forecasted = history.minThreshold
        suggested = max(0, forecasted + history.minThreshold - history.currentQuantity)
        return PredictionResult(
            productId=history.productId,
            forecastedDemand30d=forecasted,
            suggestedOrderQty=suggested,
            confidenceScore=FALLBACK_CONFIDENCE,
        )

    day_index = np.arange(len(sales)).reshape(-1, 1)
    quantities = np.array([s.quantity for s in sales])

    model = LinearRegression()
    model.fit(day_index, quantities)

    drug_seasonal = SEASONAL_INDEX.get(history.name.lower(), {})

    predicted_daily = predict_daily_series(sales, history.name, FORECAST_HORIZON_DAYS)
    forecasted_demand = int(round(predicted_daily.sum()))

    suggested_qty = max(0, forecasted_demand + history.minThreshold - history.currentQuantity)

    # Confidence reflects how well trend x seasonal explains actual history - but
    # scored on WEEKLY totals, not raw daily counts. A straight line almost never
    # fits day-to-day Poisson noise (R^2 near zero regardless of real signal), so
    # scoring daily R^2 pinned confidence at CONFIDENCE_FLOOR for nearly everything.
    # Weekly aggregation smooths that noise out while still penalizing a model that
    # doesn't actually track the drug's trend/seasonal pattern.
    trend_fitted = model.predict(day_index)
    seasonal_fitted = np.array([drug_seasonal.get(s.date.month, 1.0) for s in sales])
    fitted_daily = np.clip(trend_fitted * seasonal_fitted, a_min=0, a_max=None)

    dates = [s.date for s in sales]
    actual_weekly = _weekly_totals(dates, quantities)
    fitted_weekly = _weekly_totals(dates, fitted_daily)
    common_weeks = sorted(actual_weekly.keys())

    if len(common_weeks) >= MIN_WEEKS_FOR_WEEKLY_R2:
        r2 = _r_squared(
            [actual_weekly[w] for w in common_weeks],
            [fitted_weekly[w] for w in common_weeks],
        )
    else:
        r2 = model.score(day_index, quantities)

    confidence = CONFIDENCE_FLOOR + max(0.0, min(1.0, r2)) * (CONFIDENCE_CEILING - CONFIDENCE_FLOOR)

    return PredictionResult(
        productId=history.productId,
        forecastedDemand30d=forecasted_demand,
        suggestedOrderQty=suggested_qty,
        confidenceScore=int(round(confidence)),
    )
