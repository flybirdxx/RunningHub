package com.runninghub.feature.discovery.domain

/**
 * 目录列表查询参数。
 *
 * 该模型属于 Discovery Domain 层，用稳定业务字段表达目录筛选条件，避免 Presentation 直接维护
 * 服务端请求字段或协议字符串。Data 层负责把本模型转换为远端 DTO。
 *
 * @property pageNum 页码，从 1 开始；调用方不应传入 0 或负数。
 * @property pageSize 每页数量，单位为条；具体上限由服务端控制。
 * @property tagIds 标签 ID 列表；空列表表示不过滤标签。
 * @property keyword 搜索关键词；`null` 表示不过滤关键词，空白字符串应由调用方在创建查询前规整。
 * @property sort 目录排序方式；`null` 表示使用服务端默认排序，非空值由 Data 层映射为远端参数。
 */
data class CatalogQuery(
    val pageNum: Int,
    val pageSize: Int,
    val tagIds: List<String> = emptyList(),
    val keyword: String? = null,
    val sort: CatalogSort? = null,
)

/**
 * 目录排序方式。
 *
 * 枚举值表达产品侧稳定排序语义，不暴露远端 API 字符串。带时间窗口的排序使用
 * [heatWindowDays] 描述业务含义，Data 层再转换为接口需要的参数。
 *
 * @property heatWindowDays 热度统计时间窗口，单位为天；`null` 表示该排序不限定固定时间窗口。
 */
enum class CatalogSort(
    val heatWindowDays: Int? = null,
) {
    RECOMMEND,
    REPUTATION(heatWindowDays = 3),
    HOTTEST,
    NEWEST,
}

/**
 * 标签树业务范围。
 *
 * 该枚举隔离服务端 `rang` 参数，Presentation 只选择业务范围，不直接传递远端协议字符串。
 */
enum class CatalogTagRange {
    WEB_APP,
}

/**
 * WebApp 目录领域错误。
 *
 * Repository 使用该稳定错误类型承载服务端业务失败、响应体缺失和未知异常。Presentation 应通过
 * 类型分支映射为用户可见文案，不直接展示 [Throwable.message]，避免把服务端原始信息暴露给 UI。
 */
sealed class CatalogError(
    cause: Throwable? = null,
) : RuntimeException(cause) {

    /**
     * 服务端返回非成功业务码。
     *
     * @property code 服务端业务码。
     * @property serverMessage 服务端原始消息，仅用于诊断和日志脱敏记录，不应直接作为 UI 文案展示。
     */
    data class Remote(
        val code: Int,
        val serverMessage: String,
    ) : CatalogError()

    /**
     * 接口成功但缺少业务数据。
     *
     * @property operation 当前目录操作名称，用于定位是列表、标签树还是详情数据缺失。
     */
    data class EmptyResponse(
        val operation: String,
    ) : CatalogError()

    /**
     * 网络、序列化或其他未归类异常。
     *
     * @property operation 当前目录操作名称。
     * @property original 原始异常，调用方可用于测试或脱敏日志，不应直接展示给用户。
     */
    data class Unexpected(
        val operation: String,
        val original: Throwable,
    ) : CatalogError(original)
}
