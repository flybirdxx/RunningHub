package com.runninghub.core.model

/**
 * 当前用户的账户资料快照。
 *
 * 该模型来自登录恢复或用户信息接口，位于 core:model 层供多个 Feature 共享。敏感字段只能在受控业务链路中使用，
 * 不得写入日志、注释、测试快照或 UI 调试输出。
 *
 * @property id 用户唯一 ID。
 * @property nickName 用户昵称，服务端允许为空。
 * @property headIcon 头像 URL，可能为空。
 * @property mobile 手机号或脱敏手机号，具体格式由服务端决定。
 * @property totalCoin 账户总点数字符串，保持服务端精度和展示格式。
 * @property memberInfo 会员权益摘要，非会员或接口缺失时为空。
 * @property walletInfo 钱包余额摘要，接口缺失时为空。
 * @property apiKey 旧接口返回的 API Key，属于敏感凭据，调用方不得展示或记录。
 * @property apiType API Key 类型或套餐类型，保持服务端枚举字符串。
 * @property introduce 用户简介，可能为空。
 * @property fanCount 粉丝数字符串。
 * @property followCount 关注数字符串。
 * @property likeCount 获赞数字符串。
 * @property collectCount 被收藏数字符串。
 */
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

/**
 * 用户会员状态摘要。
 *
 * @property memberName 会员等级名称，普通用户可能为空。
 * @property memberExpiredTime 到期时间字符串，格式由服务端决定。
 * @property userType 服务端用户类型枚举字符串。
 * @property memberRemainingDays 剩余天数字符串，服务端未返回时为空。
 * @property expired 是否已过期；为空表示服务端未提供该判断。
 */
data class MemberInfo(
    val memberName: String?,
    val memberExpiredTime: String?,
    val userType: String?,
    val memberRemainingDays: String? = null,
    val expired: Boolean? = null
)

/**
 * 钱包余额信息。
 *
 * @property balance 余额数值，单位由 [currency] 和服务端账户规则决定。
 * @property currency 币种代码或服务端货币名称，可能为空。
 * @property currencySymbol 货币符号，可能为空。
 */
data class WalletInfo(
    val balance: Double,
    val currency: String?,
    val currencySymbol: String?
)

/**
 * 创作账户的运行额度和余额状态。
 *
 * @property remainCoins 剩余点数字符串，保持服务端精度。
 * @property currentTaskCounts 当前进行中的任务数量字符串。
 * @property remainMoney 剩余金额字符串，服务端未提供时为空。
 * @property currency 币种代码或名称，可能为空。
 * @property apiType 当前账户使用的 API 类型。
 */
data class AccountStatus(
    val remainCoins: String,
    val currentTaskCounts: String,
    val remainMoney: String?,
    val currency: String?,
    val apiType: String
)
