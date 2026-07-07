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
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.PhotosUI.presentLimitedLibraryPickerFromViewController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val IOS_IMAGE_UTI = "public.image"
private const val IOS_MOVIE_UTI = "public.movie"
private const val IOS_AUDIO_UTI = "public.audio"

internal enum class IosPermissionAuthorizationDecision {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

internal enum class IosPhotoLibraryManagementTarget {
    LIMITED_LIBRARY_PICKER,
    APP_SETTINGS,
}

internal fun iosPhotoAuthorizationDecision(status: Long): IosPermissionAuthorizationDecision = when (status) {
    PHAuthorizationStatusAuthorized,
    PHAuthorizationStatusLimited -> IosPermissionAuthorizationDecision.GRANTED
    PHAuthorizationStatusDenied,
    PHAuthorizationStatusRestricted -> IosPermissionAuthorizationDecision.PERMANENTLY_DENIED
    else -> IosPermissionAuthorizationDecision.DENIED
}

internal fun iosPhotoLibraryManagementTarget(
    permission: Permission,
    status: Long,
): IosPhotoLibraryManagementTarget =
    if (permission.allowsLimitedPhotoLibraryManagement() && status == PHAuthorizationStatusLimited) {
        IosPhotoLibraryManagementTarget.LIMITED_LIBRARY_PICKER
    } else {
        IosPhotoLibraryManagementTarget.APP_SETTINGS
    }

private fun Permission.allowsLimitedPhotoLibraryManagement(): Boolean =
    this == Permission.MediaImages || this == Permission.MediaVideo || this == Permission.StorageRead

internal fun iosCameraAuthorizationDecision(status: Long): IosPermissionAuthorizationDecision = when (status) {
    AVAuthorizationStatusAuthorized -> IosPermissionAuthorizationDecision.GRANTED
    AVAuthorizationStatusDenied,
    AVAuthorizationStatusRestricted -> IosPermissionAuthorizationDecision.PERMANENTLY_DENIED
    else -> IosPermissionAuthorizationDecision.DENIED
}

internal fun iosNotificationAuthorizationDecision(status: Long): IosPermissionAuthorizationDecision = when (status) {
    UNAuthorizationStatusAuthorized,
    UNAuthorizationStatusProvisional -> IosPermissionAuthorizationDecision.GRANTED
    UNAuthorizationStatusDenied -> IosPermissionAuthorizationDecision.PERMANENTLY_DENIED
    else -> IosPermissionAuthorizationDecision.DENIED
}

internal fun recordIosPermissionAuthorizationDecision(
    permissionStateStore: PermissionStateStore,
    scope: CoroutineScope,
    permission: Permission,
    decision: IosPermissionAuthorizationDecision,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
) {
    when (decision) {
        IosPermissionAuthorizationDecision.GRANTED -> {
            scope.launch { permissionStateStore.markGranted(permission.key) }
            onGranted()
        }
        IosPermissionAuthorizationDecision.DENIED -> {
            scope.launch { permissionStateStore.markDenied(permission.key) }
            onDenied()
        }
        IosPermissionAuthorizationDecision.PERMANENTLY_DENIED -> {
            // iOS 用户在系统弹窗中拒绝后，后续通常需要进入设置页修改，因此映射为永久拒绝。
            scope.launch { permissionStateStore.markPermanentlyDenied(permission.key) }
            onPermanentlyDenied()
        }
    }
}

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

    private var photoPickerDelegate: PhotoPickerDelegate? = null
    private var documentPickerDelegate: DocumentPickerDelegate? = null

    override fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
        onPickerCancelled: () -> Unit,
        onPermissionPermanentlyDenied: () -> Unit,
    ) {
        when (mediaType) {
            MediaType.IMAGE,
            MediaType.VIDEO -> presentPhotoPicker(mediaPermission, mediaType, onSuccess, onPermissionDenied, onPickerCancelled)
            MediaType.AUDIO -> {
                // iOS 文档选择器会把用户选中的音频导入应用可读范围，不需要读取整个媒体库权限。
                scope.launch { permissionStateStore.markGranted(mediaPermission.key) }
                presentAudioPicker(onSuccess, onPermissionDenied, onPickerCancelled)
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
            Permission.Camera -> checkCameraPermission(
                permission = permission,
                onGranted = onGranted,
                onDenied = onDenied,
                onPermanentlyDenied = onPermanentlyDenied,
            )
            Permission.Notifications -> checkNotificationPermission(
                permission = permission,
                onGranted = onGranted,
                onDenied = onDenied,
                onPermanentlyDenied = onPermanentlyDenied,
            )
        }
    }

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(settingsUrl)
    }

    override fun openPermissionSettings(permission: Permission) {
        when (
            iosPhotoLibraryManagementTarget(
                permission = permission,
                status = PHPhotoLibrary.authorizationStatus(),
            )
        ) {
            IosPhotoLibraryManagementTarget.LIMITED_LIBRARY_PICKER -> {
                val host = currentTopViewController() ?: return openAppSettings()
                PHPhotoLibrary.sharedPhotoLibrary().presentLimitedLibraryPickerFromViewController(host)
            }
            IosPhotoLibraryManagementTarget.APP_SETTINGS -> openAppSettings()
        }
    }

    private fun checkPhotoPermission(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        when (val status = PHPhotoLibrary.authorizationStatus()) {
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
            else -> handleAuthorizationDecision(
                permission = permission,
                decision = iosPhotoAuthorizationDecision(status),
                onGranted = onGranted,
                onDenied = onDenied,
                onPermanentlyDenied = onPermanentlyDenied,
            )
        }
    }

    private fun handlePhotoAuthorizationResult(
        permission: Permission,
        status: Long,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        handleAuthorizationDecision(
            permission = permission,
            decision = iosPhotoAuthorizationDecision(status),
            onGranted = onGranted,
            onDenied = onDenied,
            onPermanentlyDenied = onPermanentlyDenied,
        )
    }

    private fun checkCameraPermission(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        when (val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted: Boolean ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) {
                            scope.launch { permissionStateStore.markGranted(permission.key) }
                            onGranted()
                        } else {
                            scope.launch { permissionStateStore.markPermanentlyDenied(permission.key) }
                            onPermanentlyDenied()
                        }
                    }
                }
            }
            else -> handleAuthorizationDecision(
                permission = permission,
                decision = iosCameraAuthorizationDecision(status),
                onGranted = onGranted,
                onDenied = onDenied,
                onPermanentlyDenied = onPermanentlyDenied,
            )
        }
    }

    private fun checkNotificationPermission(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                dispatch_async(dispatch_get_main_queue()) {
                    val authorizationStatus = settings?.authorizationStatus
                    when (authorizationStatus) {
                        UNAuthorizationStatusNotDetermined -> requestNotificationPermission(
                            permission = permission,
                            onGranted = onGranted,
                            onPermanentlyDenied = onPermanentlyDenied,
                        )
                        else -> handleAuthorizationDecision(
                            permission = permission,
                            decision = iosNotificationAuthorizationDecision(authorizationStatus ?: Long.MIN_VALUE),
                            onGranted = onGranted,
                            onDenied = onDenied,
                            onPermanentlyDenied = onPermanentlyDenied,
                        )
                    }
                }
            }
    }

    private fun handleAuthorizationDecision(
        permission: Permission,
        decision: IosPermissionAuthorizationDecision,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        recordIosPermissionAuthorizationDecision(
            permissionStateStore = permissionStateStore,
            scope = scope,
            permission = permission,
            decision = decision,
            onGranted = onGranted,
            onDenied = onDenied,
            onPermanentlyDenied = onPermanentlyDenied,
        )
    }

    private fun requestNotificationPermission(
        permission: Permission,
        onGranted: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    ) {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (granted) {
                        scope.launch { permissionStateStore.markGranted(permission.key) }
                        onGranted()
                    } else {
                        scope.launch { permissionStateStore.markPermanentlyDenied(permission.key) }
                        onPermanentlyDenied()
                    }
                }
            }
    }

    private fun presentPhotoPicker(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
        onPickerCancelled: () -> Unit,
    ) {
        val host = currentTopViewController() ?: run {
            onPermissionDenied()
            return
        }
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = when (mediaType) {
                MediaType.IMAGE -> PHPickerFilter.imagesFilter()
                MediaType.VIDEO -> PHPickerFilter.videosFilter()
                MediaType.AUDIO -> null
            }
        }
        val picker = PHPickerViewController(configuration)
        photoPickerDelegate = PhotoPickerDelegate(
            mediaType = mediaType,
            onPicked = { uri ->
                scope.launch { permissionStateStore.markGranted(mediaPermission.key) }
                onSuccess(uri)
                photoPickerDelegate = null
            },
            onCancelled = {
                onPickerCancelled()
                photoPickerDelegate = null
            },
        )
        picker.delegate = photoPickerDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }

    private fun presentAudioPicker(
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
        onPickerCancelled: () -> Unit,
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
                onPickerCancelled()
                documentPickerDelegate = null
            },
        )
        picker.delegate = documentPickerDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }
}

/**
 * PHPicker 的最小 delegate。
 *
 * PHPicker 不要求读取整库 PhotoKit 权限，适合 iOS Limited Photos 场景。系统返回的文件
 * representation 生命周期可能很短，回调前先复制到应用临时目录，再把 URI 字符串交给
 * commonMain 上传链路。
 */
@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val mediaType: MediaType,
    private val onPicked: (String) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        val itemProvider = result?.itemProvider
        if (itemProvider == null) {
            picker.dismissViewControllerAnimated(true, completion = null)
            onCancelled()
            return
        }
        val typeIdentifier = mediaType.pickerTypeIdentifier()
        if (!itemProvider.hasItemConformingToTypeIdentifier(typeIdentifier)) {
            picker.dismissViewControllerAnimated(true, completion = null)
            onCancelled()
            return
        }
        itemProvider.loadFileRepresentationForTypeIdentifier(typeIdentifier) { url, _ ->
            val stableUrl = url?.let { copyPickedMediaToTemporaryFile(it) ?: it }
            dispatch_async(dispatch_get_main_queue()) {
                picker.dismissViewControllerAnimated(true, completion = null)
                val uri = stableUrl?.absoluteString
                if (uri.isNullOrBlank()) {
                    onCancelled()
                } else {
                    onPicked(uri)
                }
            }
        }
    }
}

private fun MediaType.pickerTypeIdentifier(): String =
    when (this) {
        MediaType.IMAGE -> IOS_IMAGE_UTI
        MediaType.VIDEO -> IOS_MOVIE_UTI
        MediaType.AUDIO -> IOS_AUDIO_UTI
    }

/**
 * Copies a picker-provided media URL into an app-owned temporary file before the picker is dismissed.
 *
 * PHPicker and legacy picker APIs can provide a temporary file whose lifetime is tied to the picker callback.
 * QuickCreate uploads asynchronously after the picker closes, so returning the original URL can leave the
 * upload job with a disappeared file. A best-effort copy keeps the URI stable without exposing the source
 * path to commonMain.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun copyPickedMediaToTemporaryFile(url: NSURL): NSURL? {
    val extension = url.pathExtension?.takeIf { it.isNotBlank() } ?: "tmp"
    val fileName = "runninghub-picked-${NSUUID.UUID().UUIDString}.$extension"
    val targetPath = NSTemporaryDirectory().trimEnd('/') + "/" + fileName
    val targetUrl = NSURL.fileURLWithPath(targetPath)
    val copied = NSFileManager.defaultManager.copyItemAtURL(
        srcURL = url,
        toURL = targetUrl,
        error = null,
    )
    return if (copied) targetUrl else null
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
