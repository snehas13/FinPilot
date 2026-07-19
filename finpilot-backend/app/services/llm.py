import json
import re

from app.core.config import settings

RAG_SYSTEM_PROMPT = """You are FinPilot AI, a financial coach assistant.
Answer the user's question using ONLY the statement excerpts provided below.
If the excerpts don't contain enough information to answer, say so explicitly
rather than guessing. Cite which chunk(s) you used by their chunk_id.
Never recommend specific financial products or give licensed financial advice.

Statement excerpts:
{context}
"""


def generate_answer(query: str, context: str) -> str:
    from groq import Groq
    from typing import Any, cast

    client = Groq(api_key=settings.GROQ_API_KEY)
    response = client.chat.completions.create(
        model=settings.GROQ_MODEL,
        messages=cast(Any, [
            {"role": "system", "content": RAG_SYSTEM_PROMPT.format(context=context)},
            {"role": "user", "content": query},
        ]),
        temperature=0.2,
        max_tokens=500,
    )
    return response.choices[0].message.content or ""

CHAT_SYSTEM_PROMPT = """You are FinPilot AI, a friendly financial coach.
You have access to the user's financial summary and relevant statement excerpts below.
Use them to answer naturally, referencing real numbers where relevant.
If something isn't covered by the data provided, say so rather than guessing.
Never recommend specific financial products or give licensed financial advice —
offer general planning guidance instead.

Financial context:
{context}
"""


def chat_completion(messages: list, context: str) -> str:
    from groq import Groq
    from typing import Any, cast

    client = Groq(api_key=settings.GROQ_API_KEY)
    full_messages = [
        {"role": "system", "content": CHAT_SYSTEM_PROMPT.format(context=context)}
    ] + messages

    response = client.chat.completions.create(
        model=settings.GROQ_MODEL,
        messages=cast(Any, full_messages),
        temperature=0.3,
        max_tokens=500,
    )
    return response.choices[0].message.content or ""

GOAL_CLASSIFY_PROMPT = """Extract structured goal details from the user's message.
Return ONLY a JSON object with these exact keys, no other text:
{{
  "goal_type": one of "vacation", "emergency_fund", "purchase", "retirement", "other",
  "target_amount": number (rupees, no commas or currency symbols),
  "target_months": integer (number of months to reach the goal)
}}

User message: "{goal_text}"
"""


def _regex_extract_goal_details(goal_text: str) -> dict:
    """Offline fallback for LLM_BACKEND=mock — lets you test the graph without Groq."""
    amount_match = re.search(r"(?:rs\.?|inr|₹)\s*([\d,]+)", goal_text, re.IGNORECASE)
    target_amount = float(amount_match.group(1).replace(",", "")) if amount_match else 0.0

    months_match = re.search(r"(\d+)\s*(?:month|months)", goal_text, re.IGNORECASE)
    years_match = re.search(r"(\d+)\s*(?:year|years)", goal_text, re.IGNORECASE)
    if months_match:
        target_months = int(months_match.group(1))
    elif years_match:
        target_months = int(years_match.group(1)) * 12
    else:
        target_months = 12

    text_lower = goal_text.lower()
    if "vacation" in text_lower or "trip" in text_lower or "travel" in text_lower:
        goal_type = "vacation"
    elif "emergency" in text_lower:
        goal_type = "emergency_fund"
    elif "retire" in text_lower:
        goal_type = "retirement"
    elif "buy" in text_lower or "purchase" in text_lower:
        goal_type = "purchase"
    else:
        goal_type = "other"

    return {"goal_type": goal_type, "target_amount": target_amount, "target_months": target_months}


def _groq_extract_goal_details(goal_text: str) -> dict:
    from groq import Groq

    client = Groq(api_key=settings.GROQ_API_KEY)
    response = client.chat.completions.create(
        model=settings.GROQ_MODEL,
        messages=[{"role": "user", "content": GOAL_CLASSIFY_PROMPT.format(goal_text=goal_text)}],
        temperature=0.0,
        max_tokens=200,
    )
    raw = (response.choices[0].message.content or "").strip()
    raw = re.sub(r"^```(?:json)?|```$", "", raw.strip(), flags=re.MULTILINE).strip()
    return json.loads(raw)


def extract_goal_details(goal_text: str) -> dict:
    if settings.LLM_BACKEND == "mock":
        return _regex_extract_goal_details(goal_text)
    return _groq_extract_goal_details(goal_text)

PLAN_NARRATIVE_PROMPT = """You are a financial coach writing a short, encouraging
explanation for a savings plan. Use ONLY the numbers given below — do not invent
any figures. Keep it to 2-3 sentences, plain and specific.

Goal type: {goal_type}
Target amount: Rs. {target_amount}
Timeline: {target_months} months
Required monthly saving: Rs. {monthly_saving_required}
Current monthly surplus: Rs. {surplus}
Recommended adjustments: {recommendations}
"""


def generate_plan_narrative(**kwargs) -> str:
    from groq import Groq

    client = Groq(api_key=settings.GROQ_API_KEY)
    response = client.chat.completions.create(
        model=settings.GROQ_MODEL,
        messages=[{"role": "user", "content": PLAN_NARRATIVE_PROMPT.format(**kwargs)}],
        temperature=0.4,
        max_tokens=200,
    )
    return response.choices[0].message.content or ""