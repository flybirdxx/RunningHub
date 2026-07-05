package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionType
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostKind
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusType
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskSourceBadgeState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskSourceBadgeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhHistoryTaskCardComponentsContractTest {
    @Test
    fun `history task card state carries first screen task semantics`() {
        val state = HistoryTaskCardState(
            title = "Portrait",
            thumbnailUrl = "https://example.com/thumb.png",
            status = HistoryTaskCardStatusState(
                type = HistoryTaskCardStatusType.Success,
                label = "已完成",
            ),
            sourceLabel = "快捷创作",
            sourceBadge = HistoryTaskSourceBadgeState(
                type = HistoryTaskSourceBadgeType.QuickCreate,
                label = "快",
                contentDescription = "快捷创作",
            ),
            cost = HistoryTaskCardCostState(
                kind = HistoryTaskCardCostKind.Rhb,
                amountLabel = "12.5 RHB",
            ),
            durationLabel = "00:42",
            expiryLabel = "2 天后过期",
            outputCountLabel = "1 个结果",
            primaryAction = HistoryTaskCardActionState(
                type = HistoryTaskCardActionType.ViewResult,
                label = "查看结果",
            ),
            secondaryActions = listOf(
                HistoryTaskCardActionState(
                    type = HistoryTaskCardActionType.ReuseParameters,
                    label = "复用参数",
                )
            ),
        )

        assertEquals(HistoryTaskCardStatusType.Success, state.status.type)
        assertEquals(HistoryTaskSourceBadgeType.QuickCreate, state.sourceBadge?.type)
        assertEquals("快", state.sourceBadge?.label)
        assertEquals(HistoryTaskCardCostKind.Rhb, state.cost?.kind)
        assertEquals("2 天后过期", state.expiryLabel)
        assertEquals(HistoryTaskCardActionType.ViewResult, state.primaryAction?.type)
        assertTrue(state.secondaryActions.any { it.type == HistoryTaskCardActionType.ReuseParameters })
    }

    @Test
    fun `failed history task card uses retry as primary action without result`() {
        val state = HistoryTaskCardState(
            title = "Image",
            thumbnailUrl = null,
            status = HistoryTaskCardStatusState(
                type = HistoryTaskCardStatusType.Failed,
                label = "失败",
            ),
            sourceLabel = "标准模型",
            cost = HistoryTaskCardCostState(
                kind = HistoryTaskCardCostKind.Fiat,
                amountLabel = "4 CNY",
            ),
            durationLabel = null,
            expiryLabel = null,
            outputCountLabel = "0 个结果",
            primaryAction = HistoryTaskCardActionState(
                type = HistoryTaskCardActionType.Retry,
                label = "重试",
            ),
        )

        assertEquals(HistoryTaskCardStatusType.Failed, state.status.type)
        assertEquals(HistoryTaskCardActionType.Retry, state.primaryAction?.type)
        assertEquals(emptyList(), state.secondaryActions)
    }
}
