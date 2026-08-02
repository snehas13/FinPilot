package com.user.finpilot.data

import com.user.finpilot.Constants
import com.user.finpilot.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class FinPilotApi(
    private val client: HttpClient = createHttpClient(),
    private val baseUrl: String = Constants.BASE_URL,
) {
    suspend fun analyze(req: AnalyzeRequest): AnalyzeResponse =
        client.post("$baseUrl/analyze") {
            auth()
            req.filename?.let { parameter("filename", it) }
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun chat(req: ChatRequest): ChatResponse =
        client.post("$baseUrl/chat") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun chatSummary(filename: String?): FinancialSummaryResponse =
        client.get("$baseUrl/chat/summary") {
            auth()
            header(HttpHeaders.Connection, "close")
            filename?.let { parameter("filename", it) }
        }.body()

    suspend fun goalPlan(req: GoalPlanRequest): GoalPlanResponse =
        client.post("$baseUrl/goal-plan") {
            auth()
            req.filename?.let { parameter("filename", it) }
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun signup(req: SignupRequest): TokenResponse =
        client.post("$baseUrl/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun login(req: LoginRequest): TokenResponse =
        client.post("$baseUrl/auth/login") {3
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun adminLogs(interactionType: String? = null, limit: Int = 20): PaginatedLogsResponse =
        client.get("$baseUrl/admin/logs") {
            auth()
            interactionType?.let { parameter("interaction_type", it) }
            parameter("limit", limit)
        }.body()

    suspend fun adminMetrics(): AdminMetricsResponse =
        client.get("$baseUrl/admin/metrics") { auth() }.body()

    private fun HttpRequestBuilder.auth() {
        TokenStore.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}
