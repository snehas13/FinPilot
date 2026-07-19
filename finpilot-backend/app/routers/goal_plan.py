from fastapi import APIRouter

from app.services.agent.graph import GoalPlanState, goal_plan_graph_v1
from app.models.schemas import GoalPlanRequest, GoalPlanResponse

router = APIRouter()


@router.post("", response_model=GoalPlanResponse)
async def goal_plan(req: GoalPlanRequest):
    initial_state: GoalPlanState = {
        "goal_text": req.goal_text,
        "filename": req.filename,
        "monthly_income_override": req.monthly_income_override,
        "errors": [],
        "retry_count": 0,
    }

    final_state = goal_plan_graph_v1.invoke(initial_state)
    plan = final_state["final_plan"]

    return GoalPlanResponse(
        goal_type=plan["goal_type"],
        target_amount=plan["target_amount"],
        target_months=plan["target_months"],
        monthly_saving_required=plan["monthly_saving_required"],
        recommendations=plan["recommendations"],
        narrative=plan["narrative"],
        is_feasible=plan["is_feasible"],
        confidence_level=plan["confidence_level"],
        validation_notes=plan["validation_notes"],
        retry_count=plan["retry_count"],
    )