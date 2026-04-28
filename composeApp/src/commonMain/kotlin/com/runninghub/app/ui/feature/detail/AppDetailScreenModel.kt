package com.runninghub.app.ui.feature.detail

import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.AppDetail
import com.runninghub.shared.domain.model.InputNode
import com.runninghub.shared.domain.model.TaskOutput
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val detail: AppDetail? = null,
    val inputValues: Map<String, String> = emptyMap(),
    val isRunningTask: Boolean = false,
    val taskStep: com.runninghub.app.ui.component.TaskStep = com.runninghub.app.ui.component.TaskStep.IDLE,
    val taskElapsedSeconds: Int = 0,
    val taskOutputs: List<TaskOutput> = emptyList(),
    val taskError: String? = null,
    val uploadingNodes: Map<String, UploadingState> = emptyMap(),
    val localUris: Map<String, String> = emptyMap(),
    val pendingImagePick: PendingImagePick? = null,
    val error: String? = null
)

data class UploadingState(
    val localUri: String,
    val progress: Float = 0f,
    val isError: Boolean = false
)

data class PendingImagePick(
    val nodeId: String,
    val fieldName: String
)

class AppDetailScreenModel(
    private val webAppRepository: WebAppRepository,
    private val settingsRepository: SettingsRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var currentAppId: String = ""

    private var onImagePicked: ((String) -> Unit)? = null

    fun setOnImagePickedCallback(cb: (String) -> Unit) {
        onImagePicked = cb
    }

    fun setPendingImagePick(nodeId: String, fieldName: String) {
        // The actual URI will be passed via onImagePicked callback after picker returns
        // Screen holds reference to pending pick info
        _uiState.update {
            it.copy(pendingImagePick = PendingImagePick(nodeId, fieldName))
        }
    }

    fun onImageUriReceived(uri: String) {
        val pending = _uiState.value.pendingImagePick ?: return
        setLocalFileUri(pending.nodeId, uri)
        uploadFile(pending.nodeId, pending.fieldName, uri)
        _uiState.update { it.copy(pendingImagePick = null) }
    }

    fun loadDetail(appId: String) {
        if (appId == currentAppId && _uiState.value.detail != null) return
        currentAppId = appId

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val detail = webAppRepository.getAppDetail(appId).getOrNull()
                ?: run {
                    val apiKey = settingsRepository.getApiKey().orEmpty()
                    if (apiKey.isNotBlank()) {
                        webAppRepository.getApiCallDemo(apiKey, appId).getOrNull()
                    } else null
                }

            if (detail != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        inputValues = detail.inputNodes.associate { node ->
                            inputKey(node) to (node.fieldValue ?: "")
                        }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "加载失败，请检查网络后重试")
                }
            }
        }
    }

    fun updateInputValue(nodeId: String, fieldName: String, value: String) {
        _uiState.update {
            it.copy(inputValues = it.inputValues + (inputKey(nodeId, fieldName) to value))
        }
    }

    fun setLocalFileUri(nodeId: String, localUri: String) {
        _uiState.update {
            it.copy(
                localUris = it.localUris + (nodeId to localUri),
                uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0f))
            )
        }
    }

    fun uploadFile(nodeId: String, fieldName: String, localUri: String) {
        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0.1f))
                )
            }

            val apiKey = settingsRepository.getApiKey().orEmpty()
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0f, isError = true)))
                }
                return@launch
            }

            try {
                val fileName = localUri
                    .substringAfterLast("/")
                    .substringAfterLast("%2F")
                    .substringAfterLast(":")
                    .take(64)
                    .ifBlank { "upload.png" }

                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0.4f)))
                }

                val result = webAppRepository.uploadFile(
                    apiKey = apiKey,
                    fileType = "image/png",
                    fileBytes = ByteArray(0),
                    fileName = fileName
                )

                result.onSuccess { uploadResult ->
                    val returnedName = uploadResult.fileName ?: fileName
                    updateInputValue(nodeId, fieldName, returnedName)
                    _uiState.update {
                        it.copy(
                            uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 1f))
                        )
                    }
                }.onFailure {
                    _uiState.update {
                        it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0f, isError = true)))
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0f, isError = true)))
                }
            }
        }
    }

    fun removeLocalFile(nodeId: String, fieldName: String) {
        _uiState.update {
            it.copy(
                localUris = it.localUris - nodeId,
                uploadingNodes = it.uploadingNodes - nodeId,
                inputValues = it.inputValues - inputKey(nodeId, fieldName)
            )
        }
    }

    fun runTask() {
        val detail = _uiState.value.detail ?: return
        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningTask = true,
                    taskStep = com.runninghub.app.ui.component.TaskStep.SUBMITTING,
                    taskError = null,
                    taskOutputs = emptyList(),
                    taskElapsedSeconds = 0
                )
            }

            val apiKey = settingsRepository.getApiKey().orEmpty()
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        isRunningTask = false,
                        taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                        taskError = "请先在设置中绑定 API Key"
                    )
                }
                return@launch
            }

            val inputNodes = detail.inputNodes.map { node ->
                node.copy(
                    fieldValue = _uiState.value.inputValues[inputKey(node)] ?: node.fieldValue
                )
            }

            webAppRepository.runTask(
                webappId = detail.id.toLongOrNull() ?: 0L,
                apiKey = apiKey,
                nodeInfoList = inputNodes
            ).onSuccess {
                pollTaskOutputs(it.taskId, apiKey)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isRunningTask = false,
                        taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                        taskError = e.message ?: "提交失败"
                    )
                }
            }
        }
    }

    private fun pollTaskOutputs(taskId: Long, apiKey: String) {
        screenModelScope.launch {
            var attempts = 0
            val maxAttempts = 120
            val startTime = Clock.System.now().toEpochMilliseconds()

            while (attempts < maxAttempts) {
                val elapsed = ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).toInt()
                _uiState.update {
                    it.copy(
                        taskStep = when {
                            attempts < 1 -> com.runninghub.app.ui.component.TaskStep.SUBMITTING
                            attempts < 3 -> com.runninghub.app.ui.component.TaskStep.QUEUEING
                            else -> com.runninghub.app.ui.component.TaskStep.RUNNING
                        },
                        taskElapsedSeconds = elapsed
                    )
                }

                delay(5_000)
                attempts++

                webAppRepository.getTaskOutputs(taskId, apiKey)
                    .onSuccess { outputs ->
                        if (outputs.isEmpty()) return@onSuccess

                        val failed = outputs.firstOrNull { it.failedReason != null }
                        if (failed != null) {
                            _uiState.update {
                                it.copy(
                                    isRunningTask = false,
                                    taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                                    taskError = failed.failedReason?.exceptionMessage ?: "任务失败"
                                )
                            }
                            return@launch
                        }

                        if (outputs.any { !it.fileUrl.isNullOrBlank() }) {
                            _uiState.update {
                                it.copy(
                                    isRunningTask = false,
                                    taskStep = com.runninghub.app.ui.component.TaskStep.SUCCESS,
                                    taskElapsedSeconds = ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).toInt(),
                                    taskOutputs = outputs
                                )
                            }
                            return@launch
                        }
                    }
                    .onFailure { }
            }

            _uiState.update {
                it.copy(
                    isRunningTask = false,
                    taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                    taskError = "任务超时，请稍后重试"
                )
            }
        }
    }

    fun resetTask() {
        _uiState.update {
            it.copy(
                taskOutputs = emptyList(),
                taskError = null,
                taskStep = com.runninghub.app.ui.component.TaskStep.IDLE,
                taskElapsedSeconds = 0
            )
        }
    }

    companion object {
        fun inputKey(node: InputNode): String = "${node.nodeId}:${node.fieldName}"
        fun inputKey(nodeId: String, fieldName: String): String = "$nodeId:$fieldName"
    }
}
