"""
Shared state passed between every node in the Goal Planner agent graph.
This is what makes it agentic rather than a single prompt: each node reads
and writes to this state, and the graph decides which node runs next based
on what's in it.
"""
from typing import TypedDict, Optional, List


class GoalPlanState(TypedDict, total=False):
    # --- input ---
    goal_text: str            # raw user input, e.g. "I want to save 1,50,000 for a vacation"
    filename: Optional[str]   # which uploaded statement to use for context

    # --- set by classify_goal ---
    goal_type: str            # "vacation" | "emergency_fund" | "purchase" | "retirement" | "other"
    target_amount: float
    target_months: int

    # --- set by retrieve_context ---
    retrieved_chunks: List[dict]   # [{chunk_id, text, chunk_type}, ...]
    monthly_income: float
    monthly_expenses: float
    category_breakdown: List[dict]  # [{category, amount, percent_of_total}, ...] sorted desc

    # --- set by generate_plan (Day 5) ---
    monthly_saving_required: float
    draft_recommendations: List[str]
    plan_narrative: str
    unaddressed_gap: float   # what's left after cuts + capped income increase; >0 means infeasible

    # --- set by validate_plan (Day 5-6) ---
    is_feasible: bool
    validation_notes: str
    retry_count: int

    # --- set by format_result (Day 6) ---
    final_plan: dict

    # --- error tracking, used throughout ---
    errors: List[str]