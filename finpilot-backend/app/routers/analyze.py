"""
/analyze endpoint — retrieval + generation over uploaded statements.
Wrapped with all three guardrails: context sufficiency (before generation),
numeric consistency and advice boundary (after generation).
"""
from fastapi import APIRouter

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
async def analyze(req: AnalyzeRequest):
    chunks = retrieve(req.query, top_k=req.top_k, filename=req.filename)

    if not check_context_sufficiency(len(chunks)):
        return AnalyzeResponse(answer=INSUFFICIENT_CONTEXT_MESSAGE, sources=[], retrieved_chunks=0)

    context = "\n\n".join(f"[chunk_id={c.chunk_id}]\n{c.text}" for c in chunks)
    answer = generate_answer(req.query, context)

    report = apply_response_guardrails(answer, context)

    return AnalyzeResponse(
        answer=report.final_text,
        sources=[c.chunk_id for c in chunks],
        retrieved_chunks=len(chunks),
    )