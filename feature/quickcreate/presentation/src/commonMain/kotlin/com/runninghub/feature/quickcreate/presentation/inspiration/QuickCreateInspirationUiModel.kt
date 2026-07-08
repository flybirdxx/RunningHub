package com.runninghub.feature.quickcreate.presentation.inspiration

import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTag
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplate

/**
 * 快捷创作灵感标签的 Presentation UI 模型。
 *
 * 标签接口返回的 [QuickCreateInspirationTag] 是 Domain 层筛选实体；选中态由 StateHolder 传入，
 * 映射层只负责把当前筛选 ID 转换为稳定视觉状态，避免 Composable 依赖列表下标理解业务规则。
 *
 * @property source 原始标签实体，保留给筛选请求复用。Composable 不应从该对象读取展示文案，应使用 [label]。
 * @property id 标签稳定 ID，来自服务端。
 * 空字符串表示服务端返回异常 ID；当前仅用于未来筛选，不参与列表 key。
 * @property label 标签展示文案，来自服务端标签名称。
 * 空字符串表示服务端返回异常名称，UI 会按空文本渲染以暴露数据问题。
 * @property selected 当前标签是否处于选中视觉态。
 * `true` 表示使用强调背景和文字颜色；`false` 表示普通标签样式。
 */
data class QuickCreateInspirationTagUi(
    val source: QuickCreateInspirationTag,
    val id: String,
    val label: String,
    val selected: Boolean,
)

/**
 * 灵感模板预览区域的稳定渲染类型。
 *
 * 该类型把 `videoUrl`、`coverUrl` 和 `categoryId` 的空值优先级收敛到映射层，
 * Composable 只根据具体子类型选择视频缩略图、图片或占位图标。
 */
sealed interface QuickCreateInspirationPreviewUi {
    /**
     * 视频模板预览。
     *
     * @property url 视频远程地址，来自服务端 `videoUrl`。
     * 映射层只在非空字符串时创建该类型。
     */
    data class Video(val url: String) : QuickCreateInspirationPreviewUi

    /**
     * 图片模板预览。
     *
     * @property url 图片封面远程地址，来自服务端 `coverUrl`。
     * 映射层只在没有视频地址且封面地址非空时创建该类型。
     */
    data class Image(val url: String) : QuickCreateInspirationPreviewUi

    /**
     * 无媒体地址时的占位预览。
     *
     * @property mediaType 占位图标对应的媒体类型。
     * `VIDEO` 表示展示视频图标；`IMAGE` 表示展示图片图标。其他服务端类别统一降级为 `IMAGE`。
     */
    data class Placeholder(val mediaType: QuickCreateInspirationPlaceholderMediaType) :
        QuickCreateInspirationPreviewUi
}

/**
 * 灵感模板占位图标类型。
 */
enum class QuickCreateInspirationPlaceholderMediaType {
    /** 图片或未知类别模板使用的占位图标。 */
    IMAGE,

    /** 视频类别模板使用的占位图标。 */
    VIDEO,
}

/**
 * 灵感模板徽标的视觉语义。
 */
enum class QuickCreateInspirationBadgeTone {
    /** 热门模板徽标，使用错误强调色。 */
    HOT,

    /** 新模板徽标，使用主品牌色。 */
    NEW,
}

/**
 * 灵感模板卡片徽标 UI 模型。
 *
 * @property label 卡片上展示的短文本，例如 `HOT` 或 `NEW`。
 * 空字符串表示异常配置，UI 会直接展示以暴露映射问题。
 * @property tone 徽标视觉语义，用于选择颜色和字重。
 */
data class QuickCreateInspirationBadgeUi(
    val label: String,
    val tone: QuickCreateInspirationBadgeTone,
)

/**
 * 快捷创作灵感模板卡片的 Presentation UI 模型。
 *
 * @property source 原始模板实体，保留给迁移期测试和后续模板筛选功能使用。
 * Composable 不应从该对象读取展示字段，应使用下列稳定字段。
 * @property id 模板稳定 ID，来自服务端 `templateId`，用于列表 key 和模板应用请求。
 * 空字符串表示异常数据；点击时仍会回传该值，由 StateHolder 拒绝空 ID。
 * @property title 模板标题，来自服务端。
 * 空字符串表示服务端未返回标题，UI 不在卡片层自行兜底。
 * @property categoryLabel 模板类别展示文案。
 * 服务端缺失类别时统一降级为 `IMAGE`，保持既有视觉与应用逻辑兼容。
 * @property preview 卡片左侧预览渲染模型。
 * 视频地址优先于图片封面；二者都缺失时根据 [categoryLabel] 生成占位类型。
 * @property badges 卡片徽标列表，顺序固定为 HOT 在前、NEW 在后。
 * 空集合表示模板没有热门或新模板标记。
 */
data class QuickCreateInspirationTemplateUi(
    val source: QuickCreateInspirationTemplate,
    val id: String,
    val title: String,
    val categoryLabel: String,
    val preview: QuickCreateInspirationPreviewUi,
    val badges: List<QuickCreateInspirationBadgeUi>,
)

/**
 * 将 Domain 灵感标签列表映射为页面标签 UI 模型。
 *
 * @param selectedTagId 当前用于请求模板分页的标签 ID；为空时回退选中首个标签，保持初次加载的视觉锚点。
 */
fun List<QuickCreateInspirationTag>.toQuickCreateInspirationTagUiItems(
    selectedTagId: String? = null,
):
    List<QuickCreateInspirationTagUi> =
    mapIndexed { index, tag ->
        val selected = selectedTagId?.let { tag.id == it } ?: (index == 0)
        QuickCreateInspirationTagUi(
            source = tag,
            id = tag.id,
            label = tag.name,
            selected = selected,
        )
    }

/**
 * 将 Domain 灵感模板映射为卡片 UI 模型。
 */
fun QuickCreateInspirationTemplate.toQuickCreateInspirationTemplateUi(): QuickCreateInspirationTemplateUi {
    val categoryLabel = categoryId?.takeIf { it.isNotBlank() } ?: "IMAGE"
    return QuickCreateInspirationTemplateUi(
        source = this,
        id = templateId,
        title = title,
        categoryLabel = categoryLabel,
        preview = toQuickCreateInspirationPreviewUi(categoryLabel),
        badges = toQuickCreateInspirationBadges(),
    )
}

private fun QuickCreateInspirationTemplate.toQuickCreateInspirationPreviewUi(
    categoryLabel: String,
): QuickCreateInspirationPreviewUi {
    val videoPreviewUrl = videoUrl?.takeIf { it.isNotBlank() }
    val imagePreviewUrl = coverUrl?.takeIf { it.isNotBlank() }
    return when {
        videoPreviewUrl != null -> QuickCreateInspirationPreviewUi.Video(videoPreviewUrl)
        imagePreviewUrl != null -> QuickCreateInspirationPreviewUi.Image(imagePreviewUrl)
        else -> QuickCreateInspirationPreviewUi.Placeholder(categoryLabel.toInspirationPlaceholderMediaType())
    }
}

private fun String.toInspirationPlaceholderMediaType(): QuickCreateInspirationPlaceholderMediaType =
    if (equals("VIDEO", ignoreCase = true)) {
        QuickCreateInspirationPlaceholderMediaType.VIDEO
    } else {
        QuickCreateInspirationPlaceholderMediaType.IMAGE
    }

private fun QuickCreateInspirationTemplate.toQuickCreateInspirationBadges(): List<QuickCreateInspirationBadgeUi> =
    buildList {
        if (tagHot) add(QuickCreateInspirationBadgeUi(label = "HOT", tone = QuickCreateInspirationBadgeTone.HOT))
        if (tagNew) add(QuickCreateInspirationBadgeUi(label = "NEW", tone = QuickCreateInspirationBadgeTone.NEW))
    }
