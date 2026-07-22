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
    val filename: String,
    val status: String,
    val pages_extracted: Int,
    val chunks_created: Int,
    val points_stored: Int = 0,
    val chunk_preview: List<TransactionChunk>,
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
    val monthly_income: Double,
    val monthly_expenses: Double,
    val surplus: Double,
    val savings_rate_percent: Double,
    val health_score: Int,
    val biggest_category: CategorySpend? = null,
    val category_breakdown: List<CategorySpend>,
    val transaction_count: Int,
    val income_is_estimated: Boolean,
)

@Serializable
data class GoalPlanRequest(
    val goal_text: String,
    val filename: String? = null,
    val monthly_income_override: Double? = null,
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