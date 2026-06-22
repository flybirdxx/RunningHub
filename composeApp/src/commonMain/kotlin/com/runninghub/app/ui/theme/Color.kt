package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color

// RunningHub web brand palette captured from runninghub.cn on 2026-06-19.
val BrandLime = Color(0xFFB6FF00)
val BaseBlack = Color(0xFF000000)
val Surface900 = Color(0xFF080808)
val Surface850 = Color(0xFF09090B)
val Surface800 = Color(0xFF18181B)
val Surface700 = Color(0xFF27272A)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextDefaultDark = Color(0xFFEFEFEF)
val TextSecondaryDark = Color(0xFFD8D8D8)
val TextMutedDark = Color(0xFF9DA2A8)
val StatusError = Color(0xFFFF4144)
val ControlTeal = Color(0xFF02DBA3)
val ControlTealActive = Color(0xFF01A47A)

// Shared app chrome colors used by the branded dark UI.
val RhAppBackground = BaseBlack
val RhAppSurface = Surface850
val RhAppCard = Surface800
val RhAppSelected = Color(0xFF202515)
val RhAppLine = Color(0xFF30363A)
val RhAppText = TextPrimaryDark
val RhAppMuted = TextMutedDark
val RhAppBottomBar = Color(0xF209090B)

// Primary：Electric Violet 蓝紫色阶。
val Primary50 = Color(0xFFF3F1FF)
val Primary100 = Color(0xFFE0DBFF)
val Primary200 = Color(0xFFC4B5FD)
val Primary300 = Color(0xFFA78BFA)
val Primary400 = Color(0xFF8B6CF7)
val Primary500 = Color(0xFF6C5CE7)
val Primary600 = Color(0xFF5B4BD4)
val Primary700 = Color(0xFF4C3EC0)
val Primary800 = Color(0xFF3D31A8)
val Primary900 = Color(0xFF2E2490)

// Secondary：Cyan 青蓝色阶。
val Secondary50 = Color(0xFFE8FBFF)
val Secondary100 = Color(0xFFBFF3FF)
val Secondary200 = Color(0xFF80E7FF)
val Secondary300 = Color(0xFF40DBFF)
val Secondary400 = Color(0xFF1AD4FF)
val Secondary500 = Color(0xFF00D2FF)
val Secondary600 = Color(0xFF00B8E0)
val Secondary700 = Color(0xFF009EC0)
val Secondary800 = Color(0xFF0084A0)
val Secondary900 = Color(0xFF006A80)

// Neutral：灰度色阶。
val Neutral50 = Color(0xFFF8FAFC)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral300 = Color(0xFFCBD5E1)
val Neutral400 = Color(0xFF94A3B8)
val Neutral500 = Color(0xFF64748B)
val Neutral600 = Color(0xFF475569)
val Neutral700 = Color(0xFF334155)
val Neutral800 = Color(0xFF1E293B)
val Neutral900 = Color(0xFF0F172A)

// Semantic colors
val SuccessLight = Color(0xFF16A34A)
val SuccessDark = Color(0xFF4ADE80)
val WarningLight = Color(0xFFD97706)
val WarningDark = Color(0xFFFBBF24)
val ErrorLight = Color(0xFFDC2626)
val ErrorDark = Color(0xFFF87171)
val InfoLight = Color(0xFF2563EB)
val InfoDark = Color(0xFF60A5FA)

// Light ColorScheme tokens
val LightPrimary = Color(0xFF4E6200)
val LightOnPrimary = Color.Black
val LightPrimaryContainer = BrandLime
val LightOnPrimaryContainer = Color.Black

val LightSecondary = Color(0xFF0084A0)
val LightOnSecondary = Color.White
val LightSecondaryContainer = Secondary100
val LightOnSecondaryContainer = Color(0xFF001F2A)

val LightTertiary = Color(0xFF9C4230)
val LightOnTertiary = Color.White
val LightTertiaryContainer = Color(0xFFFFDBD1)
val LightOnTertiaryContainer = Color(0xFF3A0B00)

val LightError = ErrorLight
val LightOnError = Color.White
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Neutral50
val LightOnBackground = Neutral900
val LightSurface = Color.White
val LightOnSurface = Neutral900
val LightSurfaceVariant = Neutral100
val LightOnSurfaceVariant = Neutral600
val LightOutline = Neutral400
val LightOutlineVariant = Neutral200
val LightInverseSurface = Neutral800
val LightInverseOnSurface = Neutral100
val LightInversePrimary = BrandLime
val LightSurfaceTint = LightPrimary
val LightScrim = Color.Black

// Dark ColorScheme tokens
val DarkPrimary = BrandLime
val DarkOnPrimary = Color.Black
val DarkPrimaryContainer = Color(0xFF334000)
val DarkOnPrimaryContainer = BrandLime

val DarkSecondary = Secondary200
val DarkOnSecondary = Color(0xFF003544)
val DarkSecondaryContainer = Color(0xFF004D63)
val DarkOnSecondaryContainer = Secondary100

val DarkTertiary = Color(0xFFFFB4A8)
val DarkOnTertiary = Color(0xFF5C1900)
val DarkTertiaryContainer = Color(0xFF7A2E15)
val DarkOnTertiaryContainer = Color(0xFFFFDBD1)

val DarkError = StatusError
val DarkOnError = Color(0xFF410002)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = RhAppBackground
val DarkOnBackground = RhAppText
val DarkSurface = RhAppSurface
val DarkOnSurface = RhAppText
val DarkSurfaceVariant = RhAppCard
val DarkOnSurfaceVariant = RhAppMuted
val DarkOutline = RhAppLine
val DarkOutlineVariant = Color(0x14FFFFFF)
val DarkInverseSurface = Neutral200
val DarkInverseOnSurface = Neutral800
val DarkInversePrimary = BrandLime
val DarkSurfaceTint = BrandLime
val DarkScrim = Color.Black
