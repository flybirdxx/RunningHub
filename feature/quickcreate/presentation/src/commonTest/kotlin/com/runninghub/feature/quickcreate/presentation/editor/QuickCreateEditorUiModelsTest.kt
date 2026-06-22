package com.runninghub.feature.quickcreate.presentation.editor

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 覆盖快捷创作编辑器本地枚举的 UI 语义契约。
 *
 * 这些枚举仍承担旧接口兼容和本地默认值职责；测试优先锁定固定展示文案不再保存在
 * feature presentation 模块，避免后续继续扩大硬编码 UI 文案基线。
 */
class QuickCreateEditorUiModelsTest {

    @Test
    fun `image quality exposes stable label semantics instead of hardcoded display text`() {
        assertEquals(ImageQualityLabel.Low, ImageQuality.QUALITY_LOW.label)
        assertEquals(ImageQualityLabel.Medium, ImageQuality.QUALITY_MEDIUM.label)
        assertEquals(ImageQualityLabel.High, ImageQuality.QUALITY_HIGH.label)
    }
}
