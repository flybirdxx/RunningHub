package com.runninghub.app.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class RhWidthClass {
    CompactSmall,
    Compact,
    Medium,
    Expanded,
    Large,
}

enum class RhHeightClass {
    Compact,
    Regular,
}

@Immutable
data class RhWindowInfo(
    val widthClass: RhWidthClass,
    val heightClass: RhHeightClass,
    val windowWidth: Dp,
    val windowHeight: Dp,
) {
    val isCompactWidth: Boolean
        get() = widthClass == RhWidthClass.CompactSmall || widthClass == RhWidthClass.Compact

    val shouldUseNavigationRail: Boolean
        get() = (widthClass == RhWidthClass.Medium && heightClass == RhHeightClass.Regular) ||
            widthClass == RhWidthClass.Expanded ||
            widthClass == RhWidthClass.Large

    val feedGridMinCardWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.CompactSmall -> 220.dp
            RhWidthClass.Compact -> 160.dp
            RhWidthClass.Medium -> 180.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 220.dp
        }

    val feedContentMaxWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.Medium -> 720.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 1040.dp
            else -> Dp.Infinity
        }

    val formContentMaxWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.Medium -> 560.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 640.dp
            else -> Dp.Infinity
        }

    val detailContentMaxWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.Medium -> 720.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 960.dp
            else -> Dp.Infinity
        }

    val bottomSheetMaxWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.Medium -> 640.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 720.dp
            else -> Dp.Infinity
        }

    val quickActionMinWidth: Dp
        get() = when (widthClass) {
            RhWidthClass.CompactSmall -> 84.dp
            RhWidthClass.Compact -> 96.dp
            RhWidthClass.Medium -> 108.dp
            RhWidthClass.Expanded,
            RhWidthClass.Large,
            -> 120.dp
        }

    val compactCardStatsLimit: Int
        get() = if (widthClass == RhWidthClass.CompactSmall) 1 else 2

    val bottomPanelMaxHeightFraction: Float
        get() = if (heightClass == RhHeightClass.Compact) 0.60f else 0.45f
}

val LocalRhWindowInfo = staticCompositionLocalOf {
    RhWindowInfo(
        widthClass = RhWidthClass.Compact,
        heightClass = RhHeightClass.Regular,
        windowWidth = 360.dp,
        windowHeight = 640.dp,
    )
}

@Composable
fun ProvideRhWindowInfo(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowInfo = remember(maxWidth, maxHeight) {
            RhWindowInfo(
                widthClass = when {
                    maxWidth < 360.dp -> RhWidthClass.CompactSmall
                    maxWidth < 600.dp -> RhWidthClass.Compact
                    maxWidth < 840.dp -> RhWidthClass.Medium
                    maxWidth < 1200.dp -> RhWidthClass.Expanded
                    else -> RhWidthClass.Large
                },
                heightClass = if (maxHeight < 480.dp) RhHeightClass.Compact else RhHeightClass.Regular,
                windowWidth = maxWidth,
                windowHeight = maxHeight,
            )
        }

        CompositionLocalProvider(LocalRhWindowInfo provides windowInfo) {
            content()
        }
    }
}
