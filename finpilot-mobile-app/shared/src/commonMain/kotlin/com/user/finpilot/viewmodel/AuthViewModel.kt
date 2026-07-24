package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.LoginRequest
import com.user.finpilot.domain.SignupRequest
import com.user.finpilot.domain.TokenStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val api: FinPilotApi = FinPilotApi()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _authState = MutableStateFlow<UiState<String>?>(null)
    val authState: StateFlow<UiState<String>?> = _authState.asStateFlow()

    fun signup(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = UiState.Error("Please enter a username and password.")
            return
        }
        scope.launch {
            _authState.value = UiState.Loading
            try {
                val resp = api.signup(SignupRequest(username, password))
                TokenStore.save(resp.access_token, resp.username)
                _authState.value = UiState.Success(resp.username)
            } catch (e: Exception) {
                _authState.value = UiState.Error(parseError(e))
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = UiState.Error("Please enter a username and password.")
            return
        }
        scope.launch {
            _authState.value = UiState.Loading
            try {
                val resp = api.login(LoginRequest(username, password))
                TokenStore.save(resp.access_token, resp.username)
                _authState.value = UiState.Success(resp.username)
            } catch (e: Exception) {
                _authState.value = UiState.Error(parseError(e))
            }
        }
    }

    private fun parseError(e: Exception): String {
        // Ktor throws ClientRequestException for 4xx responses — surface a
        // friendlier message than the raw exception text where possible.
        val msg = e.message ?: "Something went wrong"
        return when {
            msg.contains("401") -> "Incorrect username or password."
            msg.contains("409") -> "That username is already taken."
            msg.contains("400") -> "Check your username/password requirements."
            else -> "Couldn't connect — check your network and try again."
        }
    }

    fun onCleared() { scope.cancel() }
}