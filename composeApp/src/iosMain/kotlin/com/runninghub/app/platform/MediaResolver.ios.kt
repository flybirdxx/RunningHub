package com.runninghub.app.platform

import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual fun createMediaResolver(): MediaResolver = IosMediaResolver()

/**
 * iOS 媒体文件读取器。
 *
 * UIImagePickerController 和 UIDocumentPicker 返回的 URL 可能指向应用沙盒内副本，也可能带有
 * 安全作用域访问语义。读取前通过 [withSecurityScopedAccess] 尝试开启临时访问，可以兼容
 * 系统文件选择器返回的外部文件；不支持安全作用域的普通 file URL 会按原路径直接读取。
 */
private class IosMediaResolver : MediaResolver {
    @OptIn(ExperimentalForeignApi::class)
    override fun readBytes(uri: String): ByteArray {
        val url = NSURL.URLWithString(uri) ?: error("Invalid media URI.")
        val data = withSecurityScopedAccess(url) {
            NSData.dataWithContentsOfURL(url)
        } ?: error("Cannot read media data.")
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, length.toULong())
            }
        }
        return bytes
    }

    override fun getDisplayName(uri: String): String? =
        NSURL.URLWithString(uri)?.lastPathComponent

    @OptIn(ExperimentalForeignApi::class)
    override fun getFileSizeBytes(uri: String): Long {
        val url = NSURL.URLWithString(uri) ?: return 0L
        val path = url.path ?: return 0L
        return withSecurityScopedAccess(url) {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return@withSecurityScopedAccess 0L
            val size = attrs["NSFileSize"] as? Number
            size?.toLong() ?: 0L
        }
    }
}

/**
 * 在需要时开启 iOS 安全作用域 URL 访问。
 *
 * `startAccessingSecurityScopedResource()` 对非安全作用域 URL 可能返回 `false`，这不代表普通沙盒
 * 文件不可读；因此无论返回值如何都会执行 [block]。只有成功开启访问时才在结束后释放作用域，
 * 避免泄漏文件访问句柄。
 */
private inline fun <T> withSecurityScopedAccess(url: NSURL, block: () -> T): T {
    val accessStarted = url.startAccessingSecurityScopedResource()
    return try {
        block()
    } finally {
        if (accessStarted) {
            url.stopAccessingSecurityScopedResource()
        }
    }
}
