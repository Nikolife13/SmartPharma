# FastAPI microservice that does the actual demand forecasting. Called by the
# Spring Boot backend's PredictionClient - never called directly by the frontend.
from fastapi import FastAPI

from app.model import forecast
from app.schemas import PredictionResult, PredictRequest

app = FastAPI(title="SmartPharma ML Service")


@app.get("/health")
def health():
    return {"status": "ok"}


# One forecast per product in the request - see app/model.py for the actual logic.
@app.post("/predict", response_model=list[PredictionResult])
def predict(request: PredictRequest):
    return [forecast(product) for product in request.products]
