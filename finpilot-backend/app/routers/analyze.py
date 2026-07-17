from fastapi import APIRouter

from app.services.retrieval import retrieve
from app.services.llm import generate_answer
from app.models.schemas import AnalyzeRequest, AnalyzeResponse

router = APIRouter()


@router.post("", response_model=AnalyzeResponse)
async def analyze(req: AnalyzeRequest):
    chunks = retrieve(req.query, top_k=req.top_k, filename=req.filename)

    if not chunks:
        return AnalyzeResponse(
            answer="I don't have enough information to answer that — please upload a statement first.",
            sources=[],
            retrieved_chunks=0,
        )

    context = "\n\n".join(f"[chunk_id={c.chunk_id}]\n{c.text}" for c in chunks)
    answer = generate_answer(req.query, context)

    return AnalyzeResponse(
        answer=answer,
        sources=[c.chunk_id for c in chunks],
        retrieved_chunks=len(chunks),
    )