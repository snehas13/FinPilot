package com.user.finpilot.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.user.finpilot.viewmodel.AuthViewModel
import com.user.finpilot.viewmodel.UiState

@Composable
fun LoginScreen(navController: NavHostController) {
    val viewModel = remember { AuthViewModel() }
    val state by viewModel.authState.collectAsState()

    var isSignupMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Navigate to Home once auth succeeds — clears Login off the back stack
    // so the back button doesn't return the user to the login screen.
    LaunchedEffect(state) {
        if (state is UiState.Success) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("FinPilot AI", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your AI Financial Coach",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        Text(
            if (isSignupMode) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (isSignupMode) viewModel.signup(username, password)
                else viewModel.login(username, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is UiState.Loading,
        ) {
            if (state is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isSignupMode) "Sign Up" else "Login")
            }
        }

        (state as? UiState.Error)?.let {
            Spacer(Modifier.height(12.dp))
            Text(it.message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSignupMode) "Already have an account?" else "Don't have an account?")
            Spacer(Modifier.width(4.dp))
            Text(
                if (isSignupMode) "Login" else "Sign up",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { isSignupMode = !isSignupMode; password = "" },
            )
        }
    }
}