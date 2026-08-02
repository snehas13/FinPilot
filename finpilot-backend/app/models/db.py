"""
Database setup. This User model is also the first table of Day 11's full
schema (Users, Goals, Statements, Chats, AuditLogs).
"""
from sqlalchemy import create_engine, Column, Integer, String, DateTime, Boolean, Text, inspect
from sqlalchemy.orm import declarative_base, sessionmaker
from datetime import datetime

from app.core.config import settings

engine = create_engine(
    settings.DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in settings.DATABASE_URL else {},
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(Integer, primary_key=True, index=True)
    interaction_type = Column(String, index=True, nullable=False)  # "chat" | "analyze" | "goal_plan"
    username = Column(String, index=True, nullable=True)
    user_id = Column(Integer, index=True, nullable=True)
    request_summary = Column(Text, nullable=False)
    response_summary = Column(Text, nullable=False)
    success = Column(Boolean, default=True, nullable=False)
    latency_ms = Column(Integer, nullable=False)
    error_message = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, index=True)


def _ensure_auditlog_user_id_column():
    inspector = inspect(engine)
    if "audit_logs" in inspector.get_table_names():
        columns = {col["name"] for col in inspector.get_columns("audit_logs")}
        if "user_id" not in columns:
            with engine.begin() as conn:
                conn.exec_driver_sql("ALTER TABLE audit_logs ADD COLUMN user_id INTEGER")


def init_db():
    Base.metadata.create_all(bind=engine)
    _ensure_auditlog_user_id_column()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
