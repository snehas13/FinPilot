from pydantic import BaseModel
from typing import List, Optional


class TransactionChunk(BaseModel):
    chunk_id: str
    statement_filename: str
    text: str
    transaction_count: int


class UploadResponse(BaseModel):
    filename: str
    status: str
    pages_extracted: int
    chunks_created: int
    points_stored: int = 0
    chunk_preview: List[TransactionChunk]
    error: Optional[str] = None

class AnalyzeRequest(BaseModel):
    query: str
    filename: Optional[str] = None
    top_k: int = 5


class AnalyzeResponse(BaseModel):
    answer: str
    sources: List[str]
    retrieved_chunks: int