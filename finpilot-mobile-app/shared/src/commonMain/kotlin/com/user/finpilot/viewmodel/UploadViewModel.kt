package com.user.finpilot.viewmodel

import com.user.finpilot.Constants
import com.user.finpilot.domain.TokenStore
import com.user.finpilot.domain.UploadResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UploadViewModel(
    private val client: HttpClient,
    private val baseUrl: String = Constants.BASE_URL,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<UiState<UploadResponse>>(UiState.Loading)
    val state: StateFlow<UiState<UploadResponse>> = _state.asStateFlow()

    fun uploadPdf(fileBytes: ByteArray, filename: String) {
        scope.launch {
            _state.value = UiState.Loading
            try {
                val response: UploadResponse = client.post("$baseUrl/upload") {
                    TokenStore.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    setBody(MultiPartFormDataContent(formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            append(HttpHeaders.ContentType, "application/pdf")
                        })
                    }))
                }.body()
                _state.value = UiState.Success(response)
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun onCleared() { scope.cancel() }
}