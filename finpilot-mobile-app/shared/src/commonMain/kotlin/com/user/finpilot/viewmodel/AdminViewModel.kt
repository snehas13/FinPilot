package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.AdminMetricsResponse
import com.user.finpilot.domain.PaginatedLogsResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel(private val api: FinPilotApi = FinPilotApi()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _metrics = MutableStateFlow<UiState<AdminMetricsResponse>>(UiState.Loading)
    val metrics: StateFlow<UiState<AdminMetricsResponse>> = _metrics.asStateFlow()

    private val _logs = MutableStateFlow<UiState<PaginatedLogsResponse>>(UiState.Loading)
    val logs: StateFlow<UiState<PaginatedLogsResponse>> = _logs.asStateFlow()

    fun loadMetrics() {
        scope.launch {
            _metrics.value = UiState.Loading
            try { _metrics.value = UiState.Success(api.adminMetrics()) }
            catch (e: Exception) { _metrics.value = UiState.Error(e.message ?: "Failed to load metrics") }
        }
    }

    fun loadLogs(filterType: String? = null) {
        scope.launch {
            _logs.value = UiState.Loading
            try { _logs.value = UiState.Success(api.adminLogs(filterType)) }
            catch (e: Exception) { _logs.value = UiState.Error(e.message ?: "Failed to load logs") }
        }
    }

    fun onCleared() { scope.cancel() }
}