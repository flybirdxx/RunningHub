package com.runninghub.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhWidthClass

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded;

    val isWide: Boolean
        get() = this >= Medium
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalRhWindowInfo.current
    return when (windowInfo.widthClass) {
        RhWidthClass.CompactSmall,
        RhWidthClass.Compact,
        -> WindowSizeClass.Compact
        RhWidthClass.Medium -> WindowSizeClass.Medium
        RhWidthClass.Expanded,
        RhWidthClass.Large,
        -> WindowSizeClass.Expanded
    }
}

fun adaptiveGridSpacing(sizeClass: WindowSizeClass): Dp = when (sizeClass) {
    WindowSizeClass.Compact -> 12.dp
    WindowSizeClass.Medium -> 16.dp
    WindowSizeClass.Expanded -> 20.dp
}
