package com.runninghub.app.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class IosMediaSaverTest {
    @Test
    fun fetchFailureReturnsFetchFailedWithoutRequestingPhotoAccess() = runTest {
        var requestedPhotoAccess = false
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = {
                requestedPhotoAccess = true
                true
            },
            fetchImageData = { null },
            saveImageDataToPhotos = {
                wroteToPhotos = true
                true
            },
        )

        val result = saver.saveImageToGallery("not-a-url", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED), result)
        assertFalse(requestedPhotoAccess)
        assertFalse(wroteToPhotos)
    }

    @Test
    fun deniedPhotoAccessReturnsPermissionDeniedWithoutWritingToPhotos() = runTest {
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = { false },
            fetchImageData = { fixtureData() },
            saveImageDataToPhotos = {
                wroteToPhotos = true
                true
            },
        )

        val result = saver.saveImageToGallery("https://example.test/result.png", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.PHOTO_PERMISSION_DENIED), result)
        assertFalse(wroteToPhotos)
    }

    @Test
    fun photoWriteFailureReturnsWriteFailed() = runTest {
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = { true },
            fetchImageData = { fixtureData() },
            saveImageDataToPhotos = {
                wroteToPhotos = true
                false
            },
        )

        val result = saver.saveImageToGallery("https://example.test/result.png", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED), result)
        assertTrue(wroteToPhotos)
    }

    @Test
    fun videoFetchFailureReturnsFetchFailedWithoutRequestingPhotoAccess() = runTest {
        var requestedPhotoAccess = false
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = {
                requestedPhotoAccess = true
                true
            },
            fetchVideoData = { null },
            saveVideoDataToPhotos = {
                wroteToPhotos = true
                true
            },
        )

        val result = saver.saveVideoToGallery("not-a-url", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED), result)
        assertFalse(requestedPhotoAccess)
        assertFalse(wroteToPhotos)
    }

    @Test
    fun deniedPhotoAccessReturnsPermissionDeniedWithoutWritingVideoToPhotos() = runTest {
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = { false },
            fetchVideoData = { fixtureData() },
            saveVideoDataToPhotos = {
                wroteToPhotos = true
                true
            },
        )

        val result = saver.saveVideoToGallery("https://example.test/result.mp4", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.PHOTO_PERMISSION_DENIED), result)
        assertFalse(wroteToPhotos)
    }

    @Test
    fun videoWriteFailureReturnsWriteFailed() = runTest {
        var wroteToPhotos = false
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = { true },
            fetchVideoData = { fixtureData() },
            saveVideoDataToPhotos = {
                wroteToPhotos = true
                false
            },
        )

        val result = saver.saveVideoToGallery("https://example.test/result.mp4", "result")

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED), result)
        assertTrue(wroteToPhotos)
    }

    @Test
    fun videoWriteSuccessReturnsSuccess() = runTest {
        val saver = IosMediaSaver(
            requestPhotoWriteAccess = { true },
            fetchVideoData = { fixtureData() },
            saveVideoDataToPhotos = { true },
        )

        val result = saver.saveVideoToGallery("https://example.test/result.mp4", "result")

        assertEquals(MediaSaveResult.Success, result)
    }

    private fun fixtureData(): NSData =
        NSString.create(string = "runninghub-ios-media-saver")
            .dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("Expected UTF-8 test data.")
}
