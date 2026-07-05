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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
 * History 任务卡来源标签类型。
 *
 * Design System 仅根据类型选择视觉样式，具体来源识别由调用方完成。
 */
enum class HistoryTaskSourceBadgeType {
    QuickCreate,
    Workflow,
    Api,
    WebApp,
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
 * History 任务卡来源短标签状态。
 *
 * @property type 稳定来源类型，用于选择弱强调视觉。
 * @property label 标题前展示的短标签文案。
 * @property contentDescription 完整来源语义，供辅助功能读取。
 */
data class HistoryTaskSourceBadgeState(
    val type: HistoryTaskSourceBadgeType,
    val label: String,
    val contentDescription: String,
)

/**
 * History 任务卡完整状态。
 *
 * @property title 任务名或模型名。
 * @property thumbnailUrl 可选缩略图地址，仅供调用方媒体 slot 使用。
 * @property status 状态徽标状态。
 * @property sourceLabel 生成方式文案。
 * @property sourceBadge 标题前的来源短标签；为空时不展示来源标签。
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
    val sourceBadge: HistoryTaskSourceBadgeState? = null,
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
                HistoryTaskCardTitle(
                    title = state.title,
                    sourceBadge = state.sourceBadge,
                )
                RhTaskStatusBadge(
                    status = state.status.type.toRhTaskStatus(),
                    label = state.status.label,
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
                modifier = Modifier
                    .width(92.dp)
                    .padding(top = RhSpacing.xxl),
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
private fun HistoryTaskCardTitle(
    title: String,
    sourceBadge: HistoryTaskSourceBadgeState?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sourceBadge?.let { badge -> HistoryTaskSourceBadge(badge) }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryTaskSourceBadge(state: HistoryTaskSourceBadgeState) {
    val colors = RhTheme.colors
    val contentColor = when (state.type) {
        HistoryTaskSourceBadgeType.QuickCreate -> colors.brandPrimary
        HistoryTaskSourceBadgeType.Workflow -> colors.statusProcessing
        HistoryTaskSourceBadgeType.Api -> colors.brandSecondary
        HistoryTaskSourceBadgeType.WebApp -> colors.textTertiary
    }
    val containerColor = when (state.type) {
        HistoryTaskSourceBadgeType.WebApp -> colors.surfaceSunken
        else -> contentColor.copy(alpha = 0.16f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(containerColor)
            .clearAndSetSemantics { contentDescription = state.contentDescription }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = state.label,
            color = contentColor,
            style = RhTypography.meta,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
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
    val visiblePrimaryAction = primaryAction?.takeIf { it.type.shouldShowInHistoryCard() }
    val visibleSecondaryActions = secondaryActions.filter { it.type.shouldShowInHistoryCard() }
    if (visiblePrimaryAction == null && visibleSecondaryActions.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        visiblePrimaryAction?.let { action ->
            RhButton(
                text = action.label,
                onClick = { onAction(action.type) },
                enabled = action.enabled,
                style = RhButtonStyle.Secondary,
            )
        }
        visibleSecondaryActions.take(1).forEach { action ->
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

private fun HistoryTaskCardActionType.shouldShowInHistoryCard(): Boolean = when (this) {
    HistoryTaskCardActionType.ViewResult,
    HistoryTaskCardActionType.ReuseParameters,
    HistoryTaskCardActionType.ViewDetail -> false
    HistoryTaskCardActionType.Retry,
    HistoryTaskCardActionType.Cancel -> true
}

private fun HistoryTaskCardStatusType.toRhTaskStatus(): RhTaskStatus = when (this) {
    HistoryTaskCardStatusType.Success -> RhTaskStatus.Success
    HistoryTaskCardStatusType.Failed -> RhTaskStatus.Failed
    HistoryTaskCardStatusType.InProgress -> RhTaskStatus.Running
    HistoryTaskCardStatusType.Canceled -> RhTaskStatus.Canceled
    HistoryTaskCardStatusType.Unknown -> RhTaskStatus.Queued
}
