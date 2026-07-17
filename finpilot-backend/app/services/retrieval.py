from dataclasses import dataclass
from typing import List, Optional

from app.core.config import settings
from app.services.embedding import embed_texts, get_qdrant_client


@dataclass
class RetrievedChunk:
    chunk_id: str
    text: str
    chunk_type: str
    filename: str
    score: float


def retrieve(query: str, top_k: int = 5, filename: Optional[str] = None) -> List[RetrievedChunk]:
    client = get_qdrant_client()

    existing = [c.name for c in client.get_collections().collections]
    if settings.QDRANT_COLLECTION not in existing:
        return []  # nothing uploaded yet

    query_vector = embed_texts([query])[0]

    query_filter = None
    if filename:
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        query_filter = Filter(must=[FieldCondition(key="filename", match=MatchValue(value=filename))])

    result = client.query_points(
        collection_name=settings.QDRANT_COLLECTION,
        query=query_vector,
        limit=top_k,
        query_filter=query_filter,
    )

    return [
        RetrievedChunk(
            chunk_id=(point.payload or {}).get("chunk_id", ""),
            text=(point.payload or {}).get("text", ""),
            chunk_type=(point.payload or {}).get("chunk_type", ""),
            filename=(point.payload or {}).get("filename", ""),
            score=point.score,
        )
        for point in result.points
    ]