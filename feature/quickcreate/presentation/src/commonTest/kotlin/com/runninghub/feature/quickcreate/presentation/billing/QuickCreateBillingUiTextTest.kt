package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickCreateBillingUiTextTest {
    @Test
    fun `send button label exposes stable display state instead of localized copy`() {
        assertEquals(
            QuickCreateSendButtonLabel.Confirming,
            quickCreateSendButtonLabel(feePreviewLoading = true, feePreviewError = null),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Pending,
            quickCreateSendButtonLabel(
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Amount(
                QuickCreateBillingAmount("0.76", QuickCreateBillingUnit.CnyCash),
            ),
            quickCreateSendButtonLabel(
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = QuickCreateBillingPreviewUi(requiredCashAmount = 0.76),
            ),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Generate,
            quickCreateSendButtonLabel(
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = QuickCreateBillingPreviewUi(free = true),
            ),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Pending,
            quickCreateSendButtonLabel(feePreviewLoading = false, feePreviewError = null),
        )
    }

    @Test
    fun `price badge state covers loading pending free amount and insufficient`() {
        assertEquals(
            QuickCreatePriceBadgeState.Loading,
            quickCreatePriceBadgeState(feePreviewLoading = true, feePreviewError = null),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Pending,
            quickCreatePriceBadgeState(
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Insufficient,
            quickCreatePriceBadgeState(
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Pending,
            quickCreatePriceBadgeState(feePreviewLoading = false, feePreviewError = null),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Amount(
                QuickCreateBillingAmount("2.00", QuickCreateBillingUnit.CnyCash),
            ),
            quickCreatePriceBadgeState(
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = QuickCreateBillingPreviewUi(requiredCashAmount = 2.0),
            ),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Free,
            quickCreatePriceBadgeState(
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = QuickCreateBillingPreviewUi(free = true),
            ),
        )
    }

    @Test
    fun `billing preview prefers rh points for creative consumption amount`() {
        val preview = QuickCreateBillingPreviewUi(
            requiredRhAmount = 37.0,
            requiredCashAmount = 2.0,
            userCashBalance = 20.0,
            cashCurrency = "CNY",
        )

        assertEquals(
            QuickCreatePriceBadgeState.Amount(
                QuickCreateBillingAmount("37", QuickCreateBillingUnit.RhbPoints),
            ),
            quickCreatePriceBadgeState(
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = preview,
            ),
        )
    }

    @Test
    fun `generation confirmation is required for paid preview but not for free preview`() {
        val paidState = QuickCreateUiState(
            billingPreview = QuickCreateBillingPreviewUi(
                requiredCashAmount = 2.0,
                userCashBalance = 20.0,
                cashCurrency = "CNY",
            ),
        )
        val freeState = QuickCreateUiState(
            billingPreview = QuickCreateBillingPreviewUi(free = true),
        )

        assertTrue(quickCreateGenerationConfirmState(paidState).requiresConfirmation)
        assertFalse(quickCreateGenerationConfirmState(freeState).requiresConfirmation)
        assertEquals(
            QuickCreateBillingAmount("20.00", QuickCreateBillingUnit.CnyCash),
            quickCreateGenerationConfirmState(paidState).currentBalance,
        )
    }
}
