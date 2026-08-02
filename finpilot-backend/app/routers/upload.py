import shutil
import uuid
from pathlib import Path
from typing import cast

from fastapi import APIRouter, UploadFile, File, HTTPException, Depends

from app.core.config import settings
from app.core.security import get_current_user
from app.models.db import User
from app.services.pdf_extraction import extract_pdf
from app.services.chunking import chunk_extraction
from app.models.schemas import UploadResponse, TransactionChunk
from app.services.embedding import store_chunks

router = APIRouter()


@router.post("", response_model=UploadResponse)
async def upload_statement(file: UploadFile = File(...), current_user: User = Depends(get_current_user)):
    filename = file.filename or ""
    if not filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF statements are supported.")

    dest_path = settings.UPLOAD_DIR / f"{uuid.uuid4().hex[:8]}_{filename}"
    with open(dest_path, "wb") as f:
        shutil.copyfileobj(file.file, f)

    try:
        extraction = extract_pdf(str(dest_path), filename)
        chunks = chunk_extraction(extraction)
        points_stored = store_chunks(
            chunks,
            user_id=cast(int, current_user.id),
            username=cast(str, current_user.username),
        )
    except ValueError as e:
        return UploadResponse(
            filename=filename,
            status="error",
            pages_extracted=0,
            chunks_created=0,
            points_stored=0,
            chunk_preview=[],
            error=str(e),
        )

    preview = [
        TransactionChunk(
            chunk_id=c.chunk_id,
            statement_filename=filename,
            text=c.text[:300],
            transaction_count=c.transaction_count,
        )
        for c in chunks[:5]
    ]

    return UploadResponse(
        filename=filename,
        status="extracted",
        pages_extracted=extraction.page_count,
        chunks_created=len(chunks),
        chunk_preview=preview,
        points_stored=points_stored
    )