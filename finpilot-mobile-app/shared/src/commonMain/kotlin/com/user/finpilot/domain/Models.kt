package com.user.finpilot.domain

import kotlinx.serialization.Serializable

@Serializable
data class TransactionChunk(
    val chunk_id: String,
    val statement_filename: String,
    val text: String,
    val transaction_count: Int,
)

@Serializable
data class UploadResponse(
    val filename: String = "",
    val status: String = "error",
    val pages_extracted: Int = 0,
    val chunks_created: Int = 0,
    val points_stored: Int = 0,
    val chunk_preview: List<TransactionChunk> = emptyList(),
    val error: String? = null,
)

@Serializable
data class AnalyzeRequest(
    val query: String,
    val filename: String? = null,
    val top_k: Int = 5,
)

@Serializable
data class AnalyzeResponse(
    val answer: String,
    val sources: List<String>,
    val retrieved_chunks: Int,
)

@Serializable
data class ChatMessage(
    val role: String,   // "user" | "assistant"
    val content: String,
)

@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val filename: String? = null,
    val top_k: Int = 5,
)

@Serializable
data class ChatResponse(
    val answer: String,
    val sources: List<String>,
)

@Serializable
data class CategorySpend(
    val category: String,
    val amount: Double,
    val percent_of_total: Double,
)

@Serializable
data class FinancialSummaryResponse(
    val monthly_income: Double = 0.0,
    val monthly_expenses: Double = 0.0,
    val surplus: Double = 0.0,
    val savings_rate_percent: Double = 0.0,
    val health_score: Int = 0,
    val biggest_category: CategorySpend? = null,
    val category_breakdown: List<CategorySpend> = emptyList(),
    val transaction_count: Int = 0,
    val income_is_estimated: Boolean = false,
)

@Serializable
data class GoalPlanRequest(
    val goal_text: String,
    val filename: String? = null,
    val monthly_income_override: Double? = null,
)

@Serializable
data class SignupRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val username: String,
)

@Serializable
data class GoalPlanResponse(
    val goal_type: String,
    val target_amount: Double,
    val target_months: Int,
    val monthly_saving_required: Double,
    val recommendations: List<String>,
    val narrative: String,
    val is_feasible: Boolean,
    val confidence_level: String,
    val validation_notes: String,
    val retry_count: Int,
)

@Serializable
data class AuditLogEntry(
    val id: Int, val interaction_type: String, val username: String?,
    val request_summary: String, val response_summary: String,
    val success: Boolean, val latency_ms: Int, val error_message: String?,
    val created_at: String,
)

@Serializable
data class PaginatedLogsResponse(val total: Int, val logs: List<AuditLogEntry>)

@Serializable
data class DailyTrendPoint(val date: String, val count: Int)

@Serializable
data class AdminMetricsResponse(
    val total_requests: Int, val requests_by_type: Map<String, Int>,
    val avg_latency_ms: Double, val success_rate_percent: Double,
    val last_7_days_trend: List<DailyTrendPoint>,
)
