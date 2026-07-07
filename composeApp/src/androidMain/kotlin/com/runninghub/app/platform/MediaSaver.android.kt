package com.runninghub.app.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer

private lateinit var appContext: Context

/**
 * 在 Application 启动阶段注入应用上下文。
 *
 * 必须在 Koin 组合根创建 [MediaSaver] 之前调用，与 [initMediaResolver] 同一初始化窗口。
 */
fun initMediaSaver(context: Context) {
    appContext = context.applicationContext
}

actual fun createMediaSaver(): MediaSaver = AndroidMediaSaver(appContext)

/** 日志 tag；日志只输出异常类型，禁止携带 URL、凭据或异常消息。 */
private const val TAG = "MediaSaver"

/** 相册中的应用相对目录（Pictures/RunningHub）。 */
private const val GALLERY_RELATIVE_DIR = "RunningHub"

/**
 * Android 相册保存实现。
 *
 * 字节来源复用 Coil 单例 ImageLoader：结果图已在会话区展示过，磁盘缓存大概率命中，
 * 优先读取缓存中的原始编码字节（免二次下载、保留原图质量）；缓存不可用时降级为
 * 解码位图后 JPEG 重编码。写入走 API 29+ 的 MediaStore scoped storage 标准流程
 * （RELATIVE_PATH + IS_PENDING），无需存储权限。
 *
 * 本批不实现 API 29 以下的 WRITE_EXTERNAL_STORAGE 旧权限分支：minSdk 为 26，
 * Android 8/9 设备会收到 [MediaSaveFailureReason.UNSUPPORTED_OS_VERSION] 失败语义，
 * 由页面层统一提示保存失败。
 */
private class AndroidMediaSaver(private val context: Context) : MediaSaver {

    override suspend fun saveImageToGallery(url: String, displayName: String): MediaSaveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return MediaSaveResult.Failure(MediaSaveFailureReason.UNSUPPORTED_OS_VERSION)
        }
        return withContext(Dispatchers.IO) {
            val payload = fetchImagePayload(url)
                ?: return@withContext MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED)
            writeToMediaStore(payload, displayName)
        }
    }

    override suspend fun saveVideoToGallery(url: String, displayName: String): MediaSaveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return MediaSaveResult.Failure(MediaSaveFailureReason.UNSUPPORTED_OS_VERSION)
        }
        return withContext(Dispatchers.IO) {
            writeRemoteVideoToMediaStore(url, displayName)
        }
    }

    /** 保存所需的字节与类型信息；bytes 为最终写入相册的完整编码内容。 */
    private class ImagePayload(
        val bytes: ByteArray,
        val mimeType: String,
        val extension: String,
    )

    /**
     * 通过 Coil 获取结果图字节。
     *
     * 先 execute 一次请求保证磁盘缓存就绪（已展示过的图直接命中，不触发网络），
     * 再读取缓存原始字节；缓存读取失败时用解码结果重编码兜底。
     */
    private suspend fun fetchImagePayload(url: String): ImagePayload? {
        val imageLoader = SingletonImageLoader.get(context)
        val result = try {
            imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    // 保存场景不需要硬件位图：硬件位图 toBitmap() 后 compress 会抛 ISE，
                    // 会让重编码兜底在现代设备上形同虚设。
                    .allowHardware(false)
                    // 无 target 的 execute 默认按屏幕尺寸降采样；保存必须保留原始分辨率，
                    // 否则 4K 结果图走兜底路径会被降到屏幕大小。
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            // execute 正常路径返回 ErrorResult；这里兜底非预期异常，只记类型不记 URL。
            Log.d(TAG, "image fetch threw: ${t::class.simpleName}")
            null
        }
        if (result is ErrorResult) {
            // 与异常路径保持同格式诊断：只记失败语义与异常类型，不含 URL。
            Log.d(TAG, "image request failed: ${result.throwable::class.simpleName}")
        }

        val diskCacheKey = (result as? SuccessResult)?.diskCacheKey ?: url
        readDiskCacheBytes(imageLoader, diskCacheKey)?.let { cachedBytes ->
            val (mimeType, extension) = inferImageMime(cachedBytes, url)
            return ImagePayload(cachedBytes, mimeType, extension)
        }

        // 磁盘缓存不可用（被清理/禁用/异常）时降级：用解码位图重编码为 JPEG。
        // 会丢失动图帧并轻微降质，但保证下载动作仍可完成。
        // toBitmap() 对非位图 Image 实现可抛 IllegalArgumentException，超大图重编码也可能 OOM；
        // 这里统一归一化为获取失败（返回 null → FETCH_FAILED），不让异常穿透 saveImageToGallery 契约。
        if (result is SuccessResult) {
            return try {
                val bitmap = result.image.toBitmap()
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, FALLBACK_JPEG_QUALITY, output)
                ImagePayload(output.toByteArray(), "image/jpeg", "jpg")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Log.d(TAG, "bitmap re-encode failed: ${t::class.simpleName}")
                null
            }
        }
        return null
    }

    /**
     * 读取 Coil 磁盘缓存中的原始编码字节。
     *
     * [diskCacheKey] 优先取 [SuccessResult.diskCacheKey]（真实写入 key，兼容全局
     * ImageLoader 未来配置自定义 Keyer 的情况），请求失败或 key 缺失时回退请求 URL。
     */
    private fun readDiskCacheBytes(imageLoader: ImageLoader, diskCacheKey: String): ByteArray? {
        val diskCache = imageLoader.diskCache ?: return null
        return try {
            diskCache.openSnapshot(diskCacheKey)?.use { snapshot ->
                diskCache.fileSystem.source(snapshot.data).buffer().use { source ->
                    source.readByteArray()
                }
            }
        } catch (t: Throwable) {
            // 缓存条目损坏或并发淘汰时按未命中处理，由调用方走重编码兜底。
            Log.d(TAG, "disk cache read failed: ${t::class.simpleName}")
            null
        }
    }

    /**
     * 按 IS_PENDING 标准流程把字节写入系统相册。
     *
     * 协程取消或写入失败时尽力删除未发布的 pending 条目，避免相册残留不可见文件。
     */
    private fun writeToMediaStore(payload: ImagePayload, displayName: String): MediaSaveResult {
        val resolver = context.contentResolver
        // 传入名 + 毫秒时间戳防重名；扩展名跟随实际写入的编码格式。
        val fileName = "${displayName}_${System.currentTimeMillis()}.${payload.extension}"
        val pendingValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, payload.mimeType)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$GALLERY_RELATIVE_DIR",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = try {
            resolver.insert(collection, pendingValues)
        } catch (t: Throwable) {
            Log.d(TAG, "media store insert failed: ${t::class.simpleName}")
            null
        } ?: return MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)

        return try {
            val written = resolver.openOutputStream(itemUri)?.use { output ->
                output.write(payload.bytes)
                true
            } ?: false
            if (!written) {
                deletePendingItem(resolver, itemUri)
                return MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)
            }
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(itemUri, publishValues, null, null)
            MediaSaveResult.Success
        } catch (cancellation: CancellationException) {
            deletePendingItem(resolver, itemUri)
            throw cancellation
        } catch (t: Throwable) {
            Log.d(TAG, "media store write failed: ${t::class.simpleName}")
            deletePendingItem(resolver, itemUri)
            MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)
        }
    }

    /**
     * 流式下载远端视频并写入系统相册。
     *
     * 视频结果可能明显大于图片，不能先整体读入内存；这里先创建 pending 媒体条目，再把网络输入流
     * 直接复制到 MediaStore 输出流，失败时清理未发布条目。
     */
    private fun writeRemoteVideoToMediaStore(url: String, displayName: String): MediaSaveResult {
        val resolver = context.contentResolver
        val (mimeType, extension) = inferVideoMime(url)
        val fileName = "${displayName}_${System.currentTimeMillis()}.$extension"
        val pendingValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/$GALLERY_RELATIVE_DIR",
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = try {
            resolver.insert(collection, pendingValues)
        } catch (t: Throwable) {
            Log.d(TAG, "video media store insert failed: ${t::class.simpleName}")
            null
        } ?: return MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)

        return try {
            val written = resolver.openOutputStream(itemUri)?.use { output ->
                URL(url).openStream().use { input ->
                    input.copyTo(output)
                }
                true
            } ?: false
            if (!written) {
                deletePendingItem(resolver, itemUri)
                return MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)
            }
            val publishValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(itemUri, publishValues, null, null)
            MediaSaveResult.Success
        } catch (cancellation: CancellationException) {
            deletePendingItem(resolver, itemUri)
            throw cancellation
        } catch (t: Throwable) {
            Log.d(TAG, "video media store write failed: ${t::class.simpleName}")
            deletePendingItem(resolver, itemUri)
            MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED)
        }
    }

    /** 尽力删除未发布的 pending 条目；清理失败只记日志，不影响主结果语义。 */
    private fun deletePendingItem(resolver: android.content.ContentResolver, itemUri: Uri) {
        try {
            resolver.delete(itemUri, null, null)
        } catch (t: Throwable) {
            Log.d(TAG, "pending cleanup failed: ${t::class.simpleName}")
        }
    }

    private companion object {
        /** 磁盘缓存不可用时位图重编码的 JPEG 质量。 */
        const val FALLBACK_JPEG_QUALITY = 95
    }
}

/**
 * 推断图片 MIME 类型与扩展名（二者联动）。
 *
 * 只用于磁盘缓存原始字节路径：优先按字节魔数嗅探（PNG/JPEG/GIF/WebP），
 * 内容真源比 URL 后缀可靠（CDN 地址可能无后缀或后缀与实际编码不一致）；
 * 嗅探未命中时回退 URL 后缀，仍未知则默认 image/jpeg。
 * avif/heic 等新格式暂未覆盖魔数嗅探，会走后缀回退或默认值。
 */
private fun inferImageMime(bytes: ByteArray, url: String): Pair<String, String> {
    sniffImageMime(bytes)?.let { return it }
    val extension = url
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return when (extension) {
        "png" -> "image/png" to "png"
        "webp" -> "image/webp" to "webp"
        "gif" -> "image/gif" to "gif"
        "jpg", "jpeg" -> "image/jpeg" to "jpg"
        else -> "image/jpeg" to "jpg"
    }
}

/** 按文件头魔数嗅探常见图片格式；无法识别返回 null 由调用方回退。 */
private fun sniffImageMime(bytes: ByteArray): Pair<String, String>? = when {
    // PNG：89 50 4E 47
    bytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> "image/png" to "png"
    // JPEG：FF D8
    bytes.startsWith(0xFF, 0xD8) -> "image/jpeg" to "jpg"
    // GIF："GIF8"
    bytes.startsWith(0x47, 0x49, 0x46, 0x38) -> "image/gif" to "gif"
    // WebP：0-3 为 "RIFF"，8-11 为 "WEBP"
    bytes.size >= 12 &&
        bytes.startsWith(0x52, 0x49, 0x46, 0x46) &&
        bytes[8] == 0x57.toByte() &&
        bytes[9] == 0x45.toByte() &&
        bytes[10] == 0x42.toByte() &&
        bytes[11] == 0x50.toByte() -> "image/webp" to "webp"
    else -> null
}

/** 按 URL 后缀推断视频 MIME；未知时使用最常见的 mp4 容器类型。 */
private fun inferVideoMime(url: String): Pair<String, String> {
    val extension = url
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return when (extension) {
        "mov" -> "video/quicktime" to "mov"
        "webm" -> "video/webm" to "webm"
        "m4v" -> "video/x-m4v" to "m4v"
        "mp4" -> "video/mp4" to "mp4"
        else -> "video/mp4" to "mp4"
    }
}

/** 判断字节数组是否以给定魔数序列开头。 */
private fun ByteArray.startsWith(vararg magic: Int): Boolean {
    if (size < magic.size) return false
    for (index in magic.indices) {
        if (this[index] != magic[index].toByte()) return false
    }
    return true
}
