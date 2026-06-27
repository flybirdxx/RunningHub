package com.runninghub.app.ui.feature.plaza

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaShortCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlazaImagePreviewItemsTest {
    @Test
    fun `preview items keep media url cards in server order`() {
        val items = plazaCreationPreviewItems(
            listOf(
                PlazaCreationCard(id = "first", mediaUrl = "https://example.com/first.png", intro = "First"),
                PlazaCreationCard(id = "blank", mediaUrl = " "),
                PlazaCreationCard(id = "video", mediaUrl = "https://example.com/video.mp4", mediaType = "mp4"),
                PlazaCreationCard(id = "second", mediaUrl = "https://example.com/second.png", intro = "Second"),
            ),
        )

        assertEquals(listOf("https://example.com/first.png", "https://example.com/second.png"), items.map { it.imageUrl })
        assertEquals(listOf("First", "Second"), items.map { it.contentDescription })
    }

    @Test
    fun `preview item ids stay unique when source card ids repeat`() {
        val items = plazaCreationPreviewItems(
            listOf(
                PlazaCreationCard(id = "same", mediaUrl = "https://example.com/first.png"),
                PlazaCreationCard(id = "same", mediaUrl = "https://example.com/second.png"),
            ),
        )

        assertNotEquals(items[0].id, items[1].id)
    }

    @Test
    fun `clicked creation index maps to preview index after blank media cards are skipped`() {
        val creations = listOf(
            PlazaCreationCard(id = "first", mediaUrl = "https://example.com/first.png"),
            PlazaCreationCard(id = "blank", mediaUrl = " "),
            PlazaCreationCard(id = "video", mediaUrl = "https://example.com/video.mp4", mediaType = "mp4"),
            PlazaCreationCard(id = "second", mediaUrl = "https://example.com/second.png"),
            PlazaCreationCard(id = "third", mediaUrl = "https://example.com/third.png"),
        )

        assertEquals(0, plazaCreationPreviewIndex(creations = creations, creationIndex = 0))
        assertNull(plazaCreationPreviewIndex(creations = creations, creationIndex = 1))
        assertNull(plazaCreationPreviewIndex(creations = creations, creationIndex = 2))
        assertEquals(1, plazaCreationPreviewIndex(creations = creations, creationIndex = 3))
        assertEquals(2, plazaCreationPreviewIndex(creations = creations, creationIndex = 4))
    }

    @Test
    fun `auto load more triggers only near the end of loaded items`() {
        assertFalse(
            plazaShouldAutoLoadMore(
                loadedItemCount = 0,
                lastVisibleItemIndex = 0,
                hasMore = true,
                isLoading = false,
            ),
        )
        assertFalse(
            plazaShouldAutoLoadMore(
                loadedItemCount = 20,
                lastVisibleItemIndex = 19,
                hasMore = true,
                isLoading = true,
            ),
        )
        assertFalse(
            plazaShouldAutoLoadMore(
                loadedItemCount = 20,
                lastVisibleItemIndex = 12,
                hasMore = true,
                isLoading = false,
            ),
        )
        assertTrue(
            plazaShouldAutoLoadMore(
                loadedItemCount = 20,
                lastVisibleItemIndex = 15,
                hasMore = true,
                isLoading = false,
            ),
        )
    }

    @Test
    fun `short preview item uses video poster title and metadata`() {
        val item = plazaShortPreviewItem(
            PlazaShortCard(
                id = "short-1",
                name = " Demo short ",
                videoUrl = " https://example.com/demo.mp4 ",
                thumbnailUrl = " https://example.com/demo.jpg ",
                durationSeconds = 65,
                categoryName = "Video",
                authorName = "Creator",
            ),
        ) ?: error("Expected short preview item")

        assertEquals("https://example.com/demo.mp4", item.videoUrl)
        assertEquals("https://example.com/demo.jpg", item.posterUrl)
        assertEquals("Demo short", item.title)
        assertEquals("Creator / Video / 1:05", item.subtitle)
    }

    @Test
    fun `creation video preview item maps mp4 cards to video preview`() {
        val item = plazaCreationVideoPreviewItem(
            PlazaCreationCard(
                id = "creation-video",
                intro = " Demo video ",
                ownerName = "Creator",
                mediaUrl = " https://example.com/demo.mp4 ",
                mediaType = "mp4",
            ),
        ) ?: error("Expected creation video preview item")

        assertEquals("https://example.com/demo.mp4", item.videoUrl)
        assertNull(item.posterUrl)
        assertEquals("Demo video", item.title)
        assertEquals("Creator / mp4", item.subtitle)
    }

    @Test
    fun `creation video detection accepts media type and url suffix`() {
        assertTrue(plazaCreationIsVideo(PlazaCreationCard(id = "type", mediaUrl = "https://example.com/file", mediaType = "VIDEO")))
        assertTrue(plazaCreationIsVideo(PlazaCreationCard(id = "url", mediaUrl = "https://example.com/file.mp4?token=1")))
        assertFalse(plazaCreationIsVideo(PlazaCreationCard(id = "image", mediaUrl = "https://example.com/file.png", mediaType = "png")))
    }

    @Test
    fun `short preview item can fall back to poster only`() {
        val item = plazaShortPreviewItem(
            PlazaShortCard(
                id = "short-poster",
                name = "Poster only",
                thumbnailUrl = "https://example.com/poster.jpg",
            ),
        ) ?: error("Expected short preview item")

        assertNull(item.videoUrl)
        assertEquals("https://example.com/poster.jpg", item.posterUrl)
    }

    @Test
    fun `short preview ignores cards without playable or poster media`() {
        assertNull(
            plazaShortPreviewItem(
                PlazaShortCard(
                    id = "short-empty",
                    name = "Empty",
                    videoUrl = " ",
                    thumbnailUrl = " ",
                ),
            ),
        )
    }
}
