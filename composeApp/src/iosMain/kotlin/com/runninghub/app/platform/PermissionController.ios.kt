package com.runninghub.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.permission.PermissionStateStore

/**
 * 创建 iOS 平台权限控制器。
 *
 * 当前 iOS 侧依赖系统弹窗和 Info.plist 权限文案，权限状态存储仅作为跨平台接口占位。
 * 后续接入 PHPicker 或文件选择器时，应在这里补充真实授权结果写回。
 */
@Composable
actual fun rememberPermissionController(
    permissionStateStore: PermissionStateStore,
): PermissionController = remember(permissionStateStore) {
    object : PermissionController {
        override fun pickMedia(
            mediaPermission: Permission,
            mediaType: MediaType,
            onSuccess: (String) -> Unit,
            onPermissionDenied: () -> Unit,
        ) {
            // iOS: photo library access is granted at app install time
            // Implement PHPickerViewController here if needed
        }

        override fun checkAndRequest(
            permission: Permission,
            onGranted: () -> Unit,
            onDenied: () -> Unit,
            onPermanentlyDenied: () -> Unit,
        ) {
            // iOS: permissions are handled by Info.plist strings and system dialogs
            onGranted()
        }

        override fun openAppSettings() {
            // iOS: open app settings via URL scheme
        }
    }
}
