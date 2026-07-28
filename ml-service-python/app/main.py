from fastapi import FastAPI

from app.model import forecast
from app.schemas import PredictionResult, PredictRequest

app = FastAPI(title="SmartPharma ML Service")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict", response_model=list[PredictionResult])
def predict(request: PredictRequest):
    return [forecast(product) for product in request.products]
