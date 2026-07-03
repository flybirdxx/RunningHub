package com.runninghub.app.ui.designsystem.components.feedback

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 提示条语义级别。文案必须是 Presentation 层归一化后的安全文案。 */
enum class RhSnackbarSeverity(val tokenName: String) {
    Info("info"),
    Error("error"),
}

/**
 * 轻量提示条，由调用方控制显隐与消失时机。顶部横幅语义，非 Material Snackbar 的队列式瞬态提示。
 *
 * @param message 本地化后的提示文案，不得直接透出服务端原始 message。
 * @param severity 语义级别，决定配色。
 */
@Composable
fun RhSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    severity: RhSnackbarSeverity = RhSnackbarSeverity.Info,
) {
    val colors = RhTheme.colors
    val background = when (severity) {
        RhSnackbarSeverity.Info -> colors.surfaceElevated
        RhSnackbarSeverity.Error -> colors.statusFailed
    }
    val content = when (severity) {
        RhSnackbarSeverity.Info -> colors.textPrimary
        RhSnackbarSeverity.Error -> colors.textInverse
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        modifier = modifier,
    ) {
        Text(
            text = message,
            color = content,
            style = RhTypography.caption,
            modifier = Modifier.padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
        )
    }
}
