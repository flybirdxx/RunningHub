package com.runninghub.app.ui.designsystem.components.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 结算信息卡中的一行展示数据。
 *
 * @property label 行标题，调用方传入本地化文案。
 * @property value 行内容，调用方传入本地化文案或格式化后的金额。
 * @property emphasized `true` 表示该行需要强调，通常用于预计消耗。
 */
data class BillingInfoRow(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
)

/**
 * 渲染生成确认中的结算信息卡。
 *
 * 组件只负责统一行距、边框和文字层级，不读取账户、会员或任务状态。
 */
@Composable
fun BillingInfoCard(
    rows: List<BillingInfoRow>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = RhTheme.colors.surfaceSunken,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.sm),
        border = BorderStroke(1.dp, RhTheme.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RhSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
                ) {
                    Text(
                        text = row.label,
                        color = RhTheme.colors.textTertiary,
                        style = RhTypography.meta,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.value,
                        color = if (row.emphasized) {
                            RhTheme.colors.textPrimary
                        } else {
                            RhTheme.colors.textSecondary
                        },
                        style = RhTypography.body,
                        fontWeight = if (row.emphasized) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1.35f),
                    )
                }
            }
        }
    }
}
