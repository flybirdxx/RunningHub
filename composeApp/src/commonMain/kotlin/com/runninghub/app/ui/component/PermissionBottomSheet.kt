package com.runninghub.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runninghub.shared.domain.model.Permission

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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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
                contentDescription = permission.description,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(16.dp))

            // "Don't ask again" checkbox
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
                        text = "不再提示",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = onAuthorize,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("授权")
            }

            if (onOpenSettings != null) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("权限已被拒绝，前往设置开启")
                }
            } else {
                TextButton(
                    onClick = {
                        if (dontAskAgain) onDontAskAgain?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("暂不需要")
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
