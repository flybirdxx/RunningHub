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

private class IosMediaResolver : MediaResolver {
    @OptIn(ExperimentalForeignApi::class)
    override fun readBytes(uri: String): ByteArray {
        val url = NSURL.URLWithString(uri) ?: error("Invalid URI: $uri")
        val data = NSData.dataWithContentsOfURL(url) ?: error("Cannot read data from $uri")
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

    override fun getFileSizeBytes(uri: String): Long {
        val url = NSURL.URLWithString(uri) ?: return 0L
        val path = url.path ?: return 0L
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return 0L
        val size = attrs["NSFileSize"] as? Number
        return size?.toLong() ?: 0L
    }
}
