package com.runninghub.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val hotBadge: Color,
    val onHotBadge: Color,
    val newBadge: Color,
    val onNewBadge: Color,
    val cardBorder: Color,
    val linkText: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val premiumGold: Color,
    val premiumOrange: Color
)

val LightExtendedColors = ExtendedColors(
    gradientStart = Primary500,
    gradientEnd = Color(0xFF00B8E0),
    shimmerBase = Neutral200,
    shimmerHighlight = Neutral50,
    hotBadge = WarningLight,
    onHotBadge = Color.White,
    newBadge = Primary500,
    onNewBadge = Color.White,
    cardBorder = Color(0x140F172A),
    linkText = InfoLight,
    success = SuccessLight,
    onSuccess = Color.White,
    warning = WarningLight,
    onWarning = Color.White,
    info = InfoLight,
    onInfo = Color.White,
    premiumGold = Color(0xFFD6A94A),
    premiumOrange = Color(0xFFFF8A3D)
)

val DarkExtendedColors = ExtendedColors(
    gradientStart = Primary300,
    gradientEnd = Secondary500,
    shimmerBase = Color(0xFF141929),
    shimmerHighlight = Color(0xFF1E2438),
    hotBadge = WarningDark,
    onHotBadge = Color(0xFF1A0800),
    newBadge = Primary300,
    onNewBadge = Color(0xFF1A0045),
    cardBorder = Color(0x0FFFFFFF),
    linkText = Secondary200,
    success = SuccessDark,
    onSuccess = Color(0xFF003314),
    warning = WarningDark,
    onWarning = Color(0xFF3D2800),
    info = InfoDark,
    onInfo = Color(0xFF001A40),
    premiumGold = Color(0xFFFFD166),
    premiumOrange = Color(0xFFFF9F43)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
