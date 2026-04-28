package com.runninghub.app.platform

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import kotlinx.coroutines.launch

private class PermissionControllerImpl(
    private val dataStore: PermissionDataStore,
    private val activity: ComponentActivity,
) : PermissionController {

    private val scope = activity.lifecycleScope

    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null
    private var pendingMediaCallback: ((String) -> Unit)? = null
    private var pendingMediaDeniedCallback: (() -> Unit)? = null
    private var pendingMediaType: MediaType? = null

    private val permissionLauncher = activity.activityResultRegistry.register(
        "rh_permission_request",
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val manifest = permissions.keys.firstOrNull() ?: return@register
        val granted = permissions[manifest] == true
        scope.launch {
            if (granted) {
                dataStore.markGranted(manifest)
                pendingMediaType?.let { type ->
                    launchMediaPicker(type)
                }
            } else {
                val shouldShowRationale = activity.shouldShowRequestPermissionRationale(manifest)
                if (!shouldShowRationale) {
                    dataStore.markPermanentlyDenied(manifest)
                    pendingMediaDeniedCallback?.invoke()
                } else {
                    dataStore.markDenied(manifest)
                    pendingMediaDeniedCallback?.invoke()
                }
            }
            pendingPermissionCallback?.invoke(granted)
            pendingPermissionCallback = null
            pendingMediaCallback = null
            pendingMediaDeniedCallback = null
            pendingMediaType = null
        }
    }

    private val mediaImageLauncher = activity.activityResultRegistry.register(
        "rh_media_image",
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleMediaResult(uri)
    }

    private val mediaVideoLauncher = activity.activityResultRegistry.register(
        "rh_media_video",
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleMediaResult(uri)
    }

    private val mediaAudioLauncher = activity.activityResultRegistry.register(
        "rh_media_audio",
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleMediaResult(uri)
    }

    private fun handleMediaResult(uri: Uri?) {
        try {
            uri?.let {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                pendingMediaCallback?.invoke(it.toString())
            }
        } catch (t: Throwable) {
            // MIUI PhotoPicker may deliver malformed Intent with null extras bundle.
            // Catch any unexpected exception so pending state is always cleaned up.
        } finally {
            pendingMediaCallback = null
            pendingMediaType = null
        }
    }

    private fun launchMediaPicker(type: MediaType) {
        val mimeType = when (type) {
            MediaType.IMAGE -> "image/*"
            MediaType.VIDEO -> "video/*"
            MediaType.AUDIO -> "audio/*"
        }
        val launcher = when (type) {
            MediaType.IMAGE -> mediaImageLauncher
            MediaType.VIDEO -> mediaVideoLauncher
            MediaType.AUDIO -> mediaAudioLauncher
        }
        launcher.launch(mimeType)
    }

    override fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        // Photo Picker (PickVisualMedia) 在独立进程中提供安全访问, 无需运行时权限
        // 见 https://developer.android.com/training/data-storage/shared/photopicker
        when (mediaType) {
            MediaType.IMAGE, MediaType.VIDEO -> {
                pendingMediaCallback = onSuccess
                pendingMediaDeniedCallback = onPermissionDenied
                pendingMediaType = mediaType
                launchMediaPicker(mediaType)
            }
            MediaType.AUDIO -> {
                scope.launch {
                    when (dataStore.getCurrentStatus(mediaPermission)) {
                        PermissionStatus.GRANTED -> {
                            pendingMediaCallback = onSuccess
                            pendingMediaType = mediaType
                            launchMediaPicker(mediaType)
                        }
                        PermissionStatus.PERMANENTLY_DENIED -> onPermissionDenied()
                        else -> {
                            pendingMediaCallback = onSuccess
                            pendingMediaDeniedCallback = onPermissionDenied
                            pendingMediaType = mediaType
                            permissionLauncher.launch(arrayOf(mediaPermission.androidManifest))
                        }
                    }
                }
            }
        }
    }

    override fun checkAndRequest(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        scope.launch {
            when (dataStore.getCurrentStatus(permission)) {
                PermissionStatus.GRANTED -> onGranted()
                PermissionStatus.PERMANENTLY_DENIED -> onPermanentlyDenied()
                else -> {
                    pendingPermissionCallback = { granted ->
                        if (granted) onGranted() else onDenied()
                    }
                    permissionLauncher.launch(arrayOf(permission.androidManifest))
                }
            }
        }
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}

@Composable
actual fun rememberPermissionController(
    dataStore: PermissionDataStore,
): PermissionController {
    val activity = LocalContext.current as ComponentActivity
    return remember(dataStore, activity) {
        PermissionControllerImpl(dataStore, activity)
    }
}
