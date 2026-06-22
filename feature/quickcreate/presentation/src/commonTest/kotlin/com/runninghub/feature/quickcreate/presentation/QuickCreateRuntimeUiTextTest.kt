package com.runninghub.feature.quickcreate.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 覆盖 QuickCreate 运行时错误提示的稳定语义契约。
 *
 * 这些断言确保生成阻塞和上传失败不再由 Presentation 保存最终中文文案；
 * composeApp 负责把这些语义映射到 Compose Resources。
 */
class QuickCreateRuntimeUiTextTest {

    @Test
    fun `generation runtime text is represented as stable semantics`() {
        assertEquals(QuickCreateRuntimeUiText.FeeConfirming, QuickCreateRuntimeUiText.FeeConfirming)
        assertEquals(QuickCreateRuntimeUiText.FeePending, QuickCreateRuntimeUiText.FeePending)
        assertEquals(QuickCreateRuntimeUiText.DuplicateGeneration, QuickCreateRuntimeUiText.DuplicateGeneration)
        assertEquals(QuickCreateRuntimeUiText.PromptRequired, QuickCreateRuntimeUiText.PromptRequired)
        assertEquals(QuickCreateRuntimeUiText.PromptTooLong(1200), QuickCreateRuntimeUiText.PromptTooLong(1200))
    }

    @Test
    fun `media upload runtime text is represented as stable semantics`() {
        assertEquals(QuickCreateRuntimeUiText.MediaUploadFailed, QuickCreateRuntimeUiText.MediaUploadFailed)
        assertEquals(QuickCreateRuntimeUiText.MediaUploadBlocked, QuickCreateRuntimeUiText.MediaUploadBlocked)
        assertEquals(QuickCreateRuntimeUiText.MediaUploadTimeout, QuickCreateRuntimeUiText.MediaUploadTimeout)
    }
}
