package com.runninghub.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [INPUT]: apikey
 * [OUTPUT]: AccountStatusDto
 * [POS]: 个人账户信息请求与响应模型
 */
data class AccountStatusRequest(
    val apikey: String
)

data class AccountStatusDto(
    @SerializedName("remainCoins") val remainCoins: String,
    @SerializedName("currentTaskCounts") val currentTaskCounts: String,
    @SerializedName("remainMoney") val remainMoney: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("apiType") val apiType: String
)

data class MemberInfoDto(
    @SerializedName("memberName") val memberName: String?,
    @SerializedName("memberExpiredTime") val memberExpiredTime: String?,
    @SerializedName("userType") val userType: String?
)

data class WalletInfoDto(
    @SerializedName("balance") val balance: Double,
    @SerializedName("currency") val currency: String?,
    @SerializedName("currencySymbol") val currencySymbol: String?
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("nickName") val nickName: String?,
    @SerializedName("headIcon") val headIcon: String?,
    @SerializedName("mobile") val mobile: String?,
    @SerializedName("totalCoin") val totalCoin: String?,
    @SerializedName("memberInfo") val memberInfo: MemberInfoDto?,
    @SerializedName("walletInfo") val walletInfo: WalletInfoDto?,
    @SerializedName("apiKey") val apiKey: String?,
    @SerializedName("apiType") val apiType: String? = null,
    @SerializedName("introduce") val introduce: String? = null,
    @SerializedName("fanCount") val fanCount: String? = "0",
    @SerializedName("followCount") val followCount: String? = "0",
    @SerializedName("likeCount") val likeCount: String? = "0",
    @SerializedName("collectCount") val collectCount: String? = "0"
)
