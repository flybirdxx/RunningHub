package com.runninghub.app.ui.designsystem.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelector
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

data class AdvancedSettingsSectionState(
    val id: String = "",
    val title: String,
    val selectors: List<ParameterSelectorState>,
    val collapsed: Boolean = false,
    val contentCount: Int = selectors.size,
)

data class AdvancedSettingsSheetState(
    val title: String,
    val commonSection: AdvancedSettingsSectionState,
    val advancedSection: AdvancedSettingsSectionState,
    val emptyText: String,
    val doneLabel: String = "",
    val advancedToggleText: String = "",
    val closeContentDescription: String? = null,
) {
    val empty: Boolean
        get() = commonSection.contentCount == 0 && advancedSection.contentCount == 0
}

@Composable
fun AdvancedSettingsSheet(
    state: AdvancedSettingsSheetState,
    onOptionSelected: (String, String) -> Unit,
    onAdvancedToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentMaxHeight: Dp = 560.dp,
    contentBottomPadding: Dp = 0.dp,
    dragHandle: (@Composable () -> Unit)? = null,
    leadingContent: @Composable ColumnScope.() -> Unit = {},
    sectionContent: @Composable ColumnScope.(AdvancedSettingsSectionState) -> Unit = { section ->
        section.selectors.forEach { selector ->
            ParameterSelector(
                state = selector,
                onOptionSelected = onOptionSelected,
            )
        }
    },
) {
    RhBottomSheetSurface(modifier = modifier) {
        dragHandle?.invoke()
        AdvancedSettingsHeader(
            title = state.title,
            closeContentDescription = state.closeContentDescription,
            onDismiss = onDismiss,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = contentMaxHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                leadingContent()
                if (state.empty) {
                    Text(
                        text = state.emptyText,
                        color = RhTheme.colors.textSecondary,
                        style = RhTypography.body,
                        modifier = Modifier.padding(vertical = RhSpacing.xl),
                    )
                } else {
                    if (state.commonSection.contentCount > 0) {
                        AdvancedSettingsSection(
                            state = state.commonSection,
                            content = sectionContent,
                        )
                    }
                    if (state.advancedSection.contentCount > 0) {
                        AdvancedSettingsAdvancedHeader(
                            title = state.advancedSection.title,
                            toggleText = state.advancedToggleText,
                            onToggle = onAdvancedToggle,
                        )
                        if (!state.advancedSection.collapsed) {
                            AdvancedSettingsSection(
                                state = state.advancedSection,
                                content = sectionContent,
                            )
                        }
                    }
                }
                if (state.doneLabel.isNotBlank()) {
                    RhPrimaryButton(
                        text = state.doneLabel,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (contentBottomPadding > 0.dp) {
                    Spacer(modifier = Modifier.height(contentBottomPadding))
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsHeader(
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun AdvancedSettingsAdvancedHeader(
    title: String,
    toggleText: String,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        color = RhTheme.colors.surfaceSunken,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(RhTheme.shapes.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            Text(
                text = title,
                color = RhTheme.colors.textPrimary,
                style = RhTypography.bodyStrong,
                modifier = Modifier.weight(1f),
            )
            if (toggleText.isNotBlank()) {
                Text(
                    text = toggleText,
                    color = RhTheme.colors.brandPrimary,
                    style = RhTypography.meta,
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    state: AdvancedSettingsSectionState,
    content: @Composable ColumnScope.(AdvancedSettingsSectionState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
        Text(
            text = state.title,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.caption,
        )
        content(state)
    }
}
