package com.runninghub.app.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.nareshchocha.filepickerlibrary.FilePickerResultContracts
import com.nareshchocha.filepickerlibrary.models.DocumentFilePickerConfig
import com.nareshchocha.filepickerlibrary.models.FilePickerResult
import com.runninghub.app.ui.component.MediaType
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStatus
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.androidManifestPermission
import com.runninghub.core.storage.fromAndroidManifestPermission
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
    private var pendingMediaCancelledCallback: (() -> Unit)? = null
    private var pendingMediaPermanentlyDeniedCallback: (() -> Unit)? = null
    private var pendingMediaType: MediaType? = null
    private var pendingRequestedPermission: Permission? = null

    private val permissionLauncher = activity.activityResultRegistry.register(
        "rh_permission_request",
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val manifest = permissions.keys.firstOrNull() ?: run {
            pendingPermissionCallback = null
            pendingRequestedPermission = null
            return@register
        }
        val requestedPermission = pendingRequestedPermission ?: Permission.fromAndroidManifestPermission(manifest)
        val permissionKey = requestedPermission?.key
        val granted = permissions[manifest] == true
        scope.launch {
            if (granted) {
                permissionKey?.let { permissionStateStore.markGranted(it) }
                pendingMediaType?.let { type -> launchMediaPicker(type) }
            } else {
                val shouldShowRationale = activity.shouldShowRequestPermissionRationale(manifest)
                if (!shouldShowRationale) {
                    permissionKey?.let { permissionStateStore.markPermanentlyDenied(it) }
                    pendingMediaPermanentlyDeniedCallback?.invoke()
                } else {
                    permissionKey?.let { permissionStateStore.markDenied(it) }
                    pendingMediaDeniedCallback?.invoke()
                }
                pendingMediaCallback = null
                pendingMediaDeniedCallback = null
                pendingMediaCancelledCallback = null
                pendingMediaPermanentlyDeniedCallback = null
                pendingMediaType = null
            }
            pendingPermissionCallback?.invoke(granted)
            pendingPermissionCallback = null
            pendingRequestedPermission = null
        }
    }

    private val visualMediaDocumentLauncher = activity.activityResultRegistry.register(
        "rh_visual_media_document_picker",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleVisualMediaResult(
            resultCode = result.resultCode,
            intent = result.data,
        )
    }

    private val mediaImageLauncher = activity.activityResultRegistry.register(
        "rh_media_image_picker",
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handleVisualMediaUri(uri)
    }

    private val mediaVideoLauncher = activity.activityResultRegistry.register(
        "rh_media_video_picker",
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handleVisualMediaUri(uri)
    }

    private val mediaAudioLauncher = activity.activityResultRegistry.register(
        "rh_media_audio_fp",
        FilePickerResultContracts.PickDocumentFile()
    ) { result: FilePickerResult ->
        handleMediaResult(result)
    }

    private fun handleVisualMediaResult(
        resultCode: Int,
        intent: Intent?,
    ) {
        if (resultCode != Activity.RESULT_OK) {
            pendingMediaCancelledCallback?.invoke()
            pendingMediaCallback = null
            pendingMediaDeniedCallback = null
            pendingMediaCancelledCallback = null
            pendingMediaPermanentlyDeniedCallback = null
            pendingMediaType = null
            return
        }
        handleVisualMediaUri(intent.firstVisualMediaUri())
    }

    private fun handleVisualMediaUri(uri: Uri?) {
        try {
            if (uri != null) {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (error: SecurityException) {
                    ignorePersistableGrantFailure(error)
                }
                pendingMediaCallback?.invoke(uri.toString())
            } else {
                pendingMediaCancelledCallback?.invoke()
            }
        } catch (error: Exception) {
            handleMediaPickerFailure(error)
            pendingMediaDeniedCallback?.invoke()
        } finally {
            pendingMediaCallback = null
            pendingMediaDeniedCallback = null
            pendingMediaCancelledCallback = null
            pendingMediaPermanentlyDeniedCallback = null
            pendingMediaType = null
        }
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
                } catch (error: SecurityException) {
                    ignorePersistableGrantFailure(error)
                }
                pendingMediaCallback?.invoke(uri.toString())
                return
            }
            // Fallback to file path
            val path = result.selectedFilePath
            if (!path.isNullOrBlank()) {
                pendingMediaCallback?.invoke(path)
            } else {
                pendingMediaCancelledCallback?.invoke()
            }
        } catch (error: Exception) {
            handleMediaPickerFailure(error)
            pendingMediaDeniedCallback?.invoke()
        } finally {
            pendingMediaCallback = null
            pendingMediaDeniedCallback = null
            pendingMediaCancelledCallback = null
            pendingMediaPermanentlyDeniedCallback = null
            pendingMediaType = null
        }
    }

    override fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
        onPickerCancelled: () -> Unit,
        onPermissionPermanentlyDenied: () -> Unit,
    ) {
        pendingMediaCallback = onSuccess
        pendingMediaDeniedCallback = onPermissionDenied
        pendingMediaCancelledCallback = onPickerCancelled
        pendingMediaPermanentlyDeniedCallback = onPermissionPermanentlyDenied
        pendingMediaType = mediaType

        when (mediaType) {
            MediaType.IMAGE -> {
                launchVisualMediaPicker(mediaType)
            }
            MediaType.VIDEO -> {
                launchVisualMediaPicker(mediaType)
            }
            MediaType.AUDIO -> {
                scope.launch {
                    when (permissionStateStore.getCurrentStatus(mediaPermission)) {
                        PermissionStatus.GRANTED -> {
                            launchAudioPicker()
                        }
                        PermissionStatus.PERMANENTLY_DENIED -> {
                            onPermissionPermanentlyDenied()
                            pendingMediaCallback = null
                            pendingMediaDeniedCallback = null
                            pendingMediaCancelledCallback = null
                            pendingMediaPermanentlyDeniedCallback = null
                            pendingMediaType = null
                        }
                        else -> {
                            pendingRequestedPermission = mediaPermission
                            permissionLauncher.launch(arrayOf(mediaPermission.androidManifestPermission))
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

    private fun launchVisualMediaPicker(mediaType: MediaType) {
        try {
            when (mediaType) {
                MediaType.IMAGE -> mediaImageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                MediaType.VIDEO -> mediaVideoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
                MediaType.AUDIO -> Unit
            }
        } catch (error: RuntimeException) {
            handleMediaPickerFailure(error)
            launchVisualMediaDocumentPicker(mediaType.visualMimeType())
        }
    }

    private fun launchVisualMediaDocumentPicker(mimeType: String) {
        visualMediaDocumentLauncher.launch(createVisualMediaDocumentIntent(mimeType))
    }

    private fun launchMediaPicker(type: MediaType) {
        when (type) {
            MediaType.AUDIO -> launchAudioPicker()
            else -> {} // IMAGE/VIDEO launch directly via pickMedia, not via permission flow
        }
    }

    private fun ignorePersistableGrantFailure(error: SecurityException) {
        // 部分系统相册不会授予可持久化 URI 权限，但当前选择回调仍带有临时读权限。
        // 保留无日志降级，避免把本地媒体 URI 或系统异常细节写入生产日志。
        error.message
    }

    private fun handleMediaPickerFailure(error: Exception) {
        // 文件选择器可能返回无法解析的结果对象。此时不能静默吞掉异常，
        // 调用方会通过 denied 回调恢复按钮和错误状态；这里不记录异常，避免泄露本地 URI 或文件路径。
        error.message
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
                    pendingRequestedPermission = permission
                    permissionLauncher.launch(arrayOf(permission.androidManifestPermission))
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
    // ActivityResultRegistry 只能从真实 ComponentActivity 获取；Compose 可能提供被主题包装过的
    // Context，因此需要逐层展开 ContextWrapper，而不是直接把 LocalContext 强转为 Activity。
    val activity = LocalContext.current.findComponentActivity()
        ?: error("PermissionController requires a ComponentActivity host.")
    return remember(permissionStateStore, activity) {
        PermissionControllerImpl(permissionStateStore, activity)
    }
}

/**
 * 从 Compose 提供的 Android Context 中查找宿主 ComponentActivity。
 *
 * Android 页面通常会经过 ContextThemeWrapper 包装；递归展开可以保留 Preview 和测试环境的
 * 明确失败路径，同时避免对 LocalContext 执行不安全强转。
 */
private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

private fun createVisualMediaDocumentIntent(mimeType: String): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }

private fun Intent?.firstVisualMediaUri(): Uri? =
    this?.data ?: this?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

private fun MediaType.visualMimeType(): String = when (this) {
    MediaType.IMAGE -> "image/*"
    MediaType.VIDEO -> "video/*"
    MediaType.AUDIO -> "*/*"
}
