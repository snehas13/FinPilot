from langgraph.graph import StateGraph, END

from app.services.agent.state import GoalPlanState
from app.services.agent.nodes import classify_goal, retrieve_context, generate_plan, validate_plan


def build_graph():
    graph = StateGraph(GoalPlanState)

    graph.add_node("classify_goal", classify_goal)
    graph.add_node("retrieve_context", retrieve_context)
    graph.add_node("generate_plan", generate_plan)
    graph.add_node("validate_plan", validate_plan)

    graph.set_entry_point("classify_goal")
    graph.add_edge("classify_goal", "retrieve_context")
    graph.add_edge("retrieve_context", "generate_plan")
    graph.add_edge("generate_plan", "validate_plan")
    # Day 6 replaces this fixed edge with a conditional edge: retry back to
    # generate_plan if is_feasible is False and retries remain.
    graph.add_edge("validate_plan", END)

    return graph.compile()


goal_plan_graph_v1 = build_graph()