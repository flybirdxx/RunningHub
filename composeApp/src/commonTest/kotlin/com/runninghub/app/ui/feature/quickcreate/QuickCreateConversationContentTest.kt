package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.result.QuickCreateConversationItemUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
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

    @Test
    fun `conversation stage rail marks uploading media as first active stage`() {
        val states = quickCreateConversationStageStates(
            status = QuickCreateTaskUiStatus.SUBMITTING,
            statusText = QuickCreateTaskStatusText.UploadingMedia(pendingCount = 1),
        ).map { it.state }

        assertEquals(
            listOf(
                QuickCreateGenerationStageState.Current,
                QuickCreateGenerationStageState.Pending,
                QuickCreateGenerationStageState.Pending,
                QuickCreateGenerationStageState.Pending,
            ),
            states,
        )
    }

    @Test
    fun `conversation stage rail marks running task as generation stage`() {
        val states = quickCreateConversationStageStates(
            status = QuickCreateTaskUiStatus.RUNNING,
            statusText = QuickCreateTaskStatusText.Running(progressPercent = 42),
        ).map { it.state }

        assertEquals(
            listOf(
                QuickCreateGenerationStageState.Done,
                QuickCreateGenerationStageState.Done,
                QuickCreateGenerationStageState.Current,
                QuickCreateGenerationStageState.Pending,
            ),
            states,
        )
    }

    @Test
    fun `conversation stage rail keeps failed task recoverable at generation stage`() {
        val states = quickCreateConversationStageStates(
            status = QuickCreateTaskUiStatus.FAILED,
            statusText = QuickCreateTaskStatusText.Failed,
        ).map { it.state }

        assertEquals(
            listOf(
                QuickCreateGenerationStageState.Done,
                QuickCreateGenerationStageState.Done,
                QuickCreateGenerationStageState.Failed,
                QuickCreateGenerationStageState.Pending,
            ),
            states,
        )
    }

    @Test
    fun `retry target keeps submitted source tab when user changed current tab`() {
        val item = QuickCreateConversationItemUi(
            prompt = "image prompt",
            taskStatus = QuickCreateTaskUiStatus.FAILED,
            sourceTab = QuickCreateTab.IMAGE,
        )

        assertEquals(QuickCreateTab.IMAGE, quickCreateRetryTargetTab(item, QuickCreateTab.VIDEO))
    }

    @Test
    fun `retry target falls back to current tab for legacy conversation item`() {
        val item = QuickCreateConversationItemUi(
            prompt = "legacy prompt",
            taskStatus = QuickCreateTaskUiStatus.FAILED,
        )

        assertEquals(QuickCreateTab.VIDEO, quickCreateRetryTargetTab(item, QuickCreateTab.VIDEO))
    }
}
