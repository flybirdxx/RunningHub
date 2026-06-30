package com.runninghub.app.ui.designsystem.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.cards.ModelCard
import com.runninghub.app.ui.designsystem.components.cards.ModelCardState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 模型选择器分类筛选项。 */
data class ModelPickerFilterItem(
    val id: String,
    val label: String,
    val selected: Boolean,
)

/** 模型选择器 Sheet 的可渲染状态。 */
data class ModelPickerSheetState(
    val title: String,
    val searchQuery: String,
    val searchPlaceholder: String,
    val filters: List<ModelPickerFilterItem>,
    val cards: List<ModelCardState>,
    val loading: Boolean,
    val emptyText: String,
    val loadingText: String = "",
    val closeContentDescription: String? = null,
)

/** 渲染模型选择 Sheet。 */
@Composable
fun ModelPickerSheet(
    state: ModelPickerSheetState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onCardClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    RhBottomSheetSurface(modifier = modifier) {
        dragHandle?.let { handle ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                handle()
            }
        }
        ModelPickerHeader(
            title = state.title,
            closeContentDescription = state.closeContentDescription,
            onDismiss = onDismiss,
        )
        ModelPickerSearchField(
            query = state.searchQuery,
            placeholder = state.searchPlaceholder,
            onQueryChange = onSearchQueryChange,
        )
        ModelPickerFilterRow(
            filters = state.filters,
            onFilterSelect = onFilterSelect,
        )
        when {
            state.loading -> ModelPickerLoading(text = state.loadingText)
            state.cards.isEmpty() -> ModelPickerEmpty(text = state.emptyText)
            else -> ModelPickerCardList(
                cards = state.cards,
                onCardClick = onCardClick,
            )
        }
    }
}

@Composable
private fun ModelPickerHeader(
    title: String,
    closeContentDescription: String?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
    ) {
        Text(
            text = title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = closeContentDescription,
                tint = RhTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ModelPickerSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.sm),
        color = RhTheme.colors.surfaceSunken,
        border = BorderStroke(1.dp, RhTheme.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RhSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = RhTheme.colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = RhTheme.colors.textTertiary,
                        style = RhTypography.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = RhTheme.colors.textPrimary,
                        fontSize = RhTypography.body.fontSize,
                    ),
                    cursorBrush = SolidColor(RhTheme.colors.brandPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ModelPickerFilterRow(
    filters: List<ModelPickerFilterItem>,
    onFilterSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        filters.forEach { filter ->
            Surface(
                onClick = { onFilterSelect(filter.id) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.xs),
                color = if (filter.selected) RhTheme.colors.brandMuted else RhTheme.colors.surfaceDefault,
                border = BorderStroke(
                    1.dp,
                    if (filter.selected) RhTheme.colors.brandPrimary else RhTheme.colors.borderSubtle,
                ),
            ) {
                Text(
                    text = filter.label,
                    color = if (filter.selected) RhTheme.colors.brandPrimary else RhTheme.colors.textSecondary,
                    style = RhTypography.meta,
                    modifier = Modifier.padding(horizontal = RhSpacing.md, vertical = RhSpacing.xs),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ModelPickerCardList(
    cards: List<ModelCardState>,
    onCardClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        cards.forEach { card ->
            ModelCard(
                state = card,
                onClick = { onCardClick(card.id) },
            )
        }
    }
}

@Composable
private fun ModelPickerLoading(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = RhSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = RhTheme.colors.brandPrimary,
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.body,
        )
    }
}

@Composable
private fun ModelPickerEmpty(text: String) {
    Text(
        text = text,
        color = RhTheme.colors.textSecondary,
        style = RhTypography.body,
        modifier = Modifier.padding(vertical = RhSpacing.xl),
    )
}
