package com.user.finpilot.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
            var name = "statement.pdf"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            // Ensure filename ends with .pdf for server-side validation
            if (!name.lowercase().endsWith(".pdf")) {
                name += ".pdf"
            }
            bytes?.let { b -> uploadViewModel.uploadPdf(b, name) }
        }
    }

    DisposableEffect(Unit) { onDispose { uploadViewModel.onCleared(); analyzeViewModel.onCleared() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                    Text("Statement Analyzer", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        "Upload and analyze your statements", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // --- Upload section ---
        if (uploadState !is UiState.Success || (uploadState as UiState.Success).data.status == "error") {
            Column(Modifier.padding(16.dp)) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { filePicker.launch("application/pdf") },
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxSize(), 
                        Arrangement.Center, 
                        Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                Icons.Filled.CloudUpload, 
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Drop your PDF statement here", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Maximum size: 10MB", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { filePicker.launch("application/pdf") },
                            shape = RoundedCornerShape(8.dp)
                        ) { 
                            Text("Choose File") 
                        }
                    }
                }
                
                when (val s = uploadState) {
                    is UiState.Loading -> Column(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(8.dp))
                        Text("Analyzing your financial patterns...", style = MaterialTheme.typography.bodySmall)
                    }
                    is UiState.Empty -> {}
                    is UiState.Error -> Text(
                        "Upload failed: ${s.message}", 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    is UiState.Success -> if (s.data.status == "error") {
                        Text(
                            "Couldn't process this file: ${s.data.error}", 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    null -> {}
                }
            }
        }

        // --- Q&A section, only once a statement is successfully uploaded ---
        val uploadedFilename = (uploadState as? UiState.Success)?.data?.takeIf { it.status != "error" }?.filename

        if (uploadedFilename != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Description, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        uploadedFilename, 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.CheckCircle, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                if (qnaHistory.isEmpty()) {
                    item { 
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Ask a question about this statement.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        }
                    }
                }
                items(qnaHistory) { entry -> QnaBubble(entry) }
                if (isAsking) item { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                askError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("What's my biggest expense?") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (questionInput.isNotBlank() && !isAsking) {
                                analyzeViewModel.askQuestion(questionInput, uploadedFilename)
                                questionInput = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(48.dp)
                    ) { 
                        Icon(Icons.Filled.Search, contentDescription = "Search") 
                    }
                }
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