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
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import java.io.ByteArrayOutputStream
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

        readDiskCacheBytes(imageLoader, url)?.let { cachedBytes ->
            val (mimeType, extension) = inferImageMime(url)
            return ImagePayload(cachedBytes, mimeType, extension)
        }

        // 磁盘缓存不可用（被清理/禁用/异常）时降级：用解码位图重编码为 JPEG。
        // 会丢失动图帧并轻微降质，但保证下载动作仍可完成。
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, FALLBACK_JPEG_QUALITY, output)
            return ImagePayload(output.toByteArray(), "image/jpeg", "jpg")
        }
        return null
    }

    /** 读取 Coil 磁盘缓存中的原始编码字节；默认缓存 key 即请求 URL。 */
    private fun readDiskCacheBytes(imageLoader: ImageLoader, url: String): ByteArray? {
        val diskCache = imageLoader.diskCache ?: return null
        return try {
            diskCache.openSnapshot(url)?.use { snapshot ->
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
 * 根据 URL 后缀推断图片 MIME 类型与扩展名；未知后缀默认 image/jpeg。
 *
 * 只用于磁盘缓存原始字节路径：缓存内容即服务端原始编码，后缀通常与内容一致。
 */
private fun inferImageMime(url: String): Pair<String, String> {
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
