"""
/analyze endpoint — retrieval + generation over uploaded statements.
Wrapped with all three guardrails: context sufficiency (before generation),
numeric consistency and advice boundary (after generation).
"""
import time

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.models.db import User, get_db
from app.services.audit import log_interaction
from app.services.retrieval import retrieve
from app.services.llm import generate_answer
from app.services.guardrails import (
    check_context_sufficiency,
    apply_response_guardrails,
    INSUFFICIENT_CONTEXT_MESSAGE,
)
from app.models.schemas import AnalyzeRequest, AnalyzeResponse

router = APIRouter()


@router.post("", response_model=AnalyzeResponse)
async def analyze(
    req: AnalyzeRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start = time.time()
    chunks = retrieve(req.query, top_k=req.top_k, filename=req.filename, user_id=current_user.id)

    if not check_context_sufficiency(len(chunks)):
        latency_ms = int((time.time() - start) * 1000)
        log_interaction(
            db,
            "analyze",
            req.query,
            INSUFFICIENT_CONTEXT_MESSAGE,
            latency_ms,
            success=True,
            username=current_user.username,
            user_id=current_user.id,
        )
        return AnalyzeResponse(answer=INSUFFICIENT_CONTEXT_MESSAGE, sources=[], retrieved_chunks=0)

    context = "\n\n".join(f"[chunk_id={c.chunk_id}]\n{c.text}" for c in chunks)
    answer = generate_answer(req.query, context)

    report = apply_response_guardrails(answer, context)

    latency_ms = int((time.time() - start) * 1000)
    log_interaction(
        db,
        "analyze",
        req.query,
        report.final_text,
        latency_ms,
        success=True,
        username=current_user.username,
        user_id=current_user.id,
    )

    return AnalyzeResponse(
        answer=report.final_text,
        sources=[c.chunk_id for c in chunks],
        retrieved_chunks=len(chunks),
    )