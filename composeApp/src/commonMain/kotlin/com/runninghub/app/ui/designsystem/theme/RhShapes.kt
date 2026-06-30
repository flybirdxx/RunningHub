package com.runninghub.app.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RunningHub 设计系统的圆角刻度。
 *
 * @property xs 4dp，小型分隔或极紧凑控件圆角。
 * @property sm 8dp，徽标、小按钮和轻量容器圆角。
 * @property md 12dp，默认按钮和普通卡片圆角。
 * @property lg 16dp，强调卡片和大面积交互容器圆角。
 * @property xl 20dp，大型视觉容器圆角。
 * @property sheet 24dp，底部弹层顶部圆角。
 * @property full 胶囊形控件的近似全圆角，调用方应只用于高度稳定的控件。
 */
@Immutable
data class RhShapes(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val sheet: Dp,
    val full: Dp,
)

/**
 * 当前 redesign 默认圆角表，单位均为 dp。
 */
val RhDefaultShapes = RhShapes(
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    lg = 16.dp,
    xl = 20.dp,
    sheet = 24.dp,
    full = 999.dp,
)
