package com.user.finpilot.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.user.finpilot.data.createHttpClient
import com.user.finpilot.viewmodel.AnalyzeViewModel
import com.user.finpilot.viewmodel.QnaEntry
import com.user.finpilot.viewmodel.UiState
import com.user.finpilot.viewmodel.UploadViewModel


@Composable
fun StatementUploadScreen(navController: NavHostController) {
    val context = LocalContext.current
    val uploadViewModel = remember { UploadViewModel(createHttpClient()) }
    val analyzeViewModel = remember { AnalyzeViewModel() }

    val uploadState by uploadViewModel.state.collectAsState()
    val qnaHistory by analyzeViewModel.history.collectAsState()
    val isAsking by analyzeViewModel.isLoading.collectAsState()
    val askError by analyzeViewModel.error.collectAsState()

    var questionInput by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() }
            val name = it.lastPathSegment ?: "statement.pdf"
            bytes?.let { b -> uploadViewModel.uploadPdf(b, name) }
        }
    }

    DisposableEffect(Unit) { onDispose { uploadViewModel.onCleared(); analyzeViewModel.onCleared() } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Statement Analyzer", style = MaterialTheme.typography.titleLarge)
                Text("Upload and analyze your statements", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // --- Upload section ---
        if (uploadState !is UiState.Success || (uploadState as UiState.Success).data.status == "error") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp).clickable { filePicker.launch("application/pdf") },
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null,
                            modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Upload PDF Statement", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { filePicker.launch("application/pdf") }) { Text("Choose File") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                when (val s = uploadState) {
                    is UiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading and analyzing...")
                    }
                    is UiState.Error -> Text("Upload failed: ${s.message}", color = Color.Red)
                    is UiState.Success -> if (s.data.status == "error") {
                        Text("Couldn't process this file: ${s.data.error}", color = Color.Red)
                    }
                    null -> {}
                }
            }
        }

        // --- Q&A section, only once a statement is successfully uploaded ---
        val uploadedFilename = (uploadState as? UiState.Success)?.data?.takeIf { it.status != "error" }?.filename

        if (uploadedFilename != null) {
            AssistChip(
                onClick = {},
                label = { Text(uploadedFilename) },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (qnaHistory.isEmpty()) {
                    item { Text("Ask a question about this statement below.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(qnaHistory) { entry -> QnaBubble(entry) }
                if (isAsking) item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
                askError?.let { item { Text(it, color = Color.Red) } }
            }

            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    placeholder = { Text("e.g. What did I spend most on in June?") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        analyzeViewModel.askQuestion(questionInput, uploadedFilename)
                        questionInput = ""
                    },
                    enabled = questionInput.isNotBlank() && !isAsking,
                ) { Text("Ask") }
            }
        }
    }
}

@Composable
fun QnaBubble(entry: QnaEntry) {
    Column {
        // Question bubble, right-aligned like the chat screen
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(entry.question, modifier = Modifier.padding(10.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Answer card, left-aligned, with source chunk pills
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text(entry.answer.answer)
                if (entry.answer.sources.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(entry.answer.sources) { chunkId ->
                            AssistChip(onClick = {}, label = { Text("Chunk ${chunkId.take(6)}") })
                        }
                    }
                }
            }
        }
    }
}