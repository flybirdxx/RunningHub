package com.runninghub.feature.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API Key 账户状态请求 DTO。 */
@Serializable
data class AccountStatusRequestDto(
    @SerialName("apikey") val apikey: String,
)

/**
 * API Key 账户状态 DTO。
 *
 * 金额和次数沿用服务端字符串格式，Repository 映射时保持原值，避免客户端误解析造成精度或单位变化。
 */
@Serializable
data class AccountStatusDto(
    @SerialName("remainCoins") val remainCoins: String = "0",
    @SerialName("currentTaskCounts") val currentTaskCounts: String = "0",
    @SerialName("remainMoney") val remainMoney: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("apiType") val apiType: String = "",
)

/** 用户会员信息 DTO，字段均来自用户中心返回。 */
@Serializable
data class MemberInfoDto(
    @SerialName("memberName") val memberName: String? = null,
    @SerialName("memberExpiredTime") val memberExpiredTime: String? = null,
    @SerialName("userType") val userType: String? = null,
    @SerialName("memberRemainingDays") val memberRemainingDays: String? = null,
    @SerialName("expired") val expired: Boolean? = null,
)

/** 用户钱包信息 DTO，余额单位由服务端 currency 字段决定。 */
@Serializable
data class WalletInfoDto(
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("currency") val currency: String? = null,
    @SerialName("currencySymbol") val currencySymbol: String? = null,
    @SerialName("hasRecharged") val hasRecharged: Boolean? = null,
)

/**
 * 用户中心用户资料 DTO。
 *
 * `apiKey` 只用于登录后同步用户绑定凭据，Repository 会写入 CredentialStore，
 * Presentation 不应展示或记录该字段。
 */
@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("nickName") val nickName: String? = null,
    @SerialName("headIcon") val headIcon: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("totalCoin") val totalCoin: String? = null,
    @SerialName("memberInfo") val memberInfo: MemberInfoDto? = null,
    @SerialName("walletInfo") val walletInfo: WalletInfoDto? = null,
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("apiType") val apiType: String? = null,
    @SerialName("introduce") val introduce: String? = null,
    @SerialName("fanCount") val fanCount: String? = "0",
    @SerialName("followCount") val followCount: String? = "0",
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
)
