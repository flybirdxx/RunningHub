package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import kotlin.math.abs
import kotlin.math.roundToInt

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

private fun quickCreateFormatCashAmount(value: Double): String {
    val scaled = (value * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}
