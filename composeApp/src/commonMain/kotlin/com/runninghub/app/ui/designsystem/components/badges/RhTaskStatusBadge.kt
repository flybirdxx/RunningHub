package com.runninghub.app.ui.designsystem.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 任务状态徽标的视觉状态。
 *
 * 该状态是 Presentation 层可复用的 UI 语义，不直接暴露服务端原始状态字符串。
 */
sealed interface RhTaskStatus {
    /** 稳定 token 名称，用于测试和设计映射，不作为用户可见文案。 */
    val tokenName: String

    /** 任务已进入队列但尚未开始执行。 */
    data object Queued : RhTaskStatus {
        override val tokenName: String = "queued"
    }

    /** 任务正在执行或轮询中。 */
    data object Running : RhTaskStatus {
        override val tokenName: String = "running"
    }

    /** 任务已成功完成。 */
    data object Success : RhTaskStatus {
        override val tokenName: String = "success"
    }

    /** 任务失败，调用方应展示经过归一化的错误提示。 */
    data object Failed : RhTaskStatus {
        override val tokenName: String = "failed"
    }

    /** 任务已取消或被用户主动终止。 */
    data object Canceled : RhTaskStatus {
        override val tokenName: String = "canceled"
    }
}

/**
 * 渲染任务状态徽标。
 *
 * @param status 任务视觉状态，决定颜色和背景。
 * @param label 调用方本地化后的展示文案，组件不解析远端状态字符串。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun RhTaskStatusBadge(
    status: RhTaskStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    val color = when (status) {
        RhTaskStatus.Queued -> colors.statusWarning
        RhTaskStatus.Running -> colors.statusProcessing
        RhTaskStatus.Success -> colors.statusSuccess
        RhTaskStatus.Failed -> colors.statusFailed
        RhTaskStatus.Canceled -> colors.textTertiary
    }

    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(RhTheme.shapes.sm))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
        color = color.takeUnless { status == RhTaskStatus.Canceled } ?: Color(0xFF8B929D),
        style = RhTypography.statusBadge,
    )
}
