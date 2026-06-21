package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.Dimens
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
        title = {
            Text(
                text = stringResource(Res.string.settings_dialog_api_key_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_dialog_api_key_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.SpaceLG))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(Res.string.settings_dialog_api_key_label)) },
                    placeholder = { Text(stringResource(Res.string.settings_dialog_api_key_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.SpaceSM))
                Text(
                    text = stringResource(Res.string.settings_dialog_api_key_help),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
            ) {
                Text(stringResource(Res.string.settings_dialog_api_key_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_dialog_cancel_action))
            }
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
        title = {
            Text(
                text = stringResource(Res.string.settings_dialog_cookie_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_dialog_cookie_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.SpaceLG))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(Res.string.settings_dialog_cookie_label)) },
                    placeholder = { Text(stringResource(Res.string.settings_dialog_cookie_placeholder)) },
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
                Spacer(Modifier.height(Dimens.SpaceSM))
                Text(
                    text = stringResource(Res.string.settings_dialog_cookie_help),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
            ) {
                Text(stringResource(Res.string.settings_dialog_cookie_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_dialog_cancel_action))
            }
        },
    )
}
