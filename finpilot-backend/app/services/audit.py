"""
Audit logging — one function, called from every AI-facing endpoint after
it computes a response. Callers pass short summaries, not full payloads,
so logs stay readable and don't become a second place financial data leaks from.
"""
from typing import Optional

from sqlalchemy.orm import Session

from app.models.db import AuditLog


def _truncate(text: str, max_len: int = 300) -> str:
    text = text or ""
    return text if len(text) <= max_len else text[:max_len] + "..."


def log_interaction(
    db: Session,
    interaction_type: str,
    request_summary: str,
    response_summary: str,
    latency_ms: int,
    success: bool = True,
    username: Optional[str] = None,
    user_id: Optional[int] = None,
    error_message: Optional[str] = None,
) -> None:
    entry = AuditLog(
        interaction_type=interaction_type,
        username=username,
        user_id=user_id,
        request_summary=_truncate(request_summary),
        response_summary=_truncate(response_summary),
        success=success,
        latency_ms=latency_ms,
        error_message=_truncate(error_message) if error_message else None,
    )
    db.add(entry)
    db.commit()