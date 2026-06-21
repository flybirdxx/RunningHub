package com.runninghub.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户中心接口的基础响应 DTO。
 *
 * Auth Data 模块保留独立 DTO，避免继续复用 shared 中的远端模型。Repository 必须检查
 * [code] 并把业务错误映射为 Auth Domain 的稳定错误类型，不能把 [msg] 直接作为最终 UI 文案。
 *
 * @property code 服务端业务码，0 表示成功。
 * @property msg 服务端诊断消息，仅用于错误分类或降级诊断。
 * @property data 响应负载；服务端成功但缺失数据时由 Repository 转换为失败结果。
 */
@Serializable
data class AuthBaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T? = null,
)
