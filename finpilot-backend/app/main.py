from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.models.db import User, init_db
from app.routers import upload, analyze, chat, goal_plan, auth, admin
from app.routers.errors import http_exception_handler, jwt_error_handler
from jose import JWTError
from starlette.exceptions import HTTPException as StarletteHTTPException

app = FastAPI(title=settings.APP_NAME, version="0.1.0")

app.add_exception_handler(StarletteHTTPException, http_exception_handler)
app.add_exception_handler(JWTError, jwt_error_handler)

@app.on_event("startup")
def on_startup():
    init_db()

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

app.include_router(auth.router, prefix="/auth", tags=["auth"])

@app.get("/health")
def health():
    return {"status": "ok", "env": settings.ENV}


app.include_router(admin.router, prefix="/admin", tags=["admin"])
