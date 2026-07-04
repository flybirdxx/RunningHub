package com.runninghub.app.ui.feature.quickcreate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickCreateConversationContentTest {
    @Test
    fun `conversation result card uses explicit server dimensions first`() {
        val ratio = quickCreateConversationResultAspectRatio(
            resultWidth = 1920,
            resultHeight = 1080,
            resolvedImageAspectRatio = 4f / 5f,
        )

        assertEquals(16f / 9f, ratio)
    }

    @Test
    fun `conversation result card uses loaded image ratio when server dimensions are missing`() {
        val ratio = quickCreateConversationResultAspectRatio(
            resultWidth = null,
            resultHeight = null,
            resolvedImageAspectRatio = 4f / 5f,
        )

        assertEquals(4f / 5f, ratio)
    }

    @Test
    fun `conversation result card has no ratio before any result dimensions are available`() {
        val ratio = quickCreateConversationResultAspectRatio(
            resultWidth = null,
            resultHeight = null,
            resolvedImageAspectRatio = null,
        )

        assertNull(ratio)
    }

    @Test
    fun `conversation generating card can use submitted aspect ratio`() {
        val ratio = quickCreateConversationGeneratingAspectRatio("9:16")

        assertEquals(0.5625f, ratio)
    }

    @Test
    fun `conversation generating card accepts full width colon ratio`() {
        val ratio = quickCreateConversationGeneratingAspectRatio("3：4")

        assertEquals(0.75f, ratio)
    }

    @Test
    fun `conversation generating card rejects invalid aspect ratio input`() {
        assertNull(quickCreateConversationGeneratingAspectRatio("abc"))
        assertNull(quickCreateConversationGeneratingAspectRatio(""))
        assertNull(quickCreateConversationGeneratingAspectRatio("9:0"))
    }

    @Test
    fun `conversation generating card accepts slash separated ratio`() {
        val ratio = quickCreateConversationGeneratingAspectRatio("16/9")

        assertEquals(16f / 9f, ratio)
    }

    @Test
    fun `conversation generating card keeps extreme ratio without clamping`() {
        // 锁定 v3 行为变更：解析函数不再 coerce，极端竖比原样返回，clamp 由卡片尺寸函数负责。
        val ratio = quickCreateConversationGeneratingAspectRatio("1:5")

        assertEquals(0.2f, ratio)
    }

    @Test
    fun `conversation generating card rejects ratio with extra separator segments`() {
        assertNull(quickCreateConversationGeneratingAspectRatio("9:16:9"))
    }

    @Test
    fun `conversation generating card rejects negative ratio`() {
        assertNull(quickCreateConversationGeneratingAspectRatio("-9:16"))
    }

    @Test
    fun `conversation generating card rejects non finite ratio`() {
        assertNull(quickCreateConversationGeneratingAspectRatio("NaN:1"))
    }
}
