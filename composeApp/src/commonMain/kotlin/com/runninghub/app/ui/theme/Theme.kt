package com.runninghub.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    primaryContainer = AppColors.PrimaryContainer,
    secondary = AppColors.Secondary,
    secondaryContainer = AppColors.SecondaryContainer,
    background = AppColors.Background,
    surface = AppColors.Surface,
    surfaceVariant = AppColors.SurfaceVariant,
    onBackground = AppColors.OnBackground,
    onSurface = AppColors.OnSurface,
    onSurfaceVariant = AppColors.OnSurfaceVariant,
    outline = AppColors.Outline,
    error = AppColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.DarkPrimary,
    background = AppColors.DarkBackground,
    surface = AppColors.DarkSurface,
    onBackground = AppColors.DarkOnBackground,
    onSurface = AppColors.DarkOnSurface,
)

@Composable
fun RunningHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
