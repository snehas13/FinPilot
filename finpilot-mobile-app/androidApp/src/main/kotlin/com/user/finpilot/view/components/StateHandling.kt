package com.user.finpilot.view.components

import com.user.finpilot.viewmodel.UiState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit = {},
    emptyCheck: (T) -> Boolean = { false },
    emptyMessage: String = "Nothing here yet.",
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Something went wrong: ${state.message}")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
        is UiState.Success -> {
            if (emptyCheck(state.data)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyMessage) }
            } else {
                content(state.data)
            }
        }
    }
}