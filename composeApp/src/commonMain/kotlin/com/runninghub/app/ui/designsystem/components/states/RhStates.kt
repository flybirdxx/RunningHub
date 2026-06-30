package com.runninghub.app.ui.designsystem.components.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 渲染空状态占位。
 *
 * @param title 调用方本地化后的标题，通常说明当前列表为空的业务原因。
 * @param modifier 外部布局修饰符。
 * @param description 可选说明，null 表示不展示说明段落。
 * @param actionLabel 可选操作文案，必须与 [onAction] 同时提供才会显示按钮。
 * @param onAction 可选恢复动作，null 表示当前空状态不可直接操作。
 */
@Composable
fun RhEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    RhMessageState(
        title = title,
        description = description,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

/**
 * 渲染加载状态占位。
 *
 * @param title 调用方本地化后的加载标题，用于说明正在等待的流程。
 * @param modifier 外部布局修饰符。
 * @param description 可选说明，null 表示不展示额外等待提示。
 */
@Composable
fun RhLoadingState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier.padding(RhSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
    ) {
        CircularProgressIndicator(color = RhTheme.colors.brandPrimary)
        Text(text = title, color = RhTheme.colors.textPrimary, style = RhTypography.bodyStrong)
        description?.let {
            Text(text = it, color = RhTheme.colors.textTertiary, style = RhTypography.caption)
        }
    }
}

/**
 * 渲染错误状态占位。
 *
 * @param title 调用方本地化后的错误标题，不能直接使用服务端原始 message。
 * @param modifier 外部布局修饰符。
 * @param description 可选说明，应来自 Presentation 层归一化后的安全文案。
 * @param actionLabel 可选重试或恢复动作文案，必须与 [onAction] 同时提供。
 * @param onAction 可选恢复动作，null 表示该错误当前不可直接恢复。
 */
@Composable
fun RhErrorState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    RhMessageState(
        title = title,
        description = description,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
        isError = true,
    )
}

@Composable
private fun RhMessageState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier.padding(RhSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        Text(
            text = title,
            color = if (isError) RhTheme.colors.statusFailed else RhTheme.colors.textPrimary,
            style = RhTypography.sectionTitle,
        )
        description?.let {
            Text(text = it, color = RhTheme.colors.textTertiary, style = RhTypography.body)
        }
        if (actionLabel != null && onAction != null) {
            RhButton(text = actionLabel, onClick = onAction, style = RhButtonStyle.Secondary)
        }
    }
}
