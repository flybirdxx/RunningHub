package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.Primary500
import com.runninghub.app.ui.theme.SuccessDark
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_list_placeholder
import runninghub.composeapp.generated.resources.app_detail_switch_off
import runninghub.composeapp.generated.resources.app_detail_switch_on

/**
 * App 详情页输入控件的叶子组件集合。
 *
 * 从 AppDetailScreen.kt 拆分而来，只承载暗色主题的表单控件（文本框、下拉、开关、分段选择器）
 * 和文本行数计算，均为无业务状态的纯 Compose 叶子。InputNodeField 仍在主屏幕文件中按控件类型分发。
 */

/**
 * App 详情页文本输入框的可见行数限制。
 *
 * @property minLines 输入框默认保留的最小可见行数，单位为文本行；单行输入固定为 1。
 * @property maxLines 输入框允许占用的最大可见行数，单位为文本行；超过后由输入框内部滚动承载。
 */
internal data class AppDetailTextFieldLineLimits(
    val minLines: Int,
    val maxLines: Int
)

/**
 * 计算详情页文本输入框的默认可见行数。
 *
 * 多行提示词只露出有限行数，避免长默认值撑满详情页；完整内容仍保留在可编辑输入框内部。
 *
 * @param multiline true 表示输入框承载长提示词并允许内部滚动；false 表示普通单行输入。
 * @return 输入框在 Compose 中使用的最小和最大可见行数。
 */
internal fun appDetailTextFieldLineLimits(multiline: Boolean): AppDetailTextFieldLineLimits =
    if (multiline) {
        AppDetailTextFieldLineLimits(
            minLines = APP_DETAIL_MULTILINE_TEXT_MIN_LINES,
            maxLines = APP_DETAIL_MULTILINE_TEXT_MAX_LINES
        )
    } else {
        AppDetailTextFieldLineLimits(
            minLines = APP_DETAIL_SINGLE_LINE_TEXT_LINES,
            maxLines = APP_DETAIL_SINGLE_LINE_TEXT_LINES
        )
    }

@Composable
internal fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = minLines,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = placeholder, color = Neutral400.copy(alpha = 0.5f), fontSize = 14.sp)
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary300,
            focusedBorderColor = Primary300,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private const val APP_DETAIL_SINGLE_LINE_TEXT_LINES = 1
private const val APP_DETAIL_MULTILINE_TEXT_MIN_LINES = 4
private const val APP_DETAIL_MULTILINE_TEXT_MAX_LINES = 6

@Composable
internal fun ListDropdown(
    options: List<String>,
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    stringResource(Res.string.app_detail_list_placeholder),
                    color = Neutral400.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                )
            },
            trailingIcon = {
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Neutral400,
                    fontSize = 12.sp,
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Primary300,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedTrailingIconColor = Neutral400,
                unfocusedTrailingIconColor = Neutral400
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DarkSurface
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == currentValue) Primary300 else Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onValueChanged(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun BooleanSwitch(
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    val checked = currentValue.equals("true", ignoreCase = true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = stringResource(
                if (checked) {
                    Res.string.app_detail_switch_on
                } else {
                    Res.string.app_detail_switch_off
                },
            ),
            color = if (checked) SuccessDark else Neutral400,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onValueChanged(it.toString()) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary500,
                uncheckedThumbColor = Neutral400,
                uncheckedTrackColor = DarkSurface,
                uncheckedBorderColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
internal fun SegmentedSelector(
    options: List<String>,
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEach { option ->
            val selected = option == currentValue
            val bgColor by animateColorAsState(
                targetValue = if (selected) Primary500 else Color.Transparent,
                animationSpec = tween(200)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onValueChanged(option) }
            ) {
                Text(
                    text = option,
                    color = if (selected) Color.White else Neutral400,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
