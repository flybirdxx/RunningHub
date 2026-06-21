package com.runninghub.core.model

/**
 * 作品封面资源的媒体类型。
 *
 * 该模型位于 core:model 层，只表达跨端业务语义，不携带具体播放器、图片加载器或平台类型。
 */
enum class CoverMediaType {
    /** 静态图片封面。 */
    IMAGE,

    /** 动图封面，通常仍由图片组件展示。 */
    GIF,

    /** 视频封面，需要 Presentation 根据平台能力选择播放或降级展示。 */
    VIDEO,
}

/**
 * 发现页和作品列表中展示的应用/工作流摘要。
 *
 * 这是跨端共享的业务模型，字段值由 Data 层从远端 DTO 映射而来。计数和尺寸当前仍沿用服务端字符串格式，
 * Presentation 只能按展示需要格式化，不应把它们再作为协议参数传回 Data 层。
 *
 * @property id 业务唯一 ID，用于分页去重、详情跳转和收藏等操作。
 * @property title 用户可见标题，Data 层应保证缺失时给出可展示兜底。
 * @property description 作品简介，服务端可能返回空值。
 * @property thumbnailUrl 缩略图 URL，可能为空；UI 需要提供占位图。
 * @property coverUrl 主封面 URL，可能是图片、GIF 或视频封面地址。
 * @property coverMediaType 封面媒体类型，决定 Presentation 的展示策略。
 * @property videoUrl 视频资源 URL，仅视频封面或短片类内容可能存在。
 * @property coverWidth 服务端返回的封面宽度字符串，单位和格式由服务端决定。
 * @property coverHeight 服务端返回的封面高度字符串，单位和格式由服务端决定。
 * @property author 作者摘要，匿名或异常数据下可能为空。
 * @property tags 关联标签摘要，列表顺序保留服务端推荐权重。
 * @property likeCount 点赞数字符串，当前不在 core 层解析为数字以避免格式丢失。
 * @property collectCount 收藏数字符串。
 * @property useCount 使用次数或运行次数的展示字符串。
 * @property pv 浏览量展示字符串。
 * @property carefullyChosen 是否为运营精选内容。
 */
data class WebApp(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val coverUrl: String?,
    val coverMediaType: CoverMediaType,
    val videoUrl: String? = null,
    val coverWidth: String?,
    val coverHeight: String?,
    val author: Author?,
    val tags: List<TagSimple>,
    val likeCount: String,
    val collectCount: String,
    val useCount: String,
    val pv: String,
    val carefullyChosen: Boolean = false,
)

/**
 * 内容作者的跨端展示信息。
 *
 * @property id 作者 ID；匿名内容或兼容旧数据时可能为空。
 * @property name 作者昵称，Presentation 需要处理空值兜底。
 * @property avatar 头像 URL，可能为空。
 * @property intro 作者简介，服务端可能返回空字符串或空值。
 * @property followCount 关注数字符串，保持服务端展示格式。
 * @property fansCount 粉丝数字符串，保持服务端展示格式。
 * @property likeCount 作者获赞数字符串。
 * @property collectCount 作者作品被收藏数字符串。
 * @property bgImage 作者主页背景图 URL，可能为空。
 */
data class Author(
    val id: String?,
    val name: String?,
    val avatar: String?,
    val intro: String?,
    val followCount: String,
    val fansCount: String,
    val likeCount: String,
    val collectCount: String,
    val bgImage: String?
)

/**
 * 列表和详情中使用的轻量标签信息。
 *
 * @property id 标签 ID，用于筛选、跳转和去重。
 * @property name 中文或默认展示名。
 * @property nameEn 英文展示名，缺失时由 Presentation 继续使用 [name]。
 * @property labels 服务端附加标签串，当前仅作为展示或兼容字段保留。
 */
data class TagSimple(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val labels: String? = null
)

/**
 * 详情页封面资源。
 *
 * @property url 封面 URL，可能为空；UI 应提供占位或跳过展示。
 * @property imageWidth 图片宽度字符串，单位和格式保持服务端原样。
 * @property imageHeight 图片高度字符串，单位和格式保持服务端原样。
 */
data class Cover(
    val url: String?,
    val imageWidth: String?,
    val imageHeight: String?
)

/**
 * 作品统计摘要。
 *
 * @property likeCount 点赞数字符串。
 * @property collectCount 收藏数字符串。
 * @property useCount 使用次数或运行次数字符串。
 * @property pv 浏览量字符串。
 */
data class StatisticsInfo(
    val likeCount: String,
    val collectCount: String,
    val useCount: String,
    val pv: String
)
