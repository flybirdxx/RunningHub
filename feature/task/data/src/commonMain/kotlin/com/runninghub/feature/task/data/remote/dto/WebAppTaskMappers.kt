package com.runninghub.feature.task.data.remote.dto

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.Cover
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.StatisticsInfo
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.model.TaskFailedReason
import com.runninghub.core.model.TaskHistoryItem
import com.runninghub.core.model.TaskHistoryOutput
import com.runninghub.core.model.TaskOutput
import com.runninghub.core.model.TaskResult
import com.runninghub.core.model.UploadResult

/**
 * 将 API 调用示例详情 DTO 转换为 Domain AppDetail。
 *
 * 该接口需要 API Key，属于 Task Data；映射结果仍复用核心详情模型，保证详情页运行面板
 * 与公开详情页读取到的字段结构一致。
 */
internal fun WebAppTaskDetailDto.toDomain(): AppDetail = AppDetail(
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
    instanceType = instanceType,
)

/**
 * 将 Domain 输入节点转换为远端任务提交 DTO。
 */
internal fun InputNode.toDto(): WebAppTaskInputNodeDto = WebAppTaskInputNodeDto(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldData = fieldData,
    fieldType = fieldType,
    description = description,
    descriptionEn = descriptionEn,
)

/**
 * 将输入节点 DTO 转换为 Domain 输入节点。
 */
internal fun WebAppTaskInputNodeDto.toDomain(): InputNode = InputNode(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldData = fieldData,
    fieldType = fieldType,
    description = description,
    descriptionEn = descriptionEn,
)

/**
 * 将作者 DTO 转换为 Domain 作者模型。
 */
internal fun WebAppTaskAuthorDto.toDomain(): Author = Author(
    id = id,
    name = name,
    avatar = avatar,
    intro = intro,
    followCount = followCount ?: "0",
    fansCount = fansCount ?: "0",
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    bgImage = bgImage,
)

/**
 * 将轻量标签 DTO 转换为 Domain 标签模型。
 */
internal fun WebAppTaskTagSimpleDto.toDomain(): TagSimple =
    TagSimple(id = id, name = name, nameEn = nameEn, labels = labels)

/**
 * 将封面 DTO 转换为 Domain 封面模型。
 */
internal fun WebAppTaskCoverDto.toDomain(): Cover =
    Cover(url = thumbnailUri ?: url, imageWidth = imageWidth, imageHeight = imageHeight)

/**
 * 将统计 DTO 转换为 Domain 统计模型。
 */
internal fun WebAppTaskStatisticsInfoDto.toDomain(): StatisticsInfo = StatisticsInfo(
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    useCount = useCount ?: "0",
    pv = pv ?: "0",
)

/**
 * 将任务提交响应 DTO 转换为 Domain TaskResult。
 *
 * Data 层负责收口远端 taskStatus 字符串，Presentation 不需要判断服务端协议值。
 */
internal fun TaskRunResponseDto.toDomain(): TaskResult = TaskResult(
    netWssUrl = netWssUrl,
    taskId = taskId,
    clientId = clientId,
    status = TaskExecutionStatus.fromRaw(taskStatus),
    promptTips = promptTips,
)

/**
 * 将任务输出 DTO 转换为 Domain TaskOutput。
 */
internal fun TaskOutputDto.toDomain(): TaskOutput = TaskOutput(
    fileUrl = fileUrl,
    fileName = fileName,
    fileType = fileType,
    failedReason = failedReason?.toDomain(),
)

/**
 * 将任务失败原因 DTO 转换为 Domain 失败原因。
 */
internal fun TaskFailedReasonDto.toDomain(): TaskFailedReason =
    TaskFailedReason(
        nodeName = nodeName,
        exceptionMessage = exceptionMessage,
        traceback = traceback,
    )

/**
 * 将上传响应 DTO 转换为 Domain 上传结果。
 */
internal fun TaskUploadResponseDto.toDomain(): UploadResult = UploadResult(
    fileName = fileName,
    fileType = fileType,
)

/**
 * 将任务历史 DTO 转换为 Domain 历史条目。
 *
 * 旧历史接口仍返回 taskStatus 字段，这里统一转成领域状态以便历史页不依赖远端字符串。
 */
internal fun TaskHistoryItemDto.toDomain(): TaskHistoryItem = TaskHistoryItem(
    taskId = taskId,
    outputs = outputList?.map { it.toDomain() } ?: emptyList(),
    status = TaskExecutionStatus.fromRaw(taskStatus),
    taskCostTime = taskCostTime,
    createTime = createTime,
    taskName = taskName,
    webappId = webappId,
)

/**
 * 将任务历史输出 DTO 转换为 Domain 历史输出。
 */
internal fun TaskHistoryOutputDto.toDomain(): TaskHistoryOutput = TaskHistoryOutput(
    id = id,
    outputName = outputName,
    outputType = outputType,
    fileUrl = fileUrl,
    filePreviewUrl = filePreviewUrl,
    outputSize = outputSize,
    expireDays = expireDays,
)
