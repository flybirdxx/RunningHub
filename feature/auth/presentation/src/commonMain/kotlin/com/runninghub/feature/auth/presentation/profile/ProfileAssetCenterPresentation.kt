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
    GenerationCapacityUnknown,
    LowBalance,
    InsufficientBalance,
}

/** 会员区块可展示的稳定状态。 */
enum class ProfileMembershipStatus {
    None,
    Active,
    Expired,
    Unknown,
}

/** 会员区块当前支持的主操作。 */
enum class ProfileMembershipAction {
    Renew,
}

/** 会员权益摘要是否来自可信数据。 */
enum class ProfileMembershipBenefitStatus {
    Available,
    Unavailable,
}

/** 消费明细类型。 */
enum class ProfileTransactionType {
    Generation,
    Refund,
    Recharge,
    Membership,
}

/** 消费明细状态。 */
enum class ProfileTransactionStatus {
    Succeeded,
    Pending,
    Failed,
    Unknown,
}

/**
 * Profile 钱包区块的 Presentation 状态。
 *
 * @property rhbPoints RHB 点数，优先来自账户状态；为空表示当前不能把未知余额当作 0。
 * @property walletBalance CNY 钱包余额文本，不与 RHB 点数合并展示。
 * @property walletCurrency 钱包余额币种，当前仅用于区分 CNY 等法币余额。
 * @property risk 余额或预计生成次数对应的稳定风险语义。
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
 * @property benefitStatus 权益摘要是否有可信来源；当前没有权益列表接口时保持不可用。
 * @property primaryAction 会员区块主操作，本轮仅提供续费入口语义。
 */
data class ProfileMembershipCenterUiModel(
    val levelName: String?,
    val remainingDays: String?,
    val expiresAt: String?,
    val status: ProfileMembershipStatus,
    val benefitStatus: ProfileMembershipBenefitStatus,
    val primaryAction: ProfileMembershipAction,
) {
    companion object {
        val None = ProfileMembershipCenterUiModel(
            levelName = null,
            remainingDays = null,
            expiresAt = null,
            status = ProfileMembershipStatus.None,
            benefitStatus = ProfileMembershipBenefitStatus.Unavailable,
            primaryAction = ProfileMembershipAction.Renew,
        )
    }
}

/**
 * Profile 消费明细单项 Presentation 状态。
 *
 * @property relatedTaskId 关联任务 ID；为空时 UI 不得声称可以跳转 Task Detail。
 */
data class ProfileTransactionUiModel(
    val id: String,
    val type: ProfileTransactionType,
    val amount: String,
    val status: ProfileTransactionStatus,
    val relatedTaskId: String?,
) {
    val canOpenTaskDetail: Boolean
        get() = !relatedTaskId.isNullOrBlank()
}

/**
 * Profile 消费明细列表 Presentation 状态。
 *
 * 当前没有独立账单接口，因此默认显式输出空态，后续接入真实账单时复用同一模型。
 */
data class ProfileTransactionCenterUiModel(
    val loadState: ProfileAssetLoadState,
    val transactions: List<ProfileTransactionUiModel>,
) {
    companion object {
        val Empty = ProfileTransactionCenterUiModel(
            loadState = ProfileAssetLoadState.Empty,
            transactions = emptyList(),
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
    val transactions: ProfileTransactionCenterUiModel,
) {
    companion object {
        val Hidden = ProfileAssetCenterUiModel(
            visible = false,
            loadState = ProfileAssetLoadState.Hidden,
            wallet = ProfileWalletCenterUiModel.Hidden,
            membership = ProfileMembershipCenterUiModel.None,
            transactions = ProfileTransactionCenterUiModel.Empty,
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
        wallet = toWalletCenter(user, accountStatus, loadState, isAccountStatusLoadFailed),
        membership = toMembershipCenter(user?.memberInfo),
        transactions = ProfileTransactionCenterUiModel.Empty,
    )
}

private fun toWalletCenter(
    user: User?,
    accountStatus: AccountStatus?,
    loadState: ProfileAssetLoadState,
    accountStatusFailed: Boolean,
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
        rhbPoints.toNumericBalance() == 0.0 -> ProfileWalletRisk.InsufficientBalance
        accountStatusFailed -> ProfileWalletRisk.GenerationCapacityUnknown
        else -> ProfileWalletRisk.GenerationCapacityUnknown
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
        benefitStatus = ProfileMembershipBenefitStatus.Unavailable,
        primaryAction = ProfileMembershipAction.Renew,
    )
}

private fun String?.toNumericBalance(): Double? =
    this
        ?.replace(",", "")
        ?.filter { it.isDigit() || it == '.' || it == '-' }
        ?.toDoubleOrNull()

private fun formatWalletAmount(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
