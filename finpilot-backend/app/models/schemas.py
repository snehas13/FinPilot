from pydantic import BaseModel
from typing import List, Optional


class TransactionChunk(BaseModel):
    chunk_id: str
    statement_filename: str
    text: str
    transaction_count: int


class UploadResponse(BaseModel):
    filename: str
    status: str
    pages_extracted: int
    chunks_created: int
    points_stored: int = 0
    chunk_preview: List[TransactionChunk]
    error: Optional[str] = None

class AnalyzeRequest(BaseModel):
    query: str
    filename: Optional[str] = None
    top_k: int = 5


class AnalyzeResponse(BaseModel):
    answer: str
    sources: List[str]
    retrieved_chunks: int

class ChatMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    filename: Optional[str] = None
    top_k: int = 5


class ChatResponse(BaseModel):
    answer: str
    sources: List[str]


class CategorySpendOut(BaseModel):
    category: str
    amount: float
    percent_of_total: float


class FinancialSummaryResponse(BaseModel):
    monthly_income: float
    monthly_expenses: float
    surplus: float
    savings_rate_percent: float
    health_score: int
    biggest_category: Optional[CategorySpendOut]
    category_breakdown: List[CategorySpendOut]
    transaction_count: int
    income_is_estimated: bool

class GoalPlanRequest(BaseModel):
    goal_text: str
    filename: Optional[str] = None
    monthly_income_override: Optional[float] = None

class GoalPlanResponse(BaseModel):
    goal_type: str
    target_amount: float
    target_months: int
    monthly_saving_required: float
    recommendations: List[str]
    narrative: str
    is_feasible: bool
    confidence_level: str
    validation_notes: str
    retry_count: int

class SignupRequest(BaseModel):
    username: str
    password: str


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str