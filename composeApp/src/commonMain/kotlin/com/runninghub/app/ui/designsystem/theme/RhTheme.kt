package com.runninghub.app.ui.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前 Compose 树中的 RunningHub 语义色板。
 */
val LocalRhColors = staticCompositionLocalOf { RhDarkColors }

/**
 * 当前 Compose 树中的 RunningHub 圆角刻度。
 */
val LocalRhShapes = staticCompositionLocalOf { RhDefaultShapes }

/**
 * RunningHub 设计系统 token 的统一读取入口。
 *
 * 页面和通用组件通过该对象读取语义 token，避免直接依赖具体色值、圆角或文字样式。
 */
object RhTheme {
    /** 当前 CompositionLocal 中的语义色板。 */
    val colors: RhColors
        @Composable get() = LocalRhColors.current

    /** 当前 CompositionLocal 中的圆角刻度。 */
    val shapes: RhShapes
        @Composable get() = LocalRhShapes.current

    /** 当前静态文字层级表，所有字距保持 0.sp。 */
    val typography: RhTypography
        get() = RhTypography

    /** 当前静态间距刻度表，页面布局统一从这里取值。 */
    val spacing: RhSpacing
        get() = RhSpacing
}
