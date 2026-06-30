package com.runninghub.app.ui.designsystem.components.billing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadge
import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadgeState

/**
 * RM-05 价格徽标的稳定视觉状态名称。
 *
 * 该枚举用于设计契约测试和后续设计稿映射；实际渲染仍复用 [RhPriceBadgeState]。
 */
enum class PriceBadgeVisualState(val tokenName: String) {
    Loading("loading"),
    Pending("pending"),
    Free("free"),
    Amount("amount"),
    Insufficient("insufficient"),
}

/**
 * 渲染计费价格徽标。
 *
 * @param state 价格视觉状态，由页面把业务计费状态映射后传入。
 * @param label 已本地化的价格文案，组件不拼接业务文案。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun PriceBadge(
    state: RhPriceBadgeState,
    label: String,
    modifier: Modifier = Modifier,
) {
    RhPriceBadge(
        state = state,
        label = label,
        modifier = modifier,
    )
}
