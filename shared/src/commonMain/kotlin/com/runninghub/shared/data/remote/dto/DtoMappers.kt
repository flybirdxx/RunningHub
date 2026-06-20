package com.runninghub.shared.data.remote.dto

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.Cover
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.PageData
import com.runninghub.core.model.StatisticsInfo
import com.runninghub.core.model.Tag
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo
import com.runninghub.core.model.WebApp
import com.runninghub.shared.domain.model.AudioResult
import com.runninghub.shared.domain.model.AudioTaskResult
import com.runninghub.shared.domain.model.TaskExecutionStatus
import com.runninghub.shared.domain.model.TaskFailedReason
import com.runninghub.shared.domain.model.TaskHistoryItem
import com.runninghub.shared.domain.model.TaskHistoryOutput
import com.runninghub.shared.domain.model.TaskOutput
import com.runninghub.shared.domain.model.TaskResult
import com.runninghub.shared.domain.model.UploadResult

fun WebAppDto.toDomain(): WebApp {
    val firstCover = covers?.firstOrNull()
    val rawCoverUrl = firstCover?.url ?: preview?.url
    val mediaType = inferMediaType(rawCoverUrl)
    val resolvedThumbnailUrl = thumbnailUrl ?: firstCover?.thumbnailUri
    val resolvedCoverUrl = when (mediaType) {
        CoverMediaType.VIDEO -> resolvedThumbnailUrl ?: rawCoverUrl
        else -> firstCover?.thumbnailUri ?: rawCoverUrl
    }
    return WebApp(
        id = id ?: "",
        title = title ?: "",
        description = desc,
        thumbnailUrl = resolvedThumbnailUrl,
        coverUrl = resolvedCoverUrl,
        coverMediaType = mediaType,
        videoUrl = rawCoverUrl.takeIf { mediaType == CoverMediaType.VIDEO },
        coverWidth = firstCover?.imageWidth,
        coverHeight = firstCover?.imageHeight,
        author = author?.toDomain(),
        tags = tags?.map { it.toDomain() } ?: emptyList(),
        likeCount = statisticsInfo?.likeCount ?: likeCount ?: "0",
        collectCount = statisticsInfo?.collectCount ?: collectCount ?: "0",
        useCount = statisticsInfo?.useCount ?: useCount ?: "0",
        pv = statisticsInfo?.pv ?: pv ?: "0",
        carefullyChosen = carefullyChosen,
    )
}

private fun inferMediaType(url: String?): CoverMediaType {
    if (url == null) return CoverMediaType.IMAGE
    val path = url.substringBefore("?").lowercase()
    return when {
        path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mov") -> CoverMediaType.VIDEO
        path.endsWith(".gif") -> CoverMediaType.GIF
        else -> CoverMediaType.IMAGE
    }
}

fun WebAppDetailDto.toDomain(): AppDetail = AppDetail(
    id = id ?: "",
    name = name,
    workflowId = workflowId,
    description = description,
    tags = tags?.map { it.toDomain() } ?: emptyList(),
    owner = owner?.toDomain(),
    publishTime = publishTime,
    inputNodes = inputNodes?.map { it.toDomain() } ?: emptyList(),
    covers = covers?.map { it.toDomain() } ?: emptyList(),
    statisticsInfo = statisticsInfo?.toDomain(),
    authorName = authorName,
    authorAvatar = authorAvatar,
    runningSuccessRate = runningSuccessRate,
    avgRunningSeconds = avgRunningSeconds,
    instanceType = instanceType
)

fun AuthorDto.toDomain(): Author = Author(
    id = id,
    name = name,
    avatar = avatar,
    intro = intro,
    followCount = followCount ?: "0",
    fansCount = fansCount ?: "0",
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    bgImage = bgImage
)

fun TagSimpleDto.toDomain(): TagSimple = TagSimple(id = id, name = name, nameEn = nameEn, labels = labels)

fun CoverDto.toDomain(): Cover = Cover(url = thumbnailUri ?: url, imageWidth = imageWidth, imageHeight = imageHeight)

fun StatisticsInfoDto.toDomain(): StatisticsInfo = StatisticsInfo(
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    useCount = useCount ?: "0",
    pv = pv ?: "0"
)

fun InputNodeDto.toDomain(): InputNode = InputNode(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldData = fieldData,
    fieldType = fieldType,
    description = description,
    descriptionEn = descriptionEn
)

fun InputNode.toDto(): InputNodeDto = InputNodeDto(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldData = fieldData,
    fieldType = fieldType,
    description = description,
    descriptionEn = descriptionEn
)

fun TagDto.toDomain(maxDepth: Int = 10): Tag = Tag(
    id = id,
    name = name,
    level = level,
    parentId = parentId,
    rang = rang,
    enable = enable,
    childTags = if (maxDepth > 0) childTags?.map { it.toDomain(maxDepth - 1) } else null
)

fun AccountStatusDto.toDomain(): AccountStatus = AccountStatus(
    remainCoins = remainCoins,
    currentTaskCounts = currentTaskCounts,
    remainMoney = remainMoney,
    currency = currency,
    apiType = apiType
)

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
    collectCount = collectCount ?: "0"
)

fun MemberInfoDto.toDomain(): MemberInfo = MemberInfo(
    memberName = memberName,
    memberExpiredTime = memberExpiredTime,
    userType = userType,
    memberRemainingDays = memberRemainingDays,
    expired = expired
)

fun WalletInfoDto.toDomain(): WalletInfo = WalletInfo(
    balance = balance,
    currency = currency,
    currencySymbol = currencySymbol
)

fun TaskRunResponseDto.toDomain(): TaskResult = TaskResult(
    netWssUrl = netWssUrl,
    taskId = taskId,
    clientId = clientId,
    // Data 层负责把服务端 taskStatus 协议值收口成领域状态，Presentation 不再判断远端字符串。
    status = TaskExecutionStatus.fromRaw(taskStatus),
    promptTips = promptTips
)

fun TaskOutputDto.toDomain(): TaskOutput = TaskOutput(
    fileUrl = fileUrl,
    fileName = fileName,
    fileType = fileType,
    failedReason = failedReason?.toDomain()
)

fun TaskFailedReasonDto.toDomain(): TaskFailedReason =
    TaskFailedReason(
        nodeName = nodeName,
        exceptionMessage = exceptionMessage,
        traceback = traceback
    )

fun UploadResponseDto.toDomain(): UploadResult = UploadResult(
    fileName = fileName,
    fileType = fileType
)

fun TaskHistoryItemDto.toDomain(): TaskHistoryItem = TaskHistoryItem(
    taskId = taskId,
    outputs = outputList?.map { it.toDomain() } ?: emptyList(),
    // 旧历史接口仍返回 taskStatus 字段，这里统一转成领域状态以便后续拆分历史仓库。
    status = TaskExecutionStatus.fromRaw(taskStatus),
    taskCostTime = taskCostTime,
    createTime = createTime,
    taskName = taskName,
    webappId = webappId,
)

fun TaskHistoryOutputDto.toDomain(): TaskHistoryOutput = TaskHistoryOutput(
    id = id,
    outputName = outputName,
    outputType = outputType,
    fileUrl = fileUrl,
    filePreviewUrl = filePreviewUrl,
    outputSize = outputSize,
    expireDays = expireDays,
)

fun PageDataDto<WebAppDto>.toDomain(): PageData<WebApp> = PageData(
    records = records.map { it.toDomain() },
    total = total,
    size = size,
    current = current,
    hasNext = hasNext
)

fun AudioResultDto.toDomain(): AudioResult =
    AudioResult(
        url = url,
        outputType = outputType,
        text = text
    )

fun TaskQueryResultDto.toDomain(): AudioTaskResult = AudioTaskResult(
    taskId = taskId,
    status = status,
    errorCode = errorCode,
    errorMessage = errorMessage,
    results = results?.map { it.toDomain() }
)
