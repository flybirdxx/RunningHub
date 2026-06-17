package com.runninghub.app.ui.feature.detail

import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.domain.model.AppDetail
import com.runninghub.shared.domain.model.InputNode
import com.runninghub.shared.domain.model.TaskOutput
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    val pendingMediaPick: PendingMediaPick? = null,
    val error: String? = null
)

data class UploadingState(
    val localUri: String,
    val progress: Float = 0f,
    val isError: Boolean = false
)

data class PendingMediaPick(
    val nodeId: String,
    val fieldName: String,
    val mediaType: MediaType,
    val requestId: Long
)

class AppDetailScreenModel(
    private val webAppRepository: WebAppRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaResolver: MediaResolver
) : ScreenModel {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var currentAppId: String = ""
    private var mediaPickRequestId: Long = 0L

    private var onImagePicked: ((String) -> Unit)? = null

    fun setOnImagePickedCallback(cb: (String) -> Unit) {
        onImagePicked = cb
    }

    fun setPendingMediaPick(nodeId: String, fieldName: String, mediaType: MediaType) {
        // The actual URI will be passed via onImagePicked callback after picker returns
        // Screen holds reference to pending pick info
        mediaPickRequestId += 1
        _uiState.update {
            it.copy(pendingMediaPick = PendingMediaPick(nodeId, fieldName, mediaType, mediaPickRequestId))
        }
    }

    fun clearPendingMediaPick() {
        _uiState.update { it.copy(pendingMediaPick = null) }
    }

    fun onMediaUriReceived(uri: String) {
        val pending = _uiState.value.pendingMediaPick ?: return
        setLocalFileUri(pending.nodeId, uri)
        uploadFile(pending.nodeId, pending.fieldName, uri, pending.mediaType)
        _uiState.update { it.copy(pendingMediaPick = null) }
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

    fun uploadFile(
        nodeId: String,
        fieldName: String,
        localUri: String,
        mediaType: MediaType = MediaType.IMAGE
    ) {
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
                val displayName = mediaResolver.getDisplayName(localUri)
                val fileName = displayName
                    ?: localUri
                        .substringAfterLast("/")
                        .substringAfterLast("%2F")
                        .substringAfterLast(":")
                        .take(64)
                        .ifBlank { defaultFileName(mediaType) }

                val mimeType = inferMimeType(fileName, mediaType)

                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0.3f)))
                }

                val fileBytes = withContext(Dispatchers.IO) {
                    mediaResolver.readBytes(localUri)
                }

                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0.5f)))
                }

                val result = webAppRepository.uploadFile(
                    apiKey = apiKey,
                    fileType = mimeType,
                    fileBytes = fileBytes,
                    fileName = fileName
                )

                result.onSuccess { uploadResult ->
                    val returnedName = uploadResult.fileName ?: fileName
                    updateInputValue(nodeId, fieldName, returnedName)
                    _uiState.update {
                        it.copy(
                            uploadingNodes = it.uploadingNodes - nodeId
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
        val state = _uiState.value
        if (state.isRunningTask) return  // guard against concurrent submissions
        val detail = state.detail ?: return
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

    private fun defaultFileName(mediaType: MediaType): String = when (mediaType) {
        MediaType.IMAGE -> "upload.png"
        MediaType.VIDEO -> "upload.mp4"
        MediaType.AUDIO -> "upload.mp3"
    }

    private fun inferMimeType(fileName: String, mediaType: MediaType): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".webp") -> "image/webp"
            lowerName.endsWith(".gif") -> "image/gif"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
            lowerName.endsWith(".mp4") -> "video/mp4"
            lowerName.endsWith(".mov") -> "video/quicktime"
            lowerName.endsWith(".webm") -> "video/webm"
            lowerName.endsWith(".mp3") -> "audio/mpeg"
            lowerName.endsWith(".m4a") -> "audio/mp4"
            lowerName.endsWith(".wav") -> "audio/wav"
            lowerName.endsWith(".aac") -> "audio/aac"
            lowerName.endsWith(".ogg") -> "audio/ogg"
            mediaType == MediaType.VIDEO -> "video/mp4"
            mediaType == MediaType.AUDIO -> "audio/mpeg"
            else -> "image/jpeg"
        }
    }

    companion object {
        fun inputKey(node: InputNode): String = "${node.nodeId}:${node.fieldName}"
        fun inputKey(nodeId: String, fieldName: String): String = "$nodeId:$fieldName"
    }
}
