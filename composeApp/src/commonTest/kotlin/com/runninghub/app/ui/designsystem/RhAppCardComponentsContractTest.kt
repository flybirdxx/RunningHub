package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricIcon
import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewType
import com.runninghub.app.ui.designsystem.components.cards.AppCardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhAppCardComponentsContractTest {
    @Test
    fun `app card exposes overlay tile slots`() {
        val state = AppCardState(
            id = "webapp-1",
            title = "商品短片生成",
            capabilityLabel = "视频生成",
            preview = AppCardPreviewState(
                url = "https://cdn.example.com/preview.mp4",
                type = AppCardPreviewType.Video,
            ),
            metrics = listOf(
                AppCardMetricState(icon = AppCardMetricIcon.Use, value = "42"),
                AppCardMetricState(icon = AppCardMetricIcon.Like, value = "7"),
            ),
            featured = true,
        )

        assertEquals("商品短片生成", state.title)
        assertEquals("视频生成", state.capabilityLabel)
        assertEquals(AppCardPreviewType.Video, state.preview.type)
        assertEquals(2, state.metrics.size)
        assertEquals(AppCardMetricIcon.Use, state.metrics.first().icon)
        assertEquals("42", state.metrics.first().value)
        assertTrue(state.featured)
    }

    @Test
    fun `app card defaults to non featured with no metrics`() {
        val state = AppCardState(
            id = "webapp-2",
            title = "海报生成",
            capabilityLabel = "图片生成",
            preview = AppCardPreviewState(url = null, type = AppCardPreviewType.Empty),
            metrics = emptyList(),
        )

        assertEquals(false, state.featured)
        assertTrue(state.metrics.isEmpty())
    }
}
