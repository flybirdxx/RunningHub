package com.runninghub.app.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImagePreviewOverlayTest {
    @Test
    fun `selected candidate is full size sharp and opaque`() {
        val visual = imagePreviewCandidateVisual(index = 2, selectedIndex = 2)

        assertTrue(visual.selected)
        assertEquals(76, visual.widthDp)
        assertEquals(104, visual.heightDp)
        assertEquals(1f, visual.alpha)
        assertEquals(0f, visual.blurDp)
    }

    @Test
    fun `unselected candidates are blurred and dimmed`() {
        val visual = imagePreviewCandidateVisual(index = 3, selectedIndex = 2)

        assertFalse(visual.selected)
        assertTrue(visual.alpha < 1f)
        assertTrue(visual.alpha > 0.4f)
        assertTrue(visual.blurDp > 0f)
    }

    @Test
    fun `candidates shrink symmetrically as distance grows`() {
        val nearAbove = imagePreviewCandidateVisual(index = 2, selectedIndex = 3)
        val farAbove = imagePreviewCandidateVisual(index = 1, selectedIndex = 3)
        val nearBelow = imagePreviewCandidateVisual(index = 4, selectedIndex = 3)
        val farBelow = imagePreviewCandidateVisual(index = 5, selectedIndex = 3)

        assertTrue(farAbove.widthDp < nearAbove.widthDp)
        assertTrue(farAbove.heightDp < nearAbove.heightDp)
        assertTrue(farAbove.blurDp > nearAbove.blurDp)
        assertEquals(nearAbove.widthDp, nearBelow.widthDp)
        assertEquals(nearAbove.heightDp, nearBelow.heightDp)
        assertEquals(farAbove.widthDp, farBelow.widthDp)
        assertEquals(farAbove.heightDp, farBelow.heightDp)
    }

    @Test
    fun `indicator maps selected item to bounded segment`() {
        assertEquals(0, imagePreviewIndicatorActiveSegment(itemCount = 3, selectedIndex = -1))
        assertEquals(2, imagePreviewIndicatorActiveSegment(itemCount = 3, selectedIndex = 5))
        assertEquals(12, imagePreviewIndicatorSegmentCount(itemCount = 24))
        assertEquals(11, imagePreviewIndicatorActiveSegment(itemCount = 24, selectedIndex = 23))
    }

    @Test
    fun `visible item closest to viewport center becomes selected`() {
        val centeredIndex = imagePreviewClosestCenterIndex(
            visibleItems = listOf(
                ImagePreviewVisibleItem(index = 0, offset = 10, size = 60),
                ImagePreviewVisibleItem(index = 1, offset = 90, size = 80),
                ImagePreviewVisibleItem(index = 2, offset = 190, size = 60),
            ),
            viewportStartOffset = 0,
            viewportEndOffset = 240,
        )

        assertEquals(1, centeredIndex)
    }

    @Test
    fun `candidate list starts from selected item`() {
        assertEquals(0, imagePreviewInitialFirstVisibleItemIndex(itemCount = 0, selectedIndex = 4))
        assertEquals(0, imagePreviewInitialFirstVisibleItemIndex(itemCount = 5, selectedIndex = -1))
        assertEquals(3, imagePreviewInitialFirstVisibleItemIndex(itemCount = 5, selectedIndex = 3))
        assertEquals(4, imagePreviewInitialFirstVisibleItemIndex(itemCount = 5, selectedIndex = 8))
    }

    @Test
    fun `centered item syncs only during user scroll`() {
        assertFalse(
            imagePreviewShouldSyncCenteredIndex(
                centeredIndex = 0,
                selectedIndex = 3,
                isScrollInProgress = false,
                programmaticScrollTargetIndex = null,
            ),
        )
        assertFalse(
            imagePreviewShouldSyncCenteredIndex(
                centeredIndex = 2,
                selectedIndex = 3,
                isScrollInProgress = true,
                programmaticScrollTargetIndex = 3,
            ),
        )
        assertTrue(
            imagePreviewShouldSyncCenteredIndex(
                centeredIndex = 2,
                selectedIndex = 3,
                isScrollInProgress = true,
                programmaticScrollTargetIndex = null,
            ),
        )
    }

    @Test
    fun `candidate list auto loads more only near the end`() {
        assertFalse(
            imagePreviewShouldAutoLoadMore(
                loadedItemCount = 0,
                lastVisibleItemIndex = 0,
                hasMore = true,
                isLoading = false,
            ),
        )
        assertFalse(
            imagePreviewShouldAutoLoadMore(
                loadedItemCount = 18,
                lastVisibleItemIndex = 17,
                hasMore = true,
                isLoading = true,
            ),
        )
        assertFalse(
            imagePreviewShouldAutoLoadMore(
                loadedItemCount = 18,
                lastVisibleItemIndex = 10,
                hasMore = true,
                isLoading = false,
            ),
        )
        assertTrue(
            imagePreviewShouldAutoLoadMore(
                loadedItemCount = 18,
                lastVisibleItemIndex = 14,
                hasMore = true,
                isLoading = false,
            ),
        )
    }

    @Test
    fun `prefetch range keeps ten candidates around selected item`() {
        assertEquals(5..14, imagePreviewPrefetchIndexRange(itemCount = 30, selectedIndex = 10, prefetchCount = 10))
    }

    @Test
    fun `prefetch range clamps near list edges`() {
        assertEquals(0..9, imagePreviewPrefetchIndexRange(itemCount = 30, selectedIndex = 0, prefetchCount = 10))
        assertEquals(20..29, imagePreviewPrefetchIndexRange(itemCount = 30, selectedIndex = 29, prefetchCount = 10))
    }

    @Test
    fun `prefetch urls follow computed range`() {
        val items = (0 until 12).map { index ->
            ImagePreviewItem(id = index.toString(), imageUrl = "url-$index")
        }

        assertEquals(
            expected = (0..9).map { index -> "url-$index" },
            actual = imagePreviewPrefetchUrls(items = items, selectedIndex = 2, prefetchCount = 10),
        )
    }
}
