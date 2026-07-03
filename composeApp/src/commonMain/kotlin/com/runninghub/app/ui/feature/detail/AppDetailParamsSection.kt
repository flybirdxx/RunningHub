package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.detail.presentation.AppDetailInputRowUiModel
import com.runninghub.feature.detail.presentation.AppDetailParamsGroup
import com.runninghub.feature.detail.presentation.AppDetailParamsLayoutUiModel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_params_group_fallback
import runninghub.composeapp.generated.resources.app_detail_params_modified_count
import runninghub.composeapp.generated.resources.app_detail_params_reset_group

/**
 * 参数区容器：核心行平铺，高级分组折叠（方案 A）。
 *
 * @param layout Presentation 解析出的自适应布局。
 * @param renderRow 单行渲染插槽，由调用方接既有输入行分发函数。
 * @param onResetGroup 分组重置：调用方对组内每个字段写回默认值。
 */
@Composable
internal fun AppDetailParamsSection(
    layout: AppDetailParamsLayoutUiModel,
    renderRow: @Composable (AppDetailInputRowUiModel) -> Unit,
    onResetGroup: (AppDetailParamsGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
        layout.coreRows.forEach { row -> renderRow(row) }
        layout.advancedGroups.forEachIndexed { index, group ->
            AdvancedGroupCard(
                group = group,
                groupKey = "params-group-$index",
                renderRow = renderRow,
                onReset = { onResetGroup(group) },
            )
        }
    }
}

@Composable
private fun AdvancedGroupCard(
    group: AppDetailParamsGroup,
    groupKey: String,
    renderRow: @Composable (AppDetailInputRowUiModel) -> Unit,
    onReset: () -> Unit,
) {
    val colors = RhTheme.colors
    var expanded by rememberSaveable(groupKey) { mutableStateOf(false) }
    val borderColor = if (expanded) colors.borderActive else colors.borderDefault
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(RhTheme.shapes.md))
            .background(colors.surfaceDefault, RoundedCornerShape(RhTheme.shapes.md))
            .padding(RhSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.title ?: stringResource(Res.string.app_detail_params_group_fallback),
                color = colors.textPrimary,
                style = RhTypography.bodyStrong,
                modifier = Modifier.weight(1f),
            )
            if (group.modifiedCount > 0) {
                Text(
                    text = stringResource(Res.string.app_detail_params_modified_count, group.modifiedCount),
                    color = colors.brandPrimary,
                    style = RhTypography.meta,
                    modifier = Modifier
                        .background(colors.brandMuted, RoundedCornerShape(RhTheme.shapes.full))
                        .padding(horizontal = RhSpacing.sm, vertical = 2.dp),
                )
                Spacer(Modifier.width(RhSpacing.sm))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (expanded) colors.brandPrimary else colors.textTertiary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
                group.rows.forEach { row -> renderRow(row) }
                if (group.modifiedCount > 0) {
                    Text(
                        text = stringResource(Res.string.app_detail_params_reset_group),
                        color = colors.textSecondary,
                        style = RhTypography.caption,
                        modifier = Modifier
                            .clickable(onClick = onReset)
                            .padding(vertical = RhSpacing.xs),
                    )
                }
            }
        }
    }
}
