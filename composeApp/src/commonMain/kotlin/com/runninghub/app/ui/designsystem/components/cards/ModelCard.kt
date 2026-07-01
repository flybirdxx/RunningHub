package com.runninghub.app.ui.designsystem.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 模型卡片视觉状态。 */
enum class ModelCardVisualState {
    Default,
    Selected,
    Disabled,
}

/**
 * 模型卡片的可渲染状态。
 *
 * @property title 模型名称。
 * @property capability 能力类型，例如图片生成或视频生成。
 * @property scene 适用场景说明。
 * @property technicalTags 技术标签，作为辅助信息展示。
 * @property price 目录价格摘要或运行前确认费用占位。
 * @property selected 当前模型是否已选中。
 * @property visualState 卡片视觉状态。
 * @property id 点击回传的稳定身份键。
 */
data class ModelCardState(
    val title: String,
    val capability: String,
    val scene: String,
    val technicalTags: List<String>,
    val price: String,
    val selected: Boolean,
    val visualState: ModelCardVisualState,
    val id: String = "",
)

/** 渲染模型选择列表中的单张模型卡片。 */
@Composable
fun ModelCard(
    state: ModelCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.visualState == ModelCardVisualState.Selected || state.selected
    Surface(
        onClick = onClick,
        enabled = state.visualState != ModelCardVisualState.Disabled,
        modifier = modifier.fillMaxWidth(),
        color = if (selected) RhTheme.colors.brandMuted else RhTheme.colors.surfaceDefault,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.md),
    ) {
        Row(
            modifier = Modifier.padding(RhSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.title,
                        color = RhTheme.colors.textPrimary,
                        style = RhTypography.body,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = state.price,
                        color = RhTheme.colors.textPrimary,
                        style = RhTypography.meta,
                        maxLines = 1,
                    )
                }
                Text(
                    text = state.capability,
                    color = RhTheme.colors.textSecondary,
                    style = RhTypography.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.scene,
                    color = RhTheme.colors.textTertiary,
                    style = RhTypography.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
                ) {
                    state.technicalTags.take(2).forEach { tag ->
                        ModelTechnicalTag(text = tag)
                    }
                }
            }
            ModelSelectedMark(selected = selected)
        }
    }
}

@Composable
private fun ModelTechnicalTag(text: String) {
    Surface(
        color = RhTheme.colors.surfaceSunken,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.xs),
    ) {
        Text(
            text = text,
            color = RhTheme.colors.textTertiary,
            style = RhTypography.meta,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = RhSpacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun ModelSelectedMark(selected: Boolean) {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = if (selected) RhTheme.colors.brandPrimary else RhTheme.colors.surfaceSunken,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = RhTheme.colors.textInverse,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}
