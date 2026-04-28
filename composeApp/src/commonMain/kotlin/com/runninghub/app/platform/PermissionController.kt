package com.runninghub.app.platform

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission

interface PermissionController {
    fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    )

    fun checkAndRequest(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    )

    fun openAppSettings()
}

@Composable
expect fun rememberPermissionController(
    dataStore: PermissionDataStore,
): PermissionController
