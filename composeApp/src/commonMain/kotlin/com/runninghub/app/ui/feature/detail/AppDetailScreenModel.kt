package com.runninghub.app.ui.feature.detail

import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.ui.component.MediaType
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.InputNode
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.core.model.TaskOutput
import com.runninghub.feature.task.domain.WebAppTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * WebApp 详情页页面状态。
 *
 * @property isLoading 是否正在加载详情。
 * @property detail 当前 WebApp 详情。
 * @property inputValues 用户在任务输入表单中编辑的节点值。
 * @property isRunningTask 是否已有任务提交或轮询正在进行。
 * @property taskStep 任务执行阶段，用于驱动进度展示。
 * @property taskElapsedSeconds 当前任务从提交开始的已耗时秒数。
 * @property taskOutputs 已完成任务的输出文件。
 * @property taskError 任务提交或轮询失败时的用户可见错误。
 * @property uploadingNodes 正在上传或上传失败的输入节点状态。
 * @property localUris 用户选择的本地媒体 URI。
 * @property pendingMediaPick 等待平台媒体选择器回填的节点信息。
 * @property error 详情加载失败时的页面级错误。
 */
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

/**
 * 单个输入节点的上传状态。
 *
 * @property localUri 用户选择的本地媒体 URI。
 * @property progress 上传进度，范围 0.0 到 1.0。
 * @property isError 上传是否已经失败。
 */
data class UploadingState(
    val localUri: String,
    val progress: Float = 0f,
    val isError: Boolean = false
)

/**
 * 等待平台媒体选择结果的节点信息。
 *
 * @property nodeId 目标输入节点 ID。
 * @property fieldName 目标输入字段名。
 * @property mediaType 期望选择的媒体类型。
 * @property requestId 本次选择请求序号，用于区分连续选择动作。
 */
data class PendingMediaPick(
    val nodeId: String,
    val fieldName: String,
    val mediaType: MediaType,
    val requestId: Long
)

/**
 * WebApp 详情页的 ScreenModel。
 *
 * 页面负责详情展示、输入状态、文件选择上传和任务轮询。API Key 等敏感凭据不在本类读取或保存，
 * 公开详情读取通过 [WebAppCatalogRepository] 完成，需要凭据的上传、提交和轮询统一交给
 * [WebAppTaskRepository] 的 Data 层实现处理。
 *
 * @param webAppCatalogRepository WebApp 目录仓库，只用于读取公开详情。
 * @param webAppTaskRepository WebApp 任务仓库，封装 API 示例、上传、任务提交和输出轮询。
 * @param mediaResolver 跨平台媒体读取能力，用于把本地 URI 转换为上传文件。
 * @param ioDispatcher 媒体字节读取使用的调度器；commonMain 默认使用跨平台可用的 Default，
 * 避免把 Kotlin/Native 不稳定的 IO 调度器暴露给共享代码，测试可注入可控调度器。
 */
class AppDetailScreenModel(
    private val webAppCatalogRepository: WebAppCatalogRepository,
    private val webAppTaskRepository: WebAppTaskRepository,
    private val mediaResolver: MediaResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
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

            val detail = webAppCatalogRepository.getAppDetail(appId).getOrNull()
                ?: run {
                    webAppTaskRepository.getApiCallDemo(appId).getOrNull()
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

                // 媒体读取可能触发平台文件访问；通过可注入调度器隔离耗时操作，
                // 同时避免 commonMain 直接依赖 JVM/Android 才稳定的 Dispatchers.IO。
                val fileBytes = withContext(ioDispatcher) {
                    mediaResolver.readBytes(localUri)
                }

                _uiState.update {
                    it.copy(uploadingNodes = it.uploadingNodes + (nodeId to UploadingState(localUri = localUri, progress = 0.5f)))
                }

                val result = webAppTaskRepository.uploadFile(
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

            val inputNodes = detail.inputNodes.map { node ->
                node.copy(
                    fieldValue = _uiState.value.inputValues[inputKey(node)] ?: node.fieldValue
                )
            }

            webAppTaskRepository.runTask(
                webappId = detail.id.toLongOrNull() ?: 0L,
                nodeInfoList = inputNodes
            ).onSuccess {
                pollTaskOutputs(it.taskId)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isRunningTask = false,
                        taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                        // 任务提交失败可能包含远端 msg、状态码或底层异常摘要；详情页只展示可操作文案，
                        // 避免把数据层诊断信息直接暴露给终端用户。
                        taskError = "提交失败，请稍后重试"
                    )
                }
            }
        }
    }

    private fun pollTaskOutputs(taskId: Long) {
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

                webAppTaskRepository.getTaskOutputs(taskId)
                    .onSuccess { outputs ->
                        if (outputs.isEmpty()) return@onSuccess

                        val failed = outputs.firstOrNull { it.failedReason != null }
                        if (failed != null) {
                            _uiState.update {
                                it.copy(
                                    isRunningTask = false,
                                    taskStep = com.runninghub.app.ui.component.TaskStep.FAILED,
                                    // failedReason 来自任务输出协议，可能携带服务端内部失败原因；
                                    // UI 保持稳定文案，后续排错应依赖仓库日志和领域状态码。
                                    taskError = "任务失败，请稍后重试"
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
