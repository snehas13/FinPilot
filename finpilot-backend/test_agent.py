from app.services.pdf_extraction import extract_pdf
from app.services.chunking import chunk_extraction
from app.services.embedding import store_chunks
from app.services.agent.graph import goal_plan_graph_v1
from app.services.guardrails import check_numeric_consistency, check_advice_boundary, check_context_sufficiency

# Step 1: make sure there's statement data in Qdrant to retrieve against
# (skip this block if you've already uploaded a statement via the running server)
result = extract_pdf("tests/sample_statement.pdf", "sample_statement.pdf")
chunks = chunk_extraction(result)
store_chunks(chunks)
print("Statement embedded and stored.\n")

# Step 2: run the agent graph
initial_state = {
    "goal_text": "I want to save Rs 1,50,000 for a vacation in 12 months",
    "filename": "sample_statement.pdf",
    "errors": [],
}

final_state = goal_plan_graph_v1.invoke(initial_state)  # type: ignore[arg-type]

print("goal_type:", final_state["goal_type"])
print("target_amount:", final_state["target_amount"])
print("target_months:", final_state["target_months"])
print("monthly_income:", final_state["monthly_income"])
print("monthly_expenses:", final_state["monthly_expenses"])
print("retrieved_chunks count:", len(final_state["retrieved_chunks"]))
print("errors:", final_state["errors"])
print("monthly_saving_required:", final_state["monthly_saving_required"])
print("recommendations:", final_state["draft_recommendations"])
print("is_feasible:", final_state["is_feasible"])
print("narrative:", final_state["plan_narrative"])
print("monthly_income:", final_state["monthly_income"])
print("monthly_expenses:", final_state["monthly_expenses"])

source = "12 Jun 2024 Zomato Order #4821 Rs. 530.00"
r = check_numeric_consistency("You spent Rs. 9999 on Zomato.", source)
print(r.passed, r.flagged_numbers)  # False ['9999']

r2 = check_advice_boundary("You should invest in the HDFC Top 100 fund.")
print(r2.passed, r2.matched_phrases)  # False ['You should invest']

print(check_context_sufficiency(0))  # False