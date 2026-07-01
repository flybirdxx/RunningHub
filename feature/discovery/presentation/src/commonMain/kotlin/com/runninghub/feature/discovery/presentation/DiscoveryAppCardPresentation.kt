package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.WebApp

/** 发现页 App 卡片面向创作入口的能力类型。 */
enum class DiscoveryAppCapability {
    IMAGE,
    VIDEO,
    AUDIO,
    GENERAL,
}

/** 发现页 App 卡片结果预览的媒体类型。 */
enum class DiscoveryAppPreviewType {
    IMAGE,
    VIDEO,
    AUDIO,
    EMPTY,
}

/** 发现页 App 卡片预计费用的稳定状态。 */
enum class DiscoveryAppEstimatedCostKind {
    UNKNOWN,
}

/** 发现页 App 卡片辅助指标类型。 */
enum class DiscoveryAppCardMetricKind {
    USE_COUNT,
    VIEW_COUNT,
}

/** 发现页 App 卡片主操作。 */
enum class DiscoveryAppCardPrimaryAction {
    VIEW_DETAIL,
    GENERATE,
}

/**
 * 发现页 App 卡片的结果预览语义。
 *
 * @property url 可展示的预览资源地址；为空时页面应展示稳定占位。
 * @property type 预览资源类型，调用方据此选择图片、视频或占位样式。
 */
data class DiscoveryAppPreviewUi(
    val url: String?,
    val type: DiscoveryAppPreviewType,
)

/**
 * 发现页 App 卡片的预计费用语义。
 *
 * @property kind 当前费用状态。RM-11 暂无稳定费用协议时使用 [DiscoveryAppEstimatedCostKind.UNKNOWN]。
 * @property amountLabel 已格式化费用摘要；为空表示需要应用壳映射“运行前确认费用”资源文案。
 */
data class DiscoveryAppEstimatedCostUi(
    val kind: DiscoveryAppEstimatedCostKind,
    val amountLabel: String? = null,
)

/**
 * 发现页 App 卡片的辅助指标语义。
 *
 * @property kind 指标类型，例如使用次数或浏览次数。
 * @property value 服务端返回的展示数字，保持原格式。
 */
data class DiscoveryAppCardMetricUi(
    val kind: DiscoveryAppCardMetricKind,
    val value: String,
)

/**
 * 发现页创作入口卡片 UI 模型。
 *
 * @property id WebApp ID，用于详情跳转和后续生成入口。
 * @property templateName 普通用户可理解的模板名称。
 * @property capability 卡片能力类型，避免把 API、工作流 ID 或节点名作为首要信息。
 * @property preview 结果预览资源。
 * @property estimatedCost 预计费用状态。
 * @property supportingMetric 使用或浏览指标。
 * @property primaryAction 卡片主操作语义。
 */
data class DiscoveryAppCardUiModel(
    val id: String,
    val templateName: String,
    val capability: DiscoveryAppCapability,
    val preview: DiscoveryAppPreviewUi,
    val estimatedCost: DiscoveryAppEstimatedCostUi,
    val supportingMetric: DiscoveryAppCardMetricUi?,
    val primaryAction: DiscoveryAppCardPrimaryAction,
)

/** 把目录 WebApp 摘要转换为发现页创作入口卡片语义。 */
fun WebApp.toDiscoveryAppCardUiModel(): DiscoveryAppCardUiModel {
    val capability = inferCapability()
    return DiscoveryAppCardUiModel(
        id = id,
        templateName = title,
        capability = capability,
        preview = DiscoveryAppPreviewUi(
            url = when (capability) {
                DiscoveryAppCapability.VIDEO -> videoUrl ?: coverUrl ?: thumbnailUrl
                else -> coverUrl ?: thumbnailUrl ?: videoUrl
            },
            type = when {
                capability == DiscoveryAppCapability.AUDIO -> DiscoveryAppPreviewType.AUDIO
                capability == DiscoveryAppCapability.VIDEO -> DiscoveryAppPreviewType.VIDEO
                coverUrl.isNullOrBlank() && thumbnailUrl.isNullOrBlank() && videoUrl.isNullOrBlank() -> DiscoveryAppPreviewType.EMPTY
                else -> DiscoveryAppPreviewType.IMAGE
            },
        ),
        estimatedCost = DiscoveryAppEstimatedCostUi(kind = DiscoveryAppEstimatedCostKind.UNKNOWN),
        supportingMetric = supportingMetric(),
        primaryAction = if (id.isBlank()) {
            DiscoveryAppCardPrimaryAction.VIEW_DETAIL
        } else {
            DiscoveryAppCardPrimaryAction.GENERATE
        },
    )
}

private fun WebApp.inferCapability(): DiscoveryAppCapability {
    val searchableText = buildList {
        add(title)
        description?.let(::add)
        tags.forEach { tag ->
            add(tag.name)
            tag.nameEn?.let(::add)
            tag.labels?.let(::add)
        }
    }.joinToString(" ").lowercase()

    return when {
        coverMediaType == CoverMediaType.VIDEO || !videoUrl.isNullOrBlank() -> DiscoveryAppCapability.VIDEO
        searchableText.containsAny("video", "\u5f71\u7247", "\u89c6\u9891", "\u77ed\u7247", "\u8fd0\u955c") ->
            DiscoveryAppCapability.VIDEO
        searchableText.containsAny("audio", "music", "voice", "\u97f3\u9891", "\u97f3\u4e50", "\u914d\u97f3") ->
            DiscoveryAppCapability.AUDIO
        searchableText.containsAny("image", "photo", "\u56fe\u7247", "\u7167\u7247", "\u6d77\u62a5", "\u56fe\u50cf") ->
            DiscoveryAppCapability.IMAGE
        else -> DiscoveryAppCapability.GENERAL
    }
}

private fun WebApp.supportingMetric(): DiscoveryAppCardMetricUi? {
    useCount.takeIf { it.isNotBlank() }?.let { value ->
        return DiscoveryAppCardMetricUi(
            kind = DiscoveryAppCardMetricKind.USE_COUNT,
            value = value,
        )
    }
    return pv.takeIf { it.isNotBlank() }?.let { value ->
        DiscoveryAppCardMetricUi(
            kind = DiscoveryAppCardMetricKind.VIEW_COUNT,
            value = value,
        )
    }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }
