package com.runninghub.shared.data.remote.dto

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo
import com.runninghub.shared.domain.model.AudioResult
import com.runninghub.shared.domain.model.AudioTaskResult

/**
 * 将账户状态 DTO 转换为核心账户模型。
 *
 * shared 迁移期仍承载部分账户兼容接口，因此该映射暂时保留在 shared Data 层。
 */
fun AccountStatusDto.toDomain(): AccountStatus = AccountStatus(
    remainCoins = remainCoins,
    currentTaskCounts = currentTaskCounts,
    remainMoney = remainMoney,
    currency = currency,
    apiType = apiType,
)

/**
 * 将用户 DTO 转换为核心用户模型。
 *
 * 用户资料数据实现已迁出到 Auth Data；shared 中仅保留遗留调用点所需的兼容映射。
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

/**
 * 将会员 DTO 转换为核心会员模型。
 */
fun MemberInfoDto.toDomain(): MemberInfo = MemberInfo(
    memberName = memberName,
    memberExpiredTime = memberExpiredTime,
    userType = userType,
    memberRemainingDays = memberRemainingDays,
    expired = expired,
)

/**
 * 将钱包 DTO 转换为核心钱包模型。
 */
fun WalletInfoDto.toDomain(): WalletInfo = WalletInfo(
    balance = balance,
    currency = currency,
    currencySymbol = currencySymbol,
)

/**
 * 将 Audio 结果 DTO 转换为 shared 遗留 Audio 领域模型。
 */
fun AudioResultDto.toDomain(): AudioResult =
    AudioResult(
        url = url,
        outputType = outputType,
        text = text,
    )

/**
 * 将 Audio 任务查询 DTO 转换为 shared 遗留 Audio 任务模型。
 */
fun TaskQueryResultDto.toDomain(): AudioTaskResult = AudioTaskResult(
    taskId = taskId,
    status = status,
    errorCode = errorCode,
    errorMessage = errorMessage,
    results = results?.map { it.toDomain() },
)
