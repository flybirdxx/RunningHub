package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhResultPreviewComponentsContractTest {
    @Test
    fun `result preview state carries media expiry and actions`() {
        val state = ResultPreviewState(
            title = "生成结果",
            taskIdLabel = "任务 ID: task-1",
            statusLabel = "生成完成",
            media = ResultPreviewMediaState(
                url = "https://example.com/result.mp4",
                previewUrl = "https://example.com/thumb.jpg",
                mediaType = ResultPreviewMediaType.Video,
                aspectRatio = 16f / 9f,
            ),
            expiryLabel = "云端结果将在 24 小时后过期",
            actions = listOf(
                ResultPreviewActionState(ResultPreviewActionType.ViewResult, "查看结果"),
                ResultPreviewActionState(ResultPreviewActionType.Save, "保存"),
                ResultPreviewActionState(ResultPreviewActionType.Download, "下载"),
                ResultPreviewActionState(ResultPreviewActionType.ReuseParameters, "复用参数"),
                ResultPreviewActionState(ResultPreviewActionType.CopyPrompt, "复制 Prompt"),
            ),
        )

        assertEquals(ResultPreviewMediaType.Video, state.media?.mediaType)
        assertEquals("https://example.com/thumb.jpg", state.media?.previewUrl)
        assertEquals("云端结果将在 24 小时后过期", state.expiryLabel)
        assertEquals(5, state.actions.size)
        assertTrue(state.actions.all { it.enabled })
    }

    @Test
    fun `result preview media supports image without preview url`() {
        val media = ResultPreviewMediaState(
            url = "https://example.com/result.png",
            previewUrl = null,
            mediaType = ResultPreviewMediaType.Image,
            aspectRatio = null,
        )

        assertEquals("https://example.com/result.png", media.renderUrl)
        assertEquals(ResultPreviewMediaType.Image, media.mediaType)
    }
}
