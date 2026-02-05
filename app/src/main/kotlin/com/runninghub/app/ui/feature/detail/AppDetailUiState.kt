package com.runninghub.app.ui.feature.detail

import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.InputNodeDto

data class AppDetailUiState(
    val isLoading: Boolean = false,
    val appDetail: WebAppDetailDto? = null,
    val inputValues: List<InputNodeDto> = emptyList(), 
    val uploadingNodes: Map<String, Boolean> = emptyMap(), // nodeId to isUploading
    val nodeLocalUris: Map<String, android.net.Uri> = emptyMap(), // nodeId to local Uri for preview
    val isRunning: Boolean = false,
    val taskResultUrl: String? = null,
    val statusText: String? = null,
    val error: String? = null
)
