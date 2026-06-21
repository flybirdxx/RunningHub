package com.runninghub.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 密码登录请求 DTO。
 *
 * 密码字段由 Repository 按服务端历史协议写入 MD5 摘要，DTO 只描述网络字段形态。
 *
 * @property mobile 用户输入的中国大陆手机号，不包含 `+86` 前缀。
 * 字段来自登录页面输入，提交前由 Presentation 做基础格式校验；空字符串不应进入网络层。
 * @property password 明文密码的 MD5 摘要，不是原始密码。
 * 该字段属于敏感认证材料，不得写入日志、异常消息或持久化存储。
 * @property serviceAgreement 用户是否同意服务协议。
 * `true` 表示允许提交登录；`false` 仅用于兼容服务端字段，不应由当前登录流程提交。
 * @property channel 推广来源标识，来源于网页端 `utmSource` 或移动端后续归因能力。
 * `null` 表示当前客户端没有可用归因值，序列化时省略。
 * @property inviteCode 邀请码，来源于用户打开链接或本地归因缓存。
 * `null` 表示没有邀请码，空字符串不应主动写入。
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
 *
 * @property accessToken 用户中心 access token，来源于登录或刷新接口。
 * 空字符串表示服务端未返回有效凭据，Repository 必须将其视为登录失败。
 * @property refreshToken 用于刷新 access token 的长期凭据。
 * 空字符串表示无法刷新会话，后续过期时应进入重新登录流程。
 * @property expireIn 服务端返回的过期时间描述，当前保持原始字符串。
 * 空字符串表示服务端未提供该字段，实际过期判断以 JWT `exp` 为准。
 * @property identify 服务端返回的用户身份标识，供兼容旧 Web 会话使用。
 * 空字符串表示没有兼容身份值，不应替代 JWT 用户 ID。
 * @property firstLogin 是否为首次登录。
 * `true` 表示服务端判定为首次登录，可用于后续引导；`false` 表示普通登录或服务端未显式返回。
 * @property inviteCodeUsed 已使用的邀请码。
 * `null` 表示本次登录没有使用邀请码，区别于服务端返回空字符串的异常兼容值。
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

/**
 * 短信验证码发送请求 DTO。
 *
 * @property mobile 用户输入的中国大陆手机号，不包含 `+86` 前缀。
 * 字段来自验证码发送表单；空字符串表示调用方未完成输入校验，不应提交给服务端。
 * @property token 网页端滑块验证码返回的 `validToken`。
 * `null` 表示当前环境未启用短信滑块校验，序列化时省略；非空时只用于本次发送验证码请求。
 */
@Serializable
data class SmsCodeRequestDto(
    @SerialName("mobile") val mobile: String,
    @SerialName("token") val token: String? = null,
)

/**
 * 短信登录请求 DTO。
 *
 * 验证码只参与本次认证请求，不应被持久化。字段结构按网页端抓包保持一致，
 * 其中归因字段在移动端暂时没有来源时保持 `null` 并由 JSON 配置省略。
 *
 * @property mobile 用户输入的中国大陆手机号，不包含 `+86` 前缀。
 * 该值与验证码发送手机号必须一致，空字符串不应提交到 Data 层。
 * @property code 用户输入的短信验证码。
 * 通常为 4 到 6 位数字；空字符串表示尚未输入，提交前应由 Presentation 阻断。
 * @property serviceAgreement 用户是否同意服务协议。
 * `true` 表示允许提交短信登录；`false` 表示未同意，当前客户端不应发起请求。
 * @property channel 推广来源标识，来源于网页端 `utmSource` 或移动端后续归因能力。
 * `null` 表示当前没有归因值，序列化时省略。
 * @property inviteCode 邀请码，来源于邀请链接或本地归因缓存。
 * `null` 表示没有邀请码，区别于服务端返回或输入的空字符串。
 * @property rememberMe 是否请求服务端记住本次登录。
 * `true` 表示用户选择长期保持登录；`false` 表示使用默认会话生命周期。
 */
@Serializable
data class SmsLoginRequestDto(
    @SerialName("mobile") val mobile: String,
    @SerialName("code") val code: String,
    @SerialName("serviceAgreement") val serviceAgreement: Boolean = true,
    @SerialName("channel") val channel: String? = null,
    @SerialName("inviteCode") val inviteCode: String? = null,
    @SerialName("rememberMe") val rememberMe: Boolean = false,
)
