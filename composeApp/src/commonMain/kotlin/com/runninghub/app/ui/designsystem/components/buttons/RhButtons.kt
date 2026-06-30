package com.runninghub.app.ui.designsystem.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * RunningHub 通用按钮的视觉层级。
 *
 * @property tokenName 稳定的设计 token 名称，用于测试、审查和后续设计映射，不作为用户可见文案。
 */
enum class RhButtonStyle(val tokenName: String) {
    /** 主操作按钮，适用于提交、生成和确认等最高优先级动作。 */
    Primary("primary"),
    /** 次操作按钮，适用于取消、查看详情或非破坏性辅助动作。 */
    Secondary("secondary"),
    /** 轻量文字按钮，适用于工具栏或低优先级动作。 */
    Ghost("ghost"),
}

/**
 * 渲染符合 RunningHub redesign token 的通用按钮。
 *
 * @param text 调用方已经本地化后的按钮文案，组件不生成最终展示文案。
 * @param onClick 点击回调；当 [enabled] 为 false 或 [loading] 为 true 时不会触发。
 * @param modifier 外部布局修饰符，组件内部只固定最小高度和视觉样式。
 * @param enabled 为 true 时允许点击；为 false 时展示禁用视觉并阻止交互。
 * @param loading 为 true 时展示加载指示并临时阻止重复点击。
 * @param style 按钮视觉层级，决定容器、边框和内容颜色。
 */
@Composable
fun RhButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: RhButtonStyle = RhButtonStyle.Primary,
) {
    val colors = RhTheme.colors
    val shape = RoundedCornerShape(RhTheme.shapes.md)
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (style == RhButtonStyle.Primary) colors.textInverse else colors.brandPrimary,
                )
            }
            Text(text = text, style = RhTypography.button)
        }
    }
    val buttonModifier = modifier.height(48.dp)

    when (style) {
        RhButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandPrimary,
                contentColor = colors.textInverse,
                disabledContainerColor = colors.surfaceDisabled,
                disabledContentColor = colors.textTertiary,
            ),
            content = { content() },
        )
        RhButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            border = BorderStroke(1.dp, colors.borderDefault),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.textPrimary,
                disabledContentColor = colors.textTertiary,
            ),
            content = { content() },
        )
        RhButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.textSecondary,
                disabledContentColor = colors.textTertiary,
                containerColor = Color.Transparent,
            ),
            content = { content() },
        )
    }
}

/**
 * 主操作按钮的便捷封装。
 *
 * 参数语义与 [RhButton] 保持一致，用于减少页面反复传入 [RhButtonStyle.Primary]。
 */
@Composable
fun RhPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    RhButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = RhButtonStyle.Primary,
    )
}
