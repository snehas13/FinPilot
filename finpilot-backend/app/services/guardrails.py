"""
Guardrails — one function per check, called from every LLM-facing endpoint
(/chat, /analyze, and the Goal Planner agent's generate_plan node) rather
than baked into any single place.
"""
import re
from dataclasses import dataclass
from typing import List


# ---------------------------------------------------------------------------
# Guardrail 1: no hallucinated numbers
# ---------------------------------------------------------------------------

NUMBER_RE = re.compile(r"(?:rs\.?|inr|₹)?\s*(?P<num>[\d,]+(?:\.\d+)?)%?", re.IGNORECASE)


def _extract_numbers(text: str) -> List[str]:
    """
    Extract just the numeric portion of each match (via the 'num' group),
    NOT the whole match — otherwise a currency prefix like "Rs." contributes
    its own period and corrupts the number (e.g. "Rs. 9999" -> ".9999").
    """
    return [m.group("num").replace(",", "") for m in NUMBER_RE.finditer(text)]


@dataclass
class NumericCheckResult:
    passed: bool
    flagged_numbers: List[str]


def check_numeric_consistency(response_text: str, source_text: str) -> NumericCheckResult:
    """
    Every number the LLM states must also appear somewhere in the source
    text it was given. Compares by FLOAT VALUE (rounded to whole rupees),
    not exact string — otherwise "530" wouldn't match "530.00".

    Small numbers (below 10) are skipped — they're usually month counts or
    transaction counts, not financial figures, and are a high false-positive source.
    """
    def to_values(nums: List[str]) -> set:
        values = set()
        for n in nums:
            try:
                values.add(round(float(n)))
            except ValueError:
                continue
        return values

    response_values = to_values(_extract_numbers(response_text))
    source_values = to_values(_extract_numbers(source_text))

    flagged = [
        str(v) for v in response_values
        if v >= 10 and v not in source_values
    ]

    return NumericCheckResult(passed=len(flagged) == 0, flagged_numbers=flagged)


# ---------------------------------------------------------------------------
# Guardrail 2: no licensed financial advice
# ---------------------------------------------------------------------------

ADVICE_BOUNDARY_PATTERNS = [
    r"\binvest\s+in\s+(?:this|the)?\s*\w*\s*(?:fund|stock|share|bond)",
    r"\bbuy\s+(?:this|these)?\s*\w*\s*(?:fund|stock|share|shares|bonds)",
    r"\bsell\s+(?:this|these)?\s*\w*\s*(?:fund|stock|share|shares|bonds)",
    r"\byou should (?:invest|buy|sell|purchase)\b",
    r"\bI recommend (?:buying|investing in|selling)\b",
    r"\bguaranteed returns?\b",
    r"\b(?:mutual fund|SIP|ETF) (?:you should|to buy|to invest)\b",
]
ADVICE_BOUNDARY_RE = re.compile("|".join(ADVICE_BOUNDARY_PATTERNS), re.IGNORECASE)

ADVICE_REWRITE_MESSAGE = (
    "I can't recommend specific financial products or make buy/sell/invest calls — "
    "that requires a licensed financial advisor. What I can help with is understanding "
    "your spending patterns and general savings strategies. Would you like me to break "
    "down your budget instead?"
)


@dataclass
class AdviceBoundaryResult:
    passed: bool
    matched_phrases: List[str]
    safe_response: str


def check_advice_boundary(response_text: str) -> AdviceBoundaryResult:
    matches = ADVICE_BOUNDARY_RE.findall(response_text)
    passed = len(matches) == 0
    return AdviceBoundaryResult(
        passed=passed,
        matched_phrases=[m for m in matches if m],
        safe_response=response_text if passed else ADVICE_REWRITE_MESSAGE,
    )


# ---------------------------------------------------------------------------
# Guardrail 3: refuse when there's not enough context to answer
# ---------------------------------------------------------------------------

INSUFFICIENT_CONTEXT_MESSAGE = (
    "I don't have enough information to answer that — please upload a statement first."
)


def check_context_sufficiency(retrieved_chunk_count: int, min_chunks: int = 1) -> bool:
    """
    Returns True if there's enough retrieved context to attempt an answer.
    Callers should short-circuit BEFORE calling the LLM when this is False.
    """
    return retrieved_chunk_count >= min_chunks


# ---------------------------------------------------------------------------
# Combined wrapper — call this from /chat and /analyze after generation
# ---------------------------------------------------------------------------

@dataclass
class GuardrailReport:
    final_text: str
    numeric_check: NumericCheckResult
    advice_check: AdviceBoundaryResult


def apply_response_guardrails(response_text: str, source_text: str) -> GuardrailReport:
    """
    Runs Guardrails 1 and 2 on an already-generated response. Guardrail 3
    is checked separately, before generation, since it should prevent the
    LLM call entirely rather than filter its output.
    """
    numeric_check = check_numeric_consistency(response_text, source_text)
    advice_check = check_advice_boundary(response_text)

    final_text = response_text
    if not advice_check.passed:
        final_text = advice_check.safe_response
    elif not numeric_check.passed:
        final_text = (
            "I found some figures in your data but want to double-check them before "
            "sharing specifics — could you rephrase your question, or ask about a "
            "specific transaction category instead?"
        )

    return GuardrailReport(final_text=final_text, numeric_check=numeric_check, advice_check=advice_check)