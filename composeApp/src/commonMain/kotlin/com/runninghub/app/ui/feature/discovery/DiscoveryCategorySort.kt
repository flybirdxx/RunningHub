package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.core.model.Tag
import com.runninghub.feature.discovery.domain.CatalogSort
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_all_apps_title
import runninghub.composeapp.generated.resources.discovery_category_all
import runninghub.composeapp.generated.resources.discovery_sort_content_description
import runninghub.composeapp.generated.resources.discovery_sort_hottest
import runninghub.composeapp.generated.resources.discovery_sort_newest
import runninghub.composeapp.generated.resources.discovery_sort_recommend
import runninghub.composeapp.generated.resources.discovery_sort_reputation

/**
 * 发现页分类筛选行。从 DiscoveryScreen.kt 拆分而来，
 * 自绘 CategoryTag 胶囊退役，统一使用 RhChip 选中语义。
 */
@Composable
internal fun DiscoveryCategoryRow(
    categories: List<Tag>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        modifier = modifier.padding(vertical = RhSpacing.xs),
    ) {
        item(key = "category_all") {
            RhChip(
                label = stringResource(Res.string.discovery_category_all),
                selected = selectedIndex == 0,
                onClick = { onSelected(0) },
            )
        }
        itemsIndexed(categories, key = { _, tag -> tag.id }) { idx, tag ->
            RhChip(
                label = tag.name,
                selected = selectedIndex == idx + 1,
                onClick = { onSelected(idx + 1) },
            )
        }
    }
}

/**
 * 发现页排序行。保留下拉交互（选项可扩展、占位小），容器色与文字走 Rh 语义 token。
 */
@Composable
internal fun DiscoverySortRow(
    selectedSort: CatalogSort,
    onSortSelected: (CatalogSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = RhSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.discovery_all_apps_title),
            style = RhTypography.sectionTitle,
            color = colors.textPrimary,
        )

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(RhTheme.shapes.sm))
                    .background(colors.surfaceElevated)
                    .clickable { expanded = true }
                    .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = catalogSortLabel(selectedSort),
                    style = RhTypography.caption,
                    color = colors.textSecondary,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.discovery_sort_content_description),
                    modifier = Modifier.size(16.dp),
                    tint = colors.textTertiary,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.surfaceElevated),
            ) {
                CatalogSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = catalogSortLabel(option),
                                style = RhTypography.body,
                                color = if (option == selectedSort) colors.brandPrimary else colors.textSecondary,
                            )
                        },
                        onClick = {
                            onSortSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * 将发现页排序枚举映射为 Compose Resources 文案。
 *
 * 排序语义仍由 Domain 层 [CatalogSort] 表达，UI 只在最终渲染前选择本地化文案，
 * 避免独立 Presentation 模块继续持有用户可见固定字符串。
 */
@Composable
internal fun catalogSortLabel(sort: CatalogSort): String {
    val resource = when (sort) {
        CatalogSort.RECOMMEND -> Res.string.discovery_sort_recommend
        CatalogSort.REPUTATION -> Res.string.discovery_sort_reputation
        CatalogSort.HOTTEST -> Res.string.discovery_sort_hottest
        CatalogSort.NEWEST -> Res.string.discovery_sort_newest
    }
    return stringResource(resource)
}
