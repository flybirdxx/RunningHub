package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.sheets.RhBottomSheetSurface
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_option_picker_empty
import runninghub.composeapp.generated.resources.app_detail_option_picker_search_hint

/** 选项数超过该值时展示搜索过滤框。 */
internal const val APP_DETAIL_OPTION_SEARCH_THRESHOLD = 12

/** 归一化搜索过滤：空白查询返回全量，否则忽略大小写包含匹配。 */
internal fun filterPickerOptions(options: List<String>, query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return options
    return options.filter { it.contains(trimmed, ignoreCase = true) }
}

/**
 * 长下拉字段的底部弹层选择器。
 *
 * @param title 字段标题。
 * @param options 服务端候选项。
 * @param selected 当前选中值。
 * @param onSelect 用户选择后回传选项；调用方负责关闭弹层并写回状态。
 * @param onDismiss 点击遮罩关闭。
 */
@Composable
internal fun AppDetailOptionPickerSheet(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = RhTheme.colors
    var query by remember { mutableStateOf("") }
    val visibleOptions = filterPickerOptions(options, query)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.overlayScrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(modifier = Modifier.clickable(enabled = false, onClick = {})) {
            RhBottomSheetSurface {
                Text(text = title, color = colors.textPrimary, style = RhTypography.cardTitle)
                if (options.size > APP_DETAIL_OPTION_SEARCH_THRESHOLD) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = RhTypography.body.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.brandPrimary),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = RhSpacing.sm)
                                    .background(colors.surfaceSunken, RoundedCornerShape(RhTheme.shapes.sm))
                                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.app_detail_option_picker_search_hint),
                                        color = colors.textTertiary,
                                        style = RhTypography.body,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = RhSpacing.sm)) {
                    items(visibleOptions) { option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .background(
                                    if (isSelected) colors.surfaceSelected else colors.overlaySheet,
                                    RoundedCornerShape(RhTheme.shapes.sm),
                                )
                                .padding(horizontal = RhSpacing.md, vertical = RhSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) colors.brandPrimary else colors.textPrimary,
                                style = RhTypography.body,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                )
                            }
                        }
                    }
                    if (visibleOptions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.app_detail_option_picker_empty),
                                color = colors.textTertiary,
                                style = RhTypography.caption,
                                modifier = Modifier.padding(RhSpacing.lg),
                            )
                        }
                    }
                }
            }
        }
    }
}
