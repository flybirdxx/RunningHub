package com.runninghub.feature.community.domain

/**
 * Plaza 内容标签。
 *
 * 该模型属于 Community Feature Domain，用于描述广场内容筛选维度。
 * Data 层负责把远端树形标签压平成可展示列表，Presentation 只根据 [level] 和 [enable]
 * 决定是否展示，不直接依赖远端 DTO 结构。
 *
 * @property id 标签稳定标识。
 * @property name 标签展示名称。
 * @property level 标签层级，0 表示根节点，1 表示一级业务标签。
 * @property enable 标签是否可用。
 */
data class PlazaTag(
    val id: String,
    val name: String,
    val level: Int = 0,
    val enable: Boolean = true,
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
