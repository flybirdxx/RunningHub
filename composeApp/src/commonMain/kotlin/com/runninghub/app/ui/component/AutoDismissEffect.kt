package com.runninghub.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay

/**
 * 页面瞬态反馈的自动关闭 effect。
 *
 * 只处理 UI 横幅、提示条这类展示生命周期；业务轮询、重试和状态机仍应留在对应
 * Feature Presentation 层。
 */
@Composable
internal fun AutoDismissEffect(
    key: Any?,
    visible: Boolean,
    durationMillis: Long,
    onDismiss: () -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(key, visible, durationMillis) {
        if (visible && durationMillis >= 0L) {
            delay(durationMillis)
            currentOnDismiss()
        }
    }
}
