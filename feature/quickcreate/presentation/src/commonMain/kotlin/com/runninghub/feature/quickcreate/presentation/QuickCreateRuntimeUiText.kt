package com.runninghub.feature.quickcreate.presentation

/**
 * QuickCreate 页面可展示消息的稳定语义承载类型。
 *
 * 本类型用于替代直接写入 [String] 的页面错误槽。运行时阻塞、上传失败和 Presentation 错误
 * 均必须先建模为稳定语义，再由 composeApp 映射最终文案；不得把最终中文文案或远端异常摘要写入本类型。
 */
sealed interface QuickCreateUiMessage {
    /**
     * 已完成语义化的运行时提示。
     *
     * @property text 由 feature presentation 产生的稳定提示语义，最终中文文案由 composeApp 资源层映射。
     */
    data class RuntimeText(
        val text: QuickCreateRuntimeUiText,
    ) : QuickCreateUiMessage

    /**
     * 已完成语义化的错误提示。
     *
     * @property error Presentation 层稳定错误语义，最终展示文案由 composeApp 资源层映射。
     */
    data class PresentationErrorText(
        val error: QuickCreatePresentationError,
    ) : QuickCreateUiMessage

}

/**
 * QuickCreate 生成入口和上传流程的稳定运行时提示语义。
 *
 * 该类型位于 feature presentation 层，只表达阻塞或失败原因，不保存最终中文 UI 文案。
 * composeApp 负责把这些语义映射到 Compose Resources，从而让后续多语言和文案调整不再修改状态机。
 */
sealed interface QuickCreateRuntimeUiText {
    /** 计费预览仍在确认中时阻止生成。 */
    data object FeeConfirming : QuickCreateRuntimeUiText

    /** 当前提交参数与最近一次计费预览不一致，或计费预览失败后需要重新确认价格。 */
    data object FeePending : QuickCreateRuntimeUiText

    /** 已有活跃生成任务时阻止重复提交。 */
    data object DuplicateGeneration : QuickCreateRuntimeUiText

    /** 描述词为空时阻止生成。 */
    data object PromptRequired : QuickCreateRuntimeUiText

    /**
     * 描述词超过当前限制时阻止生成。
     *
     * @property maxChars 允许的最大字符数，单位为 Kotlin 字符数量；必须大于 `0`。
     */
    data class PromptTooLong(
        val maxChars: Int,
    ) : QuickCreateRuntimeUiText

    /** 单个素材上传失败后写入媒体引用。 */
    data object MediaUploadFailed : QuickCreateRuntimeUiText

    /** 生成前发现相关素材失败、被移除或等待过程中失败。 */
    data object MediaUploadBlocked : QuickCreateRuntimeUiText

    /** 生成前等待相关素材上传超时。 */
    data object MediaUploadTimeout : QuickCreateRuntimeUiText

    /**
     * 服务字段缺少必填值。
     *
     * @property fieldTitle 服务端字段标题；为空标题已在 Domain 层使用字段 key 兜底。
     */
    data class ServiceFieldRequired(
        val fieldTitle: String,
    ) : QuickCreateRuntimeUiText

    /**
     * 服务字段当前值不属于服务端候选项。
     *
     * @property fieldTitle 服务端字段标题；为空标题已在 Domain 层使用字段 key 兜底。
     */
    data class ServiceFieldInvalidOption(
        val fieldTitle: String,
    ) : QuickCreateRuntimeUiText

    /**
     * 服务文本字段未达到最小长度。
     *
     * @property fieldTitle 服务端字段标题；为空标题已在 Domain 层使用字段 key 兜底。
     * @property minLength 最少字符数，单位为 Kotlin 字符数量。
     */
    data class ServiceFieldMinLength(
        val fieldTitle: String,
        val minLength: Int,
    ) : QuickCreateRuntimeUiText

    /**
     * 服务上传字段超过最大文件数。
     *
     * @property fieldTitle 服务端字段标题；为空标题已在 Domain 层使用字段 key 兜底。
     * @property maxCount 最多文件数。
     */
    data class ServiceUploadMaxCount(
        val fieldTitle: String,
        val maxCount: Int,
    ) : QuickCreateRuntimeUiText
}

/**
 * 把已治理的运行时提示语义包装为页面消息。
 *
 * @return 可写入 [com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState.error]
 * 或媒体引用错误槽的稳定页面消息。
 */
internal fun QuickCreateRuntimeUiText.asQuickCreateUiMessage(): QuickCreateUiMessage =
    QuickCreateUiMessage.RuntimeText(this)
