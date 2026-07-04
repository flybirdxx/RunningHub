package com.runninghub.feature.community.presentation

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaReuseMediaKind

/**
 * Plaza 作品预览类型。
 *
 * Presentation 先把服务端媒体类型归一到该枚举，composeApp 只根据该类型选择图片、视频或占位渲染，
 * 不直接把远端 fileType 字符串作为用户主要信息展示。
 */
enum class PlazaWorkPreviewType {
    /** 图片预览。 */
    Image,

    /** 视频预览。 */
    Video,

    /** 音频或未知媒体的占位预览。 */
    Placeholder,
}

/** Plaza 作品卡片主动作。 */
enum class PlazaWorkCardPrimaryAction {
    /** 打开使用同款确认流程。 */
    UseSame,
}

/** Plaza 作品详情主动作。 */
enum class PlazaWorkDetailPrimaryAction {
    /** 使用同款参数进入快捷创作生成流程。 */
    UseSameGenerate,
}

/** Plaza 复用参数的可用状态。 */
enum class PlazaReuseParameterStatus {
    /** 当前参数已知，可带入 Create。 */
    Available,

    Missing,
}

/**
 * Plaza 作品预览 UI 模型。
 *
 * @property url 远端媒体地址；为空表示只能展示占位。
 * @property type 媒体预览类型。
 */
data class PlazaWorkPreviewUiModel(
    val url: String?,
    val type: PlazaWorkPreviewType,
)

/**
 * Plaza 单项复用参数 UI 模型。
 *
 * @property value 已知参数值；为空且 [status] 为 [PlazaReuseParameterStatus.Missing] 时表示缺失。
 * @property status 当前参数是否可用。
 */
data class PlazaReuseParameterUiModel(
    val value: String?,
    val status: PlazaReuseParameterStatus,
)

/**
 * Plaza 作品可复用参数摘要。
 *
 * 字段固定为产品允许复用和用户可修改的核心参数。UI 只展示接口实际返回的可用项，
 * 缺失项不得被文案包装成后续填写任务。
 */
data class PlazaReuseSummaryUiModel(
    val modelTemplate: PlazaReuseParameterUiModel,
    val prompt: PlazaReuseParameterUiModel,
    val aspectRatio: PlazaReuseParameterUiModel,
    val resolution: PlazaReuseParameterUiModel,
    val referenceMedia: PlazaReuseParameterUiModel,
)

/**
 * Plaza 作品卡片 UI 模型。
 *
 * @property source 原始 Domain 卡片，保留给 composeApp 适配到 QuickCreate 复用意图；UI 展示应使用稳定字段。
 * @property sourceProtected 是否需要在复用流程中展示作者来源保护说明。
 */
data class PlazaWorkCardUiModel(
    val source: PlazaCreationCard,
    val id: String,
    val title: String,
    val authorName: String?,
    val preview: PlazaWorkPreviewUiModel,
    val useCount: String?,
    val ownerId: String?,
    val ownerAvatar: String?,
    val likeCount: String?,
    val aspectRatio: Float?,
    val reuseSummary: PlazaReuseSummaryUiModel,
    val primaryAction: PlazaWorkCardPrimaryAction = PlazaWorkCardPrimaryAction.UseSame,
    val sourceProtected: Boolean = true,
)

/**
 * Plaza 作品详情 UI 模型。
 *
 * @property source 原始 Domain 卡片，保留给 composeApp 进入 QuickCreate 前做参数适配。
 * @property sourceProtected 是否保留作者和作品来源说明。
 */
data class PlazaWorkDetailUiModel(
    val source: PlazaCreationCard,
    val id: String,
    val title: String,
    val authorName: String?,
    val authorAvatar: String?,
    val preview: PlazaWorkPreviewUiModel,
    val reuseSummary: PlazaReuseSummaryUiModel,
    val primaryAction: PlazaWorkDetailPrimaryAction = PlazaWorkDetailPrimaryAction.UseSameGenerate,
    val sourceProtected: Boolean = true,
)

internal fun PlazaCreationCard.toPlazaWorkCardUiModel(): PlazaWorkCardUiModel =
    PlazaWorkCardUiModel(
        source = this,
        id = id,
        title = intro?.takeIf { it.isNotBlank() } ?: id,
        authorName = ownerName,
        preview = toPlazaWorkPreviewUiModel(),
        useCount = useCount,
        ownerId = ownerId,
        ownerAvatar = ownerAvatar,
        likeCount = likeCount,
        aspectRatio = plazaCardAspectRatio(imageWidth, imageHeight),
        reuseSummary = toPlazaReuseSummaryUiModel(),
    )

/**
 * 计算 Plaza 瀑布流作品卡片的宽高比。
 *
 * 当 [width] 或 [height] 为空或非正数时返回 null，表示无法确定卡片比例；
 * 否则用宽除以高得到比例，并夹紧到 0.6f 到 1.4f，避免极端长宽卡片破坏瀑布流布局。
 */
internal fun plazaCardAspectRatio(width: Int?, height: Int?): Float? {
    if (width == null || height == null || width <= 0 || height <= 0) return null
    return (width.toFloat() / height.toFloat()).coerceIn(0.6f, 1.4f)
}

internal fun PlazaCreationCard.toPlazaWorkDetailUiModel(): PlazaWorkDetailUiModel =
    PlazaWorkDetailUiModel(
        source = this,
        id = id,
        title = intro?.takeIf { it.isNotBlank() } ?: id,
        authorName = ownerName,
        authorAvatar = ownerAvatar,
        preview = toPlazaWorkPreviewUiModel(),
        reuseSummary = toPlazaReuseSummaryUiModel(),
    )

private fun PlazaCreationCard.toPlazaWorkPreviewUiModel(): PlazaWorkPreviewUiModel =
    PlazaWorkPreviewUiModel(
        url = mediaUrl?.takeIf { it.isNotBlank() },
        type = reuseSnapshot.referenceMediaType.toPlazaWorkPreviewType(mediaType),
    )

private fun PlazaCreationCard.toPlazaReuseSummaryUiModel(): PlazaReuseSummaryUiModel =
    PlazaReuseSummaryUiModel(
        modelTemplate = listOfNotNull(reuseSnapshot.templateId, reuseSnapshot.skuId)
            .joinToString(" / ")
            .toParameter(),
        prompt = reuseSnapshot.prompt.toParameter(),
        aspectRatio = reuseSnapshot.aspectRatio.toParameter(),
        resolution = reuseSnapshot.resolution.toParameter(),
        referenceMedia = reuseSnapshot.referenceMediaUrl.toParameter(),
    )

private fun String?.toParameter(): PlazaReuseParameterUiModel {
    val value = this?.takeIf { it.isNotBlank() }
    return PlazaReuseParameterUiModel(
        value = value,
        status = if (value == null) PlazaReuseParameterStatus.Missing else PlazaReuseParameterStatus.Available,
    )
}

private fun PlazaReuseMediaKind.toPlazaWorkPreviewType(rawMediaType: String?): PlazaWorkPreviewType =
    when (this) {
        PlazaReuseMediaKind.IMAGE -> PlazaWorkPreviewType.Image
        PlazaReuseMediaKind.VIDEO -> PlazaWorkPreviewType.Video
        PlazaReuseMediaKind.AUDIO -> PlazaWorkPreviewType.Placeholder
        PlazaReuseMediaKind.UNKNOWN -> rawMediaType.toPlazaWorkPreviewType()
    }

private fun String?.toPlazaWorkPreviewType(): PlazaWorkPreviewType {
    val marker = this?.uppercase().orEmpty()
    return when {
        marker.contains("VIDEO") || marker.contains("MP4") || marker.contains("MOV") -> PlazaWorkPreviewType.Video
        marker.contains("IMAGE") ||
            marker.contains("PNG") ||
            marker.contains("JPG") ||
            marker.contains("JPEG") ||
            marker.contains("WEBP") -> PlazaWorkPreviewType.Image
        else -> PlazaWorkPreviewType.Placeholder
    }
}
