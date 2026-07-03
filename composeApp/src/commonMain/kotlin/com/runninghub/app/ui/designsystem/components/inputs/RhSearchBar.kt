package com.runninghub.app.ui.designsystem.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhTheme

/** RhSearchBar 的固定尺寸契约。 */
object RhSearchBarDefaults {
    val minHeight = 44.dp
}

/**
 * 设计系统搜索输入框。下沉表面色承载输入区，聚焦时边框切换为激活色。
 *
 * @param query 当前搜索关键词，空字符串表示尚未输入。
 * @param onQueryChange 用户编辑关键词时触发，调用方负责保存状态。
 * @param placeholder 占位文案，调用方负责本地化。
 * @param modifier 外层布局修饰符，默认填满父容器宽度。
 * @param onSearch 用户通过键盘搜索动作提交时触发，参数为当前关键词。
 * @param searchIconContentDescription 搜索图标无障碍描述，调用方负责本地化。
 * @param clearContentDescription 清空按钮无障碍描述，调用方负责本地化。
 */
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = RhSearchBarDefaults.minHeight),
        placeholder = { Text(text = placeholder, color = colors.textTertiary) },
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
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = clearContentDescription,
                        tint = colors.textTertiary,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            focusedBorderColor = colors.borderActive,
            unfocusedBorderColor = colors.borderDefault,
            cursorColor = colors.brandPrimary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
