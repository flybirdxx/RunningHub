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
            quickCreateSendButtonLabel(cost = 0.76, feePreviewLoading = true, feePreviewError = null),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Pending,
            quickCreateSendButtonLabel(
                cost = 0.76,
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Amount("0.76"),
            quickCreateSendButtonLabel(cost = 0.76, feePreviewLoading = false, feePreviewError = null),
        )
        assertEquals(
            QuickCreateSendButtonLabel.Generate,
            quickCreateSendButtonLabel(cost = 0.0, feePreviewLoading = false, feePreviewError = null),
        )
    }

    @Test
    fun `price badge state covers loading pending free amount and insufficient`() {
        assertEquals(
            QuickCreatePriceBadgeState.Loading,
            quickCreatePriceBadgeState(cost = 0.76, feePreviewLoading = true, feePreviewError = null),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Pending,
            quickCreatePriceBadgeState(
                cost = 0.76,
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Insufficient,
            quickCreatePriceBadgeState(
                cost = 0.76,
                feePreviewLoading = false,
                feePreviewError = QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage(),
            ),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Free,
            quickCreatePriceBadgeState(cost = 0.0, feePreviewLoading = false, feePreviewError = null),
        )
        assertEquals(
            QuickCreatePriceBadgeState.Amount(
                QuickCreateBillingAmount("2.00", QuickCreateBillingUnit.CnyCash),
            ),
            quickCreatePriceBadgeState(cost = 2.0, feePreviewLoading = false, feePreviewError = null),
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
                cost = 2.0,
                feePreviewLoading = false,
                feePreviewError = null,
                billingPreview = preview,
            ),
        )
    }

    @Test
    fun `generation confirmation is required for paid preview but not for free preview`() {
        val paidState = QuickCreateUiState(
            estimatedCost = 2.0,
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
