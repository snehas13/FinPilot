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


def retrieve(query: str, top_k: int = 5, filename: Optional[str] = None, user_id: Optional[int] = None) -> List[RetrievedChunk]:
    client = get_qdrant_client()

    existing = [c.name for c in client.get_collections().collections]
    if settings.QDRANT_COLLECTION not in existing:
        return []  # nothing uploaded yet

    query_vector = embed_texts([query])[0]

    query_filter = None
    must = []
    if filename:
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        must.append(FieldCondition(key="filename", match=MatchValue(value=filename)))
    if user_id is not None:
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        must.append(FieldCondition(key="user_id", match=MatchValue(value=user_id)))
    if must:
        from qdrant_client.models import Filter
        query_filter = Filter(must=must)

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

def get_all_chunks(filename: Optional[str] = None, user_id: Optional[int] = None) -> List[RetrievedChunk]:
    client = get_qdrant_client()

    existing = [c.name for c in client.get_collections().collections]
    if settings.QDRANT_COLLECTION not in existing:
        return []

    query_filter = None
    must = []
    if filename:
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        must.append(FieldCondition(key="filename", match=MatchValue(value=filename)))
    if user_id is not None:
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        must.append(FieldCondition(key="user_id", match=MatchValue(value=user_id)))
    if must:
        from qdrant_client.models import Filter
        query_filter = Filter(must=must)

    points, _ = client.scroll(
        collection_name=settings.QDRANT_COLLECTION,
        scroll_filter=query_filter,
        limit=1000,
    )

    return [
        RetrievedChunk(
            chunk_id=(p.payload or {}).get("chunk_id", ""),
            text=(p.payload or {}).get("text", ""),
            chunk_type=(p.payload or {}).get("chunk_type", ""),
            filename=(p.payload or {}).get("filename", ""),
            score=1.0,
        )
        for p in points
    ]