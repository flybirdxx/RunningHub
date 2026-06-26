package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable

/**
 * iOS 当前没有使用 Android IME inset 计算路径，返回 `0` 让 common 侧退回 Compose root 高度。
 */
@Composable
internal actual fun rememberRootWindowHeightPx(): Int = 0
