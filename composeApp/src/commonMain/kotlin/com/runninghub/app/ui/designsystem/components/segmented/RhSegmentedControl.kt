package com.runninghub.app.ui.designsystem.components.segmented

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** RhSegmentedControl 的固定尺寸契约。 */
object RhSegmentedControlDefaults {
    val height = 32.dp
}

/**
 * 分段切换控件，用于图片/视频这类互斥模式切换。
 * 选中段使用品牌底色胶囊，未选中段保持透明，贴合暗色面板。
 *
 * @param options 分段文案，调用方负责本地化；顺序即展示顺序。
 * @param selectedIndex 当前选中下标。
 * @param onSelect 点击某段时回传其下标。
 */
@Composable
fun RhSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    Row(
        modifier = modifier.height(RhSegmentedControlDefaults.height),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RhTheme.shapes.full))
                    .background(if (selected) colors.brandMuted else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = RhSpacing.lg, vertical = RhSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    color = if (selected) colors.brandPrimary else colors.textTertiary,
                    style = if (selected) RhTypography.bodyStrong else RhTypography.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
