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
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationTaskDetail
import com.runninghub.feature.task.domain.GenerationTaskDetailField
import com.runninghub.feature.task.domain.GenerationTaskDetailFieldKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
 * 将控制台任务宽表记录转换为通用历史条目。
 *
 * 宽表接口覆盖所有任务来源，并且运行中任务通常没有输出列表，因此 outputs 始终先保持为空；
 * 历史页会把它与 output history 或后续详情接口返回的输出信息再合并。
 */
internal fun BillingUsageTaskDto.toHistoryDomain(): TaskHistoryItem = TaskHistoryItem(
    taskId = taskId,
    outputs = emptyList(),
    status = TaskExecutionStatus.fromRaw(taskStatus),
    taskCostTime = moneyDuration?.takeIf { it.isNotBlank() && it != "0" }
        ?: coinUsedDuration?.takeIf { it.isNotBlank() && it != "0" },
    createTime = createTime ?: taskStartTime,
    taskName = taskName
        ?: workflowName
        ?: skuNameCn
        ?: skuName,
    webappId = webappId ?: workflowId ?: skuId,
    taskCategoryCode = taskCategoryCode ?: taskResourceType ?: originalTaskCategory,
    taskCategoryDisplay = taskCategoryDisplay,
    taskRelation = taskRelation,
    parentTaskId = parentTaskId,
    moneyAmount = moneyAmount,
    currency = currency,
    coinAmount = coinAmount,
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

/**
 * 将控制台任务详情转换为统一历史详情模型。
 *
 * 请求和响应详情在这里完成递归脱敏；后续 Presentation 和 UI 只能看到安全 JSON 文本，
 * 不再接触 API Key、Authorization、Cookie、Token 等敏感字段。
 */
internal fun OpenApiCallLogDetailDataDto.toDomain(requestedTaskId: String): GenerationTaskDetail {
    val resolvedTaskId = basicInfo?.taskId?.takeIf { it.isNotBlank() } ?: requestedTaskId
    val title = basicInfo?.apiName?.takeIf { it.isNotBlank() }
    val status = basicInfo?.taskStatus?.takeIf { it.isNotBlank() } ?: "UNKNOWN"
    val rhCoins = basicInfo?.coinNum.asDetailText() ?: costInfo?.coinNum.asDetailText()
    val finalAmount = costInfo?.afterDiscountAmount.asDetailText()
        ?: costInfo?.finalAmount.asDetailText()
        ?: costInfo?.amount.asDetailText()
        ?: basicInfo?.amount.asDetailText()
    return GenerationTaskDetail(
        taskId = resolvedTaskId,
        title = title,
        sourceLabel = basicInfo?.apiType?.takeIf { it.isNotBlank() },
        status = status,
        duration = basicInfo?.duration.asDetailText(),
        rhCoins = rhCoins,
        finalAmount = finalAmount,
        outputs = list.mapIndexedNotNull { index, output -> output.toGenerationHistoryOutput(resolvedTaskId, index) },
        basicFields = basicInfo.toBasicFields(resolvedTaskId, title),
        costFields = costInfo.toCostFields(),
        requestInfo = requestInfo?.apiRequestParams?.toSanitizedJsonString(),
        responseInfo = responseInfo?.toSanitizedPrettyJsonString(),
    )
}

private fun OpenApiCallLogBasicInfoDto?.toBasicFields(
    taskId: String,
    title: String?,
): List<GenerationTaskDetailField> = listOfNotNull(
    detailField(GenerationTaskDetailFieldKey.TASK_ID, taskId),
    detailField(GenerationTaskDetailFieldKey.CALL_TIME, this?.callTime),
    detailField(GenerationTaskDetailFieldKey.TASK_NAME, title),
    detailField(GenerationTaskDetailFieldKey.TASK_SOURCE, this?.apiType),
    detailField(GenerationTaskDetailFieldKey.CALL_TYPE, this?.callMethod ?: this?.callType ?: this?.apiType),
    detailField(GenerationTaskDetailFieldKey.ACCOUNT, this?.account ?: this?.accountId),
    detailField(GenerationTaskDetailFieldKey.API_KEY, this?.apiKeyName ?: this?.apiKey?.maskSecret()),
    detailField(GenerationTaskDetailFieldKey.API_KEY_TYPE, this?.apiKeyType.asDetailText()),
    detailField(GenerationTaskDetailFieldKey.MODE, this?.mode),
)

private fun OpenApiCallLogCostInfoDto?.toCostFields(): List<GenerationTaskDetailField> = listOfNotNull(
    detailField(
        GenerationTaskDetailFieldKey.ORIGINAL_AMOUNT,
        this?.originalAmount.asDetailText() ?: this?.originAmount.asDetailText() ?: this?.amount.asDetailText(),
    ),
    detailField(GenerationTaskDetailFieldKey.DISCOUNT_RATIO, this?.discountRatio.asDetailText() ?: this?.discountRate.asDetailText()),
    detailField(GenerationTaskDetailFieldKey.DISCOUNT_AMOUNT, this?.discountAmount.asDetailText()),
    detailField(
        GenerationTaskDetailFieldKey.FINAL_AMOUNT,
        this?.afterDiscountAmount.asDetailText() ?: this?.finalAmount.asDetailText() ?: this?.amount.asDetailText(),
    ),
    detailField(GenerationTaskDetailFieldKey.RH_COINS, this?.coinNum.asDetailText()),
)

private fun detailField(
    key: GenerationTaskDetailFieldKey,
    value: String?,
): GenerationTaskDetailField? {
    val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return GenerationTaskDetailField(key = key, value = text)
}

private fun TaskHistoryOutputDto.toGenerationHistoryOutput(
    taskId: String,
    index: Int,
): GenerationHistoryOutput? {
    val resolvedUrl = fileUrl?.takeIf { it.isNotBlank() }
        ?: filePreviewUrl?.takeIf { it.isNotBlank() }
        ?: return null
    val outputIdentity = id?.takeIf { it.isNotBlank() }
        ?: outputName?.takeIf { it.isNotBlank() }
        ?: fileUrl?.takeIf { it.isNotBlank() }
        ?: filePreviewUrl?.takeIf { it.isNotBlank() }
        ?: index.toString()
    return GenerationHistoryOutput(
        outputId = "detail:$taskId:$outputIdentity",
        url = resolvedUrl,
        type = outputType?.takeIf { it.isNotBlank() } ?: resolvedUrl.toDetailOutputType(),
        thumbnailUrl = filePreviewUrl?.takeIf { it.isNotBlank() } ?: resolvedUrl,
        outputName = outputName,
        expireDays = expireDays,
    )
}

private fun JsonElement?.asDetailText(): String? {
    val element = this ?: return null
    val text = if (element is JsonPrimitive) {
        element.contentOrNull ?: element.toString()
    } else {
        DETAIL_JSON.encodeToString(JsonElement.serializer(), element)
    }
    return text.trim().takeIf { it.isNotBlank() && it != "null" }
}

private fun String.toSanitizedJsonString(): String? {
    val raw = trim().takeIf { it.isNotBlank() } ?: return null
    return try {
        DETAIL_JSON.parseToJsonElement(raw).toSanitizedPrettyJsonString()
    } catch (_: Exception) {
        raw.redactSensitiveText()
    }
}

private fun JsonElement.toSanitizedPrettyJsonString(): String =
    DETAIL_JSON.encodeToString(JsonElement.serializer(), redactSensitiveJson())

private fun JsonElement.redactSensitiveJson(): JsonElement = when (this) {
    is JsonObject -> JsonObject(mapValues { (key, value) ->
        if (key.isSensitiveJsonKey()) JsonPrimitive(REDACTED_SECRET) else value.redactSensitiveJson()
    })
    is JsonArray -> JsonArray(map { it.redactSensitiveJson() })
    else -> this
}

private fun String.isSensitiveJsonKey(): Boolean {
    val normalized = lowercase().replace("_", "").replace("-", "")
    return normalized in SENSITIVE_JSON_KEYS ||
        normalized.endsWith("token") ||
        normalized.endsWith("secret")
}

private fun String.redactSensitiveText(): String =
    SENSITIVE_TEXT_PATTERN.replace(this) { match ->
        "${match.groupValues[1]}$REDACTED_SECRET${match.groupValues[3]}"
    }

private fun String.maskSecret(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.length <= 10) {
        REDACTED_SECRET
    } else {
        "${trimmed.take(5)}********${trimmed.takeLast(4)}"
    }
}

private fun String.toDetailOutputType(): String {
    val normalized = substringBefore('?').substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return normalized.takeIf { it.isNotBlank() } ?: "file"
}

private val DETAIL_JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    prettyPrint = true
}

private val SENSITIVE_JSON_KEYS = setOf(
    "apikey",
    "authorization",
    "cookie",
    "password",
    "validtoken",
    "accesstoken",
    "refreshtoken",
)
private const val REDACTED_SECRET = "******"
private val SENSITIVE_TEXT_PATTERN =
    Regex("(\"(?:apiKey|authorization|cookie|validToken|accessToken|refreshToken|password)\"\\s*:\\s*\")([^\"]*)(\")")
