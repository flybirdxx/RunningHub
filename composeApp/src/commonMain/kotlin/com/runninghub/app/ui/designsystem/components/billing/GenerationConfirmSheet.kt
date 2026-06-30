package com.runninghub.app.ui.designsystem.components.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadgeState
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.components.sheets.RhBottomSheetSurface
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 生成确认 Sheet 的可渲染状态。
 *
 * @property title Sheet 标题文案。
 * @property priceBadgeState 预计消耗对应的价格徽标视觉状态。
 * @property priceLabel 已格式化的预计消耗文案。
 * @property rows 结算信息行，至少应包含预计消耗、余额、会员减免和失败扣费说明。
 * @property confirmActionLabel 主按钮文案。
 * @property dismissActionLabel 取消按钮文案。
 * @property confirmEnabled `true` 表示当前价格确认完成且允许继续提交。
 */
data class GenerationConfirmSheetState(
    val title: String,
    val priceBadgeState: RhPriceBadgeState,
    val priceLabel: String,
    val rows: List<BillingInfoRow>,
    val confirmActionLabel: String,
    val dismissActionLabel: String,
    val confirmEnabled: Boolean,
)

/**
 * 渲染生成前价格确认 Sheet。
 *
 * 组件不持有 QuickCreate 业务对象；调用方负责把计费状态映射成 [GenerationConfirmSheetState]。
 */
@Composable
fun GenerationConfirmSheet(
    state: GenerationConfirmSheetState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RhBottomSheetSurface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.sectionTitle,
                    modifier = Modifier.weight(1f),
                )
                PriceBadge(
                    state = state.priceBadgeState,
                    label = state.priceLabel,
                )
            }
            BillingInfoCard(rows = state.rows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                RhButton(
                    text = state.dismissActionLabel,
                    onClick = onDismiss,
                    style = RhButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                RhPrimaryButton(
                    text = state.confirmActionLabel,
                    onClick = onConfirm,
                    enabled = state.confirmEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
