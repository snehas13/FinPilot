package com.user.finpilot.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.user.finpilot.domain.FinancialSummaryResponse
import com.user.finpilot.viewmodel.HomeViewModel
import com.user.finpilot.viewmodel.UiState

@Composable
fun HomeDashboardScreen(navController: androidx.navigation.NavHostController) {
    val viewModel = remember { HomeViewModel() }
    val state by viewModel.summaryState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSummary() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Modern Header with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(
                    "Hello, Sneha 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Here's your financial overview",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Empty -> Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No statements uploaded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { navController.navigate("upload") }) {
                            Text("Upload Now")
                        }
                    }
                }
                is UiState.Error -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Couldn't load your summary: ${s.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                is UiState.Success -> FinancialHealthCard(s.data)
            }

            Spacer(Modifier.height(24.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            QuickActionsGrid(navController)
        }
    }
}

data class QuickAction(
    val title: String, val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String,
)

@Composable
fun QuickActionsGrid(navController: androidx.navigation.NavHostController) {
    val actions = listOf(
        QuickAction("AI Coach", "Chat with your financial coach",
            Icons.AutoMirrored.Filled.Chat, "chat"),
        QuickAction("Statement Analyzer", "Upload & analyze statements",
            Icons.Filled.Description, "upload"),
        QuickAction("Goal Planner", "Plan and achieve your goals",
            Icons.Filled.TrackChanges, "goals"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { action -> QuickActionCard(action, navController, Modifier.weight(1f)) }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QuickActionCard(action: QuickAction, navController: androidx.navigation.NavHostController, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .height(110.dp)
            .clickable { navController.navigate(action.route) },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(action.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(action.subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}
@Composable
fun FinancialHealthCard(summary: FinancialSummaryResponse) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Financial Health Score", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            HealthScoreGauge(score = summary.health_score)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    summary.health_score >= 70 -> "You're doing great! Keep it up."
                    summary.health_score >= 40 -> "There's room to improve."
                    else -> "Let's work on a plan together."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            summary.biggest_category?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Top Category: ${it.category} (${it.percent_of_total.toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Income", "₹${summary.monthly_income.toInt()}")
                StatColumn("Expenses", "₹${summary.monthly_expenses.toInt()}")
                StatColumn("Surplus", "₹${summary.surplus.toInt()}")
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