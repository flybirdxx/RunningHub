package com.runninghub.app.ui.designsystem.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 复用参数在使用同款确认面板里的状态。 */
enum class ReuseTemplateParameterStatus {
    Available,
    Missing,
}

/**
 * 使用同款确认面板的单项参数。
 *
 * @property label 调用方已本地化的参数名。
 * @property value 参数值；为空且 [status] 为 [ReuseTemplateParameterStatus.Missing] 时表示需要补齐。
 * @property status 参数是否可带入 Create。
 */
data class ReuseTemplateParameterState(
    val label: String,
    val value: String?,
    val status: ReuseTemplateParameterStatus,
)

/**
 * 使用同款确认面板状态。
 *
 * @property confirmBypassesPriceConfirmation 固定为 `false`，表示确认只进入 Create，不能绕过生成价格确认。
 */
data class ReuseTemplateSheetState(
    val title: String,
    val sourceTitle: String,
    val sourceAuthor: String?,
    val sourceProtectionText: String,
    val parameters: List<ReuseTemplateParameterState>,
    val confirmActionLabel: String,
    val dismissActionLabel: String,
    val confirmEnabled: Boolean = true,
    val confirmBypassesPriceConfirmation: Boolean = false,
)

@Composable
fun ReuseTemplateSheet(
    state: ReuseTemplateSheetState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RhSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
    ) {
        Text(
            text = state.title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.sectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.sourceTitle,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.sourceAuthor?.takeIf { it.isNotBlank() }?.let { author ->
            Text(
                text = author,
                color = RhTheme.colors.textSecondary,
                style = RhTypography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = state.sourceProtectionText,
            color = RhTheme.colors.textTertiary,
            style = RhTypography.caption,
        )
        Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.xs)) {
            state.parameters.forEach { parameter ->
                ReuseTemplateParameterRow(parameter)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
            RhButton(
                text = state.dismissActionLabel,
                onClick = onDismiss,
                style = RhButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
            RhButton(
                text = state.confirmActionLabel,
                onClick = onConfirm,
                enabled = state.confirmEnabled && !state.confirmBypassesPriceConfirmation,
                style = RhButtonStyle.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReuseTemplateParameterRow(parameter: ReuseTemplateParameterState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        Text(
            text = parameter.label,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.caption,
            modifier = Modifier.weight(0.42f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = parameter.value.orEmpty(),
            color = when (parameter.status) {
                ReuseTemplateParameterStatus.Available -> RhTheme.colors.textPrimary
                ReuseTemplateParameterStatus.Missing -> RhTheme.colors.textTertiary
            },
            style = RhTypography.caption,
            modifier = Modifier.weight(0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
