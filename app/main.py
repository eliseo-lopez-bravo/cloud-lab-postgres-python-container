from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
from sklearn.ensemble import IsolationForest

app = FastAPI(title="Mini Anomaly Detector")

class DataPoint(BaseModel):
    values: list[float]

clf = IsolationForest(n_estimators=50, contamination=0.05, random_state=42)
clf.fit(np.random.normal(size=(100,3)))

@app.get("/health")
def health():
    return {"status":"ok"}

@app.post("/predict")
def predict(data: DataPoint):
    arr = np.array(data.values).reshape(1, -1)
    score = float(-clf.decision_function(arr)[0])
    return {"anomaly_score": score}
