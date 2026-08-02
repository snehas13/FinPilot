import time

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.models.db import User, get_db
from app.services.agent.graph import GoalPlanState, goal_plan_graph_v1
from app.models.schemas import GoalPlanRequest, GoalPlanResponse
from app.services.audit import log_interaction

router = APIRouter()


@router.post("", response_model=GoalPlanResponse)
async def goal_plan(
    req: GoalPlanRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    start = time.time()
    initial_state: GoalPlanState = {
        "goal_text": req.goal_text,
        "filename": req.filename,
        "user_id": current_user.id,
        "monthly_income_override": req.monthly_income_override,
        "errors": [],
        "retry_count": 0,
    }

    final_state = goal_plan_graph_v1.invoke(initial_state)
    plan = final_state["final_plan"]
    latency_ms = int((time.time() - start) * 1000)
    log_interaction(
        db,
        "goal_plan",
        req.goal_text,
        plan["narrative"],
        latency_ms,
        success=True,
        username=current_user.username,
        user_id=current_user.id,
    )

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