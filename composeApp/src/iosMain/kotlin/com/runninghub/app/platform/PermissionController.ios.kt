package com.runninghub.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission

@Composable
actual fun rememberPermissionController(
    dataStore: PermissionDataStore,
): PermissionController = remember(dataStore) {
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
