
from typing import cast

from app.services.retrieval import retrieve, get_all_chunks
from app.services.llm import chat_completion
from app.services.financial_summary import compute_summary
from fastapi import APIRouter, Depends
import time
from sqlalchemy.orm import Session
from app.core.security import get_current_user
from app.models.db import User, get_db
from app.services.audit import log_interaction
from app.services.guardrails import (
    check_context_sufficiency,
    apply_response_guardrails,
    INSUFFICIENT_CONTEXT_MESSAGE,
)
from app.models.schemas import (
    ChatRequest,
    ChatResponse,
    FinancialSummaryResponse,
    CategorySpendOut,
)

router = APIRouter()

import time
from sqlalchemy.orm import Session
from app.models.db import get_db
from app.services.audit import log_interaction

@router.post("", response_model=ChatResponse)
async def chat(
    req: ChatRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start = time.time()
    last_user_message = next((m.content for m in reversed(req.messages) if m.role == "user"), "")

    retrieved = retrieve(
        last_user_message,
        top_k=req.top_k,
        filename=req.filename,
        user_id=cast(int, current_user.id),
    )

    if not check_context_sufficiency(len(retrieved)):
        latency_ms = int((time.time() - start) * 1000)
        log_interaction(
            db,
            "chat",
            last_user_message,
            INSUFFICIENT_CONTEXT_MESSAGE,
            latency_ms,
            success=True,
            username=cast(str, current_user.username),
            user_id=cast(int, current_user.id),
        )
        return ChatResponse(answer=INSUFFICIENT_CONTEXT_MESSAGE, sources=[])

    context = "\n\n".join(f"[chunk_id={c.chunk_id}]\n{c.text}" for c in retrieved)
    history = [{"role": m.role, "content": m.content} for m in req.messages]

    try:
        answer = chat_completion(history, context)
        report = apply_response_guardrails(answer, context)
        latency_ms = int((time.time() - start) * 1000)
        log_interaction(
            db,
            "chat",
            last_user_message,
            report.final_text,
            latency_ms,
            success=True,
            username=cast(str, current_user.username),
            user_id=cast(int, current_user.id),
        )
        return ChatResponse(answer=report.final_text, sources=[c.chunk_id for c in retrieved])
    except Exception as e:
        latency_ms = int((time.time() - start) * 1000)
        log_interaction(
            db,
            "chat",
            last_user_message,
            "",
            latency_ms,
            success=False,
            error_message=str(e),
            username=cast(str, current_user.username),
            user_id=cast(int, current_user.id),
        )
        raise

@router.get("/summary", response_model=FinancialSummaryResponse)
async def chat_summary(filename: str | None = None, current_user: User = Depends(get_current_user)):
    all_chunks = get_all_chunks(filename=filename, user_id=cast(int, current_user.id))

    if not all_chunks:
        return FinancialSummaryResponse(
            monthly_income=0, monthly_expenses=0, surplus=0,
            savings_rate_percent=0, health_score=0, biggest_category=None,
            category_breakdown=[], transaction_count=0, income_is_estimated=True,
        )

    summary = compute_summary(all_chunks)

    return FinancialSummaryResponse(
        monthly_income=summary.monthly_income,
        monthly_expenses=summary.monthly_expenses,
        surplus=summary.surplus,
        savings_rate_percent=summary.savings_rate_percent,
        health_score=summary.health_score,
        biggest_category=(
            CategorySpendOut(
                category=summary.biggest_category.category,
                amount=summary.biggest_category.amount,
                percent_of_total=summary.biggest_category.percent_of_total,
            ) if summary.biggest_category else None
        ),
        category_breakdown=[
            CategorySpendOut(category=c.category, amount=c.amount, percent_of_total=c.percent_of_total)
            for c in summary.category_breakdown
        ],
        transaction_count=summary.transaction_count,
        income_is_estimated=summary.income_is_estimated,
    )