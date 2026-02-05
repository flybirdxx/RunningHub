package com.runninghub.app.ui.feature.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.TaskRunRequest
import com.runninghub.app.data.remote.model.TaskStatusRequest
import com.runninghub.app.data.remote.model.UploadResponse
import com.runninghub.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val webAppApi: WebAppApi,
    private val application: Application,
    private val taskHistoryManager: com.runninghub.app.data.local.TaskHistoryManager,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var currentAppId: String? = null

    // Get current API Key
    private val currentApiKey: String
        get() = userPrefs.getApiKey() ?: ""

    fun fetchAppDetail(appId: String) {
        currentAppId = appId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val apiKey = currentApiKey
                val response = webAppApi.getApiCallDemo(apiKey = apiKey, webappId = appId)
                if (response.code == 0) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            appDetail = response.data,
                            inputValues = response.data.inputNodes ?: emptyList()
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun updateNodeValue(nodeId: String, fieldName: String, newValue: String) {
        _uiState.update { state ->
            val updatedNodes = state.inputValues.map { node ->
                if (node.nodeId == nodeId && node.fieldName == fieldName) {
                    node.copy(fieldValue = newValue)
                } else {
                    node
                }
            }
            state.copy(inputValues = updatedNodes)
        }
    }

    fun uploadMedia(nodeId: String, fieldName: String, fieldType: String, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingNodes = it.uploadingNodes + (nodeId to true)) }
            try {
                val file = uriToFile(uri, fieldType)
                if (file != null) {
                    val apiKey = currentApiKey
                    val apiKeyBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
                    val fileTypeBody = "input".toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val mimeType = when (fieldType) {
                        "AUDIO" -> "audio/*"
                        "VIDEO" -> "video/*"
                        else -> "image/*"
                    }

                    val filePart = MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody(mimeType.toMediaTypeOrNull())
                    )

                    val response = webAppApi.uploadFile(apiKeyBody, fileTypeBody, filePart)
                    if (response.code == 0 && response.data.fileName != null) {
                        updateNodeValue(nodeId, fieldName, response.data.fileName)
                        // Save local Uri for preview
                        _uiState.update { it.copy(nodeLocalUris = it.nodeLocalUris + (nodeId to uri)) }
                    } else {
                        _uiState.update { it.copy(error = "上传失败: ${response.msg}") }
                    }
                } else {
                    _uiState.update { it.copy(error = "无法读取选择的文件") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "上传出错: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(uploadingNodes = it.uploadingNodes - nodeId) }
            }
        }
    }

    private fun uriToFile(uri: Uri, fieldType: String): File? {
        return try {
            val contentResolver = application.contentResolver
            val extension = when (fieldType) {
                "AUDIO" -> "mp3"
                "VIDEO" -> "mp4"
                else -> "jpg"
            }
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(application.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
            val outputStream = FileOutputStream(tempFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun runTask() {
        val detail = _uiState.value.appDetail ?: return
        val nodes = _uiState.value.inputValues
        
        viewModelScope.launch {
            val apiKey = currentApiKey
            if (apiKey.isBlank()) {
                _uiState.update { it.copy(error = "请先在个人中心绑定 API Key") }
                return@launch
            }

            _uiState.update { it.copy(isRunning = true, statusText = "正在提交任务...", error = null, taskResultUrl = null) }
            try {
                val runRequest = TaskRunRequest(
                    webappId = (currentAppId ?: detail.id)?.toLongOrNull() ?: 0L,
                    apiKey = apiKey,
                    nodeInfoList = nodes
                )
                val response = webAppApi.runTask(runRequest)
                if (response.code == 0) {
                    val taskId = response.data.taskId
                    startStatusPolling(taskId, apiKey)
                } else {
                    _uiState.update { it.copy(isRunning = false, error = "提交失败: ${response.msg}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRunning = false, error = "网络错误: ${e.localizedMessage}") }
            }
        }
    }

    private fun startStatusPolling(taskId: Long, apiKey: String) {
        viewModelScope.launch {
            var isFinished = false
            val startTime = System.currentTimeMillis()
            val timeout = 600000 // 10 minutes

            while (!isFinished && System.currentTimeMillis() - startTime < timeout) {
                try {
                    val statusRequest = TaskStatusRequest(taskId = taskId, apiKey = apiKey)
                    val response = webAppApi.getTaskOutputs(statusRequest)
                    
                    when (response.code) {
                        0 -> { // Success
                            val output = response.data.firstOrNull()
                            if (output?.fileUrl != null) {
                                _uiState.update { it.copy(isRunning = false, taskResultUrl = output.fileUrl, statusText = "生成成功！") }
                                // Save to history
                                val detail = _uiState.value.appDetail
                                detail?.let {
                                    taskHistoryManager.saveTask(
                                        com.runninghub.app.data.local.HistoryTask(
                                            taskId = taskId,
                                            appName = it.name,
                                            resultUrl = output.fileUrl
                                        )
                                    )
                                }
                                isFinished = true
                            }
                        }
                        805 -> { // Failed
                            val reason = response.data.firstOrNull()?.failedReason
                            val errorMsg = "任务失败: ${reason?.exception_message ?: "获取详情失败"}"
                            _uiState.update { it.copy(isRunning = false, error = errorMsg) }
                            isFinished = true
                        }
                        804, 813 -> { // Running or Queuing
                            val status = if (response.code == 804) "运行中..." else "排队中..."
                            _uiState.update { it.copy(statusText = status) }
                        }
                        else -> {
                            // Keep polling or handle other codes
                        }
                    }
                } catch (e: Exception) {
                    // Log error but continue polling if not critical
                }
                
                if (!isFinished) {
                    kotlinx.coroutines.delay(5000) // Poll every 5 seconds
                }
            }
            
            if (!isFinished) {
                _uiState.update { it.copy(isRunning = false, error = "任务超时") }
            }
        }
    }
}
