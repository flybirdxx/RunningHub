package com.runninghub.feature.discovery.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discovery Data 使用的服务端基础响应包装。
 *
 * 该 DTO 位于 Data 层，专门承载 RunningHub 目录接口的业务码、消息和可选数据。
 * Repository 会把非零业务码和空数据转换为 Discovery Domain 的 [com.runninghub.feature.discovery.domain.CatalogError]，
 * 避免 Presentation 直接依赖服务端响应格式。
 *
 * @property code 服务端业务码，0 表示成功。
 * @property msg 服务端原始消息，仅用于错误对象保留上下文，不直接作为最终 UI 文案。
 * @property data 业务数据，目录接口成功时可能仍为空，需要 Repository 按接口语义处理。
 */
@Serializable
data class DiscoveryBaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T? = null,
)

/**
 * Discovery 目录分页响应。
 *
 * 服务端分页字段沿用 Web 端命名，本 DTO 只做协议承载；是否继续加载下一页由 Domain 分页模型判断。
 *
 * @property records 当前页记录，服务端缺失时由序列化默认值降级为空列表。
 * @property total 服务端声明的总数。
 * @property size 当前页请求或返回的页大小。
 * @property current 当前页码。
 * @property hasNext 是否还有下一页。
 * @property hasPrevious 是否存在上一页。
 */
@Serializable
data class DiscoveryPageDataDto<T>(
    @SerialName("records") val records: List<T> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("current") val current: Int = 0,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false,
)
