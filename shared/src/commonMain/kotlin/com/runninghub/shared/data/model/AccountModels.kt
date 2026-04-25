package com.runninghub.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    @SerialName("nickName") val nickName: String? = null,
    @SerialName("headIcon") val headIcon: String? = null,
    val mobile: String? = null,
    val totalCoin: String? = null,
    val memberInfo: MemberInfoDto? = null,
    val walletInfo: WalletInfoDto? = null,
    val apiKey: String? = null,
    val apiType: String? = null,
    val introduce: String? = null,
    val fanCount: String? = "0",
    val followCount: String? = "0",
    val likeCount: String? = "0",
    val collectCount: String? = "0"
)

@Serializable
data class MemberInfoDto(
    val memberName: String? = null,
    val memberExpiredTime: String? = null,
    val userType: String? = null
)

@Serializable
data class WalletInfoDto(
    val balance: Double = 0.0,
    val currency: String? = null,
    val currencySymbol: String? = null
)
