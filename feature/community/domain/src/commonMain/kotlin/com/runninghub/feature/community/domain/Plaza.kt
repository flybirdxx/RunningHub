package com.runninghub.feature.community.domain

/**
 * Plaza 内容标签。
 *
 * 该模型属于 Community Feature Domain，用于描述广场内容筛选维度。
 * Data 层负责把远端树形标签压平成可展示列表，并保留 [childIds] 供 Presentation 在筛选父标签时
 * 展开到可查询的子孙标签 ID；Presentation 不直接依赖远端 DTO 结构。
 *
 * @property id 标签稳定标识。
 * @property name 标签展示名称。
 * @property level 标签层级，0 表示根节点，1 表示一级业务标签。
 * @property enable 标签是否可用。
 * @property childIds 当前标签的子孙标签 ID，顺序按服务端树形结构保留；空集合表示叶子标签或无可用子节点。
 */
data class PlazaTag(
    val id: String,
    val name: String,
    val level: Int = 0,
    val enable: Boolean = true,
    val childIds: List<String> = emptyList(),
)

/**
 * Plaza 创作内容分页。
 *
 * @property page 当前页码，从 1 开始。
 * @property total 服务端可查询的总条数。
 * @property items 当前页创作卡片，顺序保留服务端推荐或排序结果。
 */
data class PlazaCreationPage(
    val page: Int,
    val total: Int,
    val items: List<PlazaCreationCard>,
)

/**
 * Plaza 复用流程可识别的参考媒体类型。
 *
 * 该类型只描述作品结果媒体可否作为快捷创作参考素材使用，不代表最终生成任务类型；
 * QuickCreate 仍需根据当前模型和用户确认后的参数决定实际提交请求。
 */
enum class PlazaReuseMediaKind {
    /** 图片作品，可作为图片参考素材。 */
    IMAGE,

    /** 视频作品，可作为视频参考素材。 */
    VIDEO,

    /** 音频作品，可作为音频参考素材。 */
    AUDIO,

    /** 服务端未返回或无法识别媒体类型。 */
    UNKNOWN,
}

/**
 * Plaza 作品可复用参数快照。
 *
 * 广场列表接口可能只返回部分字段，因此所有参数都允许为空。Presentation 必须把空值展示为缺失项，
 * 不能把缺失模板、SKU、Prompt 或参考图当作完整复用参数，也不能绕过 QuickCreate 的价格确认。
 *
 * @property templateId 可复用模板 ID；为空表示服务端未返回模板绑定。
 * @property skuId 可复用服务 SKU；为空表示服务端未返回 SKU。
 * @property prompt 可编辑 Prompt 初稿；为空表示需要用户在 Create 中补齐。
 * @property aspectRatio 可复用比例协议值，例如 `3:4`；为空表示未知。
 * @property resolution 可复用分辨率协议值；为空表示未知。
 * @property quantity 可复用生成数量；为空表示沿用 Create 当前默认值。
 * @property referenceMediaUrl 可带入 Create 的参考媒体 URL；为空表示没有可复用参考素材。
 * @property referenceMediaType 参考媒体类型；未知时为 [PlazaReuseMediaKind.UNKNOWN]。
 */
data class PlazaCreationReuseSnapshot(
    val templateId: String? = null,
    val skuId: String? = null,
    val prompt: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val quantity: Int? = null,
    val referenceMediaUrl: String? = null,
    val referenceMediaType: PlazaReuseMediaKind = PlazaReuseMediaKind.UNKNOWN,
)

/**
 * Plaza 创作内容卡片。
 *
 * @property id 内容稳定标识。
 * @property intro 内容简介。
 * @property publishTime 发布时间文案，保留服务端格式。
 * @property ownerName 作者名称。
 * @property ownerAvatar 作者头像地址。
 * @property mediaUrl 展示媒体地址。
 * @property mediaType 媒体类型，例如 IMAGE、VIDEO 或 WORKFLOW。
 * @property imageWidth 图片宽度，单位为像素；非图片或未知时为空。
 * @property imageHeight 图片高度，单位为像素；非图片或未知时为空。
 * @property likeCount 点赞数文案，保留服务端格式。
 * @property useCount 使用数文案，保留服务端格式。
 * @property collectCount 收藏数文案，保留服务端格式。
 * @property liked 当前用户是否已点赞。
 * @property collected 当前用户是否已收藏。
 * @property reuseSnapshot 当前作品可复用参数快照；字段可部分缺失，UI 需要明确展示缺失态。
 */
data class PlazaCreationCard(
    val id: String,
    val intro: String? = null,
    val publishTime: String? = null,
    val ownerName: String? = null,
    val ownerAvatar: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val likeCount: String? = null,
    val useCount: String? = null,
    val collectCount: String? = null,
    val liked: Boolean = false,
    val collected: Boolean = false,
    val reuseSnapshot: PlazaCreationReuseSnapshot = PlazaCreationReuseSnapshot(),
)

/**
 * Plaza 短片分类。
 *
 * @property id 服务端分类 ID，可能为空。
 * @property code 分类代码，用于请求短片分页。
 * @property name 分类展示名称。
 */
data class PlazaShortCategory(
    val id: String? = null,
    val code: String,
    val name: String,
)

/**
 * Plaza 短片内容分页。
 *
 * @property page 当前页码，从 1 开始；0 只用于 Presentation 尚未加载任何短片页的初始状态。
 * @property total 服务端声明的短片总条数，单位为条；0 表示没有数据或服务端未返回总数。
 * @property items 当前页短片卡片，顺序保留服务端返回顺序。
 */
data class PlazaShortPage(
    val page: Int,
    val total: Int,
    val items: List<PlazaShortCard>,
)

/**
 * Plaza 短片卡片。
 *
 * @property id 短片稳定标识。
 * @property name 短片名称。
 * @property videoUrl 视频地址。
 * @property thumbnailUrl 缩略图地址。
 * @property durationSeconds 视频时长，单位为秒。
 * @property categoryName 分类名称。
 * @property authorName 作者名称。
 * @property authorAvatar 作者头像地址。
 */
data class PlazaShortCard(
    val id: String,
    val name: String,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val categoryName: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null,
)
