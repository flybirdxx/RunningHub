package com.runninghub.app.ui.designsystem.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * 通用换行布局：子项超出可用宽度时自动换到下一行，行内水平对齐可配置，仅依赖稳定 Layout API。
 *
 * 用于替代实验性 FlowRow，规避编译期与运行期 compose-foundation
 * 版本偏差导致的 NoSuchMethodError。
 *
 * @param spacing 子项之间与行之间的统一间距。
 * @param modifier 外部布局修饰符。
 * @param horizontalAlignment 行内子项的水平对齐方式，默认居中；RTL 镜像由 placeRelative 统一处理。
 * @param content 需要换行排布的子项内容。
 */
@Composable
fun RhWrapRow(
    spacing: Dp,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val limit = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }

        val rows = mutableListOf<MutableList<Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        var row = mutableListOf<Placeable>()
        var rowWidth = 0
        var rowHeight = 0
        placeables.forEach { placeable ->
            val extra = if (row.isEmpty()) placeable.width else placeable.width + spacingPx
            if (row.isNotEmpty() && rowWidth + extra > limit) {
                rows.add(row); rowWidths.add(rowWidth); rowHeights.add(rowHeight)
                row = mutableListOf(); rowWidth = 0; rowHeight = 0
            }
            rowWidth += if (row.isEmpty()) placeable.width else placeable.width + spacingPx
            rowHeight = maxOf(rowHeight, placeable.height)
            row.add(placeable)
        }
        if (row.isNotEmpty()) {
            rows.add(row); rowWidths.add(rowWidth); rowHeights.add(rowHeight)
        }

        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (rowWidths.maxOrNull() ?: 0)
        val height = rowHeights.sum() + spacingPx * (rows.size - 1).coerceAtLeast(0)
        layout(width, height) {
            var y = 0
            rows.forEachIndexed { index, items ->
                val free = (width - rowWidths[index]).coerceAtLeast(0)
                // placeRelative 已负责 RTL 镜像，这里固定用 Ltr 计算逻辑偏移，避免二次镜像。
                var x = horizontalAlignment.align(0, free, LayoutDirection.Ltr)
                items.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeights[index] - placeable.height) / 2)
                    x += placeable.width + spacingPx
                }
                y += rowHeights[index] + spacingPx
            }
        }
    }
}
