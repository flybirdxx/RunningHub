package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
