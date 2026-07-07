package com.runninghub.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * iOS 媒体读取器的 actual 运行契约。
 *
 * QuickCreate 与 AppDetail 都依赖该读取器把系统 picker 返回的 file URL 转换为上传字节、
 * 展示文件名和大小；这里用真实 Foundation 文件 API 覆盖最小可自动化路径。
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaResolverTest {
    @Test
    fun fileUrlReadsBytesDisplayNameAndSize() {
        val payload = "runninghub-ios-media"
        val fileName = "rh-ios-media-resolver.txt"
        val path = NSTemporaryDirectory().trimEnd('/') + "/" + fileName
        val expectedBytes = payload.encodeToByteArray()
        val wrote = writeFixture(path, expectedBytes)

        assertTrue(wrote, "Expected test media fixture to be written before resolver assertions.")

        try {
            val resolver = createMediaResolver()
            val uri = NSURL.fileURLWithPath(path).absoluteString ?: error("Expected file URL.")

            assertContentEquals(expectedBytes, resolver.readBytes(uri))
            assertEquals(fileName, resolver.getDisplayName(uri))
            assertEquals(expectedBytes.size.toLong(), resolver.getFileSizeBytes(uri))
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        }
    }

    private fun writeFixture(path: String, bytes: ByteArray): Boolean {
        val file = fopen(path, "wb") ?: return false
        return try {
            bytes.usePinned { pinned ->
                fwrite(
                    pinned.addressOf(0),
                    1u,
                    bytes.size.toULong(),
                    file,
                )
            } == bytes.size.toULong()
        } finally {
            fclose(file)
        }
    }
}
