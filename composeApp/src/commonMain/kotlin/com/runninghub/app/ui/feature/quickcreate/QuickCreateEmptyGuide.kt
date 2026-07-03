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
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.components.layout.RhWrapRow
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
        RhWrapRow(spacing = RhSpacing.sm) {
            samples.forEach { sample ->
                RhChip(label = sample, onClick = { onSampleClick(sample) })
            }
        }
    }
}
