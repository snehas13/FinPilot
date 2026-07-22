package com.user.finpilot.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.user.finpilot.data.createHttpClient
import com.user.finpilot.viewmodel.UiState
import com.user.finpilot.viewmodel.UploadViewModel

@Composable
fun StatementUploadScreen() {
    val context = LocalContext.current
    val viewModel = remember { UploadViewModel(createHttpClient()) }
    val state by viewModel.state.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            val name = it.lastPathSegment ?: "statement.pdf"
            bytes?.let { b -> viewModel.uploadPdf(b, name) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Upload PDF Statement", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { filePicker.launch("application/pdf") }) {
            Text("Choose File")
        }
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> Text("Upload failed: ${s.message}")
            is UiState.Success -> Text(
                "Uploaded ${s.data.filename} — ${s.data.chunks_created} chunks, ${s.data.points_stored} stored."
            )
        }
    }
}