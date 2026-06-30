package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.cards.AppCardActionState
import com.runninghub.app.ui.designsystem.components.cards.AppCardActionType
import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewType
import com.runninghub.app.ui.designsystem.components.cards.AppCardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhAppCardComponentsContractTest {
    @Test
    fun `app card exposes creation entry slots`() {
        val state = AppCardState(
            id = "webapp-1",
            title = "商品短片生成",
            capabilityLabel = "视频生成",
            preview = AppCardPreviewState(
                url = "https://cdn.example.com/preview.mp4",
                type = AppCardPreviewType.Video,
            ),
            estimatedCostLabel = "费用待确认",
            metric = AppCardMetricState(label = "使用次数", value = "42"),
            primaryAction = AppCardActionState(
                type = AppCardActionType.Generate,
                label = "立即生成",
            ),
        )

        assertEquals("商品短片生成", state.title)
        assertEquals("视频生成", state.capabilityLabel)
        assertEquals(AppCardPreviewType.Video, state.preview.type)
        assertEquals("费用待确认", state.estimatedCostLabel)
        assertEquals("42", state.metric?.value)
        assertEquals(AppCardActionType.Generate, state.primaryAction.type)
        assertTrue(state.primaryAction.enabled)
    }
}
