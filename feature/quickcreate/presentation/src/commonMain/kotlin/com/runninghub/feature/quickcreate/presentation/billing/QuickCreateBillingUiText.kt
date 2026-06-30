package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 快捷创作计费金额的展示单位语义。
 *
 * Presentation 只保存稳定单位，不保存最终文案；composeApp 会把单位映射为中文资源。
 */
enum class QuickCreateBillingUnit {
    /** 创作消耗使用的 RunningHub 点数单位。 */
    RhbPoints,

    /** 钱包现金余额或人民币金额单位。 */
    CnyCash,
}

/**
 * 快捷创作计费金额的稳定展示值。
 *
 * @property amount 已格式化但不含单位文案的数值，例如 `37` 或 `2.00`。
 * @property unit 金额单位语义，由 composeApp 映射为最终展示文案。
 */
data class QuickCreateBillingAmount(
    val amount: String,
    val unit: QuickCreateBillingUnit,
)

/**
 * fee-preview 返回后可安全进入 UI 状态的结算摘要。
 *
 * 该模型不保存服务端原始错误或结算模式原文，只保存 UI 需要展示和判断的金额、余额与余额不足状态。
 */
data class QuickCreateBillingPreviewUi(
    val requiredRhAmount: Double = 0.0,
    val requiredCashAmount: Double = 0.0,
    val userCashBalance: Double? = null,
    val cashCurrency: String? = null,
    val free: Boolean = false,
    val insufficient: Boolean = false,
)

/**
 * 生成前价格徽标的稳定状态。
 *
 * 该状态只表达计费展示语义，不包含最终用户文案或服务端 message。
 */
sealed interface QuickCreatePriceBadgeState {
    /** 价格仍在确认中。 */
    data object Loading : QuickCreatePriceBadgeState

    /** 价格预览暂不可用，需要用户等待或重新触发预览。 */
    data object Pending : QuickCreatePriceBadgeState

    /** 本次生成免费。 */
    data object Free : QuickCreatePriceBadgeState

    /** 已获得可展示价格。 */
    data class Amount(
        val billingAmount: QuickCreateBillingAmount,
    ) : QuickCreatePriceBadgeState

    /** 远端预览确认余额不足或业务条件不允许提交。 */
    data object Insufficient : QuickCreatePriceBadgeState
}

/**
 * 生成确认弹层需要展示的结算状态。
 *
 * @property priceBadge 预计消耗的价格徽标状态。
 * @property currentBalance 当前现金余额；为空表示服务端未返回或尚未同步。
 * @property requiresConfirmation `true` 表示当前生成必须先经用户确认。
 * @property canConfirm `true` 表示价格已确认且没有余额不足或预览失败。
 */
data class QuickCreateGenerationConfirmState(
    val priceBadge: QuickCreatePriceBadgeState,
    val currentBalance: QuickCreateBillingAmount?,
    val requiresConfirmation: Boolean,
    val canConfirm: Boolean,
)

/**
 * 快捷创作提交按钮的稳定展示状态。
 *
 * 该模型位于 QuickCreate Presentation 模块，只表达按钮应展示哪一种业务状态，
 * 不保存最终中文文案。composeApp 负责把状态映射到 Compose Resources，避免 Feature
 * Presentation 继续持有用户可见文案。
 */
sealed interface QuickCreateSendButtonLabel {
    /**
     * 计费预览仍在刷新中，按钮应提示用户等待价格确认。
     */
    data object Confirming : QuickCreateSendButtonLabel

    /**
     * 计费预览失败或价格已失效，按钮应提示价格待确认。
     */
    data object Pending : QuickCreateSendButtonLabel

    /**
     * 当前计费预览可用且需要展示现金金额。
     *
     * @property cashAmount 已格式化但不含货币符号的现金金额，单位为人民币元。
     * 字符串固定保留两位小数；空字符串不应出现，负数仅用于异常服务端价格排查。
     */
    data class Amount(
        val cashAmount: String,
    ) : QuickCreateSendButtonLabel

    /**
     * 当前不需要展示价格，按钮展示普通生成动作。
     */
    data object Generate : QuickCreateSendButtonLabel
}

/**
 * 生成快捷创作提交按钮的计费展示状态。
 *
 * 该函数位于 QuickCreate Presentation 模块，只根据页面已经得到的计费状态生成稳定状态，
 * 不读取余额、不发起计费请求，也不接触 Data 层。加载态和错误态优先于金额展示，
 * 这样可以避免旧价格预览结果在新请求尚未完成时误导用户提交任务。
 *
 * @param cost 最近一次价格预览得到的现金金额，单位为人民币元；`0.0` 表示无需展示金额。
 * @param feePreviewLoading `true` 表示当前正在确认价格，按钮应提示等待；`false` 表示无进行中的价格请求。
 * @param feePreviewError 最近一次价格预览失败原因语义；非空时按钮提示价格待确认，避免展示过期金额。
 * @return 可由 composeApp 映射为最终文案的稳定展示状态。
 */
fun quickCreateSendButtonLabel(
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
): QuickCreateSendButtonLabel =
    when {
        feePreviewLoading -> QuickCreateSendButtonLabel.Confirming
        feePreviewError != null -> QuickCreateSendButtonLabel.Pending
        cost > 0.0 -> QuickCreateSendButtonLabel.Amount(quickCreateFormatCashAmount(cost))
        else -> QuickCreateSendButtonLabel.Generate
    }

fun quickCreatePriceBadgeState(
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    billingPreview: QuickCreateBillingPreviewUi? = null,
): QuickCreatePriceBadgeState =
    when {
        feePreviewLoading -> QuickCreatePriceBadgeState.Loading
        feePreviewError == QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage() ||
            billingPreview?.insufficient == true -> QuickCreatePriceBadgeState.Insufficient
        feePreviewError != null -> QuickCreatePriceBadgeState.Pending
        billingPreview?.free == true -> QuickCreatePriceBadgeState.Free
        billingPreview != null -> billingPreview.quickCreatePriceBadgeState()
        cost > 0.0 -> QuickCreatePriceBadgeState.Amount(
            QuickCreateBillingAmount(
                amount = quickCreateFormatCashAmount(cost),
                unit = QuickCreateBillingUnit.CnyCash,
            ),
        )
        else -> QuickCreatePriceBadgeState.Free
    }

fun QuickCreationFeePreview.toQuickCreateBillingPreviewUi(): QuickCreateBillingPreviewUi =
    QuickCreateBillingPreviewUi(
        requiredRhAmount = requiredRhAmount,
        requiredCashAmount = requiredCashAmount,
        userCashBalance = userCashBalance.takeIf { it > 0.0 },
        cashCurrency = cashCurrency,
        free = free,
        insufficient = !passed || insufficientType != null,
    )

fun quickCreateGenerationConfirmState(
    state: QuickCreateUiState,
): QuickCreateGenerationConfirmState {
    val priceBadge = quickCreatePriceBadgeState(
        cost = state.estimatedCost,
        feePreviewLoading = state.feePreviewLoading,
        feePreviewError = state.feePreviewError,
        billingPreview = state.billingPreview,
    )
    val currentBalance = state.billingPreview
        ?.userCashBalance
        ?.let { balance ->
            QuickCreateBillingAmount(
                amount = quickCreateFormatCashAmount(balance),
                unit = QuickCreateBillingUnit.CnyCash,
            )
        }
    return QuickCreateGenerationConfirmState(
        priceBadge = priceBadge,
        currentBalance = currentBalance,
        requiresConfirmation = priceBadge is QuickCreatePriceBadgeState.Amount,
        canConfirm = !state.feePreviewLoading &&
            state.feePreviewError == null &&
            priceBadge !is QuickCreatePriceBadgeState.Insufficient &&
            priceBadge !is QuickCreatePriceBadgeState.Pending,
    )
}

fun QuickCreateUiState.requiresQuickCreateGenerationConfirmation(): Boolean =
    quickCreateGenerationConfirmState(this).requiresConfirmation

private fun quickCreateFormatCashAmount(value: Double): String {
    val scaled = (value * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

private fun QuickCreateBillingPreviewUi.quickCreatePriceBadgeState(): QuickCreatePriceBadgeState =
    when {
        requiredRhAmount > 0.0 -> QuickCreatePriceBadgeState.Amount(
            QuickCreateBillingAmount(
                amount = requiredRhAmount.quickCreateFormatRhAmount(),
                unit = QuickCreateBillingUnit.RhbPoints,
            ),
        )
        requiredCashAmount > 0.0 -> QuickCreatePriceBadgeState.Amount(
            QuickCreateBillingAmount(
                amount = quickCreateFormatCashAmount(requiredCashAmount),
                unit = QuickCreateBillingUnit.CnyCash,
            ),
        )
        else -> QuickCreatePriceBadgeState.Free
    }

private fun Double.quickCreateFormatRhAmount(): String {
    val scaled = (this * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    val whole = absolute / 100
    val cents = absolute % 100
    return if (cents == 0) {
        "$sign$whole"
    } else {
        "$sign$whole.${cents.toString().padStart(2, '0')}"
    }
}
