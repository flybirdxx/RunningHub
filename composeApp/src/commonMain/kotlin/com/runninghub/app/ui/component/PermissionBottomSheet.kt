package com.runninghub.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionTextKey
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.permission_bottom_sheet_authorize
import runninghub.composeapp.generated.resources.permission_bottom_sheet_dont_ask_again
import runninghub.composeapp.generated.resources.permission_bottom_sheet_not_now
import runninghub.composeapp.generated.resources.permission_bottom_sheet_open_settings
import runninghub.composeapp.generated.resources.permission_camera_description
import runninghub.composeapp.generated.resources.permission_camera_required_for
import runninghub.composeapp.generated.resources.permission_media_audio_description
import runninghub.composeapp.generated.resources.permission_media_audio_required_for
import runninghub.composeapp.generated.resources.permission_media_images_description
import runninghub.composeapp.generated.resources.permission_media_images_required_for
import runninghub.composeapp.generated.resources.permission_media_video_description
import runninghub.composeapp.generated.resources.permission_media_video_required_for
import runninghub.composeapp.generated.resources.permission_notifications_description
import runninghub.composeapp.generated.resources.permission_notifications_required_for
import runninghub.composeapp.generated.resources.permission_storage_read_description
import runninghub.composeapp.generated.resources.permission_storage_read_required_for

/**
 * 展示跨平台权限请求的底部引导弹窗。
 *
 * 组件只承担权限说明、授权入口、跳转设置入口和“不再提示”选择的 UI 展示；
 * 实际系统权限请求、永久拒绝记录和设置页跳转由调用方通过回调完成。
 * [Permission.descriptionKey] 是业务层为具体权限提供的稳定文案 key，本组件负责把它映射到
 * Compose Resources；弹窗自身的按钮、复选框和设置引导文案同样来自 Compose Resources。
 *
 * @param permission 当前需要解释和申请的业务权限，包含展示图标 key 与说明文案 key。
 * @param onDismiss 用户关闭弹窗或选择暂不需要时触发，调用方应同步清理当前权限请求状态。
 * @param onAuthorize 用户点击授权按钮时触发，调用方负责调用平台权限 API 并处理授权结果。
 * @param onOpenSettings 当权限已被系统永久拒绝时使用；非空表示展示设置入口，由调用方打开系统设置。
 * @param onDontAskAgain 当调用方需要记录“不再提示”选择时使用；非空时弹窗展示复选框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    permission: Permission,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onDontAskAgain: (() -> Unit)? = null,
) {
    var dontAskAgain by remember { mutableStateOf(false) }
    val permissionDescription = permission.descriptionKey.asPermissionText()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RhTheme.colors.surfaceSunken,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = getIconForPermission(permission.icon),
                contentDescription = permissionDescription,
                modifier = Modifier.size(56.dp),
                tint = RhTheme.colors.brandPrimary,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = permissionDescription,
                style = RhTypography.body,
                textAlign = TextAlign.Center,
                color = RhTheme.colors.textPrimary,
            )

            Spacer(Modifier.height(16.dp))

            // “不再提示”只记录用户对当前业务引导的偏好，不等同于系统级永久拒绝。
            if (onDontAskAgain != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dontAskAgain = !dontAskAgain }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it },
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.permission_bottom_sheet_dont_ask_again),
                        style = RhTypography.body,
                        color = RhTheme.colors.textSecondary,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = onAuthorize,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.permission_bottom_sheet_authorize))
            }

            if (onOpenSettings != null) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.permission_bottom_sheet_open_settings))
                }
            } else {
                TextButton(
                    onClick = {
                        if (dontAskAgain) onDontAskAgain?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.permission_bottom_sheet_not_now))
                }
            }
        }
    }
}

private fun getIconForPermission(icon: String): ImageVector = when (icon) {
    "camera_alt" -> Icons.Default.CameraAlt
    "image" -> Icons.Default.Image
    "videocam" -> Icons.Default.Videocam
    "music_note" -> Icons.Default.MusicNote
    "notifications" -> Icons.Default.Notifications
    "folder" -> Icons.Default.Folder
    else -> Icons.Default.Lock
}

@Composable
private fun PermissionTextKey.asPermissionText(): String =
    when (this) {
        PermissionTextKey.MediaImagesDescription -> stringResource(Res.string.permission_media_images_description)
        PermissionTextKey.MediaImagesRequiredFor -> stringResource(Res.string.permission_media_images_required_for)
        PermissionTextKey.MediaVideoDescription -> stringResource(Res.string.permission_media_video_description)
        PermissionTextKey.MediaVideoRequiredFor -> stringResource(Res.string.permission_media_video_required_for)
        PermissionTextKey.MediaAudioDescription -> stringResource(Res.string.permission_media_audio_description)
        PermissionTextKey.MediaAudioRequiredFor -> stringResource(Res.string.permission_media_audio_required_for)
        PermissionTextKey.StorageReadDescription -> stringResource(Res.string.permission_storage_read_description)
        PermissionTextKey.StorageReadRequiredFor -> stringResource(Res.string.permission_storage_read_required_for)
        PermissionTextKey.CameraDescription -> stringResource(Res.string.permission_camera_description)
        PermissionTextKey.CameraRequiredFor -> stringResource(Res.string.permission_camera_required_for)
        PermissionTextKey.NotificationsDescription -> stringResource(Res.string.permission_notifications_description)
        PermissionTextKey.NotificationsRequiredFor -> stringResource(Res.string.permission_notifications_required_for)
    }
