package com.runninghub.feature.model.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标准模型接口通用响应信封。
 *
 * 该响应结构只用于 `feature:model:data` 解析 RunningHub SKU/LLM 目录协议；
 * Repository 会把非 0 code 映射为失败结果，不把 DTO 直接暴露给 Domain 或 Presentation。
 *
 * @param T data 字段的 DTO 类型，由具体接口决定。
 * @property code 服务端业务状态码；0 表示业务成功，非 0 表示服务端拒绝或处理失败。
 * @property msg 服务端业务消息；空字符串表示服务端没有提供可读消息，Repository 会使用默认错误。
 * @property data 成功响应的数据载荷；`null` 表示接口无数据或响应异常，调用方需要按具体接口判断。
 */
@Serializable
data class BaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T? = null,
)

/**
 * 标准模型旧分页响应容器。
 *
 * 部分历史接口仍可能复用该分页结构，保留它可以让 DTO 兼容测试覆盖旧响应格式。
 *
 * @param T 分页元素的 DTO 类型。
 * @property records 当前页元素列表，保持服务端返回顺序；空集合表示当前页无数据。
 * @property total 匹配总数，单位为条；0 表示无匹配数据或服务端未返回总数。
 * @property size 每页数量，单位为条；0 表示服务端未返回分页大小。
 * @property current 当前页码，通常从 1 开始；0 表示服务端未返回页码。
 * @property hasNext 是否存在下一页。`true` 表示可继续请求后续页，`false` 表示无下一页或服务端未提供。
 * @property hasPrevious 是否存在上一页。`true` 表示当前页之前仍有数据，`false` 表示无上一页或服务端未提供。
 */
@Serializable
data class PageDataDto<T>(
    @SerialName("records") val records: List<T>,
    @SerialName("total") val total: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("current") val current: Int = 0,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false,
)
