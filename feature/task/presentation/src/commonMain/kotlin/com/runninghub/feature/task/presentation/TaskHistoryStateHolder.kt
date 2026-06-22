package com.runninghub.feature.task.presentation

import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 历史页列表的业务筛选条件。
 *
 * 该枚举属于 Task Presentation 层，只表达页面可见的历史任务分组；
 * 具体状态字符串仍由 Task Domain 的 [GenerationHistoryItem.status] 承载。
 *
 */
enum class TaskHistoryFilter {
    /** 展示全部历史任务，不在本地按状态过滤。 */
    ALL,

    /** 只展示已经完成且可查看结果的历史任务。 */
    COMPLETED,

    /** 只展示失败终态的历史任务，通常允许准备重试参数。 */
    FAILED,

    /** 只展示仍处于运行、排队或其他非终态的历史任务。 */
    IN_PROGRESS,
}

/**
 * History 页面一次性操作提示的稳定语义。
 *
 * 本类型只描述发生了哪类用户操作反馈，不包含最终中文文案。composeApp 负责把语义映射为
 * Compose Resources，避免 Task Presentation 模块继续保存或拼接本地化 UI 字符串。
 */
sealed interface TaskHistoryActionMessage {
    /** 输出详情已经加载完成，可以在操作面板展示输出信息。 */
    data object OutputDetailLoaded : TaskHistoryActionMessage

    /** 用户请求复用参数，但目标历史任务没有可复用参数快照。 */
    data object NoReusableParams : TaskHistoryActionMessage

    /**
     * 已准备可复用参数。
     *
     * @property count 已写入 [TaskHistoryUiState.reuseParams] 的参数数量，单位为个；必须大于 `0`。
     */
    data class ReusableParamsPrepared(val count: Int) : TaskHistoryActionMessage

    /** 用户请求重试失败任务，但目标任务没有可用于重试的参数快照。 */
    data object NoRetryParams : TaskHistoryActionMessage

    /** 已准备失败任务的重试参数，后续仍需回到创作页确认后再提交。 */
    data object RetryParamsPrepared : TaskHistoryActionMessage

    /** 已向 Repository 发起取消任务请求，并触发列表刷新。 */
    data object CancelRequested : TaskHistoryActionMessage
}

/**
 * History 页面可见错误的稳定语义。
 *
 * Repository 可能携带远端 `msg` 或底层异常 message；StateHolder 只能输出此处定义的错误语义，
 * 最终中文文案由 composeApp 资源层决定，避免异常诊断信息直接进入 UI 状态。
 */
enum class TaskHistoryPresentationError {
    /** 历史列表加载失败，页面可以展示重试入口。 */
    HistoryLoadFailed,

    /** 单个输出详情加载失败，页面可以保留原列表并允许用户再次打开。 */
    DetailLoadFailed,

    /** 取消运行中任务失败，页面可以提示用户稍后重试。 */
    CancelFailed,

    /** 当前会话缺失、过期或权限不足，需要登录后才能同步云端历史。 */
    AuthRequired,
}

/**
 * 历史列表费用展示的稳定语义。
 *
 * 该枚举不保存最终展示金额文案，只表达当前任务应使用哪一类费用标签。
 * 费用档位暂由服务端任务标题和任务状态推断，后续服务端提供稳定类型字段后应替换本地启发式。
 */
enum class TaskHistoryCostText {
    /** 标题命中视频任务启发式，列表展示视频费用档位。 */
    VIDEO,

    /** 标题命中角色任务启发式，列表展示角色费用档位。 */
    CHARACTER,

    /** 非视频/角色任务失败后展示的失败兜底费用档位。 */
    FAILED,

    /** 未命中专项规则时展示的默认费用档位。 */
    DEFAULT,
}

/**
 * 历史列表输出数量展示的稳定语义。
 *
 * 该枚举不保存最终中文文案，只表达 UI 应选择的输出数量标签。
 * 运行中和失败态优先由任务状态决定；完成态再按任务标题启发式区分视频和默认输出。
 */
enum class TaskHistoryOutputCountText {
    /** 失败任务输出数量标签。 */
    FAILED,

    /** 运行中任务输出数量标签。 */
    RUNNING,

    /** 完成的视频任务输出数量标签。 */
    VIDEO,

    /** 完成的默认任务输出数量标签。 */
    COMPLETED,
}

/**
 * 历史页的完整可渲染状态。
 *
 * 状态由 [TaskHistoryStateHolder] 维护，只保存页面交互所需的任务历史、筛选、
 * 当前选中输出和一次性提示；认证凭据、Cookie 或 Token 不得进入本对象。
 *
 * @property isLoading 是否正在加载第一页历史记录。
 * `true` 表示首屏或用户手动刷新请求仍在进行，页面应展示加载态；
 * `false` 表示当前没有会影响整个列表的加载请求。后台轮询刷新不会把该值置为 `true`。
 * @property allItems 最近一次历史列表请求返回的原始任务集合，元素顺序保持服务端返回顺序，
 * 因为该顺序可能代表服务端的创建时间或运营排序；空集合表示尚未加载成功或服务端确实无数据。
 * @property items 根据 [filter] 从 [allItems] 派生出的轻量列表项，供 UI 直接渲染。
 * 集合顺序与 [allItems] 一致，不允许客户端为展示目的重新排序；空集合表示当前筛选条件下无可见任务。
 * @property filter 当前选中的历史任务筛选条件。
 * 默认 [TaskHistoryFilter.ALL] 表示展示全部任务；修改该值只影响本地派生列表，不触发远程请求。
 * @property selectedDetail 当前被用户打开的历史任务详情。
 * `null` 表示用户尚未打开详情，或详情加载失败后保持没有可展示详情；该值来自服务端详情接口或已缓存列表项。
 * @property selectedOutput 当前被用户打开的具体输出。
 * `null` 表示未选择输出、详情中找不到目标输出，或当前任务没有可查看输出；
 * 非空时 UI 可以展示预览地址、下载地址或输出元数据。
 * @property isDetailLoading 是否正在加载单个输出对应的历史详情。
 * `true` 时详情区域应展示进度并避免重复点击；`false` 表示没有进行中的详情请求。
 * @property reuseParams 当前已准备复用或重试的任务参数。
 * Key/Value 均来自历史任务参数快照；空 Map 表示没有可复用参数，或用户尚未触发复用/重试动作。
 * @property actionMessage 等待页面展示的一次性操作提示语义。
 * `null` 表示当前没有待展示提示；非空通常由 Snackbar 或操作面板映射为资源文案后展示，
 * 下一次加载、详情请求或失败会覆盖/清空。该字段不得保存最终中文文案。
 * @property error 等待页面展示的错误语义。
 * `null` 表示当前没有错误；非空时页面负责映射为资源文案并允许用户重试。
 * 该字段不得暴露 Repository 的底层异常细节或服务端原始 message。
 */
data class TaskHistoryUiState(
    val isLoading: Boolean = false,
    val allItems: List<GenerationHistoryItem> = emptyList(),
    val items: List<TaskHistoryEntry> = emptyList(),
    val filter: TaskHistoryFilter = TaskHistoryFilter.ALL,
    val selectedDetail: GenerationHistoryItem? = null,
    val selectedOutput: GenerationHistoryOutput? = null,
    val isDetailLoading: Boolean = false,
    val reuseParams: Map<String, String> = emptyMap(),
    val actionMessage: TaskHistoryActionMessage? = null,
    val error: TaskHistoryPresentationError? = null,
)

/**
 * 历史页列表渲染所需的轻量任务条目。
 *
 * 该模型由 [GenerationHistoryItem] 映射而来，目的是让 UI 不需要理解完整任务详情结构；
 * Domain 原始对象仍保存在 [TaskHistoryUiState.allItems] 中，供复用参数和详情兜底使用。
 *
 * @property taskId 历史任务的稳定标识，来源于服务端任务 ID。
 * 空字符串不应出现；调用取消、重试和复用参数时使用该值定位原始任务。
 * @property title 页面展示的任务标题，优先来自任务类型，其次来自模型 ID。
 * 当服务端缺失标题类字段时使用 `"Generation task"` 作为兜底文案。
 * @property status 服务端任务状态原始字符串。
 * UI 会根据兼容状态集合映射展示文案；空字符串不应出现，未知状态按进行中或未知态处理。
 * @property costTime 服务端返回的任务耗时展示值。
 * `null` 表示服务端未提供耗时或任务仍未结束；非空时按服务端格式原样展示，不在客户端换算单位。
 * @property source 任务来源标识，来自 [GenerationHistoryItem.source]。
 * 该值用于区分快捷创作、标准模型或 WebApp 历史；空字符串不应出现。
 * @property outputId 第一项输出的稳定标识。
 * `null` 表示任务没有可打开输出；非空时可传给详情接口加载完整输出上下文。
 * @property thumbnailUrl 第一项输出的缩略图或原图 URL。
 * `null` 表示当前任务没有可预览图片；非空值可能是远程 URL，访问权限和有效期由服务端控制。
 * @property outputCount 当前任务输出数量，单位为个。
 * `0` 表示没有输出；不允许为负数。该值只用于列表展示，不代表详情接口中的最新数量。
 * @property costText 当前任务费用展示语义。
 * 该字段不得保存最终金额文案；UI 应通过 Compose Resources 把语义映射为可本地化展示值。
 * @property outputCountText 当前任务输出数量展示语义。
 * 该字段不得保存最终中文文案；UI 应通过 Compose Resources 把语义映射为可本地化展示值。
 * @property canViewOutput 是否允许用户查看输出。
 * `true` 表示至少存在一个输出 ID；`false` 表示没有可打开输出，查看按钮应隐藏或禁用。
 * @property canReuseParams 是否允许用户复用本次任务参数。
 * `true` 表示历史项包含非空参数快照；`false` 表示无参数可复用。
 * @property canRetry 是否允许用户按失败任务准备重试参数。
 * `true` 表示任务状态属于失败集合；`false` 表示当前状态不应展示重试入口。
 * @property canCancel 是否允许用户发起取消请求。
 * `true` 表示任务仍处于非终态；`false` 表示任务已成功、失败或取消，客户端不再发起取消。
 */
data class TaskHistoryEntry(
    val taskId: String,
    val title: String,
    val status: String,
    val costTime: String? = null,
    val source: String,
    val outputId: String? = null,
    val thumbnailUrl: String? = null,
    val outputCount: Int = 0,
    val costText: TaskHistoryCostText = TaskHistoryCostText.DEFAULT,
    val outputCountText: TaskHistoryOutputCountText = TaskHistoryOutputCountText.COMPLETED,
    val canViewOutput: Boolean = false,
    val canReuseParams: Boolean = false,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
)

/**
 * 持有历史页状态并协调列表、详情、复用参数和轮询流程。
 *
 * 本类位于 Task Presentation 层，依赖 Task Domain 的 [GenerationHistoryRepository]，
 * 但不依赖 Compose、Voyager 或平台 API。应用壳负责把自己的生命周期 scope 注入进来，
 * 并在页面销毁时调用 [dispose] 取消轮询。
 *
 * @param generationHistoryRepository 历史任务仓库接口，负责列表、详情和取消任务请求。
 * @param coroutineScope 页面生命周期绑定的协程作用域；该作用域取消后，本类发起的异步任务也应停止。
 * @param enablePolling 是否启用进行中任务的后台轮询。
 * `true` 表示列表中存在非终态任务时每 10 秒刷新一次；`false` 主要用于单元测试或临时禁用轮询。
 */
class TaskHistoryStateHolder(
    private val generationHistoryRepository: GenerationHistoryRepository,
    private val coroutineScope: CoroutineScope,
    private val enablePolling: Boolean = true,
) {
    private val _uiState = MutableStateFlow(TaskHistoryUiState())

    /**
     * 历史页只读状态流。
     *
     * 调用方只能收集该流并根据 [TaskHistoryUiState] 渲染页面；
     * 所有状态修改必须通过本类公开动作完成，避免 UI 绕过业务规则直接改状态。
     */
    val uiState: StateFlow<TaskHistoryUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    /**
     * 加载第一页历史记录。
     *
     * 该方法会启动异步请求并进入全局加载态；成功后按当前筛选条件派生列表，
     * 失败时只展示稳定中文错误，不暴露底层异常消息。
     */
    fun loadHistory() {
        coroutineScope.launch { refreshHistory(showLoading = true) }
    }

    /**
     * 切换本地历史筛选条件。
     *
     * 筛选只基于最近一次成功加载的 [TaskHistoryUiState.allItems] 派生，
     * 不触发网络请求，避免用户切换 Tab 时反复拉取同一页数据。
     *
     * @param filter 目标筛选条件。
     */
    fun setFilter(filter: TaskHistoryFilter) {
        _uiState.update { state ->
            state.copy(
                filter = filter,
                items = state.allItems.toEntries(filter),
            )
        }
    }

    /**
     * 加载指定输出对应的历史详情。
     *
     * @param outputId 输出 ID，来源于 [TaskHistoryEntry.outputId]。
     * 详情接口成功后会同时写入任务详情和当前输出；失败时保留原详情并展示可重试错误。
     */
    fun selectOutput(outputId: String) {
        coroutineScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, error = null, actionMessage = null) }
            generationHistoryRepository.getHistoryDetail(outputId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            selectedDetail = detail,
                            selectedOutput = detail.outputs.firstOrNull { output -> output.outputId == outputId },
                            isDetailLoading = false,
                            actionMessage = TaskHistoryActionMessage.OutputDetailLoaded,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            error = error.toHistoryPresentationError(TaskHistoryPresentationError.DetailLoadFailed),
                        )
                    }
                }
        }
    }

    /**
     * 从历史任务中准备可复用参数。
     *
     * @param taskId 历史任务 ID；优先从已加载列表查找，找不到时使用当前详情兜底。
     * 当任务没有参数快照时会给出可见提示，但不会清空当前详情。
     */
    fun prepareReuseParams(taskId: String) {
        val item = _uiState.value.allItems.firstOrNull { it.taskId == taskId }
            ?: _uiState.value.selectedDetail?.takeIf { it.taskId == taskId }
            ?: return
        _uiState.update {
            it.copy(
                selectedDetail = item,
                reuseParams = item.params,
                actionMessage = item.params.toReuseActionMessage(),
            )
        }
    }

    /**
     * 为失败任务准备重试参数。
     *
     * @param taskId 历史任务 ID；本方法只准备参数和提示，不直接提交新任务，
     * 避免 History 页面绕过创作页的配置校验、计费预览和用户确认。
     */
    fun retryTask(taskId: String) {
        val item = _uiState.value.allItems.firstOrNull { it.taskId == taskId }
            ?: _uiState.value.selectedDetail?.takeIf { it.taskId == taskId }
            ?: return
        _uiState.update {
            it.copy(
                selectedDetail = item,
                reuseParams = item.params,
                actionMessage = item.params.toRetryActionMessage(),
            )
        }
    }

    /**
     * 请求取消仍在运行的历史任务。
     *
     * @param taskId 需要取消的任务 ID。
     * 成功后立即刷新列表，使终态和按钮可用性尽快与服务端一致；失败时展示稳定中文错误。
     */
    fun cancelTask(taskId: String) {
        coroutineScope.launch {
            generationHistoryRepository.cancelTask(taskId)
                .onSuccess {
                    _uiState.update {
                        state -> state.copy(actionMessage = TaskHistoryActionMessage.CancelRequested, error = null)
                    }
                    refreshHistory(showLoading = false)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            actionMessage = null,
                            error = error.toHistoryPresentationError(TaskHistoryPresentationError.CancelFailed),
                        )
                    }
                }
        }
    }

    /**
     * 释放页面持有的轮询任务。
     *
     * 应由应用壳的生命周期回调调用；取消后不会再因为不可见 History Tab 继续刷新历史列表。
     */
    fun dispose() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refreshHistory(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null, actionMessage = null) }
        }
        generationHistoryRepository.listHistory(page = 1, size = 50)
            .onSuccess { page ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allItems = page.items,
                        items = page.items.toEntries(state.filter),
                        error = null,
                    )
                }
                updatePolling(page.items)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.toHistoryPresentationError(TaskHistoryPresentationError.HistoryLoadFailed),
                    )
                }
                updatePolling(emptyList())
            }
    }

    private fun updatePolling(items: List<GenerationHistoryItem>) {
        if (!enablePolling || items.none { it.isRunning }) {
            pollingJob?.cancel()
            pollingJob = null
            return
        }
        if (pollingJob?.isActive == true) return
        pollingJob = coroutineScope.launch {
            // 只有当前列表仍存在非终态任务时才继续轮询，避免任务结束后保持后台请求。
            while (_uiState.value.allItems.any { it.isRunning }) {
                delay(HISTORY_POLLING_INTERVAL_MILLIS)
                refreshHistory(showLoading = false)
            }
        }
    }
}

private const val HISTORY_POLLING_INTERVAL_MILLIS = 10_000L

private fun List<GenerationHistoryItem>.toEntries(filter: TaskHistoryFilter): List<TaskHistoryEntry> =
    filterByStatus(filter).map { it.toTaskHistoryEntry() }

private fun List<GenerationHistoryItem>.filterByStatus(filter: TaskHistoryFilter): List<GenerationHistoryItem> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.status.isCompletedStatus() }
    TaskHistoryFilter.FAILED -> filter { it.status.isFailedStatus() }
    TaskHistoryFilter.IN_PROGRESS -> filter { it.isRunning }
}

private fun GenerationHistoryItem.toTaskHistoryEntry(): TaskHistoryEntry {
    val primaryOutput = outputs.firstOrNull()
    val titleText = taskType ?: modelId ?: "Generation task"
    return TaskHistoryEntry(
        taskId = taskId,
        title = titleText,
        status = status,
        costTime = costTime,
        source = source.key,
        outputId = primaryOutput?.outputId,
        thumbnailUrl = primaryOutput?.thumbnailUrl ?: primaryOutput?.url,
        outputCount = outputs.size,
        costText = titleText.toHistoryCostText(status),
        outputCountText = titleText.toHistoryOutputCountText(status),
        canViewOutput = primaryOutput != null,
        canReuseParams = params.isNotEmpty(),
        canRetry = status.isFailedStatus(),
        canCancel = isRunning,
    )
}

private fun Map<String, String>.toReuseActionMessage(): TaskHistoryActionMessage =
    if (isEmpty()) {
        TaskHistoryActionMessage.NoReusableParams
    } else {
        TaskHistoryActionMessage.ReusableParamsPrepared(size)
    }

private fun Map<String, String>.toRetryActionMessage(): TaskHistoryActionMessage =
    if (isEmpty()) {
        TaskHistoryActionMessage.NoRetryParams
    } else {
        TaskHistoryActionMessage.RetryParamsPrepared
    }

private fun Throwable.toHistoryPresentationError(fallback: TaskHistoryPresentationError): TaskHistoryPresentationError {
    val text = message.orEmpty()
    return if (text.looksLikeHistoryAuthError()) {
        TaskHistoryPresentationError.AuthRequired
    } else {
        // Repository 失败可能携带远端 msg 或底层异常 message。
        // StateHolder 只输出稳定错误语义，避免把内部诊断信息作为 Snackbar/错误区内容暴露给用户。
        fallback
    }
}

private fun String.looksLikeHistoryAuthError(): Boolean =
    contains("TOKEN", ignoreCase = true) ||
        contains("UNAUTHORIZED", ignoreCase = true) ||
        contains("FORBIDDEN", ignoreCase = true) ||
        Regex("(?i)(HTTP|CODE|STATUS)\\s*[:=]?\\s*(401|403|412)").containsMatchIn(this)

private val GenerationHistoryItem.isRunning: Boolean
    get() = !status.isCompletedStatus() && !status.isFailedStatus() && !status.isCancelledStatus()

private fun String.isCompletedStatus(): Boolean =
    uppercase() in setOf("SUCCESS", "COMPLETED", "DONE")

private fun String.isFailedStatus(): Boolean =
    uppercase() in setOf("FAILED", "FAIL", "ERROR")

private fun String.isCancelledStatus(): Boolean =
    uppercase() in setOf("CANCELED", "CANCELLED")

private fun String.toHistoryCostText(status: String): TaskHistoryCostText = when {
    containsAnyTitleHint(HISTORY_VIDEO_TITLE_HINTS) -> TaskHistoryCostText.VIDEO
    containsAnyTitleHint(HISTORY_CHARACTER_TITLE_HINTS) -> TaskHistoryCostText.CHARACTER
    status.isFailedStatus() -> TaskHistoryCostText.FAILED
    else -> TaskHistoryCostText.DEFAULT
}

private fun String.toHistoryOutputCountText(status: String): TaskHistoryOutputCountText = when {
    status.isFailedStatus() -> TaskHistoryOutputCountText.FAILED
    !status.isCompletedStatus() -> TaskHistoryOutputCountText.RUNNING
    containsAnyTitleHint(HISTORY_VIDEO_TITLE_HINTS) -> TaskHistoryOutputCountText.VIDEO
    else -> TaskHistoryOutputCountText.COMPLETED
}

private fun String.containsAnyTitleHint(hints: List<String>): Boolean {
    val normalized = lowercase()
    return hints.any { hint -> normalized.contains(hint) }
}

private val HISTORY_VIDEO_TITLE_HINTS = listOf("\u89c6\u9891", "video")
private val HISTORY_CHARACTER_TITLE_HINTS = listOf("\u89d2\u8272", "character")
