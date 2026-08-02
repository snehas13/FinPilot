package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.FinancialSummaryResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class HomeViewModel(
    private val api: FinPilotApi = FinPilotApi(),
    private val statementFilename: String? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _summaryState =
        MutableStateFlow<UiState<FinancialSummaryResponse>>(UiState.Loading)
    val summaryState: StateFlow<UiState<FinancialSummaryResponse>> = _summaryState.asStateFlow()

    fun loadSummary() {
        scope.launch {
            _summaryState.value = UiState.Loading
            try {
                val result = api.chatSummary(statementFilename)
                if (result.transaction_count == 0) {
                    _summaryState.value = UiState.Empty
                } else {
                    _summaryState.value = UiState.Success(result)
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("404") || msg.contains("No data") || msg.contains("empty") || msg.contains("end of stream")) {
                    _summaryState.value = UiState.Empty
                } else {
                    _summaryState.value = UiState.Error(e.message ?: "Failed to load summary")
                }
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
