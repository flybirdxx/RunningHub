package com.runninghub.app.ui.feature.history

import com.runninghub.app.platform.MediaSaveFailureReason
import com.runninghub.app.platform.MediaSaveResult
import com.runninghub.app.platform.MediaSaver
import com.runninghub.feature.task.presentation.TaskHistoryDetailMediaType
import com.runninghub.feature.task.presentation.TaskHistoryDetailOutputUi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskHistoryMediaSaveActionTest {
    @Test
    fun imageOutputIsSavedThroughImageGalleryPath() = runTest {
        val mediaSaver = RecordingMediaSaver()
        val output = output(mediaType = TaskHistoryDetailMediaType.IMAGE, url = "https://example.test/result.png")

        val result = saveTaskHistoryDetailOutputToGallery(mediaSaver, output)

        assertEquals(MediaSaveResult.Success, result)
        assertEquals(listOf("image:https://example.test/result.png"), mediaSaver.calls)
    }

    @Test
    fun videoOutputIsSavedThroughVideoGalleryPath() = runTest {
        val mediaSaver = RecordingMediaSaver()
        val output = output(mediaType = TaskHistoryDetailMediaType.VIDEO, url = "https://example.test/result.mp4")

        val result = saveTaskHistoryDetailOutputToGallery(mediaSaver, output)

        assertEquals(MediaSaveResult.Success, result)
        assertEquals(listOf("video:https://example.test/result.mp4"), mediaSaver.calls)
    }

    @Test
    fun fileOutputDoesNotCallPlatformSaver() = runTest {
        val mediaSaver = RecordingMediaSaver()
        val output = output(mediaType = TaskHistoryDetailMediaType.FILE, url = "https://example.test/result.bin")

        val result = saveTaskHistoryDetailOutputToGallery(mediaSaver, output)

        assertEquals(MediaSaveResult.Failure(MediaSaveFailureReason.UNSUPPORTED_PLATFORM), result)
        assertEquals(emptyList(), mediaSaver.calls)
    }

    private fun output(mediaType: TaskHistoryDetailMediaType, url: String): TaskHistoryDetailOutputUi =
        TaskHistoryDetailOutputUi(
            outputId = "output-1",
            url = url,
            previewUrl = null,
            mediaType = mediaType,
            aspectRatio = null,
            name = null,
        )

    private class RecordingMediaSaver : MediaSaver {
        val calls = mutableListOf<String>()

        override suspend fun saveImageToGallery(url: String, displayName: String): MediaSaveResult {
            calls += "image:$url"
            return MediaSaveResult.Success
        }

        override suspend fun saveVideoToGallery(url: String, displayName: String): MediaSaveResult {
            calls += "video:$url"
            return MediaSaveResult.Success
        }
    }
}
