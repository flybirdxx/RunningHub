package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = uiState.user?.nickName ?: "未登录",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (uiState.user != null) "ID: ${uiState.user!!.id}" else "请绑定 API Key 查看更多信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.user == null && !uiState.isLoading) {
                var apiKeyInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            viewModel.bindApiKey(apiKeyInput)
                            apiKeyInput = ""
                        }
                    },
                    enabled = apiKeyInput.isNotBlank()
                ) {
                    Text("绑定")
                }
            }
        }
    }
}
