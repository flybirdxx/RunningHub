package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountStatusRequest(
    @SerialName("apikey") val apikey: String
)

@Serializable
data class AccountStatusDto(
    @SerialName("remainCoins") val remainCoins: String = "0",
    @SerialName("currentTaskCounts") val currentTaskCounts: String = "0",
    @SerialName("remainMoney") val remainMoney: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("apiType") val apiType: String = ""
)

@Serializable
data class MemberInfoDto(
    @SerialName("memberName") val memberName: String? = null,
    @SerialName("memberExpiredTime") val memberExpiredTime: String? = null,
    @SerialName("userType") val userType: String? = null,
    @SerialName("memberRemainingDays") val memberRemainingDays: String? = null,
    @SerialName("expired") val expired: Boolean? = null
)

@Serializable
data class WalletInfoDto(
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("currency") val currency: String? = null,
    @SerialName("currencySymbol") val currencySymbol: String? = null,
    @SerialName("hasRecharged") val hasRecharged: Boolean? = null
)

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
    @SerialName("collectCount") val collectCount: String? = "0"
)
