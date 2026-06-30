package com.runninghub.app.ui.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * RunningHub 设计系统的间距刻度。
 *
 * 所有值单位为 dp，供通用组件和页面布局复用，避免同一视觉层级在不同页面出现离散硬编码。
 */
object RhSpacing {
    /** 4dp，最小内边距或紧凑徽标垂直间距。 */
    val xs = 4.dp
    /** 8dp，小型元素间距，例如图标和文字之间的距离。 */
    val sm = 8.dp
    /** 12dp，中小型容器内距，用于紧凑列表和徽标组合。 */
    val md = 12.dp
    /** 16dp，默认页面和卡片内距。 */
    val lg = 16.dp
    /** 20dp，较宽松的组件间距，用于区块之间的视觉呼吸。 */
    val xl = 20.dp
    /** 24dp，大型容器内距或底部弹层边距。 */
    val xxl = 24.dp
    /** 32dp，页面主要区块之间的分隔间距。 */
    val xxxl = 32.dp
    /** 40dp，首屏或强视觉模块使用的大间距。 */
    val huge = 40.dp
}
