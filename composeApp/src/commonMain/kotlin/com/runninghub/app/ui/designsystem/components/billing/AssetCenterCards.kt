package com.runninghub.app.ui.designsystem.components.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 钱包卡片中的操作入口。 */
sealed class WalletBalanceAction(open val label: String) {
    data class Recharge(override val label: String) : WalletBalanceAction(label)
    data class Details(override val label: String) : WalletBalanceAction(label)
}

/** 钱包卡片的风险提示状态。 */
sealed class WalletBalanceRiskState(open val message: String) {
    data class None(override val message: String = "") : WalletBalanceRiskState(message)
    data class BalanceUnavailable(override val message: String) : WalletBalanceRiskState(message)
    data class GenerationCapacityUnknown(override val message: String) : WalletBalanceRiskState(message)
    data class LowBalance(override val message: String) : WalletBalanceRiskState(message)
    data class InsufficientBalance(override val message: String) : WalletBalanceRiskState(message)
}

/**
 * 钱包余额卡片状态。
 *
 * @property rhbPointsValue RHB 点数，必须独立于钱包余额展示。
 * @property walletBalanceValue CNY 钱包余额，必须独立于创作点数展示。
 */
data class WalletBalanceCardState(
    val rhbPointsLabel: String,
    val rhbPointsValue: String,
    val walletBalanceLabel: String,
    val walletBalanceValue: String,
    val risk: WalletBalanceRiskState,
    val primaryAction: WalletBalanceAction,
    val secondaryAction: WalletBalanceAction,
)

/** 会员卡片视觉状态。 */
enum class MembershipStatusVisualState {
    Active,
    Expired,
    Unknown,
    None,
}

/** 会员卡片主操作。 */
sealed class MembershipAction(open val label: String) {
    data class Renew(override val label: String) : MembershipAction(label)
}

/**
 * 会员卡片状态。
 *
 * @property showInsideCreateInput 固定用于防止会员广告回流到 Create 主输入区。
 */
data class MembershipCardState(
    val levelLabel: String,
    val levelName: String,
    val remainingLabel: String,
    val status: MembershipStatusVisualState,
    val benefitSummary: String,
    val primaryAction: MembershipAction,
    val showInsideCreateInput: Boolean = false,
)

/** 消费明细的交易类型视觉状态。 */
enum class TransactionTypeVisualState {
    Generation,
    Refund,
    Recharge,
    Membership,
}

/** 消费明细的交易状态视觉状态。 */
enum class TransactionStatusVisualState {
    Succeeded,
    Pending,
    Failed,
    Unknown,
}

/**
 * 消费明细单项状态。
 *
 * @property relatedTaskId 关联任务 ID；为空时 [canOpenTaskDetail] 必须为 false。
 */
data class TransactionListItemState(
    val id: String,
    val type: TransactionTypeVisualState,
    val title: String,
    val amount: String,
    val status: TransactionStatusVisualState,
    val statusLabel: String = status.name,
    val relatedTaskId: String?,
) {
    val canOpenTaskDetail: Boolean
        get() = !relatedTaskId.isNullOrBlank()
}

@Composable
fun WalletBalanceCard(
    state: WalletBalanceCardState,
    onAction: (WalletBalanceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssetSurface(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
        ) {
            AssetMetric(
                label = state.rhbPointsLabel,
                value = state.rhbPointsValue,
                modifier = Modifier.weight(1f),
            )
            AssetMetric(
                label = state.walletBalanceLabel,
                value = state.walletBalanceValue,
                modifier = Modifier.weight(1f),
            )
        }
        RiskText(state.risk)
        Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
            RhButton(
                text = state.primaryAction.label,
                onClick = { onAction(state.primaryAction) },
                style = RhButtonStyle.Primary,
                modifier = Modifier.weight(1f),
            )
            RhButton(
                text = state.secondaryAction.label,
                onClick = { onAction(state.secondaryAction) },
                style = RhButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun MembershipCard(
    state: MembershipCardState,
    onAction: (MembershipAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssetSurface(modifier = modifier) {
        Text(
            text = state.levelLabel,
            color = RhTheme.colors.textTertiary,
            style = RhTypography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.levelName,
            color = when (state.status) {
                MembershipStatusVisualState.Expired -> RhTheme.colors.statusFailed
                else -> RhTheme.colors.textPrimary
            },
            style = RhTypography.sectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.remainingLabel,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.benefitSummary,
            color = RhTheme.colors.textTertiary,
            style = RhTypography.caption,
        )
        RhButton(
            text = state.primaryAction.label,
            onClick = { onAction(state.primaryAction) },
            style = RhButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun TransactionListItem(
    state: TransactionListItemState,
    onOpenTaskDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (state.canOpenTaskDetail) {
        modifier.clickable { state.relatedTaskId?.let(onOpenTaskDetail) }
    } else {
        modifier
    }
    Row(
        modifier = clickModifier
            .fillMaxWidth()
            .padding(vertical = RhSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                color = RhTheme.colors.textPrimary,
                style = RhTypography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.statusLabel,
                color = RhTheme.colors.textTertiary,
                style = RhTypography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = state.amount,
            color = when (state.type) {
                TransactionTypeVisualState.Refund,
                TransactionTypeVisualState.Recharge -> RhTheme.colors.statusSuccess
                else -> RhTheme.colors.textPrimary
            },
            style = RhTypography.body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AssetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = RhTheme.colors.surfaceElevated,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.md),
        border = BorderStroke(1.dp, RhTheme.colors.borderDefault),
    ) {
        Column(
            modifier = Modifier.padding(RhSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
            content = content,
        )
    }
}

@Composable
private fun AssetMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.sectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = RhTheme.colors.textTertiary,
            style = RhTypography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RiskText(risk: WalletBalanceRiskState) {
    if (risk.message.isBlank()) return
    Text(
        text = risk.message,
        color = when (risk) {
            is WalletBalanceRiskState.InsufficientBalance,
            is WalletBalanceRiskState.BalanceUnavailable -> RhTheme.colors.statusFailed
            is WalletBalanceRiskState.LowBalance -> RhTheme.colors.statusWarning
            else -> RhTheme.colors.textTertiary
        },
        style = RhTypography.caption,
    )
}
