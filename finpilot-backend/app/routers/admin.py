from datetime import datetime, timedelta
from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models.db import AuditLog, get_db
from app.models.schemas import AuditLogOut, PaginatedLogsResponse, AdminMetricsResponse, DailyTrendPoint

router = APIRouter()


@router.get("/logs", response_model=PaginatedLogsResponse)
async def get_logs(
    interaction_type: Optional[str] = Query(None, description="Filter: chat | analyze | goal_plan"),
    limit: int = Query(20, le=100),
    offset: int = 0,
    db: Session = Depends(get_db),
):
    query = db.query(AuditLog)
    if interaction_type:
        query = query.filter(AuditLog.interaction_type == interaction_type)

    total = query.count()
    rows = query.order_by(AuditLog.created_at.desc()).offset(offset).limit(limit).all()

    log_items = []
    for row in rows:
        payload = {
            "id": getattr(row, "id", None),
            "interaction_type": getattr(row, "interaction_type", ""),
            "username": getattr(row, "username", None),
            "request_summary": getattr(row, "request_summary", ""),
            "response_summary": getattr(row, "response_summary", ""),
            "success": getattr(row, "success", True),
            "latency_ms": getattr(row, "latency_ms", 0),
            "error_message": getattr(row, "error_message", None),
            "created_at": getattr(row, "created_at", None),
        }
        created_at = payload["created_at"]
        if created_at is not None:
            created_at_value = created_at.isoformat() if hasattr(created_at, "isoformat") else str(created_at)
        else:
            created_at_value = ""

        log_items.append(
            AuditLogOut(
                id=int(payload["id"] or 0),
                interaction_type=str(payload["interaction_type"] or ""),
                username=payload["username"] if payload["username"] is not None else None,
                request_summary=str(payload["request_summary"] or ""),
                response_summary=str(payload["response_summary"] or ""),
                success=bool(payload["success"]),
                latency_ms=int(payload["latency_ms"] or 0),
                error_message=payload["error_message"] if payload["error_message"] is not None else None,
                created_at=created_at_value,
            )
        )

    return PaginatedLogsResponse(total=total, logs=log_items)


@router.get("/metrics", response_model=AdminMetricsResponse)
async def get_metrics(db: Session = Depends(get_db)):
    total_requests = db.query(AuditLog).count()

    by_type_rows = (
        db.query(AuditLog.interaction_type, func.count(AuditLog.id))
        .group_by(AuditLog.interaction_type).all()
    )
    by_type = {t: c for t, c in by_type_rows}

    avg_latency = db.query(func.avg(AuditLog.latency_ms)).scalar() or 0
    success_count = db.query(AuditLog).filter(AuditLog.success == True).count()  # noqa: E712
    success_rate = (success_count / total_requests * 100) if total_requests > 0 else 100.0

    seven_days_ago = datetime.utcnow() - timedelta(days=7)
    trend_rows = (
        db.query(func.date(AuditLog.created_at), func.count(AuditLog.id))
        .filter(AuditLog.created_at >= seven_days_ago)
        .group_by(func.date(AuditLog.created_at))
        .order_by(func.date(AuditLog.created_at)).all()
    )
    trend = [DailyTrendPoint(date=str(d), count=c) for d, c in trend_rows]

    return AdminMetricsResponse(
        total_requests=total_requests, requests_by_type=by_type,
        avg_latency_ms=round(avg_latency, 1), success_rate_percent=round(success_rate, 1),
        last_7_days_trend=trend,
    )