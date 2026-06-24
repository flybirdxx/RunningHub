package com.runninghub.app.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android 系统返回事件拦截实现。
 *
 * 该实现同时覆盖三键导航的返回键和系统返回手势；调用方通过 [enabled] 控制是否消费事件，
 * 避免页面内 sheet 打开时返回事件继续传给 Activity 或 Voyager 导航栈。
 */
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
