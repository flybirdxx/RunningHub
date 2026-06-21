package com.runninghub.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.error_state_retry_action

/**
 * 渲染通用错误占位。
 *
 * 错误正文由调用方传入，便于保持领域错误到展示文案的上游映射；重试按钮文案通过
 * Compose Resources 获取，避免通用错误组件保留硬编码中文。
 *
 * @param message 等待展示给用户的错误正文，通常来自 Presentation 层映射后的安全文案。
 * @param modifier 外层布局修饰符，用于控制页面占位大小、边距或测试标记。
 * @param onRetry 用户点击重试按钮时触发；为 `null` 时不展示重试按钮。
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.SpaceXXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            Spacer(Modifier.height(Dimens.SpaceLG))
            FilledTonalButton(onClick = onRetry) {
                Text(stringResource(Res.string.error_state_retry_action))
            }
        }
    }
}
