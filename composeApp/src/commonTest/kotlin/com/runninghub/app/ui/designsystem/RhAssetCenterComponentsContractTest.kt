package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.billing.MembershipAction
import com.runninghub.app.ui.designsystem.components.billing.MembershipCardState
import com.runninghub.app.ui.designsystem.components.billing.MembershipStatusVisualState
import com.runninghub.app.ui.designsystem.components.billing.TransactionListItemState
import com.runninghub.app.ui.designsystem.components.billing.TransactionStatusVisualState
import com.runninghub.app.ui.designsystem.components.billing.TransactionTypeVisualState
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
            risk = WalletBalanceRiskState.GenerationCapacityUnknown("预计可生成次数待确认"),
            primaryAction = WalletBalanceAction.Recharge("充值"),
            secondaryAction = WalletBalanceAction.Details("明细"),
        )

        assertEquals("12000", state.rhbPointsValue)
        assertEquals("¥248.60", state.walletBalanceValue)
        assertTrue(state.risk is WalletBalanceRiskState.GenerationCapacityUnknown)
        assertEquals("充值", state.primaryAction.label)
        assertEquals("明细", state.secondaryAction.label)
    }

    @Test
    fun `membership card exposes expired renewal state without create placement`() {
        val state = MembershipCardState(
            levelLabel = "会员等级",
            levelName = "RunningHub Pro",
            remainingLabel = "已过期",
            status = MembershipStatusVisualState.Expired,
            benefitSummary = "权益以会员中心为准",
            primaryAction = MembershipAction.Renew("续费"),
        )

        assertEquals(MembershipStatusVisualState.Expired, state.status)
        assertEquals("续费", state.primaryAction.label)
        assertFalse(state.showInsideCreateInput)
    }

    @Test
    fun `transaction list item only enables task detail when related task id exists`() {
        val generation = TransactionListItemState(
            id = "tx-1",
            type = TransactionTypeVisualState.Generation,
            title = "生成消费",
            amount = "-12 RHB",
            status = TransactionStatusVisualState.Succeeded,
            relatedTaskId = "task-1",
        )
        val recharge = TransactionListItemState(
            id = "tx-2",
            type = TransactionTypeVisualState.Recharge,
            title = "充值",
            amount = "+100 CNY",
            status = TransactionStatusVisualState.Pending,
            relatedTaskId = null,
        )

        assertTrue(generation.canOpenTaskDetail)
        assertFalse(recharge.canOpenTaskDetail)
    }
}
