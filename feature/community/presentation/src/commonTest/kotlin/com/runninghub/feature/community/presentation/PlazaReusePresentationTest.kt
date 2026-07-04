package com.runninghub.feature.community.presentation

import com.runninghub.feature.community.domain.PlazaCreationCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlazaReusePresentationTest {
    @Test
    fun `work card exposes owner id avatar like count and clamped aspect ratio`() {
        val card = PlazaCreationCard(
            id = "work-1",
            ownerId = "u-1",
            ownerName = "Ada",
            ownerAvatar = "av",
            likeCount = "1.2k",
            imageWidth = 800,
            imageHeight = 1200,
        )

        val uiModel = card.toPlazaWorkCardUiModel()

        assertEquals("u-1", uiModel.ownerId)
        assertEquals("av", uiModel.ownerAvatar)
        assertEquals("1.2k", uiModel.likeCount)
        assertEquals((800f / 1200f).coerceIn(0.6f, 1.4f), uiModel.aspectRatio)
    }

    @Test
    fun `aspect ratio is null when width or height missing or non positive`() {
        assertNull(plazaCardAspectRatio(null, 1200))
        assertNull(plazaCardAspectRatio(800, null))
        assertNull(plazaCardAspectRatio(0, 1200))
        assertNull(plazaCardAspectRatio(800, 0))
        assertNull(plazaCardAspectRatio(-10, 1200))
    }

    @Test
    fun `aspect ratio is clamped for extreme wide cards`() {
        assertEquals(1.4f, plazaCardAspectRatio(2000, 500))
    }
}
