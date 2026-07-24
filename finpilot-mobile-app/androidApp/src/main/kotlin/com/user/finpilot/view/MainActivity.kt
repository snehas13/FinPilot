package com.user.finpilot.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Goals : Screen("goals", "Goals", Icons.Filled.TrackChanges)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

val bottomNavItems = listOf(Screen.Home, Screen.Chat, Screen.Goals, Screen.Profile)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FinPilotApp()
            }
        }
    }
}

@Composable
fun FinPilotApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // Hide the bottom nav bar on the login screen — it shouldn't be
        // reachable before authenticating.
        bottomBar = { if (currentRoute != "login") FinPilotBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding),
        ) {
            composable("login") { LoginScreen(navController) }
            composable(Screen.Home.route) { HomeDashboardScreen(navController) }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Goals.route) { GoalPlannerCreateScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable("upload") { StatementUploadScreen(navController) }
        }
    }
}

@Composable
fun FinPilotBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
            )
        }
    }
}