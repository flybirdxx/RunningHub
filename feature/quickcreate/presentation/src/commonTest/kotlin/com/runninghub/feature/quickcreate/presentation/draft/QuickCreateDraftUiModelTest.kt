package com.runninghub.feature.quickcreate.presentation.draft

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateDraftUiModelTest {
    @Test
    fun `draft resume summary returns restorable tab and prompt length`() {
        val draft = DraftData(
            currentTab = "VIDEO",
            imagePrompt = "image",
            videoPrompt = "video prompt",
        )

        assertEquals(
            QuickCreateDraftResumeSummary(
                tab = QuickCreateTab.VIDEO,
                promptLength = 12,
            ),
            draft.resumeSummary(),
        )
    }

    @Test
    fun `draft resume summary falls back to image prompt when saved video tab is empty`() {
        val draft = DraftData(
            currentTab = "VIDEO",
            imagePrompt = "image prompt",
            videoPrompt = "",
        )

        assertEquals(
            QuickCreateDraftResumeSummary(
                tab = QuickCreateTab.IMAGE,
                promptLength = 12,
            ),
            draft.resumeSummary(),
        )
    }
}
