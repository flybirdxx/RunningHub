package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingAmount
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingPreviewUi
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_billing_cny_amount_format
import runninghub.composeapp.generated.resources.quick_create_billing_rhb_amount_format

@Composable
internal fun QuickCreateBillingPreviewUi.quickCreateBillingAmountText(): String? =
    when {
        requiredRhAmount > 0.0 -> quickCreateBillingAmountText(
            QuickCreateBillingAmount(
                amount = requiredRhAmount.quickCreateFormatRhAmount(),
                unit = QuickCreateBillingUnit.RhbPoints,
            ),
        )
        requiredCashAmount > 0.0 -> quickCreateBillingAmountText(
            QuickCreateBillingAmount(
                amount = requiredCashAmount.quickCreateFormatCashAmount(),
                unit = QuickCreateBillingUnit.CnyCash,
            ),
        )
        else -> null
    }

@Composable
internal fun quickCreateBillingAmountText(amount: QuickCreateBillingAmount): String =
    when (amount.unit) {
        QuickCreateBillingUnit.RhbPoints -> stringResource(
            Res.string.quick_create_billing_rhb_amount_format,
            amount.amount,
        )
        QuickCreateBillingUnit.CnyCash -> stringResource(
            Res.string.quick_create_billing_cny_amount_format,
            amount.amount,
        )
    }

private fun Double.quickCreateFormatCashAmount(): String {
    val scaled = (this * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

private fun Double.quickCreateFormatRhAmount(): String {
    val scaled = (this * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    val whole = absolute / 100
    val cents = absolute % 100
    return if (cents == 0) {
        "$sign$whole"
    } else {
        "$sign$whole.${cents.toString().padStart(2, '0')}"
    }
}
