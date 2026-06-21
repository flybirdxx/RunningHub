package com.runninghub.feature.community.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Community Data 远端接口的通用响应包裹。
 *
 * Plaza 接口沿用 RunningHub 后端统一的 `code/msg/data` 响应格式；Data 层通过该类型
 * 判断业务成功与否，再把有效载荷映射为 Community Domain 模型。
 *
 * @property code 服务端业务状态码，`0` 表示成功，非 `0` 表示业务失败。
 * @property msg 服务端返回的业务说明，空字符串表示服务端没有提供可读原因；Data 层只用于错误收口，
 * 不把该字段直接当作最终 UI 文案。
 * @property data 成功响应的业务载荷；为 `null` 表示服务端没有返回数据，Repository 会按接口语义
 * 降级为空列表、空分页或失败结果。
 */
@Serializable
data class CommunityBaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T? = null,
)
