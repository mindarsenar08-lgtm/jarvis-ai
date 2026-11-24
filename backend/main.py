import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import urllib.request
import urllib.error
import json

app = FastAPI(title="JARVIS AI Backend")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict this to your app/domain before public deployment.
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

class ChatRequest(BaseModel):
    message: str

@app.get("/health")
def health():
    return {"status": "online", "provider": "gemini" if os.getenv("GEMINI_API_KEY") else "not_configured"}

@app.post("/chat")
def chat(request: ChatRequest):
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return {"reply": "JARVIS backend is running, but Gemini is not configured yet. Add GEMINI_API_KEY to the backend environment."}

    model = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    payload = {
        "systemInstruction": {
            "parts": [{"text": "You are JARVIS, a helpful, professional AI assistant. Support Hindi, English and Hinglish. Be honest about limitations and never claim actions you did not perform."}]
        },
        "contents": [{"role": "user", "parts": [{"text": request.message}]}],
        "generationConfig": {"temperature": 0.7, "maxOutputTokens": 1200}
    }
    req = urllib.request.Request(
        url, data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=35) as response:
            data = json.loads(response.read().decode("utf-8"))
        answer = data["candidates"][0]["content"]["parts"][0]["text"]
        return {"reply": answer}
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise HTTPException(status_code=502, detail=f"Gemini API error: {detail[:500]}")
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI provider error: {str(exc)}")
