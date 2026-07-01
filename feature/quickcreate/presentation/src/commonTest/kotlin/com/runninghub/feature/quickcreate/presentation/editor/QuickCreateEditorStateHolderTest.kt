package com.runninghub.feature.quickcreate.presentation.editor

import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingPreviewUi
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class QuickCreateEditorStateHolderTest {
    @Test
    fun `image prompt edit clears stale fee preview and keeps callbacks`() {
        var feePreviewRequests = 0
        var draftChanges = 0
        val state = MutableStateFlow(stalePricedState())
        val holder = QuickCreateEditorStateHolder(
            uiState = state,
            onFeePreviewRequired = { feePreviewRequests++ },
            onDraftChanged = { draftChanges++ },
        )

        holder.updateImagePrompt("new image prompt")

        assertEquals("new image prompt", state.value.imageConfig.prompt)
        assertStaleFeePreviewCleared(state.value)
        assertEquals(1, feePreviewRequests)
        assertEquals(1, draftChanges)
    }

    @Test
    fun `video parameter edit clears stale fee preview without draft callback`() {
        var feePreviewRequests = 0
        var draftChanges = 0
        val state = MutableStateFlow(stalePricedState())
        val holder = QuickCreateEditorStateHolder(
            uiState = state,
            onFeePreviewRequired = { feePreviewRequests++ },
            onDraftChanged = { draftChanges++ },
        )

        holder.updateVideoDuration(VideoDuration.DURATION_10S)

        assertEquals(VideoDuration.DURATION_10S, state.value.videoConfig.duration)
        assertStaleFeePreviewCleared(state.value)
        assertEquals(1, feePreviewRequests)
        assertEquals(0, draftChanges)
    }

    private fun stalePricedState(): QuickCreateUiState =
        QuickCreateUiState(
            estimatedCost = 8.6,
            feePreviewLoading = true,
            feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
            feePreviewRequestKey = "stale-request-key",
            billingPreview = QuickCreateBillingPreviewUi(requiredCashAmount = 8.6),
        )

    private fun assertStaleFeePreviewCleared(state: QuickCreateUiState) {
        assertEquals(0.0, state.estimatedCost)
        assertFalse(state.feePreviewLoading)
        assertNull(state.feePreviewError)
        assertNull(state.feePreviewRequestKey)
        assertNull(state.billingPreview)
    }
}
