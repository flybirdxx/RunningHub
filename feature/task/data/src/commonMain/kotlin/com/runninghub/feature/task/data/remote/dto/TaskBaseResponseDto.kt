package com.runninghub.feature.task.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task Data 使用的服务端基础响应包装。
 *
 * 该 DTO 只存在于 Data 层，用于承载 RunningHub WebApp 任务接口的业务码、消息和数据。
 * Repository 会统一校验业务码和空数据，Presentation 不应直接依赖服务端响应格式。
 *
 * @property code 服务端业务码，0 表示成功。
 * @property msg 服务端原始消息，仅作为错误上下文保留，不直接作为最终 UI 文案。
 * @property data 业务数据，部分成功响应仍可能为空，需要 Repository 按接口语义处理。
 */
@Serializable
data class TaskBaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T? = null,
)

/**
 * WebApp 任务历史分页响应。
 *
 * 旧历史接口返回分页对象，当前 Task Domain 只需要记录列表；分页状态仍由调用方持有。
 *
 * @property records 当前页历史记录。
 * @property total 服务端声明的总数。
 * @property size 当前页大小。
 * @property current 当前页码。
 * @property hasNext 是否还有下一页。
 * @property hasPrevious 是否存在上一页。
 */
@Serializable
data class TaskPageDataDto<T>(
    @SerialName("records") val records: List<T> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("current") val current: Int = 0,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false,
)
