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
import com.nareshchocha.filepickerlibrary.FilePickerResultContracts
import com.nareshchocha.filepickerlibrary.models.DocumentFilePickerConfig
import com.nareshchocha.filepickerlibrary.models.FilePickerResult
import com.nareshchocha.filepickerlibrary.models.PickMediaConfig
import com.nareshchocha.filepickerlibrary.models.PickMediaType
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import com.runninghub.shared.domain.permission.PermissionStateStore
import kotlinx.coroutines.launch

/**
 * Android 平台的权限申请与媒体选择控制器。
 *
 * 本实现持有 ActivityResult launcher，并通过 [PermissionStateStore] 记录权限申请轨迹。
 * 状态写入跟随 Activity 生命周期作用域，避免页面销毁后继续更新 UI 回调。
 */
private class PermissionControllerImpl(
    private val permissionStateStore: PermissionStateStore,
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
                permissionStateStore.markGranted(manifest)
                pendingMediaType?.let { type -> launchMediaPicker(type) }
            } else {
                val shouldShowRationale = activity.shouldShowRequestPermissionRationale(manifest)
                if (!shouldShowRationale) {
                    permissionStateStore.markPermanentlyDenied(manifest)
                    pendingMediaDeniedCallback?.invoke()
                } else {
                    permissionStateStore.markDenied(manifest)
                    pendingMediaDeniedCallback?.invoke()
                }
                pendingMediaCallback = null
                pendingMediaDeniedCallback = null
                pendingMediaType = null
            }
            pendingPermissionCallback?.invoke(granted)
            pendingPermissionCallback = null
        }
    }

    private val mediaImageLauncher = activity.activityResultRegistry.register(
        "rh_media_image_fp",
        FilePickerResultContracts.PickMedia()
    ) { result: FilePickerResult ->
        handleMediaResult(result)
    }

    private val mediaVideoLauncher = activity.activityResultRegistry.register(
        "rh_media_video_fp",
        FilePickerResultContracts.PickMedia()
    ) { result: FilePickerResult ->
        handleMediaResult(result)
    }

    private val mediaAudioLauncher = activity.activityResultRegistry.register(
        "rh_media_audio_fp",
        FilePickerResultContracts.PickDocumentFile()
    ) { result: FilePickerResult ->
        handleMediaResult(result)
    }

    private fun handleMediaResult(result: FilePickerResult) {
        try {
            // Prefer content URI (most reliable across Android versions)
            val uri = result.selectedFileUri
            if (uri != null) {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                pendingMediaCallback?.invoke(uri.toString())
                return
            }
            // Fallback to file path
            val path = result.selectedFilePath
            if (!path.isNullOrBlank()) {
                pendingMediaCallback?.invoke(path)
            }
        } catch (_: Exception) {
        } finally {
            pendingMediaCallback = null
            pendingMediaDeniedCallback = null
            pendingMediaType = null
        }
    }

    override fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        pendingMediaCallback = onSuccess
        pendingMediaDeniedCallback = onPermissionDenied
        pendingMediaType = mediaType

        when (mediaType) {
            MediaType.IMAGE -> {
                mediaImageLauncher.launch(
                    PickMediaConfig(mPickMediaType = PickMediaType.ImageOnly)
                )
            }
            MediaType.VIDEO -> {
                mediaVideoLauncher.launch(
                    PickMediaConfig(mPickMediaType = PickMediaType.VideoOnly)
                )
            }
            MediaType.AUDIO -> {
                scope.launch {
                    when (permissionStateStore.getCurrentStatus(mediaPermission)) {
                        PermissionStatus.GRANTED -> {
                            launchAudioPicker()
                        }
                        PermissionStatus.PERMANENTLY_DENIED -> {
                            onPermissionDenied()
                            pendingMediaCallback = null
                            pendingMediaDeniedCallback = null
                            pendingMediaType = null
                        }
                        else -> {
                            permissionLauncher.launch(arrayOf(mediaPermission.androidManifest))
                        }
                    }
                }
            }
        }
    }

    private fun launchAudioPicker() {
        mediaAudioLauncher.launch(
            DocumentFilePickerConfig(
                mMimeTypes = listOf("audio/*"),
            )
        )
    }

    private fun launchMediaPicker(type: MediaType) {
        when (type) {
            MediaType.AUDIO -> launchAudioPicker()
            else -> {} // IMAGE/VIDEO launch directly via pickMedia, not via permission flow
        }
    }

    override fun checkAndRequest(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        scope.launch {
            when (permissionStateStore.getCurrentStatus(permission)) {
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

/**
 * 创建 Android 平台权限控制器。
 *
 * @param permissionStateStore 权限状态存储边界，用于记录系统回调后的授权结果。
 */
@Composable
actual fun rememberPermissionController(
    permissionStateStore: PermissionStateStore,
): PermissionController {
    val activity = LocalContext.current as ComponentActivity
    return remember(permissionStateStore, activity) {
        PermissionControllerImpl(permissionStateStore, activity)
    }
}
