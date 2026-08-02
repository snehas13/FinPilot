from typing import Any

from fastapi import Request
from fastapi.responses import JSONResponse
from jose import JWTError
from starlette.exceptions import HTTPException as StarletteHTTPException


async def http_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    if isinstance(exc, StarletteHTTPException):
        detail = exc.detail if getattr(exc, "detail", None) else "Request failed"
        if request.url.path.startswith("/upload"):
            return JSONResponse(
                status_code=200,
                content={
                    "filename": "",
                    "status": "error",
                    "pages_extracted": 0,
                    "chunks_created": 0,
                    "points_stored": 0,
                    "chunk_preview": [],
                    "error": detail,
                },
            )
        return JSONResponse({"detail": detail}, status_code=exc.status_code)

    return JSONResponse({"detail": "Request failed"}, status_code=500)


async def jwt_error_handler(request: Request, exc: Exception) -> JSONResponse:
    if isinstance(exc, JWTError):
        if request.url.path.startswith("/upload"):
            return JSONResponse(
                status_code=200,
                content={
                    "filename": "",
                    "status": "error",
                    "pages_extracted": 0,
                    "chunks_created": 0,
                    "points_stored": 0,
                    "chunk_preview": [],
                    "error": "Authentication failed",
                },
            )
        return JSONResponse({"detail": "Invalid token"}, status_code=401)

    return JSONResponse({"detail": "Request failed"}, status_code=500)
