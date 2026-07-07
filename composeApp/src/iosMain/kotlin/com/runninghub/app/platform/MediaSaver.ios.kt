package com.runninghub.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import kotlin.coroutines.resume

actual fun createMediaSaver(): MediaSaver = IosMediaSaver()

/**
 * iOS 相册保存实现。
 *
 * 结果页传入的是远端图片地址，本实现先在后台线程读取图片数据，再通过 PhotoKit 在系统相册中
 * 创建图片资产。权限、下载和写入失败都被折叠成稳定失败原因，不向 UI 透传 URL、异常消息或
 * PhotoKit 诊断文本。
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosMediaSaver(
    private val requestPhotoWriteAccess: suspend () -> Boolean = ::requestIosPhotoWriteAccess,
    private val fetchImageData: suspend (String) -> NSData? = ::fetchIosImageData,
    private val saveImageDataToPhotos: suspend (NSData) -> Boolean = ::saveIosImageDataToPhotos,
    private val fetchVideoData: suspend (String) -> NSData? = ::fetchIosImageData,
    private val saveVideoDataToPhotos: suspend (NSData) -> Boolean = ::saveIosVideoDataToPhotos,
) : MediaSaver {
    override suspend fun saveImageToGallery(url: String, displayName: String): MediaSaveResult {
        val imageData = try {
            fetchImageData(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        } ?: return MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED)

        val hasAccess = requestPhotoWriteAccess()
        if (!hasAccess) {
            return MediaSaveResult.Failure(MediaSaveFailureReason.PHOTO_PERMISSION_DENIED)
        }

        val saved = try {
            saveImageDataToPhotos(imageData)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }

        return if (saved) {
            MediaSaveResult.Success
        } else {
            MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)
        }
    }

    override suspend fun saveVideoToGallery(url: String, displayName: String): MediaSaveResult {
        val videoData = try {
            fetchVideoData(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        } ?: return MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED)

        val hasAccess = requestPhotoWriteAccess()
        if (!hasAccess) {
            return MediaSaveResult.Failure(MediaSaveFailureReason.PHOTO_PERMISSION_DENIED)
        }

        val saved = try {
            saveVideoDataToPhotos(videoData)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }

        return if (saved) {
            MediaSaveResult.Success
        } else {
            MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)
        }
    }
}

/**
 * 读取远端图片字节。
 *
 * Foundation 的同步读取没有协程取消钩子，因此放到 Default dispatcher 执行；调用方仍会在读取前后
 * 保留取消语义，并把网络/格式失败统一映射为 [MediaSaveFailureReason.FETCH_FAILED]。
 */
private suspend fun fetchIosImageData(url: String): NSData? = withContext(Dispatchers.Default) {
    val remoteUrl = NSURL.URLWithString(url) ?: return@withContext null
    NSData.dataWithContentsOfURL(remoteUrl)
}

/**
 * 请求 PhotoKit 写入能力。
 *
 * iOS 现有媒体选择链路已经使用 PhotoKit 权限；保存图片也沿用同一授权状态。拒绝和受限状态不暴露
 * 具体系统错误，页面层只显示保存失败提示。
 */
private suspend fun requestIosPhotoWriteAccess(): Boolean = suspendCancellableCoroutine { continuation ->
    when (val status = PHPhotoLibrary.authorizationStatus()) {
        PHAuthorizationStatusAuthorized,
        PHAuthorizationStatusLimited -> continuation.resume(true)
        PHAuthorizationStatusNotDetermined -> {
            PHPhotoLibrary.requestAuthorization { requestedStatus ->
                continuation.resumeIfActive(isPhotoAccessGranted(requestedStatus))
            }
        }
        PHAuthorizationStatusDenied,
        PHAuthorizationStatusRestricted -> continuation.resume(false)
        else -> continuation.resume(isPhotoAccessGranted(status))
    }
}

/**
 * 将图片数据写入系统相册。
 *
 * 先用 UIKit 校验数据确实能解码为图片，再在 PhotoKit change block 中创建资产。completion handler
 * 可能在任意队列回调，只恢复协程结果，不在这里触达 Compose 状态。
 */
private suspend fun saveIosImageDataToPhotos(data: NSData): Boolean = suspendCancellableCoroutine { continuation ->
    val image = UIImage.imageWithData(data)
    if (image == null) {
        continuation.resume(false)
        return@suspendCancellableCoroutine
    }

    PHPhotoLibrary.sharedPhotoLibrary().performChanges(
        changeBlock = {
            PHAssetChangeRequest.creationRequestForAssetFromImage(image)
        },
        completionHandler = { success, _ ->
            continuation.resumeIfActive(success)
        },
    )
}

/**
 * 将视频数据写入系统相册。
 *
 * PhotoKit 的视频创建 API 需要 file URL，因此先写入临时文件；完成或失败后都尽力删除临时文件，
 * 不把临时路径或 PhotoKit 错误暴露给 UI。
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun saveIosVideoDataToPhotos(data: NSData): Boolean = suspendCancellableCoroutine { continuation ->
    val fileName = "runninghub_video_${kotlin.random.Random.nextLong().toString().replace("-", "")}.mp4"
    val path = NSTemporaryDirectory().trimEnd('/') + "/" + fileName
    val fileUrl = NSURL.fileURLWithPath(path)
    val wrote = data.writeToURL(fileUrl, atomically = true)
    if (!wrote) {
        continuation.resume(false)
        return@suspendCancellableCoroutine
    }

    PHPhotoLibrary.sharedPhotoLibrary().performChanges(
        changeBlock = {
            PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(fileUrl)
        },
        completionHandler = { success, _ ->
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            continuation.resumeIfActive(success)
        },
    )
}

private fun isPhotoAccessGranted(status: Long): Boolean =
    status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited

private fun <T> kotlinx.coroutines.CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}
