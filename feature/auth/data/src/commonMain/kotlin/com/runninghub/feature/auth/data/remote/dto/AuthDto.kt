package com.runninghub.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 密码登录请求 DTO。
 *
 * 密码字段由 Repository 按服务端历史协议写入 MD5 摘要，DTO 只描述网络字段形态。
 */
@Serializable
data class PwdLoginRequestDto(
    @SerialName("mobile") val mobile: String,
    @SerialName("password") val password: String,
    @SerialName("serviceAgreement") val serviceAgreement: Boolean = true,
    @SerialName("channel") val channel: String? = null,
    @SerialName("inviteCode") val inviteCode: String? = null,
)

/**
 * 用户中心登录令牌 DTO。
 *
 * access token 与 refresh token 属于敏感凭据，只能写入 [com.runninghub.core.storage.CredentialStore]，
 * 不得进入日志、异常消息或页面状态。
 */
@Serializable
data class LoginTokenDataDto(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expire_in") val expireIn: String = "",
    @SerialName("identify") val identify: String = "",
    @SerialName("firstLogin") val firstLogin: Boolean = false,
    @SerialName("inviteCodeUsed") val inviteCodeUsed: String? = null,
)

/** 短信验证码发送请求 DTO。 */
@Serializable
data class SmsCodeRequestDto(
    @SerialName("mobile") val mobile: String,
)

/** 短信登录请求 DTO，验证码只参与本次认证请求，不应被持久化。 */
@Serializable
data class SmsLoginRequestDto(
    @SerialName("mobile") val mobile: String,
    @SerialName("code") val code: String,
)
