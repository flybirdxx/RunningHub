package com.runninghub.shared.domain.model

data class User(
    val id: String,
    val nickName: String?,
    val headIcon: String?,
    val mobile: String?,
    val totalCoin: String?,
    val memberInfo: MemberInfo?,
    val walletInfo: WalletInfo?,
    val apiKey: String?,
    val apiType: String?,
    val introduce: String?,
    val fanCount: String,
    val followCount: String,
    val likeCount: String,
    val collectCount: String
)

data class MemberInfo(
    val memberName: String?,
    val memberExpiredTime: String?,
    val userType: String?,
    val memberRemainingDays: String? = null,
    val expired: Boolean? = null
)

data class WalletInfo(
    val balance: Double,
    val currency: String?,
    val currencySymbol: String?
)

data class AccountStatus(
    val remainCoins: String,
    val currentTaskCounts: String,
    val remainMoney: String?,
    val currency: String?,
    val apiType: String
)
