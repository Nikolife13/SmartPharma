# Pydantic request/response shapes for the /predict endpoint - these mirror the
# Java-side DTOs in backend-java/.../dto/Ml*.java field-for-field.
from datetime import date
from typing import List

from pydantic import BaseModel


# One day's total units sold for a product.
class DailySale(BaseModel):
    date: date
    quantity: int


# Everything forecast() needs for one product: its identity, current stock state,
# and its daily sales history (sorted by app/model.py, not guaranteed here).
class ProductHistory(BaseModel):
    productId: int
    name: str
    currentQuantity: int
    minThreshold: int
    dailySales: List[DailySale] = []


# Body of POST /predict - forecasts for every product are requested in one call.
class PredictRequest(BaseModel):
    products: List[ProductHistory]


# One product's forecast: 30-day demand, how much to order, and confidence (%).
class PredictionResult(BaseModel):
    productId: int
    forecastedDemand30d: int
    suggestedOrderQty: int
    confidenceScore: int
