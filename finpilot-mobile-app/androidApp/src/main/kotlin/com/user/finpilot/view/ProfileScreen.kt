package com.user.finpilot.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.user.finpilot.domain.TokenStore

data class SettingsItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val settingsItems = listOf(
    SettingsItem("Personal Information", Icons.Filled.Person),
    SettingsItem("Security", Icons.Filled.Lock),
    SettingsItem("Notifications", Icons.Filled.Notifications),
    SettingsItem("Privacy & Data", Icons.Filled.Shield),
    SettingsItem("Disclaimer", Icons.Filled.Info),
)

@Composable
fun ProfileScreen(navController: androidx.navigation.NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Sneha S.", style = MaterialTheme.typography.titleMedium)
                    Text("sneha@example.com", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        settingsItems.forEach { item ->
            ListItem(
                headlineContent = { Text(item.label) },
                leadingContent = { Icon(item.icon, contentDescription = null) },
                trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = {
            TokenStore.clear()
            navController.navigate("login") {
                popUpTo(0)  // clear the entire back stack — no returning to Home via back button
            }
        }) {
            Text("Logout", color = MaterialTheme.colorScheme.primary)
        }
    }
}