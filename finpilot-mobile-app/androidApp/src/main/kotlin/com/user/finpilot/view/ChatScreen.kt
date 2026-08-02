package com.user.finpilot.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.user.finpilot.domain.ChatMessage
import com.user.finpilot.viewmodel.ChatViewModel

private val quickReplies = listOf("Can I save more?", "Where do I overspend?", "Tips to improve?")

@Composable
fun ChatScreen() {
    val viewModel = remember { ChatViewModel() }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var input by remember { mutableStateOf("") }

    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Chat Header
        Surface(shadowElevation = 4.dp) {
            Box(
                Modifier.fillMaxWidth().height(60.dp).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("AI Financial Coach", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item { 
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Ask me anything about your finances.", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp)
                        ) 
                    }
                }
            }
            items(messages) { msg -> ChatBubble(msg) }
            if (isLoading) item { 
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(8.dp),
                    strokeWidth = 2.dp
                ) 
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) } }
        }

        Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp)) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp), 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickReplies) { reply ->
                    SuggestionChip(
                        onClick = { viewModel.sendMessage(reply) }, 
                        label = { Text(reply) }
                    )
                }
            }

            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f), 
                    placeholder = { Text("Ask anything...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { if(input.isNotBlank()) { viewModel.sendMessage(input); input = "" } },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp), 
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    brush = if (isUser) {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF1976D2)))
                    } else {
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                    },
                    shape = bubbleShape
                )
                .padding(12.dp)
        ) {
            Text(
                message.content, 
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
