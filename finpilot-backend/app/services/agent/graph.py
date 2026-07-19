"""
Goal Planner agent graph — full scope.

    classify_goal -> retrieve_context -> generate_plan -> validate_plan
                                               ^                |
                                               |   (infeasible, retries left)
                                               +---- extend_timeline <----+
                                                                          |
                                          (feasible, OR retries exhausted)|
                                                          v
                                                    format_result -> END
"""
from langgraph.graph import StateGraph, END

from app.services.agent.state import GoalPlanState
from app.services.agent.nodes import (
    classify_goal,
    retrieve_context,
    generate_plan,
    validate_plan,
    extend_timeline,
    format_result,
)

MAX_RETRIES = 2


def _route_after_validation(state: GoalPlanState) -> str:
    """Router for the conditional edge — returns the NAME of the next node."""
    is_feasible = state.get("is_feasible", False)
    retry_count = state.get("retry_count", 0)

    if is_feasible:
        return "format_result"
    if retry_count < MAX_RETRIES:
        return "extend_timeline"
    return "format_result"  # retries exhausted — ship the honest "not achievable" result


def build_graph():
    graph = StateGraph(GoalPlanState)

    graph.add_node("classify_goal", classify_goal)
    graph.add_node("retrieve_context", retrieve_context)
    graph.add_node("generate_plan", generate_plan)
    graph.add_node("validate_plan", validate_plan)
    graph.add_node("extend_timeline", extend_timeline)
    graph.add_node("format_result", format_result)

    graph.set_entry_point("classify_goal")
    graph.add_edge("classify_goal", "retrieve_context")
    graph.add_edge("retrieve_context", "generate_plan")
    graph.add_edge("generate_plan", "validate_plan")

    graph.add_conditional_edges(
        "validate_plan",
        _route_after_validation,
        {
            "format_result": "format_result",
            "extend_timeline": "extend_timeline",
        },
    )

    graph.add_edge("extend_timeline", "generate_plan")  # the actual retry loop
    graph.add_edge("format_result", END)

    return graph.compile()


goal_plan_graph_v1 = build_graph()