package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.billing.MembershipCardState
import com.runninghub.app.ui.designsystem.components.billing.MembershipStatusVisualState
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceAction
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceCardState
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceRiskState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RhAssetCenterComponentsContractTest {
    @Test
    fun `wallet balance card separates rhb points and cny wallet balance`() {
        val state = WalletBalanceCardState(
            rhbPointsLabel = "RHB 点数",
            rhbPointsValue = "12000",
            walletBalanceLabel = "钱包余额 CNY",
            walletBalanceValue = "¥248.60",
            risk = WalletBalanceRiskState.None(),
            primaryAction = WalletBalanceAction.Recharge("充值"),
        )

        assertEquals("12000", state.rhbPointsValue)
        assertEquals("¥248.60", state.walletBalanceValue)
        assertTrue(state.risk is WalletBalanceRiskState.None)
        assertEquals("充值", state.primaryAction.label)
    }

    @Test
    fun `membership card exposes returned status without create placement or renewal action`() {
        val state = MembershipCardState(
            levelLabel = "会员等级",
            levelName = "RunningHub Pro",
            remainingLabel = "已过期",
            status = MembershipStatusVisualState.Expired,
        )

        assertEquals(MembershipStatusVisualState.Expired, state.status)
        assertFalse(state.showInsideCreateInput)
    }

}
