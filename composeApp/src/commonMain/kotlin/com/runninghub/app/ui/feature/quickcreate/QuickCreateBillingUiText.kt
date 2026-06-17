package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.app.util.formatCashAmount

internal fun quickCreateSendButtonLabel(
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
): String =
    when {
        feePreviewLoading -> "价格确认中"
        feePreviewError != null -> "价格待确认"
        cost > 0.0 -> "¥${formatCashAmount(cost)}"
        else -> "生成"
    }
