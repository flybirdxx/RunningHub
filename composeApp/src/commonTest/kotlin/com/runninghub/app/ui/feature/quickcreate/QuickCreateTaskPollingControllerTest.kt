package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreateResultItem
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证快捷创作任务轮询控制器的状态映射。
 *
 * 这些用例直接覆盖已拆出的 [QuickCreateTaskPollingController]，避免所有任务状态行为继续只依赖
 * `QuickCreateScreenModelTest` 的间接验证。后续迁移 ScreenModel 时，可以优先保留这里的细粒度契约。
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
                QuickCreateTaskStatus.Failed("task-1", errorMessage = "render failed"),
            )
        )

        assertEquals(1, queuedCallbacks)
        assertEquals(QuickCreateTaskUiStatus.FAILED, uiState.value.taskStatus)
        assertEquals("render failed", uiState.value.statusText)
        assertEquals("render failed", uiState.value.error)
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
        assertEquals("生成完成", uiState.value.statusText)
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
    fun `collect maps error to idle and clears transient status text`() = runTest {
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                taskStatus = QuickCreateTaskUiStatus.RUNNING,
                statusText = "生成中... 42%",
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
        assertEquals("network unavailable", uiState.value.error)
    }

    @Test
    fun `clear results resets successful task display without touching existing error`() {
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                taskStatus = QuickCreateTaskUiStatus.SUCCESS,
                statusText = "生成完成",
                error = "previous warning",
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
        assertEquals("previous warning", uiState.value.error)
    }
}
