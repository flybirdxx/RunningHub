package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView

/**
 * 读取 Android 根 View 的物理像素高度，键盘弹出时该高度保持为完整窗口高度。
 *
 * 该值用于和 `WindowInsets.ime` 的像素 inset 保持同一坐标系，避免把 QuickCreate 页面局部高度误当成整屏高度。
 */
@Composable
internal actual fun rememberRootWindowHeightPx(): Int {
    val view = LocalView.current
    var heightPx by remember(view) { mutableIntStateOf(view.rootView.height) }

    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            heightPx = view.rootView.height
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        listener.onGlobalLayout()
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return heightPx
}
