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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS video picker returns a temporary media URL. The app must copy it into an app-owned temp file
 * before the picker is dismissed, otherwise asynchronous upload can read a URL that no longer exists.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPickedMediaCopyTest {
    @Test
    fun copiedPickedMediaRemainsReadableAfterSourceIsRemoved() {
        val sourcePath = NSTemporaryDirectory().trimEnd('/') + "/rh-picked-source.MP4"
        val expectedBytes = "runninghub-ios-picked-video".encodeToByteArray()
        assertTrue(writeFixture(sourcePath, expectedBytes), "Expected picked media fixture to be written.")

        val sourceUrl = NSURL.fileURLWithPath(sourcePath)
        val copiedUrl = assertNotNull(copyPickedMediaToTemporaryFile(sourceUrl))
        assertNotEquals(sourceUrl.absoluteString, copiedUrl.absoluteString)

        try {
            NSFileManager.defaultManager.removeItemAtPath(sourcePath, error = null)
            val resolver = createMediaResolver()
            val copiedUri = assertNotNull(copiedUrl.absoluteString)

            assertContentEquals(expectedBytes, resolver.readBytes(copiedUri))
        } finally {
            copiedUrl.path?.let { path ->
                NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            }
            NSFileManager.defaultManager.removeItemAtPath(sourcePath, error = null)
        }
    }

    @Test
    fun copiedPickedImageUsesAppOwnedTemporaryFileUri() {
        val tempDirectory = NSTemporaryDirectory().trimEnd('/')
        val sourcePath = "$tempDirectory/rh-picked-source.PNG"
        val expectedBytes = "runninghub-ios-picked-image".encodeToByteArray()
        assertTrue(writeFixture(sourcePath, expectedBytes), "Expected picked image fixture to be written.")

        val sourceUrl = NSURL.fileURLWithPath(sourcePath)
        val copiedUrl = assertNotNull(copyPickedMediaToTemporaryFile(sourceUrl))
        val copiedPath = assertNotNull(copiedUrl.path)

        try {
            assertNotEquals(sourceUrl.absoluteString, copiedUrl.absoluteString)
            assertTrue(
                copiedPath.startsWith("$tempDirectory/runninghub-picked-"),
                "Expected PHPicker copy to live in the app-owned temporary directory.",
            )
            assertEquals("PNG", copiedUrl.pathExtension)

            NSFileManager.defaultManager.removeItemAtPath(sourcePath, error = null)
            val resolver = createMediaResolver()
            val copiedUri = assertNotNull(copiedUrl.absoluteString)

            assertContentEquals(expectedBytes, resolver.readBytes(copiedUri))
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(copiedPath, error = null)
            NSFileManager.defaultManager.removeItemAtPath(sourcePath, error = null)
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
