package com.runninghub.feature.auth.presentation.profile

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo

/** Profile 资产中心区块的加载状态。 */
enum class ProfileAssetLoadState {
    Hidden,
    Loading,
    Loaded,
    Failed,
    Empty,
}

/** 钱包区块可展示的稳定风险语义。 */
enum class ProfileWalletRisk {
    None,
    BalanceUnavailable,
}

/** 会员区块可展示的稳定状态。 */
enum class ProfileMembershipStatus {
    None,
    Active,
    Expired,
    Unknown,
}

/**
 * Profile 钱包区块的 Presentation 状态。
 *
 * @property rhbPoints RHB 点数，优先来自账户状态；为空表示当前不能把未知余额当作 0。
 * @property walletBalance CNY 钱包余额文本，不与 RHB 点数合并展示。
 * @property walletCurrency 钱包余额币种，当前仅用于区分 CNY 等法币余额。
 * @property risk 余额接口对应的稳定可用性语义；是否足够支付必须由具体任务计费结果决定。
 */
data class ProfileWalletCenterUiModel(
    val rhbPoints: String?,
    val walletBalance: String?,
    val walletCurrency: String,
    val risk: ProfileWalletRisk,
) {
    companion object {
        val Hidden = ProfileWalletCenterUiModel(
            rhbPoints = null,
            walletBalance = null,
            walletCurrency = "CNY",
            risk = ProfileWalletRisk.BalanceUnavailable,
        )
    }
}

/**
 * Profile 会员区块的 Presentation 状态。
 *
 * @property levelName 会员等级名称；为空表示未开通或接口未返回等级。
 * @property remainingDays 剩余天数字符串，保持服务端精度。
 * @property expiresAt 到期日期字符串，保持服务端返回粒度。
 * @property status 会员是否有效、过期或未知。
 */
data class ProfileMembershipCenterUiModel(
    val levelName: String?,
    val remainingDays: String?,
    val expiresAt: String?,
    val status: ProfileMembershipStatus,
) {
    companion object {
        val None = ProfileMembershipCenterUiModel(
            levelName = null,
            remainingDays = null,
            expiresAt = null,
            status = ProfileMembershipStatus.None,
        )
    }
}

/**
 * Profile 资产中心的聚合 Presentation 状态。
 *
 * 该模型只表达可展示资产事实和稳定状态，不包含支付提交、计费确认或服务端错误原文。
 */
data class ProfileAssetCenterUiModel(
    val visible: Boolean,
    val loadState: ProfileAssetLoadState,
    val wallet: ProfileWalletCenterUiModel,
    val membership: ProfileMembershipCenterUiModel,
) {
    companion object {
        val Hidden = ProfileAssetCenterUiModel(
            visible = false,
            loadState = ProfileAssetLoadState.Hidden,
            wallet = ProfileWalletCenterUiModel.Hidden,
            membership = ProfileMembershipCenterUiModel.None,
        )
    }
}

internal fun ProfileUiState.toProfileAssetCenterUiModel(): ProfileAssetCenterUiModel {
    if (!isLoggedIn && user == null) {
        return ProfileAssetCenterUiModel.Hidden
    }
    val loadState = when {
        isLoading && user == null -> ProfileAssetLoadState.Loading
        error != null && user == null -> ProfileAssetLoadState.Failed
        isAccountStatusLoadFailed && user?.walletInfo == null && accountStatus == null -> ProfileAssetLoadState.Failed
        else -> ProfileAssetLoadState.Loaded
    }
    return ProfileAssetCenterUiModel(
        visible = true,
        loadState = loadState,
        wallet = toWalletCenter(user, accountStatus, loadState),
        membership = toMembershipCenter(user?.memberInfo),
    )
}

private fun toWalletCenter(
    user: User?,
    accountStatus: AccountStatus?,
    loadState: ProfileAssetLoadState,
): ProfileWalletCenterUiModel {
    val rhbPoints = accountStatus?.remainCoins?.takeIf { it.isNotBlank() }
        ?: user?.totalCoin?.takeIf { it.isNotBlank() }
    val walletInfo = user?.walletInfo
    val balance = accountStatus?.remainMoney?.takeIf { it.isNotBlank() }
        ?: walletInfo?.balance?.let(::formatWalletAmount)
    val currency = accountStatus?.currency?.takeIf { it.isNotBlank() }
        ?: walletInfo?.currency?.takeIf { it.isNotBlank() }
        ?: "CNY"
    val risk = when {
        loadState == ProfileAssetLoadState.Failed -> ProfileWalletRisk.BalanceUnavailable
        rhbPoints == null && balance == null -> ProfileWalletRisk.BalanceUnavailable
        else -> ProfileWalletRisk.None
    }
    return ProfileWalletCenterUiModel(
        rhbPoints = rhbPoints,
        walletBalance = balance,
        walletCurrency = currency,
        risk = risk,
    )
}

private fun toMembershipCenter(memberInfo: MemberInfo?): ProfileMembershipCenterUiModel {
    val levelName = memberInfo?.memberName?.takeIf { it.isNotBlank() }
    if (levelName == null) {
        return ProfileMembershipCenterUiModel.None
    }
    val remainingDays = memberInfo.memberRemainingDays?.takeIf { it.isNotBlank() && it != "0" }
    val expiresAt = memberInfo.memberExpiredTime
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore(" ")
    val status = when {
        memberInfo.expired == true -> ProfileMembershipStatus.Expired
        memberInfo.expired == false && remainingDays != null -> ProfileMembershipStatus.Active
        memberInfo.expired == false -> ProfileMembershipStatus.Unknown
        remainingDays != null -> ProfileMembershipStatus.Active
        else -> ProfileMembershipStatus.Unknown
    }
    return ProfileMembershipCenterUiModel(
        levelName = levelName,
        remainingDays = remainingDays,
        expiresAt = expiresAt,
        status = status,
    )
}

private fun formatWalletAmount(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
