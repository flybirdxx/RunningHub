package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_empty_sample_cat
import runninghub.composeapp.generated.resources.quick_create_empty_sample_city
import runninghub.composeapp.generated.resources.quick_create_empty_sample_coast
import runninghub.composeapp.generated.resources.quick_create_empty_subtitle
import runninghub.composeapp.generated.resources.quick_create_empty_title

/** 归一化示例提示词：去首尾空白、去空串、最多保留 3 条。 */
internal fun quickCreateEmptySamples(raw: List<String>): List<String> =
    raw.map { it.trim() }.filter { it.isNotEmpty() }.take(3)

/**
 * 无对话时的空态引导：品牌图标、引导文案和示例提示词芯片。
 *
 * @param onSampleClick 点击示例时回传其文案，由调用方写回创作输入框。
 */
@Composable
internal fun QuickCreateEmptyGuide(
    onSampleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    val samples = quickCreateEmptySamples(
        listOf(
            stringResource(Res.string.quick_create_empty_sample_city),
            stringResource(Res.string.quick_create_empty_sample_cat),
            stringResource(Res.string.quick_create_empty_sample_coast),
        ),
    )
    Column(
        modifier = modifier.fillMaxSize().padding(RhSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(colors.brandMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = colors.brandPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(RhSpacing.md))
        Text(
            text = stringResource(Res.string.quick_create_empty_title),
            color = colors.textPrimary,
            style = RhTypography.cardTitle,
        )
        Spacer(Modifier.height(RhSpacing.xs))
        Text(
            text = stringResource(Res.string.quick_create_empty_subtitle),
            color = colors.textTertiary,
            style = RhTypography.caption,
        )
        Spacer(Modifier.height(RhSpacing.lg))
        QuickCreateSampleWrapRow(spacing = RhSpacing.sm) {
            samples.forEach { sample ->
                RhChip(label = sample, onClick = { onSampleClick(sample) })
            }
        }
    }
}

/**
 * 空态示例芯片的换行布局，行内居中，仅依赖稳定 Layout API。
 * 用于替代实验性 FlowRow，规避编译期与运行期 compose-foundation
 * 版本偏差导致的 NoSuchMethodError。
 */
@Composable
private fun QuickCreateSampleWrapRow(
    spacing: Dp,
    modifier: Modifier = Modifier,
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
                var x = ((width - rowWidths[index]) / 2).coerceAtLeast(0)
                items.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeights[index] - placeable.height) / 2)
                    x += placeable.width + spacingPx
                }
                y += rowHeights[index] + spacingPx
            }
        }
    }
}
