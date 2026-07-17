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

    client = Groq(api_key=settings.GROQ_API_KEY)
    response = client.chat.completions.create(
        model=settings.GROQ_MODEL,
        messages=[
            {"role": "system", "content": RAG_SYSTEM_PROMPT.format(context=context)},
            {"role": "user", "content": query},
        ],
        temperature=0.2,
        max_tokens=500,
    )
    return response.choices[0].message.content or ""