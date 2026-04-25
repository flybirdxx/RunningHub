package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appId: String,
    onBack: () -> Unit = {},
    onCreatorClick: (String) -> Unit = {},
    viewModel: AppDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(appId) {
        viewModel.loadDetail(appId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "未知错误",
                onRetry = { viewModel.loadDetail(appId) },
                modifier = Modifier.padding(padding)
            )
            uiState.detail != null -> {
                val detail = uiState.detail!!
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(detail.name, style = MaterialTheme.typography.headlineMedium)
                    Text(detail.description ?: "", style = MaterialTheme.typography.bodyLarge)
                    detail.authorId?.let { authorId ->
                        TextButton(onClick = { onCreatorClick(authorId) }) {
                            Text("作者: ${detail.authorName}")
                        }
                    }
                    Button(
                        onClick = { /* TODO: generate */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("立即生成")
                    }
                }
            }
        }
    }
}
