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

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item { Text("Ask me anything about your finances.", modifier = Modifier.padding(16.dp)) }
            }
            items(messages) { msg -> ChatBubble(msg) }
            if (isLoading) item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
            error?.let { item { Text(it, color = androidx.compose.ui.graphics.Color.Red) } }
        }

        LazyRow(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickReplies) { reply ->
                AssistChip(onClick = { viewModel.sendMessage(reply) }, label = { Text(reply) })
            }
        }

        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f), placeholder = { Text("Ask anything...") },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.sendMessage(input); input = "" }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(message.content, modifier = Modifier.padding(12.dp))
        }
    }
}