package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadgeState
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.billing.GenerationConfirmSheetState
import com.runninghub.app.ui.designsystem.components.billing.PriceBadgeVisualState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhBillingComponentsContractTest {
    @Test
    fun `price badge exposes roadmap billing states`() {
        assertEquals("loading", PriceBadgeVisualState.Loading.tokenName)
        assertEquals("pending", PriceBadgeVisualState.Pending.tokenName)
        assertEquals("free", PriceBadgeVisualState.Free.tokenName)
        assertEquals("amount", PriceBadgeVisualState.Amount.tokenName)
        assertEquals("insufficient", PriceBadgeVisualState.Insufficient.tokenName)
    }

    @Test
    fun `generation confirmation sheet state carries required billing rows`() {
        val state = GenerationConfirmSheetState(
            title = "确认生成",
            priceBadgeState = RhPriceBadgeState.Amount("37 RHB 点数"),
            priceLabel = "37 RHB 点数",
            rows = listOf(
                BillingInfoRow(label = "预计消耗", value = "37 RHB 点数", emphasized = true),
                BillingInfoRow(label = "当前余额", value = "暂未同步"),
                BillingInfoRow(label = "会员减免", value = "暂无会员减免"),
                BillingInfoRow(label = "失败扣费说明", value = "生成失败不扣费；如已扣费将自动退回"),
            ),
            confirmActionLabel = "确认生成",
            dismissActionLabel = "取消",
            confirmEnabled = true,
        )

        assertEquals(4, state.rows.size)
        assertTrue(state.rows.first().emphasized)
        assertEquals("失败扣费说明", state.rows.last().label)
    }
}
