package com.runninghub.app.ui.designsystem.components.parameters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

enum class ParameterSelectorOptionVisualState {
    DEFAULT,
    SELECTED,
    DISABLED,
    ERROR,
}

data class ParameterSelectorOptionState(
    val id: String,
    val label: String,
    val visualState: ParameterSelectorOptionVisualState = ParameterSelectorOptionVisualState.DEFAULT,
) {
    val selected: Boolean
        get() = visualState == ParameterSelectorOptionVisualState.SELECTED

    val enabled: Boolean
        get() = visualState != ParameterSelectorOptionVisualState.DISABLED
}

data class ParameterSelectorState(
    val id: String,
    val title: String,
    val options: List<ParameterSelectorOptionState>,
    val valueText: String = "",
    val supportingText: String? = null,
)

@Composable
fun ParameterSelector(
    state: ParameterSelectorState,
    onOptionSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = RhTheme.colors.surfaceDefault,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        border = BorderStroke(1.dp, RhTheme.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(RhSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.valueText.isNotBlank()) {
                    Text(
                        text = state.valueText,
                        color = RhTheme.colors.textSecondary,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (state.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
                    state.options.chunked(3).forEach { rowOptions ->
                        Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
                            rowOptions.forEach { option ->
                                ParameterSelectorOption(
                                    option = option,
                                    onClick = { onOptionSelected(state.id, option.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - rowOptions.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            state.supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    color = RhTheme.colors.textTertiary,
                    style = RhTypography.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ParameterSelectorOption(
    option: ParameterSelectorOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = option.visualState == ParameterSelectorOptionVisualState.SELECTED
    val error = option.visualState == ParameterSelectorOptionVisualState.ERROR
    Surface(
        modifier = modifier.height(36.dp),
        onClick = onClick,
        enabled = option.enabled,
        color = when {
            selected -> RhTheme.colors.brandMuted
            error -> RhTheme.colors.statusFailed.copy(alpha = 0.16f)
            !option.enabled -> RhTheme.colors.surfaceDisabled
            else -> RhTheme.colors.surfaceSunken
        },
        shape = RoundedCornerShape(RhTheme.shapes.sm),
        border = BorderStroke(
            1.dp,
            when {
                selected -> RhTheme.colors.brandPrimary
                error -> RhTheme.colors.statusFailed
                else -> RhTheme.colors.borderSubtle
            },
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = option.label,
                color = when {
                    selected -> RhTheme.colors.brandPrimary
                    error -> RhTheme.colors.statusFailed
                    !option.enabled -> RhTheme.colors.textTertiary
                    else -> RhTheme.colors.textSecondary
                },
                style = RhTypography.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = RhSpacing.sm),
            )
        }
    }
}
