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

/** Plaza 作品卡片预览媒体类型。 */
enum class PlazaWorkCardPreviewType {
    Image,
    Video,
    Placeholder,
}

/** Plaza 作品卡片主动作。 */
enum class PlazaWorkCardAction {
    UseSame,
}

/**
 * Plaza 作品卡片预览状态。
 *
 * @property url 预览媒体地址；为空时组件展示占位背景。
 * @property type 预览媒体类型。
 */
data class PlazaWorkCardPreviewState(
    val url: String?,
    val type: PlazaWorkCardPreviewType,
)

/**
 * Plaza 作品卡片辅助指标状态。
 *
 * @property label 调用方已本地化的指标名。
 * @property value 指标值，保持调用方格式。
 */
data class PlazaWorkCardMetricState(
    val label: String,
    val value: String,
) {
    val displayText: String
        get() = listOf(label, value).filter { it.isNotBlank() }.joinToString(" ")
}

/**
 * Plaza 作品卡片完整状态。
 *
 * @property id Plaza 作品 ID。
 * @property title 作品标题或 Prompt 摘要。
 * @property authorName 作者名称；为空时调用方可选择展示默认来源。
 * @property preview 作品媒体预览。
 * @property metric 使用数等辅助指标。
 * @property primaryAction 主动作，当前用于使用同款。
 * @property sourceProtected 是否需要展示来源保护语义。
 */
data class PlazaWorkCardState(
    val id: String,
    val title: String,
    val authorName: String?,
    val preview: PlazaWorkCardPreviewState,
    val metric: PlazaWorkCardMetricState?,
    val primaryAction: PlazaWorkCardAction,
    val actionLabel: String = "",
    val sourceProtected: Boolean,
    val enabled: Boolean = true,
)

@Composable
fun PlazaWorkCard(
    state: PlazaWorkCardState,
    onClick: () -> Unit,
    onAction: (PlazaWorkCardAction) -> Unit,
    modifier: Modifier = Modifier,
    previewContent: @Composable BoxScope.(PlazaWorkCardPreviewState) -> Unit = {
        PlazaWorkCardPreviewPlaceholder()
    },
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = RhTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, RhTheme.colors.borderDefault),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            ) {
                previewContent(state.preview)
            }
            Column(
                modifier = Modifier.padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.authorName?.takeIf { it.isNotBlank() }?.let { author ->
                        Text(
                            text = author,
                            color = RhTheme.colors.textTertiary,
                            style = RhTypography.caption,
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
                }
                if (state.actionLabel.isNotBlank()) {
                    RhButton(
                        text = state.actionLabel,
                        onClick = { onAction(state.primaryAction) },
                        enabled = state.enabled,
                        style = RhButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.PlazaWorkCardPreviewPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
    )
}
