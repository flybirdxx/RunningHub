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
 * redesign 决策：App 锁定暗色，不跟随系统深浅色。浅色 Material scheme 与
 * RhLightColors 保留类型定义但不再进入运行路径，收尾批评估删除。
 */
@Composable
fun RunningHubTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides DarkExtendedColors,
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

/**
 * 旧版主题扩展色读取入口。
 *
 * 新增页面优先使用 `RhTheme`，既有页面在迁移完成前继续通过该对象读取历史 token。
 */
object RunningHubThemeExt {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
