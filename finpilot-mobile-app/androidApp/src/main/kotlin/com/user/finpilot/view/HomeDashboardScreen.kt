package com.user.finpilot.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.user.finpilot.domain.FinancialSummaryResponse
import com.user.finpilot.viewmodel.HomeViewModel
import com.user.finpilot.viewmodel.UiState

@Composable
fun HomeDashboardScreen() {
    // remember{} keeps the shared ViewModel alive across recompositions;
    // DisposableEffect cancels its coroutine scope when leaving the screen.
    val viewModel = remember { HomeViewModel() }
    val state by viewModel.summaryState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSummary() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hello, Sneha 👋", style = MaterialTheme.typography.headlineSmall)
        Text("Here's your financial overview", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Couldn't load your summary: ${s.message}", color = Color.Red)
            is UiState.Success -> FinancialHealthCard(s.data)
        }
    }
}

@Composable
fun FinancialHealthCard(summary: FinancialSummaryResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Financial Health Score", style = MaterialTheme.typography.titleMedium)
            HealthScoreGauge(score = summary.health_score)
            Text(
                when {
                    summary.health_score >= 70 -> "You're doing great! Keep it up."
                    summary.health_score >= 40 -> "There's room to improve."
                    else -> "Let's work on a plan together."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Income", "₹${summary.monthly_income.toInt()}")
                StatColumn("Expenses", "₹${summary.monthly_expenses.toInt()}")
                StatColumn("Surplus", "₹${summary.surplus.toInt()}")
            }
            summary.biggest_category?.let {
                Spacer(Modifier.height(12.dp))
                Text("Biggest category: ${it.category} (${it.percent_of_total.toInt()}%)")
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun HealthScoreGauge(score: Int) {
    val color = when {
        score >= 70 -> Color(0xFF2E7D32)
        score >= 40 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = Stroke(width = 14f)
            drawArc(
                color = Color.LightGray, startAngle = 135f, sweepAngle = 270f,
                useCenter = false, style = stroke, size = Size(size.width, size.height),
            )
            drawArc(
                color = color, startAngle = 135f, sweepAngle = 270f * (score / 100f),
                useCenter = false, style = stroke, size = Size(size.width, size.height),
            )
        }
        Text("$score", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}