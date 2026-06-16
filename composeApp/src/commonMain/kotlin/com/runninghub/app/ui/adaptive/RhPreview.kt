package com.runninghub.app.ui.adaptive

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.RunningHubTheme

@Composable
fun RunningHubPreviewSurface(
    modifier: Modifier = Modifier,
    windowWidth: Dp = 360.dp,
    windowHeight: Dp = 800.dp,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    RunningHubTheme(darkTheme = true) {
        val density = LocalDensity.current
        val windowInfo = remember(windowWidth, windowHeight) {
            RhWindowInfo(
                widthClass = when {
                    windowWidth < 360.dp -> RhWidthClass.CompactSmall
                    windowWidth < 600.dp -> RhWidthClass.Compact
                    windowWidth < 840.dp -> RhWidthClass.Medium
                    windowWidth < 1200.dp -> RhWidthClass.Expanded
                    else -> RhWidthClass.Large
                },
                heightClass = if (windowHeight < 480.dp) RhHeightClass.Compact else RhHeightClass.Regular,
                windowWidth = windowWidth,
                windowHeight = windowHeight,
            )
        }
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density, fontScale = fontScale),
            LocalRhWindowInfo provides windowInfo,
        ) {
            Surface(
                modifier = modifier.requiredSize(windowWidth, windowHeight),
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}
