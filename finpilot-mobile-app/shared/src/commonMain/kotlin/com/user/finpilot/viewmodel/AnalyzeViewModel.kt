package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.AnalyzeRequest
import com.user.finpilot.domain.AnalyzeResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QnaEntry(val question: String, val answer: AnalyzeResponse)

class AnalyzeViewModel(private val api: FinPilotApi = FinPilotApi()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _history = MutableStateFlow<List<QnaEntry>>(emptyList())
    val history: StateFlow<List<QnaEntry>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun askQuestion(query: String, filename: String) {
        if (query.isBlank()) return
        _error.value = null

        scope.launch {
            _isLoading.value = true
            try {
                val response = api.analyze(AnalyzeRequest(query = query, filename = filename))
                _history.value += QnaEntry(query, response)
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't get an answer — please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCleared() { scope.cancel() }
}