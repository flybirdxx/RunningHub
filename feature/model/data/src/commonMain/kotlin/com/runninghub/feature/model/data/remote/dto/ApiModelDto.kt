@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.runninghub.feature.model.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 标准模型 SKU 列表查询请求。
 *
 * 该 DTO 只属于 `feature:model:data`，用于匹配 RunningHub `/api/sku/list` 的远端协议；
 * Domain 层只暴露搜索词、页码和页大小等稳定语义，不直接接触这些筛选字段。
 *
 * @property categoryType 服务端 SKU 分类类型，默认 `STANDARD_MODEL` 表示只查询标准模型目录。
 * 空字符串不是有效业务值，调用方不应覆盖默认值。
 * @property tagIds 标签筛选 ID 集合，来自服务端标签树或用户筛选条件；顺序不参与业务语义，
 * 空集合表示不按普通标签过滤。
 * @property search 用户输入的搜索关键字，允许空字符串；空字符串表示加载默认标准模型列表。
 * @property categoryTagIds 分类标签 ID 集合，来自服务端分类配置；顺序不参与业务语义，
 * 空集合表示不按分类标签过滤。
 * @property owners 作者或归属方筛选集合，来自服务端支持的 owner 标识；空集合表示不过滤 owner。
 * @property isCollected 是否只查询当前用户收藏的模型。`true` 表示仅返回收藏项，
 * `false` 表示不施加收藏过滤。
 * @property region 区域筛选值，格式由服务端约定；空字符串表示使用服务端默认区域或不过滤区域。
 * @property isWhitelist 是否只查询白名单可见模型。`true` 表示仅返回白名单模型，
 * `false` 表示按普通目录权限返回。
 * @property pageNum 请求页码，从 1 开始；小于 1 不具备业务意义，调用仓库会传入正整数。
 * @property pageSize 每页数量，单位为条；默认 30，0 或负数不应由客户端主动传入。
 */
@Serializable
data class SkuListRequestDto(
    val categoryType: String = "STANDARD_MODEL",
    val tagIds: List<String> = emptyList(),
    val search: String = "",
    val categoryTagIds: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
    val isCollected: Boolean = false,
    val region: String = "",
    val isWhitelist: Boolean = false,
    val pageNum: Int = 1,
    val pageSize: Int = 30,
)

/**
 * 标准模型分组标签查询请求。
 *
 * 该 DTO 对应 RunningHub 标准模型页的 `sku/tag/query` 请求；客户端只使用 `parentId=4`
 * 查询模型分组，避免把模型名称前缀误当作分组来源。
 *
 * @property parentId 服务端标签父级 ID；标准模型分组固定为 `4`，其他值不属于当前业务入口。
 * @property levels 需要查询的标签层级；`[1]` 表示只取顶部模型分组，空集合表示服务端默认层级。
 * @property showType 服务端展示结构类型；`tree` 表示按标签树返回，空字符串不是当前客户端有效值。
 * @property search 搜索关键字，允许空字符串；空字符串表示不按名称过滤分组。
 */
@Serializable
data class SkuTagQueryRequestDto(
    val parentId: Int,
    val levels: List<Int> = emptyList(),
    val showType: String = "tree",
    val search: String = "",
)

/**
 * 标准模型分组标签 DTO。
 *
 * @property id 服务端标签 ID，可能以数字或字符串返回；空字符串表示缺少可用于筛选的稳定 ID。
 * @property parentId 父级标签 ID，来源于服务端标签树；空字符串表示接口未返回父级。
 * @property level 标签层级，`0` 表示接口未提供层级或不是当前客户端关心的层级。
 * @property name 用户可见标签名称；空字符串表示服务端没有提供展示名。
 * @property nameEn 英文标签名称；`null` 表示接口未返回英文名。
 * @property description 标签说明；`null` 表示服务端未提供说明，客户端不自行补文案。
 * @property sort 服务端排序值；`0` 表示默认排序或接口未返回排序。
 * @property apiCount 标签下模型数量，单位为个；`null` 表示接口未提供数量，映射时按 0 处理。
 */
@Serializable
data class SkuTagDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    @Serializable(with = FlexibleStringSerializer::class)
    val parentId: String = "",
    val level: Int = 0,
    val name: String = "",
    val nameEn: String? = null,
    val description: String? = null,
    val sort: Int = 0,
    val apiCount: Int? = null,
)

/**
 * 标准模型 SKU 列表响应分页容器。
 *
 * 服务端历史上会把列表放在 `records`、`list` 或嵌套 `page` 中，本 DTO 保留三种结构并通过
 * [items] 提供统一读取入口，避免 Repository 复制兼容分支。
 *
 * @property records 顶层 records 列表，保持服务端返回顺序；空集合表示该字段未返回或当前页无数据。
 * @property list 顶层 list 列表，兼容旧响应结构；当 [records] 非空时不参与最终展示列表。
 * @property page 嵌套分页对象，兼容部分环境把列表放入 `data.page` 的响应；`null` 表示没有嵌套分页。
 * @property total 服务端报告的匹配总数，单位为条；0 表示无数据或服务端未提供总数。
 */
@Serializable
data class SkuListPageDto(
    val records: List<SkuSummaryDto> = emptyList(),
    val list: List<SkuSummaryDto> = emptyList(),
    val page: SkuListNestedPageDto? = null,
    val total: Int = 0,
) {
    /**
     * 当前响应可用于映射的 SKU 列表。
     *
     * 读取优先级为 `records -> list -> page.items`，用于兼容服务端多版本响应；
     * 空集合表示没有可展示的模型摘要。
     */
    val items: List<SkuSummaryDto>
        get() = records
            .ifEmpty { list }
            .ifEmpty { page?.items.orEmpty() }
}

/**
 * 标准模型 SKU 列表的嵌套分页数据。
 *
 * @property records 嵌套 records 列表，保持服务端推荐顺序；空集合表示无数据或字段缺失。
 * @property list 嵌套 list 列表，兼容旧响应结构；当 [records] 非空时不参与最终展示列表。
 */
@Serializable
data class SkuListNestedPageDto(
    val records: List<SkuSummaryDto> = emptyList(),
    val list: List<SkuSummaryDto> = emptyList(),
) {
    /**
     * 嵌套分页容器的统一列表入口。
     *
     * 读取优先级为 `records -> list`；空集合表示嵌套分页中没有可映射的 SKU。
     */
    val items: List<SkuSummaryDto>
        get() = records.ifEmpty { list }
}

/**
 * 把服务端不稳定的字符串字段统一解码为 String。
 *
 * SKU id 和价格字段在不同环境中可能以数字、字符串或 null 返回；Data 层统一转换为字符串，
 * 使 Repository 可以继续执行映射和错误降级。`null` 被转换为空字符串，表示服务端未提供值。
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    /**
     * 解码服务端可能为字符串、数字、布尔或 null 的字段。
     *
     * @return 字符串内容；服务端返回 null 时为 `""`，复杂 JSON 值以原始 JSON 字符串保留。
     */
    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> ""
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    /**
     * 序列化客户端写出的字符串字段。
     *
     * 该路径主要用于测试和请求体构造，保持普通字符串输出即可。
     */
    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/**
 * 把服务端不稳定的标签字段统一解码为字符串列表。
 *
 * `/api/sku/list` 的分类信息在不同响应里可能是 `tags` 字符串、数组或对象数组。
 * Data 层在这里统一拆分为脱敏标签值，供 Repository 推导图片、视频、音频和 3D 分类。
 */
object FlexibleStringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleStringList", PrimitiveKind.STRING)

    /**
     * 解码标签列表。`null` 表示服务端未提供分类标签，返回空集合。
     */
    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString().splitTagText()
        return jsonDecoder.decodeJsonElement().toTagStrings()
    }

    /**
     * 序列化测试和缓存写出的标签列表。
     */
    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString("|"))
    }
}

private fun JsonElement.toTagStrings(): List<String> =
    when (this) {
        JsonNull -> emptyList()
        is JsonPrimitive -> contentOrNull.orEmpty().splitTagText()
        is JsonArray -> flatMap { it.toTagStrings() }
        is JsonObject -> listOfNotNull(
            stringTag("name")
                ?: stringTag("tagName")
                ?: stringTag("label")
                ?: stringTag("value")
                ?: stringTag("code")
        ).flatMap { it.splitTagText() }
    }

private fun JsonObject.stringTag(key: String): String? =
    (this[key] as? JsonPrimitive)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }

private fun String.splitTagText(): List<String> =
    split("|", ",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

/**
 * 标准模型 SKU 列表项 DTO。
 *
 * @property id SKU 稳定标识，来源于服务端；允许数字或字符串协议值，经 [FlexibleStringSerializer]
 * 转为字符串。空字符串表示服务端缺失 id，Repository 不应把它作为可持久化模型标识。
 * @property name 中文模型名称，来源于服务端目录；空字符串表示服务端未提供名称。
 * @property nameEn 英文模型名称，来源于服务端目录；`null` 表示服务端未返回英文名称，
 * 与空字符串“返回了空名称”不同。
 * @property type 服务端模型类型标识，兼容读取 `type` 和线上列表响应的 `categoryName`；
 * `null` 表示目录响应未提供类型。
 * @property groupName 服务端分组名称；`null` 表示未提供分组，客户端不应自行推断分组。
 * @property source 模型来源或供应方标识，兼容读取 `source` 和线上列表响应的 `sourceTypeName`；
 * `null` 表示服务端未返回来源信息。
 * @property tags 服务端模型标签，可能携带 `text-to-video`、`text-to-audio`、`image-to-3D` 等
 * 分类线索；空集合表示服务端未返回标签或标签不可解析。
 * @property price 原始价格字段，来源于服务端，可能是数字或字符串；空字符串表示服务端未提供价格。
 * @property priceSummary 服务端可直接展示的价格摘要；`null` 时 Repository 回退使用 [price]。
 * @property rhEndpoint 标准模型调用 endpoint，属于 Data 层路由细节；`null` 表示列表响应未提供，
 * 调用仓库会在提交前按详情接口回源。
 */
@Serializable
data class SkuSummaryDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    val name: String = "",
    val nameEn: String? = null,
    @JsonNames("categoryName")
    val type: String? = null,
    val groupName: String? = null,
    @JsonNames("sourceTypeName")
    val source: String? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val tags: List<String> = emptyList(),
    @Serializable(with = FlexibleStringSerializer::class)
    val price: String = "",
    val priceSummary: String? = null,
    val rhEndpoint: String? = null,
)

/**
 * 标准模型 SKU 详情查询请求。
 *
 * @property id SKU 稳定标识，来自列表项或调用请求中的 modelId；不能为空字符串，
 * 否则服务端无法定位模型详情。
 */
@Serializable
data class SkuDetailRequestDto(
    val id: String,
)

/**
 * 标准模型 SKU 详情 DTO。
 *
 * @property id SKU 稳定标识，来源于服务端详情；用于登记 endpoint 缓存和映射领域模型。
 * @property name 中文模型名称，来源于服务端；空字符串表示服务端未提供名称。
 * @property nameEn 英文模型名称；`null` 表示服务端未提供英文名称。
 * @property type 服务端模型类型标识，兼容读取 `type` 和线上详情响应可能返回的 `categoryName`；
 * `null` 表示详情响应未提供类型。
 * @property groupName 服务端分组名称；`null` 表示未提供分组。
 * @property source 模型来源或供应方标识，兼容读取 `source` 和线上详情响应可能返回的
 * `sourceTypeName`；`null` 表示服务端未返回来源信息。
 * @property tags 服务端模型标签，可能携带分类、供应方和产品线信息；空集合表示详情未返回标签。
 * @property price 原始价格字段，可能是数字或字符串；空字符串表示服务端未提供价格。
 * @property priceSummary 服务端可直接展示的价格摘要；`null` 时 Repository 回退使用 [price]。
 * @property rhEndpoint 标准模型调用 endpoint，来源于详情接口；空字符串表示服务端缺失路由，
 * 提交模型调用时会作为业务错误返回。
 * @property inputConfigJson 服务端表单字段配置 JSON；`null` 表示没有动态字段配置，
 * 与空字符串同样会被映射为空字段列表。
 * @property queueSize 当前模型排队数量，单位为个；`null` 表示服务端未提供该运行态指标。
 * @property concurrencyLimit 当前模型并发限制，单位为个；`null` 表示服务端未提供限制信息。
 */
@Serializable
data class SkuDetailDto(
    val id: String,
    val name: String = "",
    val nameEn: String? = null,
    @JsonNames("categoryName")
    val type: String? = null,
    val groupName: String? = null,
    @JsonNames("sourceTypeName")
    val source: String? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val tags: List<String> = emptyList(),
    @Serializable(with = FlexibleStringSerializer::class)
    val price: String = "",
    val priceSummary: String? = null,
    val rhEndpoint: String = "",
    val inputConfigJson: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
)

/**
 * LLM 模型目录 DTO。
 *
 * @property modelKey LLM 模型稳定调用标识，来源于服务端；不能为空字符串，否则无法发起调用。
 * @property provider 模型供应方名称；空字符串表示服务端未提供供应方。
 * @property version 模型版本号或发布日期；`null` 表示服务端未提供版本信息。
 * @property contextLength 上下文窗口长度，单位为 token；`null` 表示服务端未提供容量信息。
 * @property capabilities 服务端声明的能力标签列表，顺序按服务端返回保留；空集合表示未声明能力。
 * @property inputPrice 输入计价描述，币种和精度由服务端字符串决定；`null` 表示未提供输入价格。
 * @property outputPrice 输出计价描述，币种和精度由服务端字符串决定；`null` 表示未提供输出价格。
 */
@Serializable
data class LlmModelDto(
    val modelKey: String,
    val provider: String = "",
    val version: String? = null,
    @SerialName("context")
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)
