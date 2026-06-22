package com.runninghub.app.ui.component

import com.runninghub.app.ui.theme.Dimens
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_search_bar_clear_content_description
import runninghub.composeapp.generated.resources.app_search_bar_placeholder
import runninghub.composeapp.generated.resources.app_search_bar_search_content_description

/**
 * 应用内通用搜索输入框。
 *
 * @param query 当前搜索关键词，空字符串表示尚未输入。
 * @param onQueryChange 用户编辑关键词时触发，调用方负责保存状态和触发联想或筛选。
 * @param modifier 外层布局修饰符，默认填满父容器宽度并保持最小按钮高度。
 * @param placeholder 可选占位文案；`null` 时使用应用资源中的默认搜索占位文案。
 * @param onSearch 用户通过键盘搜索动作提交时触发，参数为当前关键词。
 */
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSearch: (String) -> Unit = {},
) {
    val placeholderText = placeholder ?: stringResource(Res.string.app_search_bar_placeholder)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.ButtonHeightLG),
        placeholder = {
            Text(
                text = placeholderText,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(Res.string.app_search_bar_search_content_description),
                modifier = Modifier.size(24.dp),
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.app_search_bar_clear_content_description),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
