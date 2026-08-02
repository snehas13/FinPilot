package com.user.finpilot.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text("Goal Planner", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        "Achieve your financial dreams", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Set Your Goal", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalText, 
                        onValueChange = { goalText = it },
                        label = { Text("What is your goal?") }, 
                        placeholder = { Text("e.g. Save ₹5,00,000 for a car in 2 years") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.generatePlan(goalText, filename = null, incomeOverride = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = goalText.isNotBlank() && state !is UiState.Loading
                    ) { 
                        Icon(Icons.Filled.TrackChanges, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate AI Plan") 
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                null -> {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tell us what you want to achieve, and we'll build a roadmap for you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                is UiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Empty -> {}
                is UiState.Error -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Error: ${s.message}", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
                is UiState.Success -> GoalPlanOutput(s.data)
            }
        }
    }
}

@Composable
fun GoalPlanOutput(plan: GoalPlanResponse) {
    Column {
        Text("Your Roadmap", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        
        ElevatedCard(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (plan.is_feasible) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (plan.is_feasible) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (plan.is_feasible) "Feasible Plan Generated! 🎉" else "Goal needs adjustment",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (plan.is_feasible) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Monthly Saving Required", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "₹${plan.monthly_saving_required.toInt()}", 
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Recommendations", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                plan.recommendations.forEach { rec -> 
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(rec, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                
                Text("Analysis", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(plan.narrative, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Confidence: ${plan.confidence_level}", style = MaterialTheme.typography.labelSmall)
                    
                    val confidenceProgress = remember(plan.confidence_level) {
                        when (plan.confidence_level.lowercase()) {
                            "high" -> 0.9f
                            "medium" -> 0.6f
                            "low" -> 0.3f
                            else -> {
                                val numericPart = plan.confidence_level.filter { it.isDigit() || it == '.' }
                                (numericPart.toFloatOrNull() ?: 0f).coerceIn(0f, 100f) / 100f
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { confidenceProgress },
                        modifier = Modifier.width(80.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
