package com.runninghub.feature.auth.data.remote.dto

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo

/**
 * 将账户状态 DTO 映射为 Core 领域模型。
 *
 * 余额、任务数和货币字段保留服务端原始字符串，避免 Data 层自行转换单位或格式。
 */
fun AccountStatusDto.toDomain(): AccountStatus = AccountStatus(
    remainCoins = remainCoins,
    currentTaskCounts = currentTaskCounts,
    remainMoney = remainMoney,
    currency = currency,
    apiType = apiType,
)

/**
 * 将用户 DTO 映射为 Core 用户模型。
 *
 * 统计字段缺失时按服务端历史约定降级为 "0"，防止 Profile 页面出现空文本。
 * API Key 字段保留给 Repository 写入凭据存储，Presentation 不应直接展示。
 */
fun UserDto.toDomain(): User = User(
    id = id,
    nickName = nickName,
    headIcon = headIcon,
    mobile = mobile,
    totalCoin = totalCoin,
    memberInfo = memberInfo?.toDomain(),
    walletInfo = walletInfo?.toDomain(),
    apiKey = apiKey,
    apiType = apiType,
    introduce = introduce,
    fanCount = fanCount ?: "0",
    followCount = followCount ?: "0",
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
)

/** 将会员信息 DTO 映射为领域模型，保持服务端时间和天数字符串。 */
fun MemberInfoDto.toDomain(): MemberInfo = MemberInfo(
    memberName = memberName,
    memberExpiredTime = memberExpiredTime,
    userType = userType,
    memberRemainingDays = memberRemainingDays,
    expired = expired,
)

/** 将钱包信息 DTO 映射为领域模型，余额数值和币种都由服务端定义。 */
fun WalletInfoDto.toDomain(): WalletInfo = WalletInfo(
    balance = balance,
    currency = currency,
    currencySymbol = currencySymbol,
)
