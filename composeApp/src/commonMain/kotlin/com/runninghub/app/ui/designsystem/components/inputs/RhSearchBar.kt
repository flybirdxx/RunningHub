package com.runninghub.app.ui.designsystem.components.inputs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** RhSearchBar 的固定尺寸契约。 */
object RhSearchBarDefaults {
    val minHeight = 44.dp
}

/**
 * 设计系统搜索输入框。下沉表面色承载输入区，聚焦时边框切换为激活色。
 *
 * 基于 BasicTextField 与 OutlinedTextFieldDefaults.DecorationBox 实现，
 * 通过紧凑内边距保证默认渲染高度落在 44dp 契约上。
 *
 * @param query 当前搜索关键词，空字符串表示尚未输入。
 * @param onQueryChange 用户编辑关键词时触发，调用方负责保存状态。
 * @param placeholder 占位文案，调用方负责本地化。
 * @param modifier 外层布局修饰符，默认填满父容器宽度。
 * @param onSearch 用户通过键盘搜索动作提交时触发，参数为当前关键词；可能收到空关键词，由调用方兜底。
 * @param searchIconContentDescription 搜索图标无障碍描述，调用方负责本地化。
 * @param clearContentDescription 清空按钮无障碍描述，调用方负责本地化；为 null 时清空按钮无无障碍标签。
 * 清空按钮固定 28dp，小于 48dp 的触摸目标是输入框内清空按钮的业界惯例，用于避免撑破 44dp 高度契约。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    searchIconContentDescription: String? = null,
    clearContentDescription: String? = null,
) {
    val colors = RhTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceSunken,
        unfocusedContainerColor = colors.surfaceSunken,
        focusedBorderColor = colors.borderActive,
        unfocusedBorderColor = colors.borderDefault,
        // 光标色由 BasicTextField 的 cursorBrush 承担，文字色由 textStyle 承担，此处不再配置。
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = RhSearchBarDefaults.minHeight),
        textStyle = RhTypography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brandPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        singleLine = true,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(text = placeholder, style = RhTypography.body, color = colors.textTertiary)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = searchIconContentDescription,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = clearContentDescription,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    null
                },
                colors = fieldColors,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 10.dp, bottom = 10.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = fieldColors,
                        shape = RoundedCornerShape(RhTheme.shapes.md),
                    )
                },
            )
        },
    )
}
