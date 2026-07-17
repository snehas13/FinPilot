import pdfplumber
from dataclasses import dataclass, field
from typing import List, Sequence


@dataclass
class ExtractedPage:
    page_number: int
    text: str
    tables: Sequence[Sequence[Sequence[str | None]]] = field(default_factory=list)


@dataclass
class ExtractionResult:
    filename: str
    pages: List[ExtractedPage]

    @property
    def page_count(self) -> int:
        return len(self.pages)

    @property
    def full_text(self) -> str:
        return "\n".join(p.text for p in self.pages if p.text)


def extract_pdf(filepath: str, filename: str) -> ExtractionResult:
    pages: List[ExtractedPage] = []

    try:
        with pdfplumber.open(filepath) as pdf:
            for i, page in enumerate(pdf.pages):
                text = page.extract_text() or ""
                tables = page.extract_tables() or []
                pages.append(ExtractedPage(page_number=i + 1, text=text, tables=tables))
    except Exception as e:
        raise ValueError(f"Could not extract '{filename}': {e}") from e

    if not pages:
        raise ValueError(f"'{filename}' has no readable pages.")

    return ExtractionResult(filename=filename, pages=pages)