package com.runninghub.app.ui.designsystem.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 价格徽标的视觉状态。
 *
 * 该状态只描述价格在 UI 中的语义层级，不保存远端原始错误或最终展示文案。
 */
sealed interface RhPriceBadgeState {
    /** 稳定 token 名称，用于测试和设计映射，不作为用户可见文案。 */
    val tokenName: String

    /** 价格仍在计算或请求中，调用方应配套展示本地化等待文案。 */
    data object Loading : RhPriceBadgeState {
        override val tokenName: String = "loading"
    }

    /** 价格存在但尚未被确认，通常用于提交前的预览阶段。 */
    data object Pending : RhPriceBadgeState {
        override val tokenName: String = "pending"
    }

    /** 当前项目不消耗 credit 或现金。 */
    data object Free : RhPriceBadgeState {
        override val tokenName: String = "free"
    }

    /**
     * 已获得可展示价格。
     *
     * @property label 调用方格式化后的价格文本，必须来自资源或格式化层，组件只负责视觉承载。
     */
    data class Amount(val label: String) : RhPriceBadgeState {
        override val tokenName: String = "amount"
    }

    /** 余额不足或价格不可支付，调用方负责提供可恢复的本地化提示。 */
    data object Insufficient : RhPriceBadgeState {
        override val tokenName: String = "insufficient"
    }
}

/**
 * 渲染价格徽标。
 *
 * @param state 价格视觉状态，决定颜色和背景。
 * @param label 调用方本地化后的展示文案，避免通用组件直接持有页面文案。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun RhPriceBadge(
    state: RhPriceBadgeState,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    val (textColor, backgroundColor) = when (state) {
        RhPriceBadgeState.Loading -> colors.statusProcessing to colors.surfaceSunken
        RhPriceBadgeState.Pending -> colors.statusWarning to colors.surfaceSunken
        RhPriceBadgeState.Free -> colors.statusSuccess to colors.surfaceSelected
        is RhPriceBadgeState.Amount -> colors.priceCredit to colors.surfaceSelected
        RhPriceBadgeState.Insufficient -> colors.statusWarning to colors.surfaceSunken
    }

    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(RhTheme.shapes.sm))
            .background(backgroundColor)
            .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
        color = textColor,
        style = RhTypography.price,
    )
}
