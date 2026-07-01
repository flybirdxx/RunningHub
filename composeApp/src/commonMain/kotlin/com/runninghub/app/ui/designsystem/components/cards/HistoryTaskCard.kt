package com.runninghub.app.ui.designsystem.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatusBadge
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * History 任务卡状态类型。
 *
 * Design System 使用该枚举选择徽标视觉，不读取服务端原始任务状态。
 */
enum class HistoryTaskCardStatusType {
    Success,
    Failed,
    InProgress,
    Canceled,
    Unknown,
}

/**
 * History 任务卡动作类型。
 *
 * 组件只把动作回传给调用方，不在 Design System 内执行详情、重试、取消或参数复用。
 */
enum class HistoryTaskCardActionType {
    ViewResult,
    Retry,
    Cancel,
    ReuseParameters,
    ViewDetail,
}

/**
 * History 任务卡费用类型。
 *
 * RHB 点数和法币使用不同类型，调用方可以避免在同一行混排不同计费体系。
 */
enum class HistoryTaskCardCostKind {
    Rhb,
    Fiat,
    Unknown,
}

/**
 * History 任务卡状态徽标状态。
 *
 * @property type 稳定状态类型。
 * @property label 调用方已本地化的状态文案。
 */
data class HistoryTaskCardStatusState(
    val type: HistoryTaskCardStatusType,
    val label: String,
)

/**
 * History 任务卡费用状态。
 *
 * @property kind 费用类型。
 * @property amountLabel 调用方已本地化或格式化的费用文案，例如 `12.5 RHB`。
 */
data class HistoryTaskCardCostState(
    val kind: HistoryTaskCardCostKind,
    val amountLabel: String,
)

/**
 * History 任务卡动作状态。
 *
 * @property type 稳定动作类型。
 * @property label 调用方已本地化的按钮文案。
 * @property enabled 当前动作是否可点击。
 */
data class HistoryTaskCardActionState(
    val type: HistoryTaskCardActionType,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * History 任务卡完整状态。
 *
 * @property title 任务名或模型名。
 * @property thumbnailUrl 可选缩略图地址，仅供调用方媒体 slot 使用。
 * @property status 状态徽标状态。
 * @property sourceLabel 生成方式文案。
 * @property cost 可选费用状态。
 * @property durationLabel 可选耗时文案。
 * @property expiryLabel 可选过期提醒文案。
 * @property outputCountLabel 结果数量文案。
 * @property primaryAction 主操作；成功任务通常是查看结果，失败任务通常是重试。
 * @property secondaryActions 次级动作集合。
 */
data class HistoryTaskCardState(
    val title: String,
    val thumbnailUrl: String?,
    val status: HistoryTaskCardStatusState,
    val sourceLabel: String,
    val cost: HistoryTaskCardCostState?,
    val durationLabel: String?,
    val expiryLabel: String?,
    val outputCountLabel: String?,
    val primaryAction: HistoryTaskCardActionState?,
    val secondaryActions: List<HistoryTaskCardActionState> = emptyList(),
)

/**
 * 展示 History 首屏任务卡。
 *
 * 卡片负责呈现任务是什么、状态如何、花了多少和下一步操作；点击行为全部通过回调返回给页面层。
 */
@Composable
fun HistoryTaskCard(
    state: HistoryTaskCardState,
    onClick: () -> Unit,
    onAction: (HistoryTaskCardActionType) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailContent: @Composable BoxScope.(String?) -> Unit = { HistoryTaskThumbnailPlaceholder() },
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        color = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.lg),
    ) {
        Row(
            modifier = Modifier.padding(RhSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(RhTheme.shapes.md)),
            ) {
                thumbnailContent(state.thumbnailUrl)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RhTaskStatusBadge(
                    status = state.status.type.toRhTaskStatus(),
                    label = state.status.label,
                )
                Text(
                    text = state.sourceLabel,
                    color = RhTheme.colors.textSecondary,
                    style = RhTypography.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.expiryLabel?.let { label ->
                    Text(
                        text = label,
                        color = RhTheme.colors.statusWarning,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HistoryTaskCardActions(
                    primaryAction = state.primaryAction,
                    secondaryActions = state.secondaryActions,
                    onAction = onAction,
                )
            }
            Column(
                modifier = Modifier.width(92.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                state.cost?.let { cost ->
                    Text(
                        text = cost.amountLabel,
                        color = RhTheme.colors.textPrimary,
                        style = RhTypography.bodyStrong,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.durationLabel?.let { duration ->
                    Text(
                        text = duration,
                        color = RhTheme.colors.textSecondary,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.outputCountLabel?.let { count ->
                    Text(
                        text = count,
                        color = RhTheme.colors.textTertiary,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.HistoryTaskThumbnailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
    )
}

@Composable
private fun HistoryTaskCardActions(
    primaryAction: HistoryTaskCardActionState?,
    secondaryActions: List<HistoryTaskCardActionState>,
    onAction: (HistoryTaskCardActionType) -> Unit,
) {
    if (primaryAction == null && secondaryActions.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        primaryAction?.let { action ->
            RhButton(
                text = action.label,
                onClick = { onAction(action.type) },
                enabled = action.enabled,
                style = RhButtonStyle.Secondary,
            )
        }
        secondaryActions.take(1).forEach { action ->
            RhButton(
                text = action.label,
                onClick = { onAction(action.type) },
                enabled = action.enabled,
                style = RhButtonStyle.Ghost,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun HistoryTaskCardStatusType.toRhTaskStatus(): RhTaskStatus = when (this) {
    HistoryTaskCardStatusType.Success -> RhTaskStatus.Success
    HistoryTaskCardStatusType.Failed -> RhTaskStatus.Failed
    HistoryTaskCardStatusType.InProgress -> RhTaskStatus.Running
    HistoryTaskCardStatusType.Canceled -> RhTaskStatus.Canceled
    HistoryTaskCardStatusType.Unknown -> RhTaskStatus.Queued
}
