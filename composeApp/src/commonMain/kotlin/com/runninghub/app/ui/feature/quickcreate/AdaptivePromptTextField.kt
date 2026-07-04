package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS

private const val MIN_HEIGHT_DP = 52
private const val LINE_HEIGHT_SP = 22
private const val MAX_LINES = 6

/**
 * QuickCreate 提示词输入框。
 *
 * 该组件只负责渲染提示词输入、占位文案和运行时字数状态；提示词长度上限来自
 * QuickCreate Presentation 状态契约，实际文案资源由调用方传入。右下角字数是根据
 * [charCount] 派生的动态数字，不应登记为硬编码 UI 文案。
 *
 * @param prompt 用户当前输入的提示词，空字符串表示尚未输入。
 * @param onPromptChange 提示词变化回调；当输入长度未超过 [MAX_PROMPT_CHARS] 时触发。
 * @param placeholder 输入为空时展示的占位文案，应由调用方从 Compose Resources 传入。
 * @param charCount 当前提示词字符数，单位为个；由上层状态计算并用于运行时计数展示。
 * @param nearLimit `true` 表示字数接近上限，计数展示使用警告色；`false` 表示未接近上限。
 * @param overLimit `true` 表示字数超过业务限制，边框和计数展示使用错误色；`false` 表示未超限。
 * @param modifier 外部布局修饰符，只影响当前输入框容器。
 */
@Composable
fun AdaptivePromptTextField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    placeholder: String,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = RhTheme.colors.textPrimary,
        fontSize = 14.sp,
        lineHeight = LINE_HEIGHT_SP.sp,
        fontWeight = FontWeight.Normal,
    )

    val minHeight = MIN_HEIGHT_DP.dp
    val maxHeight = (MIN_HEIGHT_DP + (LINE_HEIGHT_SP * (MAX_LINES - 1))).dp

    val borderColor = when {
        overLimit -> RhTheme.colors.statusFailed
        else -> RhTheme.colors.borderDefault
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(RhTheme.shapes.md),
            color = RhTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight, max = maxHeight)
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
            ) {
                if (prompt.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = RhTheme.colors.textTertiary),
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { newValue ->
                        if (newValue.length <= MAX_PROMPT_CHARS) {
                            onPromptChange(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minHeight),
                    textStyle = textStyle,
                    cursorBrush = SolidColor(RhTheme.colors.brandPrimary),
                )
            }
        }

        val countColor = when {
            overLimit -> RhTheme.colors.statusFailed
            nearLimit -> RhTheme.colors.statusWarning
            else -> RhTheme.colors.textTertiary
        }
        Text(
            text = charCount.toString(),
            color = countColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = RhSpacing.md, bottom = RhSpacing.xs),
        )
    }
}
