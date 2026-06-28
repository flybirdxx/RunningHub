package com.runninghub.app.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoPreviewOverlayTest {
    @Test
    fun `playback progress exposes played and buffered fractions`() {
        val progress = VideoPlaybackProgress(
            positionMs = 25_000L,
            durationMs = 100_000L,
            bufferedPositionMs = 60_000L,
        )

        assertEquals(0.25f, progress.progressFraction)
        assertEquals(0.6f, progress.bufferedFraction)
    }

    @Test
    fun `playback progress hides fractions until duration is known`() {
        val progress = VideoPlaybackProgress(
            positionMs = 25_000L,
            durationMs = null,
            bufferedPositionMs = 60_000L,
        )

        assertNull(progress.progressFraction)
        assertNull(progress.bufferedFraction)
    }

    @Test
    fun `video preview formats media times like a player control`() {
        assertEquals("--:--", videoPreviewFormatTime(null))
        assertEquals("0:03", videoPreviewFormatTime(3_000L))
        assertEquals("1:40", videoPreviewFormatTime(100_000L))
        assertEquals("1:01:01", videoPreviewFormatTime(3_661_000L))
    }

    @Test
    fun `video preview plays audio only when it has playable video`() {
        assertTrue(
            videoPreviewShouldPlayAudio(
                VideoPreviewItem(
                    id = "video",
                    videoUrl = " https://example.com/demo.mp4 ",
                    posterUrl = "https://example.com/demo.jpg",
                ),
            ),
        )
        assertFalse(
            videoPreviewShouldPlayAudio(
                VideoPreviewItem(
                    id = "poster",
                    videoUrl = " ",
                    posterUrl = "https://example.com/demo.jpg",
                ),
            ),
        )
    }

    @Test
    fun `video thumbnail volume is muted unless audio playback is requested`() {
        assertEquals(0f, videoThumbnailPlaybackVolume(playAudio = false))
        assertEquals(1f, videoThumbnailPlaybackVolume(playAudio = true))
    }
}
