package com.user.finpilot.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.user.finpilot.domain.AuditLogEntry
import com.user.finpilot.viewmodel.AdminViewModel
import com.user.finpilot.viewmodel.UiState

@Composable
fun AdminScreen(navController: NavHostController) {
    val viewModel = remember { AdminViewModel() }
    val metricsState by viewModel.metrics.collectAsState()
    val logsState by viewModel.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadMetrics(); viewModel.loadLogs() }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Admin Dashboard", style = MaterialTheme.typography.titleLarge)
        }

        // --- Metrics overview ---
        when (val s = metricsState) {
            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            is UiState.Empty -> Text("No metrics data available.", modifier = Modifier.padding(16.dp))
            is UiState.Error -> Text("Couldn't load metrics: ${s.message}", modifier = Modifier.padding(16.dp))
            is UiState.Success -> {
                val m = s.data
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricCard("Total Requests", m.total_requests.toString(), Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MetricCard("Avg Latency", "${m.avg_latency_ms.toInt()}ms", Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    MetricCard("Success Rate", "${m.success_rate_percent}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    m.requests_by_type.forEach { (type, count) ->
                        AssistChip(onClick = {}, label = { Text("$type: $count") },
                            modifier = Modifier.padding(end = 6.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Interaction Logs", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp))

        // --- Filter chips ---
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf(null, "chat", "analyze", "goal_plan").forEach { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = type; viewModel.loadLogs(type) },
                    label = { Text(type ?: "All") },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }

        // --- Log list ---
        when (val s = logsState) {
            is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            is UiState.Empty -> Text("No logs found.", modifier = Modifier.padding(16.dp))
            is UiState.Error -> Text("Couldn't load logs: ${s.message}", modifier = Modifier.padding(16.dp))
            is UiState.Success -> LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(s.data.logs) { entry -> AuditLogRow(entry) }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AuditLogRow(entry: AuditLogEntry) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(entry.interaction_type, fontWeight = FontWeight.SemiBold)
                Text(
                    if (entry.success) "Success" else "Failed",
                    color = if (entry.success) MaterialTheme.colorScheme.secondary else Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(entry.request_summary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("${entry.latency_ms}ms · ${entry.created_at.take(19)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}