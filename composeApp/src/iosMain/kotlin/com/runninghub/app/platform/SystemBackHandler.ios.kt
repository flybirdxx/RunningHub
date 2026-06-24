package com.runninghub.app.platform

import androidx.compose.runtime.Composable

/**
 * iOS 当前没有 Android 等价的系统返回键分发。
 *
 * 页面仍通过显式按钮、拖拽或业务回调关闭临时层级；保留空实现可以让 commonMain 使用同一返回语义，
 * 不把 Android Activity API 泄漏进共享 UI。
 */
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
