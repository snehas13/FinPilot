import re
from dataclasses import dataclass
from typing import List, Optional

from app.services.chunking import TRANSACTION_LINE_RE
from app.services.retrieval import RetrievedChunk

CATEGORY_KEYWORDS = {
    "Dining Out":     ["zomato", "swiggy", "cafe", "restaurant", "dining", "coffee"],
    "Groceries":      ["basket", "grocery", "mart", "instamart"],
    "Subscriptions":  ["netflix", "spotify", "prime", "subscription", "hotstar"],
    "Transport":      ["uber", "ola", "fuel", "petrol", "cab"],
    "Shopping":       ["amazon", "flipkart", "myntra", "purchase"],
    "Bills & Utilities": ["electricity", "bill", "recharge", "utility"],
    "Rent":           ["rent"],
    "Entertainment":  ["movie", "cinema", "bookmyshow", "tickets"],
}

INCOME_LINE_RE = re.compile(
    r"(?:monthly\s+income|salary|opening\s+balance)\s*[:\-]?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+\.\d{2}|[\d,]+)",
    re.IGNORECASE,
)


@dataclass
class CategorySpend:
    category: str
    amount: float
    percent_of_total: float


@dataclass
class FinancialSummary:
    monthly_income: float
    monthly_expenses: float
    surplus: float
    savings_rate_percent: float
    health_score: int
    biggest_category: Optional[CategorySpend]
    category_breakdown: List[CategorySpend]
    transaction_count: int
    income_is_estimated: bool


def _extract_amount(amount_str: str) -> float:
    return float(amount_str.replace(",", "").replace("Rs.", "").replace("Rs", "").strip())


def _categorize(description: str) -> str:
    desc_lower = description.lower()
    for category, keywords in CATEGORY_KEYWORDS.items():
        if any(kw in desc_lower for kw in keywords):
            return category
    return "Other"


def compute_summary(chunks: List[RetrievedChunk]) -> FinancialSummary:
    category_totals: dict = {}
    total_expenses = 0.0
    txn_count = 0
    income: Optional[float] = None

    for chunk in chunks:
        if chunk.chunk_type == "transactions":
            for line in chunk.text.splitlines():
                match = TRANSACTION_LINE_RE.search(line.strip())
                if not match:
                    continue
                amount = _extract_amount(match.group("amount"))
                category = _categorize(match.group("description"))
                category_totals[category] = category_totals.get(category, 0.0) + amount
                total_expenses += amount
                txn_count += 1
        elif chunk.chunk_type == "summary":
            income_match = INCOME_LINE_RE.search(chunk.text)
            if income_match and income is None:
                income = _extract_amount(income_match.group(1))

    income_is_estimated = income is None
    if income is None:
        income = total_expenses * 1.25 if total_expenses > 0 else 0.0

    surplus = income - total_expenses
    savings_rate = (surplus / income * 100) if income > 0 else 0.0

    score = 50 + (savings_rate * 1.2)
    if surplus < 0:
        score -= 20
    health_score = max(0, min(100, round(score)))

    breakdown = sorted(
        [
            CategorySpend(
                category=cat,
                amount=amt,
                percent_of_total=(amt / total_expenses * 100) if total_expenses > 0 else 0.0,
            )
            for cat, amt in category_totals.items()
        ],
        key=lambda c: c.amount,
        reverse=True,
    )

    return FinancialSummary(
        monthly_income=round(income, 2),
        monthly_expenses=round(total_expenses, 2),
        surplus=round(surplus, 2),
        savings_rate_percent=round(savings_rate, 1),
        health_score=health_score,
        biggest_category=breakdown[0] if breakdown else None,
        category_breakdown=breakdown,
        transaction_count=txn_count,
        income_is_estimated=income_is_estimated,
    )