/**
 * [INPUT]: 依赖 DarkColorScheme, Typography
 * [OUTPUT]: 对外提供 RunningHubTheme，整合 Compose 主题
 * [POS]: UI 主题配置中心，确保全应用视觉一致性
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RunningHubTeal,
    secondary = DarkGray,
    tertiary = DarkContainer,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun RunningHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // 假设稍后定义
        content = content
    )
}
