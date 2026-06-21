package com.runninghub.feature.discovery.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discovery 目录卡片 DTO。
 *
 * 该结构对应 WebApp 目录、精选、搜索和用户主页列表返回项。字段大多来自 Web 端接口，
 * Data 层负责在 mapper 中完成缩略图、封面媒体类型和计数字段的兼容降级。
 *
 * @property id WebApp ID，服务端可能以字符串返回。
 * @property title WebApp 展示名称。
 * @property desc WebApp 简介。
 * @property thumbnailUrl 独立缩略图地址。
 * @property preview 旧接口返回的预览资源。
 * @property covers 新接口返回的封面资源列表。
 * @property author 作者信息。
 * @property tags 轻量标签列表。
 * @property statisticsInfo 统计信息聚合对象。
 * @property likeCount 旧接口点赞数。
 * @property collectCount 旧接口收藏数。
 * @property useCount 旧接口使用数。
 * @property pv 旧接口浏览量。
 * @property labels 服务端标签文本，当前 Domain 暂不展示但保留兼容。
 * @property carefullyChosen 是否为运营精选。
 * @property publishTime 发布时间文本。
 */
@Serializable
data class WebAppCatalogDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val title: String? = null,
    @SerialName("intro") val desc: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerialName("preview") val preview: WebAppPreviewDto? = null,
    @SerialName("covers") val covers: List<WebAppCoverDto>? = null,
    @SerialName("owner") val author: WebAppAuthorDto? = null,
    @SerialName("tags") val tags: List<WebAppTagSimpleDto>? = null,
    @SerialName("statisticsInfo") val statisticsInfo: WebAppStatisticsInfoDto? = null,
    @SerialName("likeCount") val likeCount: String? = null,
    @SerialName("collectCount") val collectCount: String? = null,
    @SerialName("useCount") val useCount: String? = null,
    @SerialName("pv") val pv: String? = null,
    @SerialName("labels") val labels: String? = null,
    @SerialName("carefullyChosen") val carefullyChosen: Boolean = false,
    @SerialName("publishTime") val publishTime: String? = null,
)

/**
 * WebApp 详情 DTO。
 *
 * 详情接口被公开详情页和 API 调用示例复用；Discovery Data 只使用公开详情入口，
 * 需要 API Key 的调用示例仍留在 Task Data 迁移链路中。
 *
 * @property id WebApp ID。
 * @property name WebApp 名称。
 * @property workflowId 关联工作流 ID。
 * @property tags 详情页标签列表。
 * @property owner 作者信息。
 * @property publishTime 发布时间文本。
 * @property inputNodes 公开详情返回的输入节点描述。
 * @property description 详情说明。
 * @property covers 封面资源列表。
 * @property statisticsInfo 统计信息。
 * @property authorName 旧接口单独返回的作者名称。
 * @property authorAvatar 旧接口单独返回的作者头像。
 * @property runningSuccessRate 运行成功率文本。
 * @property avgRunningSeconds 平均运行耗时文本。
 * @property instanceType 默认实例类型。
 */
@Serializable
data class WebAppDetailCatalogDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("workflowId") val workflowId: String? = null,
    @SerialName("tags") val tags: List<WebAppTagSimpleDto>? = null,
    @SerialName("owner") val owner: WebAppAuthorDto? = null,
    @SerialName("publishTime") val publishTime: String? = null,
    @SerialName("inputNodes") val inputNodes: List<WebAppInputNodeDto>? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("covers") val covers: List<WebAppCoverDto>? = null,
    @SerialName("statisticsInfo") val statisticsInfo: WebAppStatisticsInfoDto? = null,
    @SerialName("userName") val authorName: String? = null,
    @SerialName("userAvatar") val authorAvatar: String? = null,
    @SerialName("runningSuccessRate") val runningSuccessRate: String? = null,
    @SerialName("avgRunningSeconds") val avgRunningSeconds: String? = null,
    @SerialName("instanceType") val instanceType: String? = null,
)

/**
 * WebApp 输入节点 DTO。
 *
 * Discovery 详情页只展示节点元信息；真正提交任务时的节点值仍由 Task Domain/ Data 处理。
 *
 * @property nodeId 节点 ID。
 * @property nodeName 节点名称。
 * @property fieldName 字段名称。
 * @property fieldValue 字段默认值。
 * @property fieldData 字段扩展数据。
 * @property fieldType 字段类型。
 * @property description 中文说明。
 * @property descriptionEn 英文说明。
 */
@Serializable
data class WebAppInputNodeDto(
    @SerialName("nodeId") val nodeId: String,
    @SerialName("nodeName") val nodeName: String = "",
    @SerialName("fieldName") val fieldName: String = "",
    @SerialName("fieldValue") val fieldValue: String? = null,
    @SerialName("fieldData") val fieldData: String? = null,
    @SerialName("fieldType") val fieldType: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("descriptionEn") val descriptionEn: String? = null,
)

/**
 * WebApp 轻量标签 DTO。
 *
 * @property id 标签 ID。
 * @property name 标签中文名称。
 * @property nameEn 标签英文名称。
 * @property labels 服务端标签扩展文本。
 */
@Serializable
data class WebAppTagSimpleDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("nameEn") val nameEn: String? = null,
    @SerialName("labels") val labels: String? = null,
)

/**
 * 旧版预览资源 DTO。
 *
 * @property url 预览资源地址，可能是图片、GIF 或视频。
 */
@Serializable
data class WebAppPreviewDto(
    @SerialName("url") val url: String? = null,
)

/**
 * 作者 DTO。
 *
 * @property name 作者昵称。
 * @property avatar 作者头像。
 * @property id 作者 ID。
 * @property intro 作者简介。
 * @property followCount 关注数。
 * @property fansCount 粉丝数。
 * @property likeCount 获赞数。
 * @property collectCount 被收藏数。
 * @property bgImage 个人主页背景图。
 */
@Serializable
data class WebAppAuthorDto(
    @SerialName("name") val name: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("intro") val intro: String? = null,
    @SerialName("followCount") val followCount: String? = "0",
    @SerialName("fansCount") val fansCount: String? = "0",
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("bgImage") val bgImage: String? = null,
)

/**
 * WebApp 统计信息 DTO。
 *
 * @property likeCount 点赞数。
 * @property collectCount 收藏数。
 * @property useCount 使用数。
 * @property pv 浏览数。
 */
@Serializable
data class WebAppStatisticsInfoDto(
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("useCount") val useCount: String? = "0",
    @SerialName("pv") val pv: String? = "0",
)

/**
 * WebApp 封面 DTO。
 *
 * @property url 原始资源地址。
 * @property thumbnailUri 缩略图地址。
 * @property imageWidth 图片或视频宽度文本。
 * @property imageHeight 图片或视频高度文本。
 */
@Serializable
data class WebAppCoverDto(
    @SerialName("url") val url: String? = null,
    @SerialName("thumbnailUri") val thumbnailUri: String? = null,
    @SerialName("imageWidth") val imageWidth: String? = null,
    @SerialName("imageHeight") val imageHeight: String? = null,
)

/**
 * WebApp 目录分页请求 DTO。
 *
 * @property pageSize 页大小。
 * @property pageNum 页码。
 * @property tags 标签 ID 列表。
 * @property keyword 搜索关键词。
 * @property sort 排序协议值。
 * @property days 热度窗口天数。
 */
@Serializable
data class WebAppListRequestDto(
    @SerialName("size") val pageSize: Int,
    @SerialName("current") val pageNum: Int,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("keyword") val keyword: String? = null,
    @SerialName("sort") val sort: String? = null,
    @SerialName("days") val days: Int? = null,
)

/**
 * 定制 WebApp 列表请求 DTO。
 *
 * @property tags 定制分类标签 ID 列表。
 */
@Serializable
data class CustomMadeWebappRequestDto(
    @SerialName("tags") val tags: List<String> = emptyList(),
)

/**
 * 目录标签树请求 DTO。
 *
 * @property rang 服务端字段拼写为 `rang`，当前只查询 WEBAPP 范围。
 */
@Serializable
data class CatalogTagTreeRequestDto(
    @SerialName("rang") val rang: String = "WEBAPP",
)
