package com.runninghub.feature.community.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plaza 创作列表请求 DTO。
 *
 * @property current 当前页码，从 1 开始；小于 1 的值由调用方避免传入。
 * @property size 每页条数，服务端默认 30；`0` 没有业务意义。
 * @property fromId 游标分页的起始内容 ID；`null` 表示使用普通页码分页。
 * @property sort 排序协议值，例如 `RECOMMEND`、`HOT` 或 `LATEST`，由 Domain/Presentation 的筛选状态传入。
 * @property tags 标签 ID 列表，顺序不影响服务端筛选语义；空集合表示不过滤标签。
 */
@Serializable
data class PlazaCreationListRequestDto(
    val current: Int = 1,
    val size: Int = 30,
    val fromId: String? = null,
    val sort: String = "RECOMMEND",
    val tags: List<String> = emptyList(),
)

/**
 * Plaza 创作分页响应 DTO。
 *
 * @property records 新版分页字段，按服务端推荐或排序权重排列。
 * @property list 旧版分页字段，仅当 [records] 为空时作为兼容来源。
 * @property total 服务端声明的总条数；`0` 表示无数据或服务端未提供总数。
 * @property current 当前页码，通常从 1 开始。
 * @property nextCursor 游标分页的下一页起点；`null` 表示服务端未启用游标或没有下一页。
 */
@Serializable
data class PlazaCreationPageDto(
    val records: List<PlazaCreationCardDto> = emptyList(),
    val list: List<PlazaCreationCardDto> = emptyList(),
    val total: Int = 0,
    val current: Int = 1,
    val nextCursor: String? = null,
) {
    /**
     * 兼容新旧分页字段后的创作卡片列表。
     *
     * 服务端历史上同时出现 `records` 和 `list` 两种字段，优先使用 `records` 可以保留新版推荐顺序；
     * 只有新版字段为空时才降级到 `list`。
     */
    val items: List<PlazaCreationCardDto>
        get() = if (records.isNotEmpty()) records else list
}

/**
 * Plaza 创作卡片 DTO。
 *
 * @property id 创作内容稳定 ID，来自服务端，可用于分页去重和详情跳转。
 * @property intro 内容简介；`null` 表示服务端未提供简介。
 * @property publishTime 发布时间字符串，保留服务端格式和时区。
 * @property owner 作者信息；`null` 表示匿名内容或服务端缺失作者对象。
 * @property statisticsInfo 点赞、使用、收藏等统计信息；`null` 表示统计不可用。
 * @property creationShowreelInfo 展示媒体信息；`null` 表示当前卡片没有可展示媒体。
 * @property liked 当前登录用户是否已点赞，`true` 表示已点赞，`false` 表示未点赞或服务端未返回。
 * @property collected 当前登录用户是否已收藏，`true` 表示已收藏，`false` 表示未收藏或服务端未返回。
 */
@Serializable
data class PlazaCreationCardDto(
    val id: String,
    val intro: String? = null,
    val publishTime: String? = null,
    val owner: PlazaOwnerDto? = null,
    val statisticsInfo: PlazaStatisticsDto? = null,
    val creationShowreelInfo: PlazaCreationShowreelDto? = null,
    val liked: Boolean = false,
    val collected: Boolean = false,
)

/**
 * Plaza 作者 DTO。
 *
 * @property id 作者用户 ID；`null` 表示服务端未返回作者标识。
 * @property name 作者展示名；`null` 表示服务端未返回昵称。
 * @property avatar 作者头像 URL；`null` 表示没有头像或头像不可用。
 */
@Serializable
data class PlazaOwnerDto(
    val id: String? = null,
    val name: String? = null,
    val avatar: String? = null,
)

/**
 * Plaza 统计信息 DTO。
 *
 * @property likeCount 点赞数文案，保留服务端字符串格式。
 * @property downloadCount 下载数文案，当前 Domain 暂不展示但保留解析兼容。
 * @property useCount 使用数文案，保留服务端字符串格式。
 * @property pv 浏览量文案，当前 Domain 暂不展示但保留解析兼容。
 * @property collectCount 收藏数文案，保留服务端字符串格式。
 */
@Serializable
data class PlazaStatisticsDto(
    val likeCount: String? = null,
    val downloadCount: String? = null,
    val useCount: String? = null,
    val pv: String? = null,
    val collectCount: String? = null,
)

/**
 * Plaza 展示媒体 DTO。
 *
 * @property outputId 媒体输出 ID；`null` 表示服务端未返回输出标识。
 * @property fileUrl 远端媒体 URL；`null` 表示当前卡片无法直接展示媒体。
 * @property outputName 输出文件名，保留服务端原始名称。
 * @property fileSize 文件大小文案，保留服务端字符串格式。
 * @property imageWidth 图片宽度，单位为像素；非图片或未知时为 `null`。
 * @property imageHeight 图片高度，单位为像素；非图片或未知时为 `null`。
 * @property fileType 文件类型协议值，例如图片或视频格式；`null` 表示未知。
 * @property seq 媒体在内容中的展示顺序；`null` 表示服务端未提供排序。
 * @property isWatermark 是否带水印的服务端标记，具体取值由远端定义。
 */
@Serializable
data class PlazaCreationShowreelDto(
    val outputId: String? = null,
    val fileUrl: String? = null,
    val outputName: String? = null,
    val fileSize: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val fileType: String? = null,
    val seq: Int? = null,
    val isWatermark: Int? = null,
)

/**
 * Plaza 标签树请求 DTO。
 *
 * @property rang 服务端拼写保留字段，`CREATION` 表示请求创作内容标签树。
 */
@Serializable
data class PlazaTagTreeRequestDto(
    val rang: String = "CREATION",
)

/**
 * Plaza 标签节点 DTO。
 *
 * @property id 标签稳定 ID，可作为筛选请求参数。
 * @property name 标签展示名。
 * @property level 标签层级，`0` 表示根节点。
 * @property enable `true` 表示标签可用于筛选，`false` 表示服务端暂时禁用。
 * @property childTags 子标签节点，顺序保留服务端配置；空集合表示叶子节点。
 */
@Serializable
data class PlazaTagDto(
    val id: String,
    val name: String,
    val level: Int = 0,
    val enable: Boolean = true,
    val childTags: List<PlazaTagDto> = emptyList(),
)

/**
 * Plaza 短片列表请求 DTO。
 *
 * @property page 页码，从 1 开始。
 * @property size 每页条数，默认 30。
 * @property categoryCode 分类代码；`null` 表示拉取全部分类。
 */
@Serializable
data class PlazaShortListRequestDto(
    val page: Int = 1,
    val size: Int = 30,
    val categoryCode: String? = null,
)

/**
 * Plaza 短片分页响应 DTO。
 *
 * @property records 新版短片列表字段，按服务端顺序排列。
 * @property list 旧版短片列表字段，仅当 [records] 为空时作为兼容来源。
 * @property total 服务端声明的总数；`0` 表示无数据或未返回总数。
 */
@Serializable
data class PlazaShortPageDto(
    val records: List<PlazaShortCardDto> = emptyList(),
    val list: List<PlazaShortCardDto> = emptyList(),
    val total: Int = 0,
) {
    /**
     * 兼容新旧字段后的短片列表。
     *
     * 优先使用 `records`，避免新版服务端返回顺序被旧字段覆盖。
     */
    val items: List<PlazaShortCardDto>
        get() = if (records.isNotEmpty()) records else list
}

/**
 * Plaza 短片分类 DTO。
 *
 * @property id 分类 ID，可能为空。
 * @property code 分类代码，用于短片分页筛选；为空时 Repository 会降级为 [id]。
 * @property name 分类展示名；空字符串表示服务端未提供名称。
 */
@Serializable
data class PlazaShortCategoryDto(
    val id: String? = null,
    val code: String? = null,
    val name: String = "",
)

/**
 * Plaza 短片卡片 DTO。
 *
 * @property id 短片稳定 ID。
 * @property name 短片名称，空字符串表示服务端未提供标题。
 * @property videoUrl 远端视频 URL；`null` 表示短片暂不可播放。
 * @property coverUrl 旧版封面 URL；当 [thumbnailUrl] 缺失时作为降级缩略图。
 * @property thumbnailUrl 新版缩略图 URL；`null` 时 Repository 会尝试使用 [coverUrl]。
 * @property duration 视频时长，单位为秒；`null` 表示服务端未提供时长。
 * @property categoryName 分类展示名；`null` 表示未知分类。
 * @property authorName 作者展示名，对应服务端 `userName` 字段。
 * @property authorAvatar 作者头像 URL，对应服务端 `userAvatar` 字段。
 */
@Serializable
data class PlazaShortCardDto(
    val id: String,
    val name: String = "",
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null,
    val categoryName: String? = null,
    @SerialName("userName")
    val authorName: String? = null,
    @SerialName("userAvatar")
    val authorAvatar: String? = null,
)
