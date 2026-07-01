package com.runninghub.app.ui.designsystem.components.billing

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
import androidx.compose.ui.text.style.TextOverflow
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 钱包卡片中的操作入口。 */
sealed class WalletBalanceAction(open val label: String) {
    data class Recharge(override val label: String) : WalletBalanceAction(label)
}

/** 钱包卡片的风险提示状态。 */
sealed class WalletBalanceRiskState(open val message: String) {
    data class None(override val message: String = "") : WalletBalanceRiskState(message)
    data class BalanceUnavailable(override val message: String) : WalletBalanceRiskState(message)
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
)

/** 会员卡片视觉状态。 */
enum class MembershipStatusVisualState {
    Active,
    Expired,
    Unknown,
    None,
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
    val showInsideCreateInput: Boolean = false,
)

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
        RhButton(
            text = state.primaryAction.label,
            onClick = { onAction(state.primaryAction) },
            style = RhButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MembershipCard(
    state: MembershipCardState,
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
            is WalletBalanceRiskState.BalanceUnavailable -> RhTheme.colors.statusFailed
            else -> RhTheme.colors.textTertiary
        },
        style = RhTypography.caption,
    )
}
