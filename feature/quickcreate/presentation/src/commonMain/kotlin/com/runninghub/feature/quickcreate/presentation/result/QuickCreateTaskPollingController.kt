package com.runninghub.feature.quickcreate.presentation.result

import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.toQuickCreateTaskIssueDisplayMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

/**
 * 收集并映射快捷创作生成任务的状态流。
 *
 * 本类位于 Presentation 层，因为它把 Domain 层的 [QuickCreateTaskStatus] 转换为页面可渲染的
 * [QuickCreateTaskUiStatus]、状态文案和结果卡片。它不负责提交生成请求、构建请求体、等待上传素材或处理计费拦截；
 * 这些职责仍由生成流程的上游 Interactor 协调。
 *
 * 并发与生命周期约束：
 * - 本类不创建协程，也不持有 Job；调用方必须在页面生命周期作用域内调用 [collect]。
 * - 每次收到 `Queuing` 都会触发 [onTaskQueued]，保持与旧行为一致，用于清理已成功提交的本地草稿。
 * - 收到 `Success` 后先写入结果状态，再触发 [onTaskSucceeded]，确保历史刷新发生时页面已经展示成功结果。
 * - 任务继续推进时会清理上游生成拦截产生的临时错误，避免重复点击提示在成功或进度更新后残留。
 *
 * @param uiState 页面状态流；本类只更新任务状态、状态文案、错误和生成结果。
 * @param onTaskQueued 任务被服务端接收进入排队后的回调，通常用于清理草稿。
 * @param onTaskSucceeded 任务成功完成后的回调，通常用于刷新最近历史或当前项目任务。
 */
class QuickCreateTaskPollingController(
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onTaskQueued: () -> Unit,
    private val onTaskSucceeded: () -> Unit,
) {

    /**
     * 收集远端任务状态流并逐条映射到页面状态。
     *
     * 生成仓库当前通过 Flow 暴露提交后的排队、运行和终态；调用方取消所在协程时，
     * Flow 收集会随结构化并发一起取消，本方法不会吞掉取消信号。
     */
    suspend fun collect(statuses: Flow<QuickCreateTaskStatus>) {
        statuses.collect { status ->
            handleTaskStatus(status)
        }
    }

    /**
     * 清空当前生成结果和任务状态。
     *
     * 该操作只恢复页面展示，不取消正在运行的生成 Job；用户关闭旧结果后，新的任务状态仍可继续回写。
     */
    fun clearResults() {
        uiState.update {
            it.copy(
                results = emptyList(),
                taskStatus = QuickCreateTaskUiStatus.IDLE,
                statusText = null,
            )
        }
    }

    private fun handleTaskStatus(status: QuickCreateTaskStatus) {
        if (status is QuickCreateTaskStatus.Queuing) {
            onTaskQueued()
        }
        var refreshHistory = false
        uiState.update {
            when (status) {
                is QuickCreateTaskStatus.Submitting -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.SUBMITTING,
                    statusText = QuickCreateTaskStatusText.SubmittingTask,
                    error = null,
                )
                is QuickCreateTaskStatus.Queuing -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.QUEUING,
                    statusText = QuickCreateTaskStatusText.Queuing,
                    error = null,
                )
                is QuickCreateTaskStatus.Running -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.RUNNING,
                    statusText = QuickCreateTaskStatusText.Running(status.progress),
                    error = null,
                )
                is QuickCreateTaskStatus.Success -> {
                    val results = status.results.map { item ->
                        QuickCreateResultUi(
                            url = item.url,
                            type = item.type,
                            mediaType = item.toQuickCreateResultMediaType(),
                            thumbnailUrl = item.thumbnailUrl,
                            width = item.width,
                            height = item.height,
                            duration = item.duration,
                        )
                    }
                    refreshHistory = true
                    it.copy(
                        taskStatus = QuickCreateTaskUiStatus.SUCCESS,
                        statusText = QuickCreateTaskStatusText.Success,
                        error = null,
                        results = results,
                    )
                }
                is QuickCreateTaskStatus.Failed -> {
                    val errorMessage = status.errorMessage.toQuickCreateTaskIssueDisplayMessage()
                    it.copy(
                        taskStatus = QuickCreateTaskUiStatus.FAILED,
                        statusText = QuickCreateTaskStatusText.Custom(errorMessage),
                        error = errorMessage,
                    )
                }
                is QuickCreateTaskStatus.Cancelled -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.CANCELED,
                    statusText = QuickCreateTaskStatusText.Canceled,
                    error = null,
                )
                is QuickCreateTaskStatus.Error -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.IDLE,
                    statusText = null,
                    error = status.message.toQuickCreateTaskIssueDisplayMessage(),
                )
            }
        }
        if (refreshHistory) {
            onTaskSucceeded()
        }
    }
}

/**
 * 将 Domain 层任务输出映射为 Presentation 层稳定媒体类型。
 *
 * 服务端历史上既可能返回 `mp4/webm/mov/video` 这样的类型，也可能只在 URL 中体现扩展名。
 * 兼容判断集中在轮询结果映射阶段，避免结果 Composable 直接依赖服务端字符串协议。
 */
private fun QuickCreateResultItem.toQuickCreateResultMediaType(): QuickCreateResultMediaType {
    val normalizedType = type.lowercase()
    val normalizedUrl = url.substringBefore('?').lowercase()
    return if (
        normalizedType in setOf("mp4", "webm", "mov", "video") ||
        normalizedUrl.endsWith(".mp4") ||
        normalizedUrl.endsWith(".webm") ||
        normalizedUrl.endsWith(".mov")
    ) {
        QuickCreateResultMediaType.VIDEO
    } else {
        QuickCreateResultMediaType.IMAGE
    }
}
