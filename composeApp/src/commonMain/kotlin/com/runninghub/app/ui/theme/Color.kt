package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌与暗色基础色：仅保留 DarkColorScheme 仍在引用的原子色值。
val BrandLime = Color(0xFFA3B565)
val BaseBlack = Color(0xFF000000)
val Surface850 = Color(0xFF09090B)
val Surface800 = Color(0xFF18181B)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextMutedDark = Color(0xFF9DA2A8)
val StatusError = Color(0xFFFF4144)

// Secondary：Cyan 青蓝色阶，仅保留 DarkColorScheme 引用的档位。
val Secondary100 = Color(0xFFBFF3FF)
val Secondary200 = Color(0xFF80E7FF)

// Neutral：灰度色阶，仅保留 DarkColorScheme 引用的档位。
val Neutral200 = Color(0xFFE2E8F0)
val Neutral800 = Color(0xFF1E293B)

// 品牌暗色 UI 使用的应用壳表面色，作为 DarkColorScheme 的语义中转。
val RhAppBackground = BaseBlack
val RhAppSurface = Surface850
val RhAppCard = Surface800
val RhAppLine = Color(0xFF30363A)
val RhAppText = TextPrimaryDark
val RhAppMuted = TextMutedDark

// Dark ColorScheme tokens
val DarkPrimary = BrandLime
val DarkOnPrimary = Color.Black
val DarkPrimaryContainer = Color(0xFF30381F)
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
