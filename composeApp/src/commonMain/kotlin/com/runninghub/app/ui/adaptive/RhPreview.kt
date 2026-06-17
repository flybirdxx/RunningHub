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

internal enum class RhPreviewSpec(
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float = 1f,
) {
    Phone320(widthDp = 320, heightDp = 568),
    Phone360(widthDp = 360, heightDp = 640),
    Phone430(widthDp = 430, heightDp = 932),
    Medium600(widthDp = 600, heightDp = 840),
    Expanded840(widthDp = 840, heightDp = 1180),
    Landscape800(widthDp = 800, heightDp = 360),
    FontScale13(widthDp = 360, heightDp = 800, fontScale = 1.3f),
    FontScale15(widthDp = 360, heightDp = 800, fontScale = 1.5f),
}

@Composable
internal fun RhAdaptivePreview(
    spec: RhPreviewSpec,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    RunningHubPreviewSurface(
        modifier = modifier,
        windowWidth = spec.widthDp.dp,
        windowHeight = spec.heightDp.dp,
        fontScale = spec.fontScale,
        content = content,
    )
}

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
