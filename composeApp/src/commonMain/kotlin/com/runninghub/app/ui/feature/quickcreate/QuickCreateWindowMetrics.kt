package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable

/**
 * 返回当前宿主根窗口的像素高度，用于把 IME 底部 inset 转换成键盘顶部的窗口坐标。
 *
 * 返回值单位为物理像素；当平台无法提供该值时返回 `0`，调用方需要退回到 Compose root 高度。
 */
@Composable
internal expect fun rememberRootWindowHeightPx(): Int
