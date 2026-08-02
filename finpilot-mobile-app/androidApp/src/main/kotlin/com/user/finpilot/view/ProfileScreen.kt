package com.user.finpilot.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.user.finpilot.domain.TokenStore

data class SettingsItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

private val settingsItems = listOf(
    SettingsItem("Personal Information", Icons.Filled.Person, Color(0xFF1976D2)),
    SettingsItem("Security", Icons.Filled.Lock, Color(0xFF388E3C)),
    SettingsItem("Notifications", Icons.Filled.Notifications, Color(0xFFF57C00)),
    SettingsItem("Privacy & Data", Icons.Filled.Shield, Color(0xFF7B1FA2)),
    SettingsItem("Disclaimer", Icons.Filled.Info, Color(0xFF455A64)),
)

@Composable
fun ProfileScreen(navController: androidx.navigation.NavHostController) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Profile Header with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AccountCircle, 
                        contentDescription = null, 
                        modifier = Modifier.size(70.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Sneha S.", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("sneha@example.com", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                settingsItems.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item.label, style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = item.color.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    item.icon, 
                                    contentDescription = null, 
                                    tint = item.color,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    if (index < settingsItems.size - 1) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ListItem(
            headlineContent = { Text("Admin Dashboard") },
            supportingContent = { Text("View system usage & interaction logs") },
            leadingContent = { Icon(Icons.Filled.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { navController.navigate("admin") },
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                TokenStore.clear()
                navController.navigate("login") {
                    popUpTo(0)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}
