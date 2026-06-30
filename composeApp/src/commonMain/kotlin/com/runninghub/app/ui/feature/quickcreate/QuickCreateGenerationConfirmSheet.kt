package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadgeState
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.billing.GenerationConfirmSheet
import com.runninghub.app.ui.designsystem.components.billing.GenerationConfirmSheetState
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingAmount
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingUnit
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreatePriceBadgeState
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateGenerationConfirmState
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_billing_balance_label
import runninghub.composeapp.generated.resources.quick_create_billing_balance_unavailable
import runninghub.composeapp.generated.resources.quick_create_billing_cny_amount_format
import runninghub.composeapp.generated.resources.quick_create_billing_expected_cost_label
import runninghub.composeapp.generated.resources.quick_create_billing_failure_policy_label
import runninghub.composeapp.generated.resources.quick_create_billing_failure_policy_value
import runninghub.composeapp.generated.resources.quick_create_billing_member_discount_label
import runninghub.composeapp.generated.resources.quick_create_billing_no_member_discount
import runninghub.composeapp.generated.resources.quick_create_billing_price_confirming
import runninghub.composeapp.generated.resources.quick_create_billing_price_free
import runninghub.composeapp.generated.resources.quick_create_billing_price_insufficient
import runninghub.composeapp.generated.resources.quick_create_billing_price_pending
import runninghub.composeapp.generated.resources.quick_create_billing_rhb_amount_format
import runninghub.composeapp.generated.resources.quick_create_generation_confirm_action
import runninghub.composeapp.generated.resources.quick_create_generation_confirm_cancel
import runninghub.composeapp.generated.resources.quick_create_generation_confirm_title

@Composable
internal fun QuickCreateGenerationConfirmSheet(
    uiState: QuickCreateUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GenerationConfirmSheet(
        state = quickCreateGenerationConfirmSheetState(uiState),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun quickCreateGenerationConfirmSheetState(
    uiState: QuickCreateUiState,
): GenerationConfirmSheetState {
    val confirmState = quickCreateGenerationConfirmState(uiState)
    val priceLabel = quickCreatePriceBadgeText(confirmState.priceBadge)
    return GenerationConfirmSheetState(
        title = stringResource(Res.string.quick_create_generation_confirm_title),
        priceBadgeState = confirmState.priceBadge.asRhPriceBadgeState(priceLabel),
        priceLabel = priceLabel,
        rows = listOf(
            BillingInfoRow(
                label = stringResource(Res.string.quick_create_billing_expected_cost_label),
                value = priceLabel,
                emphasized = true,
            ),
            BillingInfoRow(
                label = stringResource(Res.string.quick_create_billing_balance_label),
                value = confirmState.currentBalance?.let { quickCreateBillingAmountText(it) }
                    ?: stringResource(Res.string.quick_create_billing_balance_unavailable),
            ),
            BillingInfoRow(
                label = stringResource(Res.string.quick_create_billing_member_discount_label),
                value = stringResource(Res.string.quick_create_billing_no_member_discount),
            ),
            BillingInfoRow(
                label = stringResource(Res.string.quick_create_billing_failure_policy_label),
                value = stringResource(Res.string.quick_create_billing_failure_policy_value),
            ),
        ),
        confirmActionLabel = stringResource(Res.string.quick_create_generation_confirm_action),
        dismissActionLabel = stringResource(Res.string.quick_create_generation_confirm_cancel),
        confirmEnabled = confirmState.canConfirm,
    )
}

@Composable
private fun quickCreatePriceBadgeText(state: QuickCreatePriceBadgeState): String =
    when (state) {
        QuickCreatePriceBadgeState.Loading -> stringResource(Res.string.quick_create_billing_price_confirming)
        QuickCreatePriceBadgeState.Pending -> stringResource(Res.string.quick_create_billing_price_pending)
        QuickCreatePriceBadgeState.Free -> stringResource(Res.string.quick_create_billing_price_free)
        QuickCreatePriceBadgeState.Insufficient -> stringResource(Res.string.quick_create_billing_price_insufficient)
        is QuickCreatePriceBadgeState.Amount -> quickCreateBillingAmountText(state.billingAmount)
    }

private fun QuickCreatePriceBadgeState.asRhPriceBadgeState(
    priceLabel: String,
): RhPriceBadgeState =
    when (this) {
        QuickCreatePriceBadgeState.Loading -> RhPriceBadgeState.Loading
        QuickCreatePriceBadgeState.Pending -> RhPriceBadgeState.Pending
        QuickCreatePriceBadgeState.Free -> RhPriceBadgeState.Free
        QuickCreatePriceBadgeState.Insufficient -> RhPriceBadgeState.Insufficient
        is QuickCreatePriceBadgeState.Amount -> RhPriceBadgeState.Amount(priceLabel)
    }

@Composable
private fun quickCreateBillingAmountText(amount: QuickCreateBillingAmount): String =
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
