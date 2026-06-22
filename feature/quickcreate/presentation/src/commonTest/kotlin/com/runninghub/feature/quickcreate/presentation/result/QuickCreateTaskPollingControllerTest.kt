package com.runninghub.feature.quickcreate.presentation.result

import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证快捷创作任务轮询控制器的状态映射。
 *
 * 这些用例直接覆盖已拆出的 [QuickCreateTaskPollingController]，避免任务状态行为重新依赖
 * composeApp 应用壳测试夹具。ScreenModel 只负责生命周期转发，不再承载轮询状态机断言。
 */
class QuickCreateTaskPollingControllerTest {

    @Test
    fun `collect maps running then failed statuses and invokes queued callback once`() = runTest {
        val uiState = MutableStateFlow(QuickCreateUiState())
        var queuedCallbacks = 0
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = { queuedCallbacks += 1 },
            onTaskSucceeded = {},
        )

        // 按仓库 Flow 的实际顺序模拟提交、排队、运行、失败，验证控制器只暴露最终可渲染状态。
        controller.collect(
            flowOf(
                QuickCreateTaskStatus.Submitting,
                QuickCreateTaskStatus.Queuing("task-1"),
                QuickCreateTaskStatus.Running("task-1", progress = 42),
                QuickCreateTaskStatus.Failed("task-1", errorMessage = QuickCreateTaskIssueCode.TASK_FAILED),
            )
        )

        assertEquals(1, queuedCallbacks)
        assertEquals(QuickCreateTaskUiStatus.FAILED, uiState.value.taskStatus)
        assertEquals(
            QuickCreateTaskStatusText.Error(QuickCreatePresentationError.TaskFailed),
            uiState.value.statusText,
        )
        assertEquals(QuickCreatePresentationError.TaskFailed.asQuickCreateUiMessage(), uiState.value.error)
        assertEquals(emptyList(), uiState.value.results)
    }

    @Test
    fun `collect maps success results and invokes success callback after state update`() = runTest {
        val uiState = MutableStateFlow(QuickCreateUiState())
        var successCallbacks = 0
        var statusSeenByCallback: QuickCreateTaskUiStatus? = null
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {
                successCallbacks += 1
                statusSeenByCallback = uiState.value.taskStatus
            },
        )

        controller.collect(
            flowOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                            thumbnailUrl = "https://example.com/thumb.png",
                            width = 1024,
                            height = 768,
                            duration = 6,
                        ),
                        QuickCreateResultItem(
                            url = "https://example.com/result.mp4?download=1",
                            type = "file",
                            thumbnailUrl = "https://example.com/video-thumb.jpg",
                        ),
                        QuickCreateResultItem(
                            url = "https://example.com/result-output",
                            type = "video",
                        ),
                    ),
                )
            )
        )

        val results = uiState.value.results
        assertEquals(1, successCallbacks)
        assertEquals(QuickCreateTaskUiStatus.SUCCESS, statusSeenByCallback)
        assertEquals(QuickCreateTaskUiStatus.SUCCESS, uiState.value.taskStatus)
        assertEquals(QuickCreateTaskStatusText.Success, uiState.value.statusText)
        assertEquals(3, results.size)
        assertEquals(QuickCreateResultMediaType.IMAGE, results[0].mediaType)
        assertEquals("https://example.com/thumb.png", results[0].thumbnailUrl)
        assertEquals(1024, results[0].width)
        assertEquals(768, results[0].height)
        assertEquals(6, results[0].duration)
        assertEquals(QuickCreateResultMediaType.VIDEO, results[1].mediaType)
        assertEquals("https://example.com/result.mp4?download=1", results[1].url)
        assertEquals(QuickCreateResultMediaType.VIDEO, results[2].mediaType)
        assertEquals("https://example.com/result-output", results[2].url)
    }

    @Test
    fun `collect maps unknown error to generic message and clears transient status text`() = runTest {
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                taskStatus = QuickCreateTaskUiStatus.RUNNING,
                statusText = QuickCreateTaskStatusText.Running(progressPercent = 42),
            )
        )
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {},
        )

        controller.collect(flowOf(QuickCreateTaskStatus.Error("network unavailable")))

        assertEquals(QuickCreateTaskUiStatus.IDLE, uiState.value.taskStatus)
        assertEquals(null, uiState.value.statusText)
        assertEquals(QuickCreatePresentationError.GenerationFailed.asQuickCreateUiMessage(), uiState.value.error)
    }

    @Test
    fun `collect maps cancelled task to cancelled terminal display`() = runTest {
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                taskStatus = QuickCreateTaskUiStatus.RUNNING,
                statusText = QuickCreateTaskStatusText.Running(progressPercent = 42),
                error = QuickCreatePresentationError.GenerationFailed.asQuickCreateUiMessage(),
            )
        )
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {},
        )

        controller.collect(flowOf(QuickCreateTaskStatus.Cancelled("task-1")))

        assertEquals(QuickCreateTaskUiStatus.CANCELED, uiState.value.taskStatus)
        assertEquals(QuickCreateTaskStatusText.Canceled, uiState.value.statusText)
        assertEquals(null, uiState.value.error)
    }

    @Test
    fun `collect maps domain issue codes to presentation messages`() = runTest {
        val uiState = MutableStateFlow(QuickCreateUiState())
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {},
        )

        // Data 层返回稳定错误码，Presentation 层负责生成最终中文提示。
        controller.collect(flowOf(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED)))

        assertEquals(QuickCreateTaskUiStatus.IDLE, uiState.value.taskStatus)
        assertEquals(QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage(), uiState.value.error)
    }

    @Test
    fun `clear results resets successful task display without touching existing error`() {
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                taskStatus = QuickCreateTaskUiStatus.SUCCESS,
                statusText = QuickCreateTaskStatusText.Success,
                error = QuickCreatePresentationError.GenerationFailed.asQuickCreateUiMessage(),
                results = listOf(
                    QuickCreateResultUi(
                        url = "https://example.com/result.png",
                        type = "png",
                        mediaType = QuickCreateResultMediaType.IMAGE,
                    )
                ),
            )
        )
        val controller = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {},
        )

        // clearResults 只清理结果展示状态；错误提示由上层交互按自身生命周期处理。
        controller.clearResults()

        assertEquals(QuickCreateTaskUiStatus.IDLE, uiState.value.taskStatus)
        assertEquals(null, uiState.value.statusText)
        assertEquals(emptyList(), uiState.value.results)
        assertEquals(QuickCreatePresentationError.GenerationFailed.asQuickCreateUiMessage(), uiState.value.error)
    }
}
