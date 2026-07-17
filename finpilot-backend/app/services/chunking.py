import re
import uuid
from dataclasses import dataclass, field
from typing import List

from app.core.config import settings
from app.services.pdf_extraction import ExtractionResult

# Matches lines like:
#   12 Jun 2024   Zomato Order #4821         Rs. 530.00
#   06/12/2024    SWIGGY*ORDER               INR 840.00
TRANSACTION_LINE_RE = re.compile(
    r"""
    (?P<date>\d{1,2}[/\-\s][A-Za-z]{3}[/\-\s]?\d{2,4}|\d{1,2}/\d{1,2}/\d{2,4})
    \s+
    (?P<description>.+?)
    \s+
    (?P<amount>[-+]?\s?(?:Rs\.?|INR|₹)?\s?[\d,]+\.\d{2})
    \s*$
    """,
    re.VERBOSE | re.IGNORECASE,
)


@dataclass
class Chunk:
    chunk_id: str
    text: str
    chunk_type: str  # "transactions" | "summary"
    transaction_count: int = 0
    metadata: dict = field(default_factory=dict)


def _is_transaction_line(line: str) -> bool:
    return bool(TRANSACTION_LINE_RE.search(line.strip()))


def chunk_extraction(result: ExtractionResult) -> List[Chunk]:
    chunks: List[Chunk] = []
    max_lines = settings.CHUNK_MAX_LINES

    txn_buffer: List[str] = []
    summary_buffer: List[str] = []

    def flush_txn():
        if txn_buffer:
            header = f"[Statement: {result.filename} | Transactions {len(chunks) + 1}]"
            text = header + "\n" + "\n".join(txn_buffer)
            chunks.append(
                Chunk(
                    chunk_id=str(uuid.uuid4())[:8],
                    text=text,
                    chunk_type="transactions",
                    transaction_count=len(txn_buffer),
                    metadata={"filename": result.filename},
                )
            )
            txn_buffer.clear()

    def flush_summary():
        if summary_buffer:
            header = f"[Statement: {result.filename} | Summary/Header]"
            text = header + "\n" + "\n".join(summary_buffer)
            chunks.append(
                Chunk(
                    chunk_id=str(uuid.uuid4())[:8],
                    text=text,
                    chunk_type="summary",
                    transaction_count=0,
                    metadata={"filename": result.filename},
                )
            )
            summary_buffer.clear()

    for page in result.pages:
        for raw_line in page.text.splitlines():
            line = raw_line.strip()
            if not line:
                continue
            if _is_transaction_line(line):
                if summary_buffer:
                    flush_summary()
                txn_buffer.append(line)
                if len(txn_buffer) >= max_lines:
                    flush_txn()
            else:
                if txn_buffer:
                    flush_txn()
                summary_buffer.append(line)

    flush_txn()
    flush_summary()

    return chunks