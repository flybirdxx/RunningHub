package com.runninghub.feature.quickcreate.presentation

/**
 * QuickCreate 运行时状态文案端口。
 *
 * 本对象集中提供计费拦截、描述词校验和上传阻塞等运行时错误文案。它位于
 * QuickCreate Presentation 层，供 composeApp 的生命周期适配器和 Coordinator 复用；
 * 这样应用壳不再直接散落最终 UI 文案，后续切换到 Compose Resources 或多语言 TextProvider 时
 * 只需要替换这一处边界。
 */
object QuickCreateRuntimeUiText {
    /** 计费预览仍在确认中时阻止生成的页面错误文案。 */
    val feeConfirming: String = "价格确认中"

    /** 当前提交参数与最近一次计费预览不一致时阻止生成的页面错误文案。 */
    val feePending: String = "价格待确认"

    /** 已有活跃生成任务时阻止重复提交的页面错误文案。 */
    val duplicateGeneration: String = "已有生成任务进行中，请等待当前任务结束"

    /** 描述词为空时阻止生成的页面错误文案。 */
    val promptRequired: String = "请输入描述词"

    /**
     * 描述词超过当前限制时阻止生成的页面错误文案。
     *
     * @param maxChars 允许的最大字符数，单位为 Kotlin 字符数量。
     * @return 可写入 QuickCreate 页面错误状态的超长提示。
     */
    fun promptTooLong(maxChars: Int): String = "描述词不能超过 $maxChars 个字符"

    /** 单个素材上传失败后写入媒体引用的短错误文案。 */
    val mediaUploadFailed: String = "素材上传失败"

    /** 生成前发现相关素材失败、被移除或等待过程中失败时的阻塞文案。 */
    val mediaUploadBlocked: String = "素材上传失败，请重新选择或稍后重试"

    /** 生成前等待相关素材上传超时时的阻塞文案。 */
    val mediaUploadTimeout: String = "素材上传超时，请重新选择或稍后重试"

}
