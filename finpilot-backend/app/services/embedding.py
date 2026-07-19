from functools import lru_cache
from typing import List

from fastembed import TextEmbedding
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct

from app.core.config import settings
from app.services.chunking import Chunk


@lru_cache(maxsize=1)
def get_embedder() -> TextEmbedding:
    # Cached: loading the ONNX model is the slow part, do it once per process.
    return TextEmbedding(model_name=settings.EMBEDDING_MODEL)


@lru_cache(maxsize=1)
def get_qdrant_client() -> QdrantClient:
    if settings.QDRANT_LOCAL_ONLY:
        return QdrantClient(path=settings.QDRANT_PATH)
    return QdrantClient(url=settings.QDRANT_URL)


def ensure_collection(client: QdrantClient) -> None:
    existing = [c.name for c in client.get_collections().collections]
    if settings.QDRANT_COLLECTION not in existing:
        client.create_collection(
            collection_name=settings.QDRANT_COLLECTION,
            vectors_config=VectorParams(size=settings.EMBEDDING_DIM, distance=Distance.COSINE),
        )


def embed_texts(texts: List[str]) -> List[List[float]]:
    embedder = get_embedder()
    return [vec.tolist() for vec in embedder.embed(texts)]


def store_chunks(chunks: List[Chunk]) -> int:
    if not chunks:
        return 0

    client = get_qdrant_client()
    ensure_collection(client)

    # Delete any existing points for this filename first — makes re-uploading
    # the same statement idempotent instead of accumulating duplicates.
    filenames = {c.metadata.get("filename", "") for c in chunks}
    for fname in filenames:
        if fname:
            from qdrant_client.models import Filter, FieldCondition, MatchValue
            client.delete(
                collection_name=settings.QDRANT_COLLECTION,
                points_selector=Filter(must=[FieldCondition(key="filename", match=MatchValue(value=fname))]),
            )

    texts = [c.text for c in chunks]
    vectors = embed_texts(texts)

    points = [
        PointStruct(
            id=abs(hash(c.chunk_id)) % (10**12),
            vector=vec,
            payload={
                "chunk_id": c.chunk_id,
                "chunk_type": c.chunk_type,
                "text": c.text,
                "transaction_count": c.transaction_count,
                "filename": c.metadata.get("filename", ""),
            },
        )
        for c, vec in zip(chunks, vectors)
    ]

    client.upsert(collection_name=settings.QDRANT_COLLECTION, points=points)
    return len(points)