from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.routers import upload
from app.routers import upload, analyze
from app.routers import upload, analyze, chat, goal_plan

app = FastAPI(title=settings.APP_NAME, version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(upload.router, prefix="/upload", tags=["upload"])

app.include_router(analyze.router, prefix="/analyze", tags=["analyze"])

app.include_router(chat.router, prefix="/chat", tags=["chat"])

app.include_router(goal_plan.router, prefix="/goal-plan", tags=["goal-plan"])

@app.get("/health")
def health():
    return {"status": "ok", "env": settings.ENV}
