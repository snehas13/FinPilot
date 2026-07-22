package com.user.finpilot.data

import com.user.finpilot.domain.AnalyzeRequest
import com.user.finpilot.domain.AnalyzeResponse
import com.user.finpilot.domain.ChatRequest
import com.user.finpilot.domain.ChatResponse
import com.user.finpilot.domain.FinancialSummaryResponse
import com.user.finpilot.domain.GoalPlanRequest
import com.user.finpilot.domain.GoalPlanResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FinPilotApi(
    private val client: HttpClient = createHttpClient(),
    // 10.0.2.2 is the Android emulator's alias for your host machine's localhost.
    // Swap to your Cloud Run URL once Day 14 deployment is live.
    private val baseUrl: String = "http://10.0.2.2:8000",
) {
    suspend fun analyze(req: AnalyzeRequest): AnalyzeResponse =
        client.post("$baseUrl/analyze") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun chat(req: ChatRequest): ChatResponse =
        client.post("$baseUrl/chat") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun chatSummary(filename: String?): FinancialSummaryResponse =
        client.get("$baseUrl/chat/summary") {
            filename?.let { parameter("filename", it) }
        }.body()

    suspend fun goalPlan(req: GoalPlanRequest): GoalPlanResponse =
        client.post("$baseUrl/goal-plan") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    // Multipart upload needs platform file bytes — implemented in Day 9's
    // upload screen using Ktor's MultiPartFormDataContent directly there,
    // since it needs the Android-specific file picker result.
}