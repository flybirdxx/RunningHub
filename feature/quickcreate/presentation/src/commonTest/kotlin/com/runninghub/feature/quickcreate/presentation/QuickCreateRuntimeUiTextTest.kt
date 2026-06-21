package com.runninghub.feature.quickcreate.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 覆盖 QuickCreate 运行时文案端口的当前中文契约。
 *
 * 这些断言固定中期迁移阶段仍需由 Presentation 提供的状态文案，避免 composeApp
 * 协调器回退到散落的硬编码字符串。
 */
class QuickCreateRuntimeUiTextTest {

    @Test
    fun `generation runtime text is provided by presentation text port`() {
        assertEquals("价格确认中", QuickCreateRuntimeUiText.feeConfirming)
        assertEquals("正在提交任务...", QuickCreateRuntimeUiText.submittingTask)
        assertEquals("价格待确认", QuickCreateRuntimeUiText.feePending)
        assertEquals("已有生成任务进行中，请等待当前任务结束", QuickCreateRuntimeUiText.duplicateGeneration)
    }

    @Test
    fun `media upload runtime text keeps count and safe fallback messages`() {
        assertEquals("正在上传素材(3)...", QuickCreateRuntimeUiText.uploadingMedia(3))
        assertEquals("素材上传失败", QuickCreateRuntimeUiText.mediaUploadFailed)
        assertEquals("素材上传失败，请重新选择或稍后重试", QuickCreateRuntimeUiText.mediaUploadBlocked)
        assertEquals("素材上传超时，请重新选择或稍后重试", QuickCreateRuntimeUiText.mediaUploadTimeout)
    }
}
