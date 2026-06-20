package com.runninghub.feature.quickcreate.presentation.billing

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 生成快捷创作提交按钮的计费文案。
 *
 * 该函数位于 QuickCreate Presentation 模块，只根据页面已经得到的计费状态生成 UI 文案，
 * 不读取余额、不发起计费请求，也不接触 Data 层。加载态和错误态优先于金额展示，
 * 这样可以避免旧价格预览结果在新请求尚未完成时误导用户提交任务。
 *
 * @param cost 最近一次价格预览得到的现金金额，单位为人民币元；`0.0` 表示无需展示金额。
 * @param feePreviewLoading `true` 表示当前正在确认价格，按钮应提示等待；`false` 表示无进行中的价格请求。
 * @param feePreviewError 最近一次价格预览失败原因；非空时按钮提示价格待确认，避免展示过期金额。
 * @return 可直接展示在提交按钮上的中文文案。
 */
fun quickCreateSendButtonLabel(
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
): String =
    when {
        feePreviewLoading -> "价格确认中"
        feePreviewError != null -> "价格待确认"
        cost > 0.0 -> "¥${quickCreateFormatCashAmount(cost)}"
        else -> "生成"
    }

private fun quickCreateFormatCashAmount(value: Double): String {
    val scaled = (value * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}
