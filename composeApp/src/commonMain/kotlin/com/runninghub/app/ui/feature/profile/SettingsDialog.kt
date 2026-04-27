package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.Dimens

@Composable
fun ApiKeyDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "绑定 API Key",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入您的 RunningHub API Key 以访问个人中心和运行 AI 应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.SpaceLG))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("API Key") },
                    placeholder = { Text("输入 API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Dimens.SpaceSM))
                Text(
                    text = "获取方式：登录 RunningHub → 个人中心 → API Key",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank()
            ) {
                Text("确认绑定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CookieDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cookie 登录",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入浏览器登录后的 Cookie 信息，包含 Rh-AccessToken 或 userid 字段。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.SpaceLG))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Cookie") },
                    placeholder = { Text("粘贴浏览器 Cookie") },
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                Spacer(Modifier.height(Dimens.SpaceSM))
                Text(
                    text = "获取方式：浏览器 F12 → Application → Cookies",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank()
            ) {
                Text("确认登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
