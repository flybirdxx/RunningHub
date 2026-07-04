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

/** 发现页 App 卡片辅助指标类型。 */
enum class DiscoveryAppCardMetricKind {
    USE_COUNT,
    VIEW_COUNT,
    LIKE_COUNT,
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
 * 发现页 App 卡片的辅助指标语义。
 *
 * @property kind 指标类型，例如使用次数、浏览次数或点赞次数。
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
 * @property metrics 真实辅助指标，按展示优先级排序（0..2 条）。
 * @property featured 是否为运营精选内容。
 */
data class DiscoveryAppCardUiModel(
    val id: String,
    val templateName: String,
    val capability: DiscoveryAppCapability,
    val preview: DiscoveryAppPreviewUi,
    val metrics: List<DiscoveryAppCardMetricUi>,
    val featured: Boolean,
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
        metrics = buildMetrics(),
        featured = carefullyChosen,
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
        searchableText.containsAny("video", "影片", "视频", "短片", "运镜") ->
            DiscoveryAppCapability.VIDEO
        searchableText.containsAny("audio", "music", "voice", "音频", "音乐", "配音") ->
            DiscoveryAppCapability.AUDIO
        searchableText.containsAny("image", "photo", "图片", "照片", "海报", "图像") ->
            DiscoveryAppCapability.IMAGE
        else -> DiscoveryAppCapability.GENERAL
    }
}

/** 构建卡片真实指标：优先展示使用/浏览次数，再追加点赞次数。 */
private fun WebApp.buildMetrics(): List<DiscoveryAppCardMetricUi> = buildList {
    useCount.takeIf { it.isNotBlank() }?.let { value ->
        add(DiscoveryAppCardMetricUi(kind = DiscoveryAppCardMetricKind.USE_COUNT, value = value))
    } ?: pv.takeIf { it.isNotBlank() }?.let { value ->
        add(DiscoveryAppCardMetricUi(kind = DiscoveryAppCardMetricKind.VIEW_COUNT, value = value))
    }
    likeCount.takeIf { it.isNotBlank() }?.let { value ->
        add(DiscoveryAppCardMetricUi(kind = DiscoveryAppCardMetricKind.LIKE_COUNT, value = value))
    }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }
