package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.settings_dialog_api_key_confirm
import runninghub.composeapp.generated.resources.settings_dialog_api_key_description
import runninghub.composeapp.generated.resources.settings_dialog_api_key_help
import runninghub.composeapp.generated.resources.settings_dialog_api_key_label
import runninghub.composeapp.generated.resources.settings_dialog_api_key_placeholder
import runninghub.composeapp.generated.resources.settings_dialog_api_key_title
import runninghub.composeapp.generated.resources.settings_dialog_cancel_action
import runninghub.composeapp.generated.resources.settings_dialog_cookie_confirm
import runninghub.composeapp.generated.resources.settings_dialog_cookie_description
import runninghub.composeapp.generated.resources.settings_dialog_cookie_help
import runninghub.composeapp.generated.resources.settings_dialog_cookie_label
import runninghub.composeapp.generated.resources.settings_dialog_cookie_placeholder
import runninghub.composeapp.generated.resources.settings_dialog_cookie_title

/**
 * 渲染 Rh 风格的凭据输入框。
 *
 * 与 `RhSearchBar` 保持一致，基于 `BasicTextField` + `OutlinedTextFieldDefaults.DecorationBox` 组装，
 * 颜色和圆角全部取自 [RhTheme]，避免弹窗内输入区回退到 Material 默认视觉。
 *
 * @param value 当前输入的原始字符串；敏感凭据只在当前组合生命周期内驻留，不做持久化或回显。
 * @param onValueChange 用户编辑输入时触发的回调。
 * @param label 输入区标签文案，调用方负责本地化。
 * @param placeholder 输入区占位文案，调用方负责本地化。
 * @param modifier 外部布局修饰符。
 * @param singleLine `true` 时限制为单行输入；`false` 允许多行粘贴，例如 Cookie 原文。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RhCredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val colors = RhTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceSunken,
        unfocusedContainerColor = colors.surfaceSunken,
        focusedBorderColor = colors.borderActive,
        unfocusedBorderColor = colors.borderDefault,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = RhTypography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brandPrimary),
        singleLine = singleLine,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                label = {
                    Text(text = label, style = RhTypography.caption, color = colors.textTertiary)
                },
                placeholder = {
                    Text(text = placeholder, style = RhTypography.body, color = colors.textTertiary)
                },
                colors = fieldColors,
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

/**
 * 渲染 API Key 绑定弹窗。
 *
 * 弹窗只负责采集用户本次输入并在确认时通过 [onConfirm] 回传；
 * API Key 属于敏感凭据，不在本地日志、错误信息或资源文案中持久化。
 *
 * @param modifier 外部容器传入的布局修饰符。
 * @param onDismiss 用户取消、点击遮罩或系统返回时触发的关闭回调。
 * @param onConfirm 用户确认绑定时触发的回调，参数为当前输入的原始 API Key 字符串。
 */
@Composable
fun ApiKeyDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.lg),
        title = {
            Text(
                text = stringResource(Res.string.settings_dialog_api_key_title),
                style = RhTypography.sectionTitle,
                color = RhTheme.colors.textPrimary,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_dialog_api_key_description),
                    style = RhTypography.body,
                    color = RhTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(RhSpacing.lg))
                RhCredentialField(
                    value = input,
                    onValueChange = { input = it },
                    label = stringResource(Res.string.settings_dialog_api_key_label),
                    placeholder = stringResource(Res.string.settings_dialog_api_key_placeholder),
                    singleLine = true,
                )
                Spacer(Modifier.height(RhSpacing.sm))
                Text(
                    text = stringResource(Res.string.settings_dialog_api_key_help),
                    style = RhTypography.caption,
                    color = RhTheme.colors.brandPrimary,
                )
            }
        },
        confirmButton = {
            RhPrimaryButton(
                text = stringResource(Res.string.settings_dialog_api_key_confirm),
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
            )
        },
        dismissButton = {
            RhDialogCancelButton(onClick = onDismiss)
        },
    )
}

/**
 * 渲染 Cookie 登录弹窗。
 *
 * 弹窗只保存当前组合生命周期内的 Cookie 输入，并在确认时通过 [onConfirm] 回传；
 * Cookie 可能包含访问令牌，调用方必须按凭据处理，不得打印或写入普通日志。
 *
 * @param modifier 外部容器传入的布局修饰符。
 * @param onDismiss 用户取消、点击遮罩或系统返回时触发的关闭回调。
 * @param onConfirm 用户确认登录时触发的回调，参数为当前输入的浏览器 Cookie 原文。
 */
@Composable
fun CookieDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.lg),
        title = {
            Text(
                text = stringResource(Res.string.settings_dialog_cookie_title),
                style = RhTypography.sectionTitle,
                color = RhTheme.colors.textPrimary,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_dialog_cookie_description),
                    style = RhTypography.body,
                    color = RhTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(RhSpacing.lg))
                RhCredentialField(
                    value = input,
                    onValueChange = { input = it },
                    label = stringResource(Res.string.settings_dialog_cookie_label),
                    placeholder = stringResource(Res.string.settings_dialog_cookie_placeholder),
                    singleLine = false,
                    modifier = Modifier.heightIn(min = 120.dp),
                )
                Spacer(Modifier.height(RhSpacing.sm))
                Text(
                    text = stringResource(Res.string.settings_dialog_cookie_help),
                    style = RhTypography.caption,
                    color = RhTheme.colors.brandPrimary,
                )
            }
        },
        confirmButton = {
            RhPrimaryButton(
                text = stringResource(Res.string.settings_dialog_cookie_confirm),
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
            )
        },
        dismissButton = {
            RhDialogCancelButton(onClick = onDismiss)
        },
    )
}

/**
 * 渲染弹窗取消按钮。
 *
 * 使用 token 化的 [TextButton]，文案色取自 [RhTheme] 次级文本色，保持与主操作按钮的视觉层级区分。
 *
 * @param onClick 用户点击取消时触发的关闭回调。
 */
@Composable
private fun RhDialogCancelButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = RhTheme.colors.textSecondary,
            containerColor = Color.Transparent,
        ),
    ) {
        Text(
            text = stringResource(Res.string.settings_dialog_cancel_action),
            style = RhTypography.button,
        )
    }
}
