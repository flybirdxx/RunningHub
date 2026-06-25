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

        assertEquals(9f / 16f, ratio)
    }

    @Test
    fun `conversation generating card accepts full width colon ratio`() {
        val ratio = quickCreateConversationGeneratingAspectRatio("3：4")

        assertEquals(3f / 4f, ratio)
    }
}
