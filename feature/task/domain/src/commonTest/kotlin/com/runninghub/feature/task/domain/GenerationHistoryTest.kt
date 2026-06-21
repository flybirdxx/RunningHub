package com.runninghub.feature.task.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerationHistoryTest {
    @Test
    fun `output type helpers classify common media types`() {
        val image = GenerationHistoryOutput(
            outputId = "image-1",
            url = "https://example.com/image.webp",
            type = "WEBP",
        )
        val video = GenerationHistoryOutput(
            outputId = "video-1",
            url = "https://example.com/video.mp4",
            type = "mp4",
        )

        // 类型判断保留在 Domain 模型内，避免历史页在多个 Composable 中重复维护媒体类型集合。
        assertTrue(image.isImage)
        assertFalse(image.isVideo)
        assertTrue(video.isVideo)
        assertFalse(video.isImage)
    }
}
