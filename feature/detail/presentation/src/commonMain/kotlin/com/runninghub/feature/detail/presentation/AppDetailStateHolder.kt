package com.runninghub.feature.detail.presentation

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.TaskOutput
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.feature.task.domain.WebAppTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

/**
 * AppDetail 页面可选择上传的媒体类型。
 *
 * 该枚举位于 Detail Presentation 层，避免 StateHolder 依赖 composeApp 的上传控件类型。
 * 应用壳负责把它映射到平台媒体选择器、权限模型和具体 UI 控件所需的类型。
 */
enum class AppDetailMediaType {
    IMAGE,
    VIDEO,
    AUDIO,
}

/**
 * AppDetail 输入字段在 UI 中应使用的控件语义。
 *
 * 本类型只描述输入控件类别和运行时服务端选项，不依赖 Compose、Material 组件或平台媒体选择器。
 * composeApp 负责把这些语义映射为下拉框、分段按钮、文本框或上传控件。
 */
/**
 * AppDetail 任务执行进度阶段。
 *
 * 该枚举只表达详情页任务提交和输出轮询的稳定 UI 阶段；具体进度条样式仍由 composeApp
 * 的视觉组件映射处理，避免 Presentation 模块反向依赖应用 UI 组件。
 */
enum class AppDetailTaskStep {
    IDLE,
    SUBMITTING,
    QUEUEING,
    RUNNING,
    COMPLETING,
    SUCCESS,
    FAILED,
}

/**
 * AppDetail 页面错误的稳定展示语义。
 *
 * Presentation 状态只保存错误原因，不直接保存最终中文文案。composeApp 负责把这些原因映射到
 * Compose Resources，避免 Feature Presentation 模块继续扩大硬编码 UI 文案基线。
 */
enum class AppDetailErrorText {
    /**
     * 公开详情和 API demo fallback 均加载失败。
     *
     * UI 可展示重试入口；该错误不包含服务端 `msg` 或底层异常摘要。
     */
    DetailLoadFailed,

    /**
     * 任务提交请求失败。
     *
     * 表示远端任务尚未成功创建，用户可以稍后重新提交当前参数。
     */
    TaskSubmitFailed,

    /**
     * 任务输出轮询返回失败输出。
     *
     * 表示任务已进入终态失败，具体服务端失败原因不进入页面状态。
     */
    TaskFailed,

    /**
     * 任务输出在最大轮询次数内仍未产生可展示结果。
     *
     * 表示客户端停止等待；远端任务是否最终完成需要用户稍后通过历史记录查看。
     */
    TaskTimeout,

    /**
     * 详情页仍有媒体文件正在上传。
     *
     * 任务提交必须等待所有上传完成后才能继续，避免把本地 URI、空值或旧远端文件名提交给后端。
     */
    MediaUploadPending,

    /**
     * 详情页存在上传失败的媒体文件。
     *
     * 用户需要删除失败文件或重新上传成功后才能提交，避免远端任务收到缺失素材参数。
     */
    MediaUploadFailed,
}

/**
 * AppDetail 媒体读取端口。
 *
 * StateHolder 只通过该接口读取本地 URI 的显示名、大小和字节内容；Android/iOS 的真实权限、
 * URI 访问和安全范围由 composeApp 平台适配层负责实现。
 */
interface AppDetailMediaReader {
    /**
     * 读取本地媒体 URI 对应的字节内容。
     *
     * @param uri 平台媒体选择器返回的本地 URI 字符串；格式由平台适配层保证。
     * @return 文件字节；读取失败时允许实现抛出异常，调用方会转为上传失败状态。
     */
    fun readBytes(uri: String): ByteArray

    /**
     * 读取本地媒体 URI 的展示文件名。
     *
     * @param uri 平台媒体选择器返回的本地 URI 字符串。
     * @return 可用于上传接口的文件名；`null` 表示平台无法解析，调用方会从 URI 或默认名降级。
     */
    fun getDisplayName(uri: String): String?

    /**
     * 读取本地媒体 URI 的文件大小。
     *
     * @param uri 平台媒体选择器返回的本地 URI 字符串。
     * @return 文件大小，单位为字节；无法解析时返回 `0`，表示不阻断上传但不展示大小。
     */
    fun getFileSizeBytes(uri: String): Long
}

/**
 * WebApp 详情页页面状态。
 *
 * 状态由 [AppDetailStateHolder] 维护，只保存详情页渲染、输入编辑、媒体上传和任务轮询状态；
 * API Key、Cookie、Token 或完整认证请求头不得进入本对象。
 *
 * @property isLoading 是否正在加载 WebApp 详情。
 * `true` 表示首屏详情请求仍在进行，页面应展示整体加载态；`false` 表示当前没有详情加载请求。
 * @property detail 当前 WebApp 详情，来源于公开目录详情接口或 API demo fallback。
 * `null` 表示尚未加载成功或详情加载失败；任务提交必须在非空时才允许继续。
 * @property inputValues 用户在任务输入表单中编辑的节点值。
 * Key 使用 [appDetailInputKey] 生成，空 Map 表示详情尚未初始化或没有可编辑字段；值为空字符串表示服务端字段为
 * `null` 或用户清空输入。
 * @property isRunningTask 是否已有任务提交或轮询正在进行。
 * `true` 时应禁用重复提交；`false` 表示当前没有活跃远端任务轮询。
 * @property taskStep 当前任务执行阶段。
 * 默认 [AppDetailTaskStep.IDLE] 表示尚未提交；成功、失败或重置后由 StateHolder 按任务结果更新。
 * @property taskElapsedSeconds 当前任务从提交开始的已耗时秒数，单位为秒。
 * `0` 表示尚未开始或刚刚重置；不允许出现负数。
 * @property taskOutputs 已完成任务的输出文件列表，来源于任务输出轮询接口。
 * 空列表表示尚未完成、任务失败或服务端没有返回可展示输出；列表顺序保留服务端返回顺序。
 * @property taskError 任务提交或轮询失败时的稳定错误原因。
 * `null` 表示没有任务错误；非空时由 composeApp 映射最终文案并展示，调用 [resetTask] 后清空。
 * @property uploadingNodes 正在上传或上传失败的输入节点状态。
 * Key 为节点 ID；节点上传成功后会从 Map 移除，失败时保留 [AppDetailUploadingState.isError] 供 UI 展示重试状态。
 * @property localUris 用户选择的本地媒体 URI。
 * Key 为节点 ID；只在用户通过平台选择器选中文件时写入，直接调用 [uploadFile] 不会自动写入该 Map。
 * @property pendingMediaPick 等待平台媒体选择器回填的节点信息。
 * `null` 表示当前没有挂起选择请求；非空时 composeApp 应启动平台选择器并在回调后清空。
 * @property error 详情加载失败时的页面级稳定错误原因。
 * `null` 表示没有页面级错误；非空时由 composeApp 映射最终文案并展示重试入口。
 */
data class AppDetailUiState(
    val isLoading: Boolean = true,
    val detail: AppDetail? = null,
    val inputValues: Map<String, String> = emptyMap(),
    val isRunningTask: Boolean = false,
    val taskStep: AppDetailTaskStep = AppDetailTaskStep.IDLE,
    val taskElapsedSeconds: Int = 0,
    val taskOutputs: List<TaskOutput> = emptyList(),
    val taskError: AppDetailErrorText? = null,
    val uploadingNodes: Map<String, AppDetailUploadingState> = emptyMap(),
    val localUris: Map<String, String> = emptyMap(),
    val pendingMediaPick: AppDetailPendingMediaPick? = null,
    val error: AppDetailErrorText? = null,
)

/**
 * 单个输入节点的上传状态。
 *
 * @property localUri 用户选择或调用方传入的本地媒体 URI；只用于当前页面展示，不跨页面持久化。
 * @property progress 上传进度，范围 0.0 到 1.0；`0f` 表示尚未开始或已经失败。
 * @property isError 上传是否已经失败。
 * `true` 表示上传请求或本地读取失败，UI 应展示错误态；`false` 表示上传仍在进行。
 */
data class AppDetailUploadingState(
    val localUri: String,
    val progress: Float = 0f,
    val isError: Boolean = false,
)

/**
 * 等待平台媒体选择结果的节点信息。
 *
 * @property nodeId 目标输入节点 ID，来源于 [InputNode.nodeId]，用于回写本地 URI 和上传状态。
 * @property fieldName 目标输入字段名，来源于 [InputNode.fieldName]，用于生成输入值 Map 的 Key。
 * @property mediaType 本次期望选择的媒体类型，由 UI 根据节点字段含义推断。
 * @property requestId 本次选择请求序号，从 1 递增；用于区分用户连续触发的选择动作。
 */
data class AppDetailPendingMediaPick(
    val nodeId: String,
    val fieldName: String,
    val mediaType: AppDetailMediaType,
    val requestId: Long,
)

/**
 * AppDetail 已提交任务的轻量历史快照。
 *
 * 该模型只携带历史页首屏展示和本地兜底所需的非敏感信息；不得加入 API Key、请求体、上传文件名、
 * Cookie、Token 或服务端内部错误文本。
 *
 * @property taskId 远端任务 ID。
 * @property webappId 来源 AI 应用 ID。
 * @property taskName 来源 AI 应用名称。
 * @property status 当前任务状态协议值。
 */
data class AppDetailSubmittedTask(
    val taskId: String,
    val webappId: String?,
    val taskName: String?,
    val status: String,
)

/**
 * 持有 AppDetail 页面状态并协调详情加载、输入编辑、媒体上传和任务轮询。
 *
 * 本类位于 Detail Presentation 层，只依赖公开目录和任务领域仓库契约，不依赖 Compose、Voyager、
 * Ktor、DataStore 或平台 URI 类型。应用壳负责传入与页面生命周期绑定的 [coroutineScope]，
 * 并用 [AppDetailMediaReader] 适配真实平台媒体读取能力。
 *
 * @param webAppCatalogRepository WebApp 公开目录仓库，用于读取详情页公开信息。
 * @param webAppTaskRepository WebApp 任务仓库，用于 API demo fallback、媒体上传、任务提交和输出轮询。
 * @param mediaReader 本地媒体读取端口，由 composeApp 适配 Android/iOS 平台能力。
 * @param coroutineScope 页面生命周期绑定的协程作用域；作用域取消后本类发起的请求和轮询也应停止。
 * @param ioDispatcher 媒体字节读取使用的调度器，默认使用跨平台可用的 [Dispatchers.Default]。
 * @param maxLocalUploadBytes 本地一次性读入内存前允许的最大文件大小；超过后直接进入上传失败态。
 * @param onTaskHistoryInvalidated 远端任务被服务端接收或终态变化后触发的历史刷新信号。
 * @param onTaskHistoryTaskChanged 已提交 WebApp 任务的本地历史快照变化回调，用于服务端宽表同步前兜底展示。
 */
class AppDetailStateHolder(
    private val webAppCatalogRepository: WebAppCatalogRepository,
    private val webAppTaskRepository: WebAppTaskRepository,
    private val mediaReader: AppDetailMediaReader,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val maxLocalUploadBytes: Long = LOCAL_MEDIA_UPLOAD_MAX_BYTES,
    private val onTaskHistoryInvalidated: () -> Unit = {},
    private val onTaskHistoryTaskChanged: (AppDetailSubmittedTask) -> Unit = {},
) {
    private val _uiState = MutableStateFlow(AppDetailUiState())

    /**
     * AppDetail 只读页面状态。
     *
     * UI 只能收集该流并通过公开动作函数发送用户事件，避免 composeApp 重新实现详情页状态转换规则。
     */
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private var currentAppId: String = ""
    private var mediaPickRequestId: Long = 0L
    private var pollingJob: Job? = null

    /**
     * 加载 WebApp 详情。
     *
     * 第一阶段优先读取公开目录详情；第二阶段在公开详情失败时降级读取 API demo，兼容需要凭据的旧详情入口；
     * 第三阶段把服务端节点默认值初始化为可编辑输入 Map。相同 appId 已有详情时会跳过重复请求。
     *
     * @param appId WebApp ID，来源于详情页导航参数；空字符串会进入仓库并按仓库结果处理。
     */
    fun loadDetail(appId: String) {
        if (appId == currentAppId && _uiState.value.detail != null) return
        currentAppId = appId

        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val detail = webAppCatalogRepository.getAppDetail(appId).getOrNull()
                ?: webAppTaskRepository.getApiCallDemo(appId).getOrNull()

            if (detail != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        detail = detail,
                        inputValues = detail.inputNodes.associate { node ->
                            appDetailInputKey(node) to (node.fieldValue ?: "")
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = AppDetailErrorText.DetailLoadFailed)
                }
            }
        }
    }

    /**
     * 更新单个任务输入字段。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param value 用户当前编辑值，允许为空字符串，提交任务时会原样写入对应节点。
     */
    fun updateInputValue(nodeId: String, fieldName: String, value: String) {
        _uiState.update {
            it.copy(inputValues = it.inputValues + (appDetailInputKey(nodeId, fieldName) to value))
        }
    }

    /**
     * 记录用户选择的本地文件 URI，并把节点切入上传初始态。
     *
     * @param nodeId 输入节点 ID。
     * @param localUri 平台媒体选择器返回的本地 URI 字符串，只保存在当前页面状态中。
     */
    fun setLocalFileUri(nodeId: String, localUri: String) {
        _uiState.update {
            it.copy(
                localUris = it.localUris + (nodeId to localUri),
                uploadingNodes = it.uploadingNodes + (nodeId to AppDetailUploadingState(localUri = localUri, progress = 0f)),
            )
        }
    }

    /**
     * 记录等待平台媒体选择器处理的输入节点。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param mediaType 期望选择的媒体类型。
     */
    fun setPendingMediaPick(nodeId: String, fieldName: String, mediaType: AppDetailMediaType) {
        mediaPickRequestId += 1
        _uiState.update {
            it.copy(pendingMediaPick = AppDetailPendingMediaPick(nodeId, fieldName, mediaType, mediaPickRequestId))
        }
    }

    /**
     * 清空当前挂起的媒体选择请求。
     *
     * 通常在平台选择器取消、权限拒绝或回调已处理后调用，避免重组时重复打开选择器。
     */
    fun clearPendingMediaPick() {
        _uiState.update { it.copy(pendingMediaPick = null) }
    }

    /**
     * 处理平台媒体选择器返回的本地 URI。
     *
     * 本方法只在存在 [AppDetailUiState.pendingMediaPick] 时生效；它先记录本地 URI，再启动上传，
     * 最后清空挂起请求，避免同一 URI 因页面重组被重复上传。
     *
     * @param uri 平台媒体选择器返回的本地 URI 字符串。
     */
    fun onMediaUriReceived(uri: String) {
        val pending = _uiState.value.pendingMediaPick ?: return
        setLocalFileUri(pending.nodeId, uri)
        uploadFile(pending.nodeId, pending.fieldName, uri, pending.mediaType)
        _uiState.update { it.copy(pendingMediaPick = null) }
    }

    /**
     * 上传本地媒体文件并把远端文件名写回输入值。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param localUri 本地媒体 URI 字符串。
     * @param mediaType 文件媒体类型，默认按图片处理。
     */
    fun uploadFile(
        nodeId: String,
        fieldName: String,
        localUri: String,
        mediaType: AppDetailMediaType = AppDetailMediaType.IMAGE,
    ) {
        coroutineScope.launch {
            _uiState.update {
                it.copy(
                    uploadingNodes = it.uploadingNodes + (nodeId to AppDetailUploadingState(localUri = localUri, progress = 0.1f)),
                )
            }

            try {
                val displayName = mediaReader.getDisplayName(localUri)
                val fileName = displayName
                    ?: localUri
                        .substringAfterLast("/")
                        .substringAfterLast("%2F")
                        .substringAfterLast(":")
                        .take(64)
                        .ifBlank { defaultFileName(mediaType) }
                val mimeType = inferMimeType(fileName, mediaType)
                val fileSizeBytes = mediaReader.getFileSizeBytes(localUri)
                if (fileSizeBytes > 0L && fileSizeBytes > maxLocalUploadBytes) {
                    markUploadFailed(nodeId, localUri)
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        uploadingNodes = it.uploadingNodes + (nodeId to AppDetailUploadingState(localUri = localUri, progress = 0.3f)),
                    )
                }

                // 媒体读取可能触发平台文件访问或安全范围检查；通过可注入调度器隔离耗时操作，
                // 同时保持 commonMain 不依赖 JVM/Android 专属的 Dispatchers.IO。
                val fileBytes = withContext(ioDispatcher) {
                    mediaReader.readBytes(localUri)
                }

                _uiState.update {
                    it.copy(
                        uploadingNodes = it.uploadingNodes + (nodeId to AppDetailUploadingState(localUri = localUri, progress = 0.5f)),
                    )
                }

                webAppTaskRepository.uploadFile(
                    fileType = mimeType,
                    fileBytes = fileBytes,
                    fileName = fileName,
                ).onSuccess { uploadResult ->
                    val returnedName = uploadResult.fileName ?: fileName
                    updateInputValue(nodeId, fieldName, returnedName)
                    _uiState.update {
                        it.copy(uploadingNodes = it.uploadingNodes - nodeId)
                    }
                }.onFailure {
                    markUploadFailed(nodeId, localUri)
                }
            } catch (error: CancellationException) {
                // 协程取消通常来自页面销毁或调用方主动停止流程，必须继续向上传播，
                // 否则取消会被误写成上传失败态并污染下一次进入详情页的状态。
                throw error
            } catch (_: Exception) {
                // 读取本地 URI 或推断文件信息失败时只进入上传失败态，不把平台异常文本展示给用户。
                markUploadFailed(nodeId, localUri)
            }
        }
    }

    /**
     * 移除用户已选择或已上传的本地文件状态。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名；用于从 [AppDetailUiState.inputValues] 清理对应远端文件名。
     */
    fun removeLocalFile(nodeId: String, fieldName: String) {
        _uiState.update {
            it.copy(
                localUris = it.localUris - nodeId,
                uploadingNodes = it.uploadingNodes - nodeId,
                inputValues = it.inputValues - appDetailInputKey(nodeId, fieldName),
            )
        }
    }

    /**
     * 提交当前 WebApp 任务并启动输出轮询。
     *
     * 任务提交会捕获当前输入 Map 并写回 [InputNode.fieldValue]，避免提交后 UI 后续编辑影响已发出的请求；
     * 活跃任务期间重复调用会被忽略，防止同一详情页产生多个远端任务。
     */
    fun runTask() {
        val state = _uiState.value
        if (state.isRunningTask) return
        val detail = state.detail ?: return
        val uploadError = state.uploadingNodes.values.firstOrNull { it.isError }
        if (uploadError != null) {
            _uiState.update {
                it.copy(
                    taskStep = AppDetailTaskStep.IDLE,
                    taskError = AppDetailErrorText.MediaUploadFailed,
                )
            }
            return
        }
        val uploadPending = state.uploadingNodes.isNotEmpty()
        if (uploadPending) {
            _uiState.update {
                it.copy(
                    taskStep = AppDetailTaskStep.IDLE,
                    taskError = AppDetailErrorText.MediaUploadPending,
                )
            }
            return
        }
        coroutineScope.launch {
            _uiState.update {
                it.copy(
                    isRunningTask = true,
                    taskStep = AppDetailTaskStep.SUBMITTING,
                    taskError = null,
                    taskOutputs = emptyList(),
                    taskElapsedSeconds = 0,
                )
            }

            val inputNodes = detail.inputNodes.map { node ->
                node.copy(
                    fieldValue = _uiState.value.inputValues[appDetailInputKey(node)] ?: node.fieldValue,
                )
            }

            webAppTaskRepository.runTask(
                webappId = detail.id.toLongOrNull() ?: 0L,
                nodeInfoList = inputNodes,
            ).onSuccess { taskResult ->
                onTaskHistoryTaskChanged(detail.toSubmittedTask(taskResult.taskId, taskResult.status?.rawValue ?: "SUBMITTED"))
                onTaskHistoryInvalidated()
                startTaskOutputPolling(taskResult.taskId)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isRunningTask = false,
                        taskStep = AppDetailTaskStep.FAILED,
                        // 任务提交失败可能包含远端 msg、状态码或底层异常摘要；页面状态只保存稳定原因，
                        // 最终文案由 composeApp 资源映射，避免 Presentation 继续直接产出中文 UI 文案。
                        taskError = AppDetailErrorText.TaskSubmitFailed,
                    )
                }
            }
        }
    }

    /**
     * 重置任务结果展示状态。
     *
     * 该函数只清理当前页面中的任务输出、错误和进度，不取消已经完成的远端任务。
     */
    fun resetTask() {
        _uiState.update {
            it.copy(
                taskOutputs = emptyList(),
                taskError = null,
                taskStep = AppDetailTaskStep.IDLE,
                taskElapsedSeconds = 0,
            )
        }
    }

    /**
     * 释放详情页持有的轮询任务。
     *
     * 应由 Voyager ScreenModel 或外层生命周期调用；取消后不会继续因为不可见页面而查询任务输出。
     */
    fun dispose() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun startTaskOutputPolling(taskId: Long) {
        pollingJob?.cancel()
        pollingJob = coroutineScope.launch {
            var attempts = 0
            val startTime = Clock.System.now().toEpochMilliseconds()

            while (attempts < MAX_TASK_OUTPUT_POLLING_ATTEMPTS) {
                val elapsed = ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).toInt()
                _uiState.update {
                    it.copy(
                        taskStep = when {
                            attempts < 1 -> AppDetailTaskStep.SUBMITTING
                            attempts < 3 -> AppDetailTaskStep.QUEUEING
                            else -> AppDetailTaskStep.RUNNING
                        },
                        taskElapsedSeconds = elapsed,
                    )
                }

                delay(TASK_OUTPUT_POLLING_INTERVAL_MILLIS)
                attempts++

                webAppTaskRepository.getTaskOutputs(taskId)
                    .onSuccess { outputs ->
                        if (outputs.isEmpty()) return@onSuccess

                        val failed = outputs.firstOrNull { it.failedReason != null }
                        if (failed != null) {
                            notifySubmittedTaskStatus(taskId, "FAILED")
                            onTaskHistoryInvalidated()
                            _uiState.update {
                                it.copy(
                                    isRunningTask = false,
                                    taskStep = AppDetailTaskStep.FAILED,
                                    // failedReason 来自任务输出协议，可能携带服务端内部失败原因；
                                    // 页面状态只保留稳定错误原因，排错应依赖仓库日志和领域状态码。
                                    taskError = AppDetailErrorText.TaskFailed,
                                )
                            }
                            return@launch
                        }

                        if (outputs.any { !it.fileUrl.isNullOrBlank() }) {
                            notifySubmittedTaskStatus(taskId, "SUCCESS")
                            onTaskHistoryInvalidated()
                            _uiState.update {
                                it.copy(
                                    isRunningTask = false,
                                    taskStep = AppDetailTaskStep.SUCCESS,
                                    taskElapsedSeconds = ((Clock.System.now().toEpochMilliseconds() - startTime) / 1000).toInt(),
                                    taskOutputs = outputs,
                                )
                            }
                            return@launch
                        }
                    }
                    .onFailure {
                        // 单次轮询失败不立即终止任务，避免短暂网络抖动导致用户看不到后续成功输出。
                    }
            }

            _uiState.update {
                it.copy(
                    isRunningTask = false,
                    taskStep = AppDetailTaskStep.FAILED,
                    taskError = AppDetailErrorText.TaskTimeout,
                )
            }
            notifySubmittedTaskStatus(taskId, "FAILED")
            onTaskHistoryInvalidated()
        }
    }

    private fun notifySubmittedTaskStatus(taskId: Long, status: String) {
        val detail = _uiState.value.detail ?: return
        onTaskHistoryTaskChanged(detail.toSubmittedTask(taskId, status))
    }

    private fun markUploadFailed(nodeId: String, localUri: String) {
        _uiState.update {
            it.copy(
                uploadingNodes = it.uploadingNodes + (
                    nodeId to AppDetailUploadingState(localUri = localUri, progress = 0f, isError = true)
                    ),
            )
        }
    }

    private fun defaultFileName(mediaType: AppDetailMediaType): String = when (mediaType) {
        AppDetailMediaType.IMAGE -> "upload.png"
        AppDetailMediaType.VIDEO -> "upload.mp4"
        AppDetailMediaType.AUDIO -> "upload.mp3"
    }

    private fun inferMimeType(fileName: String, mediaType: AppDetailMediaType): String {
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
            mediaType == AppDetailMediaType.VIDEO -> "video/mp4"
            mediaType == AppDetailMediaType.AUDIO -> "audio/mpeg"
            else -> "image/jpeg"
        }
    }
}

private fun AppDetail.toSubmittedTask(taskId: Long, status: String): AppDetailSubmittedTask =
    AppDetailSubmittedTask(
        taskId = taskId.toString(),
        webappId = id,
        taskName = name,
        status = status,
    )

private const val TASK_OUTPUT_POLLING_INTERVAL_MILLIS = 5_000L
private const val MAX_TASK_OUTPUT_POLLING_ATTEMPTS = 120
private const val LOCAL_MEDIA_UPLOAD_MAX_BYTES = 100L * 1024L * 1024L

/**
 * 生成 AppDetail 输入节点值 Map 的稳定 Key。
 *
 * @param node 输入节点，使用 [InputNode.nodeId] 与 [InputNode.fieldName] 拼接。
 * @return 形如 `nodeId:fieldName` 的字符串；用于区分同一节点下不同字段。
 */
fun appDetailInputKey(node: InputNode): String = appDetailInputKey(node.nodeId, node.fieldName)

/**
 * 生成 AppDetail 输入节点值 Map 的稳定 Key。
 *
 * @param nodeId 输入节点 ID。
 * @param fieldName 输入字段名。
 * @return 形如 `nodeId:fieldName` 的字符串；调用方必须保证两个参数来自同一个输入节点。
 */
fun appDetailInputKey(nodeId: String, fieldName: String): String = "$nodeId:$fieldName"
