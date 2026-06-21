package com.runninghub.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.runninghub.app.ui.component.MediaType
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val IOS_IMAGE_UTI = "public.image"
private const val IOS_MOVIE_UTI = "public.movie"
private const val IOS_AUDIO_UTI = "public.audio"

/**
 * iOS 平台权限申请与媒体选择控制器。
 *
 * 本实现位于 composeApp/iosMain，只在平台层接触 UIKit、PhotoKit 和 Document Picker。
 * commonMain 页面只接收平台无关的 URI 字符串，避免 UIViewController、NSURL 等 iOS 类型进入
 * UiState 或领域模型。
 *
 * 生命周期约束：
 * - picker delegate 保存在控制器字段上，直到成功、取消或失败后再清空，避免系统弹窗仍存在时
 *   Kotlin/Native 对象被释放。
 * - 权限状态通过 [permissionStateStore] 写回跨平台 key；未知状态不会被伪造成已授权。
 */
@OptIn(ExperimentalForeignApi::class)
private class IosPermissionController(
    private val permissionStateStore: PermissionStateStore,
    private val scope: CoroutineScope,
) : PermissionController {

    private var imagePickerDelegate: ImagePickerDelegate? = null
    private var documentPickerDelegate: DocumentPickerDelegate? = null

    override fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        when (mediaType) {
            MediaType.IMAGE,
            MediaType.VIDEO -> checkPhotoPermission(
                permission = mediaPermission,
                onGranted = { presentImagePicker(mediaType, onSuccess, onPermissionDenied) },
                onDenied = onPermissionDenied,
                onPermanentlyDenied = onPermissionDenied,
            )
            MediaType.AUDIO -> {
                // iOS 文档选择器会把用户选中的音频导入应用可读范围，不需要读取整个媒体库权限。
                scope.launch { permissionStateStore.markGranted(mediaPermission.key) }
                presentAudioPicker(onSuccess, onPermissionDenied)
            }
        }
    }

    override fun checkAndRequest(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        when (permission) {
            Permission.MediaImages,
            Permission.MediaVideo,
            Permission.StorageRead -> checkPhotoPermission(
                permission = permission,
                onGranted = onGranted,
                onDenied = onDenied,
                onPermanentlyDenied = onPermanentlyDenied,
            )
            Permission.MediaAudio -> {
                // UIDocumentPicker 是用户主动授权单个文件的流程，不应因为没有媒体库权限而阻塞音频上传。
                scope.launch { permissionStateStore.markGranted(permission.key) }
                onGranted()
            }
            Permission.Camera,
            Permission.Notifications -> {
                // 当前 iOS 上传链路未接入相机和通知授权；返回拒绝比固定授权更安全，
                // 避免页面继续执行实际不可用的平台能力。
                scope.launch { permissionStateStore.markDenied(permission.key) }
                onDenied()
            }
        }
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(settingsUrl)
    }

    private fun checkPhotoPermission(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized,
            PHAuthorizationStatusLimited -> {
                scope.launch { permissionStateStore.markGranted(permission.key) }
                onGranted()
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { status ->
                    dispatch_async(dispatch_get_main_queue()) {
                        handlePhotoAuthorizationResult(
                            permission = permission,
                            status = status,
                            onGranted = onGranted,
                            onDenied = onDenied,
                            onPermanentlyDenied = onPermanentlyDenied,
                        )
                    }
                }
            }
            PHAuthorizationStatusDenied,
            PHAuthorizationStatusRestricted -> {
                scope.launch { permissionStateStore.markPermanentlyDenied(permission.key) }
                onPermanentlyDenied()
            }
            else -> {
                scope.launch { permissionStateStore.markDenied(permission.key) }
                onDenied()
            }
        }
    }

    private fun handlePhotoAuthorizationResult(
        permission: Permission,
        status: Long,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        when (status) {
            PHAuthorizationStatusAuthorized,
            PHAuthorizationStatusLimited -> {
                scope.launch { permissionStateStore.markGranted(permission.key) }
                onGranted()
            }
            PHAuthorizationStatusDenied,
            PHAuthorizationStatusRestricted -> {
                // iOS 用户在系统弹窗中拒绝后，后续通常需要进入设置页修改，因此映射为永久拒绝。
                scope.launch { permissionStateStore.markPermanentlyDenied(permission.key) }
                onPermanentlyDenied()
            }
            else -> {
                scope.launch { permissionStateStore.markDenied(permission.key) }
                onDenied()
            }
        }
    }

    private fun presentImagePicker(
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        val host = currentTopViewController() ?: run {
            onPermissionDenied()
            return
        }
        val picker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            mediaTypes = listOf(
                when (mediaType) {
                    MediaType.IMAGE -> IOS_IMAGE_UTI
                    MediaType.VIDEO -> IOS_MOVIE_UTI
                    MediaType.AUDIO -> IOS_AUDIO_UTI
                }
            )
        }
        imagePickerDelegate = ImagePickerDelegate(
            onPicked = { uri ->
                onSuccess(uri)
                imagePickerDelegate = null
            },
            onCancelled = {
                onPermissionDenied()
                imagePickerDelegate = null
            },
        )
        picker.delegate = imagePickerDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }

    private fun presentAudioPicker(
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    ) {
        val host = currentTopViewController() ?: run {
            onPermissionDenied()
            return
        }
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf(IOS_AUDIO_UTI),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        documentPickerDelegate = DocumentPickerDelegate(
            onPicked = { uri ->
                onSuccess(uri)
                documentPickerDelegate = null
            },
            onCancelled = {
                onPermissionDenied()
                documentPickerDelegate = null
            },
        )
        picker.delegate = documentPickerDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

/**
 * UIImagePickerController 的最小 delegate。
 *
 * 图片优先返回系统提供的图片文件 URL，视频返回媒体文件 URL；两者都为空时按取消处理。
 * 回调只传递 URI 字符串，不持有 UIKit 对象。
 */
@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onPicked: (String) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val imageUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL
        val mediaUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
        val uri = imageUrl?.absoluteString ?: mediaUrl?.absoluteString
        picker.dismissViewControllerAnimated(true, completion = null)
        if (uri.isNullOrBlank()) {
            onCancelled()
        } else {
            onPicked(uri)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onCancelled()
    }
}

/**
 * UIDocumentPicker 的最小 delegate。
 *
 * 当前使用 import 模式让系统把音频复制到应用可读范围，返回的第一个 URL 可直接交给上传链路。
 */
private class DocumentPickerDelegate(
    private val onPicked: (String) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val uri = (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.absoluteString
        if (uri.isNullOrBlank()) {
            onCancelled()
        } else {
            onPicked(uri)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}

/**
 * 查找当前可呈现系统选择器的顶层 UIViewController。
 *
 * iOS 可能已有登录弹窗或验证码弹窗处于 presented 状态；继续沿 presentedViewController
 * 查找可以避免把媒体选择器挂到不可见的根控制器上。
 */
private fun currentTopViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

/**
 * 创建 iOS 平台权限控制器。
 *
 * @param permissionStateStore 权限状态存储边界，用于记录 PhotoKit 和系统选择器回调后的结果。
 */
@Composable
actual fun rememberPermissionController(
    permissionStateStore: PermissionStateStore,
): PermissionController {
    val scope = rememberCoroutineScope()
    return remember(permissionStateStore, scope) {
        IosPermissionController(permissionStateStore, scope)
    }
}
