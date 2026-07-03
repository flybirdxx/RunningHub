package com.runninghub.app.ui.designsystem.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 芯片语义风格。Neutral 用于常规可选项，Brand 用于品牌强调入口（如当前模型）。
 */
enum class RhChipStyle(val tokenName: String) {
    Neutral("neutral"),
    Brand("brand"),
}

/** RhChip 的固定尺寸契约。 */
object RhChipDefaults {
    val height = 32.dp
}

/**
 * 胶囊形选择芯片。选中态与 Brand 风格使用品牌底色和品牌文字，其余使用默认表面色。
 *
 * @param label 芯片文案，调用方负责本地化。
 * @param onClick 点击回调。
 * @param style 语义风格。
 * @param selected 是否处于选中态。
 * @param leadingIcon 可选前置图标插槽。
 * @param trailingIcon 可选后置图标插槽。
 */
@Composable
fun RhChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RhChipStyle = RhChipStyle.Neutral,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = RhTheme.colors
    val highlighted = selected || style == RhChipStyle.Brand
    val shape = RoundedCornerShape(RhTheme.shapes.full)
    Row(
        modifier = modifier
            .height(RhChipDefaults.height)
            .clip(shape)
            .background(if (highlighted) colors.brandMuted else colors.surfaceDefault)
            .border(1.dp, if (highlighted) colors.borderActive else colors.borderDefault, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = RhSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
    ) {
        leadingIcon?.invoke()
        Text(
            text = label,
            color = if (highlighted) colors.brandPrimary else colors.textSecondary,
            style = RhTypography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingIcon?.invoke()
    }
}
