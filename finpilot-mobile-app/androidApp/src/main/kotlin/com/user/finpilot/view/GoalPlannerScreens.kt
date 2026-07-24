package com.user.finpilot.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.user.finpilot.domain.GoalPlanResponse
import com.user.finpilot.viewmodel.GoalPlannerViewModel
import com.user.finpilot.viewmodel.UiState

@Composable
fun GoalPlannerCreateScreen(navController: NavHostController) {
    val viewModel = remember { GoalPlannerViewModel() }
    val state by viewModel.planState.collectAsState()
    var goalText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Goal Planner", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = goalText, onValueChange = { goalText = it },
            label = { Text("What is your goal?") }, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.generatePlan(goalText, filename = null, incomeOverride = null) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Generate Plan") }

        Spacer(Modifier.height(24.dp))
        when (val s = state) {
            null -> {}
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Error: ${s.message}")
            is UiState.Success -> GoalPlanOutput(s.data)
        }
    }
}

@Composable
fun GoalPlanOutput(plan: GoalPlanResponse) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (plan.is_feasible) "Plan Generated Successfully! 🎉" else "This goal needs adjustment",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text("Monthly Saving Required: ₹${plan.monthly_saving_required.toInt()}")
            Spacer(Modifier.height(8.dp))
            plan.recommendations.forEach { rec -> Text("• $rec") }
            Spacer(Modifier.height(12.dp))
            Text(plan.narrative, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("Confidence Level: ${plan.confidence_level}")
        }
    }
}