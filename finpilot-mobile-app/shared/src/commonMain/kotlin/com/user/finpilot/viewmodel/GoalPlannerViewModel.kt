package com.user.finpilot.viewmodel

import com.user.finpilot.data.FinPilotApi
import com.user.finpilot.domain.GoalPlanRequest
import com.user.finpilot.domain.GoalPlanResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoalPlannerViewModel(private val api: FinPilotApi = FinPilotApi()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _planState = MutableStateFlow<UiState<GoalPlanResponse>?>(null)
    val planState: StateFlow<UiState<GoalPlanResponse>?> = _planState.asStateFlow()

    fun generatePlan(goalText: String, filename: String?, incomeOverride: Double?) {
        scope.launch {
            _planState.value = UiState.Loading
            try {
                val response = api.goalPlan(GoalPlanRequest(goalText, filename, incomeOverride))
                _planState.value = UiState.Success(response)
            } catch (e: Exception) {
                _planState.value = UiState.Error(e.message ?: "Couldn't generate a plan")
            }
        }
    }

    fun onCleared() { scope.cancel() }
}