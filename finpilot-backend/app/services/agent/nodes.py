"""
LangGraph nodes for the Goal Planner agent.
Day 4: classify_goal + retrieve_context.
Day 5: generate_plan + validate_plan.
format_result lands Day 6.
"""
from app.services.agent.state import GoalPlanState
from app.services.llm import extract_goal_details, generate_plan_narrative
from app.services.retrieval import get_all_chunks
from app.services.financial_summary import compute_summary

# Categories treated as fixed/non-discretionary — never suggested as cuts.
FIXED_CATEGORIES = {"Rent", "Bills & Utilities"}
MAX_RETRIES = 2


def classify_goal(state: GoalPlanState) -> GoalPlanState:
    """
    Node 1: parse the user's free-text goal into structured fields.
    This is the "agentic" decision point — everything downstream branches
    on goal_type, not a fixed template.
    """
    errors = state.get("errors", [])
    goal_text = state.get("goal_text", "")
    if not goal_text:
        return {
            **state,
            "goal_type": "other",
            "target_amount": 0.0,
            "target_months": 12,
            "errors": errors + ["classify_goal failed: missing goal_text"],
        }

    try:
        details = extract_goal_details(goal_text)
        return {
            **state,
            "goal_type": details["goal_type"],
            "target_amount": float(details["target_amount"]),
            "target_months": max(1, int(details["target_months"])),
            "errors": errors,
        }
    except Exception as e:
        return {
            **state,
            "goal_type": "other",
            "target_amount": 0.0,
            "target_months": 12,
            "errors": errors + [f"classify_goal failed: {e}"],
        }


def retrieve_context(state: GoalPlanState) -> GoalPlanState:
    """
    Node 2: pull the user's actual income/expense data so generate_plan
    has real numbers to work with instead of generic advice.
    """
    errors = state.get("errors", [])
    filename = state.get("filename")

    all_chunks = get_all_chunks(filename=filename)

    if not all_chunks:
        return {
            **state,
            "retrieved_chunks": [],
            "monthly_income": 0.0,
            "monthly_expenses": 0.0,
            "category_breakdown": [],
            "errors": errors + ["retrieve_context: no statement data found"],
        }

    summary = compute_summary(all_chunks)

    return {
        **state,
        "retrieved_chunks": [
            {"chunk_id": c.chunk_id, "text": c.text, "chunk_type": c.chunk_type}
            for c in all_chunks
        ],
        "monthly_income": summary.monthly_income,
        "monthly_expenses": summary.monthly_expenses,
        "category_breakdown": [
            {"category": c.category, "amount": c.amount, "percent_of_total": c.percent_of_total}
            for c in summary.category_breakdown
        ],
        "errors": errors,
    }


def generate_plan(state: GoalPlanState) -> GoalPlanState:
    """
    Node 3: DETERMINISTIC math for the core numbers (this is what makes the
    'no hallucinated numbers' guardrail actually true), then one LLM call
    purely for the human-readable narrative layered on top.
    """
    errors = state.get("errors", [])
    target_amount = state.get("target_amount", 0.0)
    target_months = max(1, state.get("target_months", 12))
    monthly_income = state.get("monthly_income", 0.0)
    monthly_expenses = state.get("monthly_expenses", 0.0)
    category_breakdown = state.get("category_breakdown", [])

    monthly_saving_required = round(target_amount / target_months, 2)
    surplus = round(monthly_income - monthly_expenses, 2)
    gap = round(monthly_saving_required - surplus, 2)

    recommendations = []
    unaddressed_gap = 0.0
    if gap > 0:
        discretionary = [c for c in category_breakdown if c["category"] not in FIXED_CATEGORIES]
        remaining_gap = gap
        for cat in discretionary[:2]:
            suggested_cut = min(round(cat["amount"] * 0.3, 0), remaining_gap)
            if suggested_cut > 0:
                recommendations.append(f"Reduce {cat['category']}: Rs. {suggested_cut:.0f}/month")
                remaining_gap -= suggested_cut

        if remaining_gap > 0:
            # Cap the income-increase ask at 20% of income — beyond that it's not
            # a credible recommendation, so we don't pretend it closes the gap.
            income_increase_cap = round(monthly_income * 0.2, 0)
            income_increase = min(remaining_gap, income_increase_cap)
            if income_increase > 0:
                recommendations.append(f"Increase Income / Side Hustle: Rs. {income_increase:.0f}/month")
                remaining_gap -= income_increase

        unaddressed_gap = round(max(0.0, remaining_gap), 2)
    else:
        recommendations = ["Your current surplus already covers this goal — stay consistent."]

    try:
        narrative = generate_plan_narrative(
            goal_type=state.get("goal_type", "other"),
            target_amount=target_amount,
            target_months=target_months,
            monthly_saving_required=monthly_saving_required,
            surplus=surplus,
            recommendations="; ".join(recommendations),
        )
    except Exception as e:
        narrative = ""
        errors = errors + [f"generate_plan narrative failed: {e}"]

    return {
        **state,
        "target_months": target_months,
        "monthly_saving_required": monthly_saving_required,
        "draft_recommendations": recommendations,
        "plan_narrative": narrative,
        "unaddressed_gap": unaddressed_gap,
        "errors": errors,
    }


def validate_plan(state: GoalPlanState) -> GoalPlanState:
    """
    Node 4: the guardrail step. A plan is feasible only if generate_plan's
    unaddressed_gap is (approximately) zero. If not, and retries remain,
    this signals the graph (Day 6) to loop back to generate_plan with an
    extended timeline instead of shipping an unachievable plan.
    """
    unaddressed_gap = state.get("unaddressed_gap", 0.0)
    retry_count = state.get("retry_count", 0)

    is_feasible = unaddressed_gap <= 1.0  # small tolerance for rounding

    if is_feasible:
        notes = "Plan is achievable with the recommended adjustments."
    elif retry_count < MAX_RETRIES:
        notes = (
            f"Rs. {unaddressed_gap:.0f}/month still unaddressed after realistic cuts "
            f"and income-increase caps. Will retry with an extended timeline."
        )
    else:
        notes = (
            f"Goal not achievable in the requested timeframe even after {MAX_RETRIES} "
            f"adjustment attempts (Rs. {unaddressed_gap:.0f}/month still short). "
            f"Recommend extending the timeline or reducing the target amount."
        )

    return {
        **state,
        "is_feasible": is_feasible,
        "validation_notes": notes,
        "retry_count": retry_count + (0 if is_feasible else 1),
    }


def format_result(state: GoalPlanState) -> GoalPlanState:
    raise NotImplementedError("Built Day 6")