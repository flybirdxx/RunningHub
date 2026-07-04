package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.components.segmented.RhSegmentedControl
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import com.runninghub.feature.community.presentation.PlazaMode
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_category_all
import runninghub.composeapp.generated.resources.plaza_filter_label
import runninghub.composeapp.generated.resources.plaza_mode_creations
import runninghub.composeapp.generated.resources.plaza_mode_shorts
import runninghub.composeapp.generated.resources.plaza_refresh_content_description
import runninghub.composeapp.generated.resources.plaza_sort_hot
import runninghub.composeapp.generated.resources.plaza_sort_latest
import runninghub.composeapp.generated.resources.plaza_sort_recommend
import runninghub.composeapp.generated.resources.plaza_title

/**
 * 广场标题行。
 *
 * 仅保留标题与刷新入口；旧的搜索图标是空实现死入口，已随本次改版移除，避免误导用户点击。
 */
@Composable
internal fun PlazaHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.plaza_title),
            color = RhTheme.colors.textPrimary,
            style = RhTypography.sectionTitle,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.plaza_refresh_content_description),
                tint = RhTheme.colors.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * 广场模式切换 + 排序行。
 *
 * “灵感/短片”模式改用 [RhSegmentedControl]，右侧排序下拉沿用发现页的 Rh token 皮肤。
 */
@Composable
internal fun PlazaModeSortRow(
    selectedMode: PlazaMode,
    onModeSelected: (PlazaMode) -> Unit,
    selectedSort: String,
    onSortSelected: (String) -> Unit,
) {
    val modes = listOf(PlazaMode.CREATIONS, PlazaMode.SHORTS)
    val modeLabels = listOf(
        stringResource(Res.string.plaza_mode_creations),
        stringResource(Res.string.plaza_mode_shorts),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RhSegmentedControl(
            options = modeLabels,
            selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0),
            onSelect = { index -> modes.getOrNull(index)?.let(onModeSelected) },
        )
        Spacer(Modifier.weight(1f))
        PlazaSortDropdown(
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
        )
    }
}

/**
 * 广场排序下拉。保留下拉交互，容器色与文字统一走 Rh 语义 token（镜像发现页排序皮肤）。
 */
@Composable
private fun PlazaSortDropdown(
    selectedSort: String,
    onSortSelected: (String) -> Unit,
) {
    val colors = RhTheme.colors
    val options = plazaSortOptions(
        recommendLabel = stringResource(Res.string.plaza_sort_recommend),
        hotLabel = stringResource(Res.string.plaza_sort_hot),
        latestLabel = stringResource(Res.string.plaza_sort_latest),
    )
    val selectedLabel = plazaSelectedSortLabel(selectedSort = selectedSort, options = options)
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(RhTheme.shapes.sm))
                .background(colors.surfaceElevated)
                .clickable { expanded = true }
                .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLabel,
                color = colors.textSecondary,
                style = RhTypography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.plaza_filter_label),
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.surfaceElevated),
        ) {
            options.forEach { option ->
                val selected = selectedSort == option.value
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (selected) colors.brandPrimary else colors.textSecondary,
                            style = RhTypography.body,
                        )
                    },
                    onClick = {
                        onSortSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** 广场短片分类行，统一使用 [RhChip] 选中语义。 */
@Composable
internal fun PlazaShortCategoryRow(
    categories: List<PlazaShortCategory>,
    selectedCode: String?,
    onCategorySelected: (String?) -> Unit,
) {
    val allLabel = stringResource(Res.string.plaza_category_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RhChip(label = allLabel, selected = selectedCode == null, onClick = { onCategorySelected(null) })
        plazaVisibleShortCategories(categories).forEach { category ->
            RhChip(
                label = category.name.ifBlank { category.code },
                selected = selectedCode == category.code,
                onClick = { onCategorySelected(category.code) },
            )
        }
    }
}

/** 广场灵感标签行，统一使用 [RhChip] 选中语义。 */
@Composable
internal fun PlazaTagRow(
    tags: List<PlazaTag>,
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit,
) {
    val allLabel = stringResource(Res.string.plaza_category_all)
    val visibleTags = plazaVisibleCreationTags(tags)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RhChip(label = allLabel, selected = selectedTagId == null, onClick = { onTagSelected(null) })
        visibleTags.forEach { tag ->
            RhChip(
                label = tag.name,
                selected = selectedTagId == tag.id,
                onClick = { onTagSelected(tag.id) },
            )
        }
    }
}
