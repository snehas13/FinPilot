package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.ChatMessage
import com.user.finpilot.domain.ChatRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel(
    private val api: FinPilotApi = FinPilotApi(),
    private val filename: String? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val updated = _messages.value + ChatMessage("user", text)
        _messages.value = updated
        _error.value = null

        scope.launch {
            _isLoading.value = true
            try {
                val response = api.chat(ChatRequest(messages = updated, filename = filename))
                _messages.value = _messages.value + ChatMessage("assistant", response.answer)
            } catch (e: Exception) {
                _error.value = e.message ?: "Something went wrong — please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCleared() { scope.cancel() }
}