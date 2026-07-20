from fastapi import APIRouter

from app.services.retrieval import retrieve, get_all_chunks
from app.services.llm import chat_completion
from app.services.financial_summary import compute_summary
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

@router.post("", response_model=ChatResponse)
async def chat(req: ChatRequest):
    last_user_message = next(
        (m.content for m in reversed(req.messages) if m.role == "user"), ""
    )

    retrieved = retrieve(last_user_message, top_k=req.top_k, filename=req.filename)

    if not check_context_sufficiency(len(retrieved)):
        return ChatResponse(answer=INSUFFICIENT_CONTEXT_MESSAGE, sources=[])

    context = "\n\n".join(f"[chunk_id={c.chunk_id}]\n{c.text}" for c in retrieved)
    history = [{"role": m.role, "content": m.content} for m in req.messages]
    answer = chat_completion(history, context)

    report = apply_response_guardrails(answer, context)

    return ChatResponse(answer=report.final_text, sources=[c.chunk_id for c in retrieved])

@router.get("/summary", response_model=FinancialSummaryResponse)
async def chat_summary(filename: str | None = None):
    all_chunks = get_all_chunks(filename=filename)

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