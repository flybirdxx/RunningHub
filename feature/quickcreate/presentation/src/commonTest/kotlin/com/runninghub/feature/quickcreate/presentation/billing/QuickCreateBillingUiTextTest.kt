package com.runninghub.feature.quickcreate.presentation.billing

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateBillingUiTextTest {
    @Test
    fun `send button label shows fee preview state before amount`() {
        assertEquals("价格确认中", quickCreateSendButtonLabel(cost = 0.76, feePreviewLoading = true, feePreviewError = null))
        assertEquals("价格待确认", quickCreateSendButtonLabel(cost = 0.76, feePreviewLoading = false, feePreviewError = "failed"))
        assertEquals("¥0.76", quickCreateSendButtonLabel(cost = 0.76, feePreviewLoading = false, feePreviewError = null))
        assertEquals("生成", quickCreateSendButtonLabel(cost = 0.0, feePreviewLoading = false, feePreviewError = null))
    }
}
