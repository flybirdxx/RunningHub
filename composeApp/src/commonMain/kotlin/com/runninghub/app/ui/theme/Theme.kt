package com.runninghub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.runninghub.app.ui.designsystem.theme.LocalRhColors
import com.runninghub.app.ui.designsystem.theme.LocalRhShapes
import com.runninghub.app.ui.designsystem.theme.RhDarkColors
import com.runninghub.app.ui.designsystem.theme.RhDefaultShapes

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkSurfaceTint,
    scrim = DarkScrim
)

/**
 * 应用壳主题入口。
 *
 * redesign 决策：App 锁定暗色，不跟随系统深浅色。主题装配统一提供 Rh 设计系统色板与圆角，
 * 并把 [DarkColorScheme] 交给 Material3 组件消费。
 */
@Composable
fun RunningHubTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRhColors provides RhDarkColors,
        LocalRhShapes provides RhDefaultShapes,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
