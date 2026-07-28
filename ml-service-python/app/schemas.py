from datetime import date
from typing import List

from pydantic import BaseModel


class DailySale(BaseModel):
    date: date
    quantity: int


class ProductHistory(BaseModel):
    productId: int
    name: str
    currentQuantity: int
    minThreshold: int
    dailySales: List[DailySale] = []


class PredictRequest(BaseModel):
    products: List[ProductHistory]


class PredictionResult(BaseModel):
    productId: int
    forecastedDemand30d: int
    suggestedOrderQty: int
    confidenceScore: int
