from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"

def test_high_risk_message_detects_sensitive_request():
    response = client.post("/v1/analyze/text", json={"text": "Urgent! Your account is blocked. Send your OTP and password immediately."})
    assert response.status_code == 200
    assert response.json()["risk_level"] == "high"
    assert response.json()["reasons"]

def test_blank_text_rejected():
    response = client.post("/v1/analyze/text", json={"text": "   "})
    assert response.status_code == 422

def test_url_requires_scheme():
    response = client.post("/v1/analyze/url", json={"url": "example.com"})
    assert response.status_code == 422
