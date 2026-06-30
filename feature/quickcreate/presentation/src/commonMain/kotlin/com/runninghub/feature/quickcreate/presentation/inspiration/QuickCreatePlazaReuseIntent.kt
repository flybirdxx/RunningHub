package com.runninghub.feature.quickcreate.presentation.inspiration

/**
 * Plaza 复用入口传给 QuickCreate 的参考媒体类型。
 *
 * 该类型由 composeApp 从 Community Presentation 的作品详情映射而来，避免 QuickCreate Presentation
 * 直接依赖 Community 模块。
 */
enum class QuickCreatePlazaReuseMediaKind {
    /** 图片参考素材。 */
    IMAGE,

    /** 视频参考素材。 */
    VIDEO,

    /** 音频参考素材。 */
    AUDIO,
}

/**
 * Plaza 使用同款进入 QuickCreate 的参数意图。
 *
 * 所有可复用参数都允许为空，QuickCreate 只写入已知的可编辑字段，并通过既有计费预览回调重新确认价格；
 * 本意图不代表可直接提交生成。
 *
 * @property sourceWorkId Plaza 作品 ID，用于追踪引用来源。
 * @property sourceAuthorName Plaza 作者名称，保留给后续来源说明或审计，不作为生成参数提交。
 * @property templateId 可复用模板 ID；为空时沿用当前已选模型。
 * @property skuId 可复用服务 SKU；为空时沿用当前已选模型。
 * @property prompt 可编辑 Prompt 初稿。
 * @property aspectRatio 比例协议值，例如 `3:4`。
 * @property resolution 分辨率协议值，例如 `1K`、`720P`。
 * @property quantity 生成数量；为空时沿用当前配置。
 * @property referenceMediaUrl 可带入的参考媒体 URL。
 * @property referenceMediaKind 参考媒体类型；为空时不写入参考素材。
 */
data class QuickCreatePlazaReuseIntent(
    val sourceWorkId: String,
    val sourceAuthorName: String? = null,
    val templateId: String? = null,
    val skuId: String? = null,
    val prompt: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val quantity: Int? = null,
    val referenceMediaUrl: String? = null,
    val referenceMediaKind: QuickCreatePlazaReuseMediaKind? = null,
)
