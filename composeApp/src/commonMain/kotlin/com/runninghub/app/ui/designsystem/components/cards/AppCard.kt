package com.runninghub.app.ui.designsystem.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** AppCard 结果预览媒体类型。 */
enum class AppCardPreviewType {
    Image,
    Video,
    Audio,
    Empty,
}

/** AppCard 主操作类型。 */
enum class AppCardActionType {
    ViewDetail,
    Generate,
}

/**
 * AppCard 结果预览状态。
 *
 * @property url 可展示的预览资源地址；为空时组件展示占位。
 * @property type 预览资源类型。
 */
data class AppCardPreviewState(
    val url: String?,
    val type: AppCardPreviewType,
)

/**
 * AppCard 辅助指标状态。
 *
 * @property label 调用方已本地化的指标名称。
 * @property value 指标值，保持调用方格式。
 */
data class AppCardMetricState(
    val label: String,
    val value: String,
) {
    val displayText: String
        get() = listOf(label, value).filter { it.isNotBlank() }.joinToString(" ")
}

/**
 * AppCard 主操作状态。
 *
 * @property type 稳定动作类型。
 * @property label 调用方已本地化的按钮文案。
 * @property enabled 当前动作是否可点击。
 */
data class AppCardActionState(
    val type: AppCardActionType,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * 创作入口 AppCard 完整状态。
 *
 * @property id WebApp ID 或卡片稳定 ID。
 * @property title 模板名。
 * @property capabilityLabel 调用方已本地化的能力类型。
 * @property preview 结果预览。
 * @property estimatedCostLabel 调用方已本地化的预计费用。
 * @property metric 使用次数或成功率等辅助指标。
 * @property primaryAction 主操作。
 */
data class AppCardState(
    val id: String,
    val title: String,
    val capabilityLabel: String,
    val preview: AppCardPreviewState,
    val estimatedCostLabel: String,
    val metric: AppCardMetricState?,
    val primaryAction: AppCardActionState,
)

/** 渲染发现页和后续广场页使用的创作入口 AppCard。 */
@Composable
fun AppCard(
    state: AppCardState,
    onClick: () -> Unit,
    onAction: (AppCardActionType) -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable BoxScope.(AppCardPreviewState) -> Unit = { AppCardPreviewPlaceholder(it) },
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        color = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        border = BorderStroke(1.dp, RhTheme.colors.borderDefault),
    ) {
        Column(
            modifier = Modifier.padding(RhSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(RhTheme.shapes.sm)),
            ) {
                previewContent(state.preview)
            }
            Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.xs)) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.capabilityLabel,
                        color = RhTheme.colors.brandPrimary,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = state.estimatedCostLabel,
                        color = RhTheme.colors.textSecondary,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                state.metric?.let { metric ->
                    Text(
                        text = metric.displayText,
                        color = RhTheme.colors.textTertiary,
                        style = RhTypography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RhButton(
                    text = state.primaryAction.label,
                    onClick = { onAction(state.primaryAction.type) },
                    enabled = state.primaryAction.enabled,
                    style = if (state.primaryAction.type == AppCardActionType.Generate) {
                        RhButtonStyle.Primary
                    } else {
                        RhButtonStyle.Secondary
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.AppCardPreviewPlaceholder(preview: AppCardPreviewState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {}
}
