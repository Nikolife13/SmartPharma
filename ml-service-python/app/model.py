from datetime import timedelta

import numpy as np
from sklearn.linear_model import LinearRegression

from app.pcrs import load_pcrs_data
from app.schemas import PredictionResult, ProductHistory

MIN_HISTORY_DAYS = 7
FORECAST_HORIZON_DAYS = 30
FALLBACK_CONFIDENCE = 50
CONFIDENCE_FLOOR = 50
CONFIDENCE_CEILING = 95

# Loaded once at import time from real HSE PCRS data (2016-2023) - see app/pcrs.py.
_, SEASONAL_INDEX = load_pcrs_data()


def forecast(history: ProductHistory) -> PredictionResult:
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

    # Trend (regression) x real seasonal index: the line captures overall growth,
    # then each forecasted day is scaled by how that calendar month actually
    # behaves for this specific drug nationally (flat 1.0 where PCRS has no data).
    drug_seasonal = SEASONAL_INDEX.get(history.name.lower(), {})
    last_date = sales[-1].date

    future_days = np.arange(len(sales), len(sales) + FORECAST_HORIZON_DAYS).reshape(-1, 1)
    trend_predicted = model.predict(future_days)
    seasonal_multipliers = np.array([
        drug_seasonal.get((last_date + timedelta(days=offset)).month, 1.0)
        for offset in range(1, FORECAST_HORIZON_DAYS + 1)
    ])

    predicted_daily = np.clip(trend_predicted * seasonal_multipliers, a_min=0, a_max=None)
    forecasted_demand = int(round(predicted_daily.sum()))

    suggested_qty = max(0, forecasted_demand + history.minThreshold - history.currentQuantity)

    r2 = model.score(day_index, quantities)
    confidence = CONFIDENCE_FLOOR + max(0.0, min(1.0, r2)) * (CONFIDENCE_CEILING - CONFIDENCE_FLOOR)

    return PredictionResult(
        productId=history.productId,
        forecastedDemand30d=forecasted_demand,
        suggestedOrderQty=suggested_qty,
        confidenceScore=int(round(confidence)),
    )
