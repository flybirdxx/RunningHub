package com.runninghub.shared.data.mapper

import com.runninghub.shared.data.model.*
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.model.Tag as DomainTag

fun WebAppDto.toDomain(): WebApp = WebApp(
    id = id ?: "",
    title = title ?: "",
    description = desc ?: "",
    thumbnailUrl = thumbnailUrl,
    previewUrl = preview?.url,
    coverUrls = covers?.mapNotNull { it.url } ?: emptyList(),
    authorName = author?.name,
    authorAvatar = author?.avatar,
    authorId = author?.id,
    tags = tags?.map { it.toDomain() } ?: emptyList(),
    likeCount = (statisticsInfo?.likeCount ?: likeCount ?: "0").toIntSafe(),
    collectCount = (statisticsInfo?.collectCount ?: collectCount ?: "0").toIntSafe(),
    useCount = (statisticsInfo?.useCount ?: useCount ?: "0").toIntSafe(),
    viewCount = (statisticsInfo?.pv ?: pv ?: "0").toIntSafe()
)

fun WebAppDetailDto.toDomain(): WebAppDetail = WebAppDetail(
    id = id ?: "",
    name = name ?: "",
    description = description,
    publishTime = publishTime,
    authorName = getDisplayName(),
    authorAvatar = getDisplayAvatar(),
    authorId = owner?.id,
    tags = tags?.map { it.toDomain() } ?: emptyList(),
    inputNodes = inputNodes?.map { it.toDomain() } ?: emptyList(),
    coverUrls = covers?.mapNotNull { it.url } ?: emptyList(),
    stats = AppStats(
        likeCount = (statisticsInfo?.likeCount ?: "0").toIntSafe(),
        collectCount = (statisticsInfo?.collectCount ?: "0").toIntSafe(),
        useCount = (statisticsInfo?.useCount ?: "0").toIntSafe(),
        viewCount = (statisticsInfo?.pv ?: "0").toIntSafe()
    )
)

fun InputNodeDto.toDomain(): InputNode = InputNode(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldType = fieldType,
    description = description,
    options = getOptions()
)

fun TagSimpleDto.toDomain(): DomainTag = DomainTag(id = id, name = name)

fun TagDto.toDomain(): DomainTag = DomainTag(
    id = id ?: "",
    name = name ?: "",
    children = children?.map { it.toDomain() } ?: emptyList()
)

fun UserDto.toDomain(): com.runninghub.shared.domain.model.User =
    com.runninghub.shared.domain.model.User(
        id = id,
        nickName = nickName ?: "",
        avatarUrl = headIcon,
        mobile = mobile,
        totalCoin = totalCoin,
        memberName = memberInfo?.memberName,
        memberExpiredTime = memberInfo?.memberExpiredTime,
        balance = walletInfo?.balance ?: 0.0,
        currency = walletInfo?.currency,
        apiKey = apiKey,
        apiType = apiType,
        introduce = introduce,
        fanCount = (fanCount ?: "0").toIntSafe(),
        followCount = (followCount ?: "0").toIntSafe(),
        likeCount = (likeCount ?: "0").toIntSafe(),
        collectCount = (collectCount ?: "0").toIntSafe()
    )

fun AccountStatusDto.toDomain(): AccountStatus = AccountStatus(
    remainCoins = remainCoins ?: "0",
    currentTaskCounts = currentTaskCounts ?: "0",
    remainMoney = remainMoney,
    currency = currency,
    apiType = apiType ?: ""
)

private fun String.toIntSafe(): Int = try {
    this.toDouble().toInt()
} catch (_: NumberFormatException) {
    0
}
