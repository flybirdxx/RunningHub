package com.runninghub.feature.task.presentation

import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationTaskDetail

/** 任务详情页的稳定分区顺序。 */
enum class TaskHistoryDetailSectionType {
    STATUS_SUMMARY,
    RESULT_PREVIEW,
    ACTIONS,
    BILLING,
    PROMPT_PARAMETERS,
    TECHNICAL_DETAILS,
}

/** 任务详情页使用的稳定状态语义。 */
enum class TaskHistoryDetailStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS,
    CANCELED,
    UNKNOWN,
}

/** 任务详情页可展示的结果处理动作。 */
enum class TaskHistoryDetailAction {
    SAVE,
    DOWNLOAD,
    REUSE_PARAMETERS,
    RETRY,
}

/** 任务详情结果媒体类型。 */
enum class TaskHistoryDetailMediaType {
    IMAGE,
    VIDEO,
    FILE,
}

/** 任务详情页本地保存状态。 */
enum class TaskHistoryDetailSaveState {
    SAVED,
    NOT_SAVED,
    UNAVAILABLE,
}

/** 任务详情计费行类型。 */
enum class TaskHistoryDetailBillingKind {
    RH_COINS,
    FINAL_AMOUNT,
    DURATION,
}

/** 任务详情技术折叠区类型。 */
enum class TaskHistoryDetailTechnicalKind {
    REQUEST_INFO,
    RESPONSE_INFO,
}

/**
 * 任务详情输出文件的 UI 语义。
 *
 * @property outputId 输出稳定标识。
 * @property url 结果原始地址。
 * @property previewUrl 缩略图或预览图地址。
 * @property mediaType 可渲染媒体类型。
 * @property aspectRatio 可选宽高比；未知时交由 UI 使用稳定默认高度。
 * @property name 输出名称；为空表示服务端未返回。
 */
data class TaskHistoryDetailOutputUi(
    val outputId: String,
    val url: String,
    val previewUrl: String?,
    val mediaType: TaskHistoryDetailMediaType,
    val aspectRatio: Float?,
    val name: String?,
)

/**
 * 任务详情结果区语义。
 *
 * @property outputs 可预览输出集合。
 * @property expiry 第一项输出的过期提示语义；为空表示服务端未返回。
 */
data class TaskHistoryDetailResultUi(
    val outputs: List<TaskHistoryDetailOutputUi>,
    val expiry: TaskHistoryExpiryUi?,
)

/**
 * 任务详情计费行。
 *
 * @property kind 计费行类型。
 * @property value 已安全归一的计费值，不包含最终 UI 文案。
 */
data class TaskHistoryDetailBillingRowUi(
    val kind: TaskHistoryDetailBillingKind,
    val value: String,
)

/**
 * 任务详情计费摘要。
 *
 * @property rows 可展示计费行，顺序代表 UI 展示优先级。
 */
data class TaskHistoryDetailBillingUi(
    val rows: List<TaskHistoryDetailBillingRowUi>,
)

/**
 * 任务详情关键参数。
 *
 * @property key 参数名，来自已脱敏请求摘要。
 * @property value 参数值，已经由 Data 层过滤敏感字段。
 */
data class TaskHistoryDetailParameterUi(
    val key: String,
    val value: String,
)

/**
 * 任务详情技术信息折叠区。
 *
 * @property kind 技术信息类型。
 * @property content 已脱敏内容。
 * @property initiallyExpanded 是否默认展开；RM-10 固定为 false，避免首屏变成调试控制台。
 */
data class TaskHistoryDetailTechnicalSectionUi(
    val kind: TaskHistoryDetailTechnicalKind,
    val content: String,
    val initiallyExpanded: Boolean = false,
)

/**
 * 任务详情页 UI 状态。
 *
 * @property sectionOrder 首屏和后续分区顺序，必须让结果处理先于技术排查。
 * @property title 任务标题。
 * @property taskId 任务 ID，只在详情层可见。
 * @property sourceLabel 来源标签。
 * @property status 稳定任务状态。
 * @property result 结果预览语义；为空表示无结果。
 * @property actions 详情页可执行动作。
 * @property billing 计费摘要；为空表示服务端未提供扣费信息。
 * @property promptParameters Prompt 和关键参数摘要。
 * @property technicalSections 技术详情折叠区。
 * @property saveState 本地保存状态。
 */
data class TaskHistoryDetailUiModel(
    val sectionOrder: List<TaskHistoryDetailSectionType>,
    val title: String,
    val taskId: String,
    val sourceLabel: String?,
    val status: TaskHistoryDetailStatus,
    val result: TaskHistoryDetailResultUi?,
    val actions: List<TaskHistoryDetailAction>,
    val billing: TaskHistoryDetailBillingUi?,
    val promptParameters: List<TaskHistoryDetailParameterUi>,
    val technicalSections: List<TaskHistoryDetailTechnicalSectionUi>,
    val saveState: TaskHistoryDetailSaveState,
)

internal fun GenerationTaskDetail.toTaskHistoryDetailUiModel(): TaskHistoryDetailUiModel {
    val detailStatus = status.toTaskHistoryDetailStatus()
    val result = outputs.takeIf { it.isNotEmpty() }?.let { outputList ->
        TaskHistoryDetailResultUi(
            outputs = outputList.map { it.toTaskHistoryDetailOutputUi() },
            expiry = outputList.firstOrNull()?.toTaskHistoryDetailExpiryUi(),
        )
    }
    val billing = taskHistoryDetailBillingUi()
    return TaskHistoryDetailUiModel(
        sectionOrder = TaskHistoryDetailSectionType.entries.toList(),
        title = title?.takeIf { it.isNotBlank() } ?: taskId,
        taskId = taskId,
        sourceLabel = sourceLabel?.takeIf { it.isNotBlank() },
        status = detailStatus,
        result = result,
        actions = taskHistoryDetailActions(
            status = detailStatus,
            hasResult = result != null,
            hasReusableParams = requestParameters.isNotEmpty(),
        ),
        billing = billing,
        promptParameters = requestParameters.toTaskHistoryDetailParameters(),
        technicalSections = taskHistoryDetailTechnicalSections(),
        saveState = if (result != null && detailStatus == TaskHistoryDetailStatus.SUCCESS) {
            TaskHistoryDetailSaveState.NOT_SAVED
        } else {
            TaskHistoryDetailSaveState.UNAVAILABLE
        },
    )
}

private fun taskHistoryDetailActions(
    status: TaskHistoryDetailStatus,
    hasResult: Boolean,
    hasReusableParams: Boolean,
): List<TaskHistoryDetailAction> = buildList {
    if (status == TaskHistoryDetailStatus.SUCCESS && hasResult) {
        add(TaskHistoryDetailAction.SAVE)
        add(TaskHistoryDetailAction.DOWNLOAD)
    }
    if (hasReusableParams) {
        add(TaskHistoryDetailAction.REUSE_PARAMETERS)
    }
    if (status == TaskHistoryDetailStatus.FAILED) {
        add(0, TaskHistoryDetailAction.RETRY)
    }
}

private fun GenerationTaskDetail.taskHistoryDetailBillingUi(): TaskHistoryDetailBillingUi? {
    val rows = buildList {
        rhCoins?.takeIf { it.isNotBlank() }?.let {
            add(TaskHistoryDetailBillingRowUi(TaskHistoryDetailBillingKind.RH_COINS, it))
        }
        finalAmount?.takeIf { it.isNotBlank() }?.let {
            add(TaskHistoryDetailBillingRowUi(TaskHistoryDetailBillingKind.FINAL_AMOUNT, it))
        }
        duration?.takeIf { it.isNotBlank() }?.let {
            add(TaskHistoryDetailBillingRowUi(TaskHistoryDetailBillingKind.DURATION, it))
        }
    }
    return rows.takeIf { it.isNotEmpty() }?.let(::TaskHistoryDetailBillingUi)
}

private fun Map<String, String>.toTaskHistoryDetailParameters(): List<TaskHistoryDetailParameterUi> =
    entries
        .filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        .sortedWith(compareByDescending<Map.Entry<String, String>> { it.key.equals("prompt", ignoreCase = true) }
            .thenBy { it.key })
        .map { (key, value) -> TaskHistoryDetailParameterUi(key = key, value = value) }

private fun GenerationTaskDetail.taskHistoryDetailTechnicalSections(): List<TaskHistoryDetailTechnicalSectionUi> =
    buildList {
        requestInfo?.takeIf { it.isNotBlank() }?.let {
            add(TaskHistoryDetailTechnicalSectionUi(TaskHistoryDetailTechnicalKind.REQUEST_INFO, it))
        }
        responseInfo?.takeIf { it.isNotBlank() }?.let {
            add(TaskHistoryDetailTechnicalSectionUi(TaskHistoryDetailTechnicalKind.RESPONSE_INFO, it))
        }
    }

private fun GenerationHistoryOutput.toTaskHistoryDetailOutputUi(): TaskHistoryDetailOutputUi =
    TaskHistoryDetailOutputUi(
        outputId = outputId,
        url = url,
        previewUrl = displayThumbnailUrl,
        mediaType = when {
            isImage -> TaskHistoryDetailMediaType.IMAGE
            isVideo -> TaskHistoryDetailMediaType.VIDEO
            else -> TaskHistoryDetailMediaType.FILE
        },
        aspectRatio = width?.let { w -> height?.takeIf { it > 0 }?.let { h -> w.toFloat() / h.toFloat() } },
        name = outputName?.takeIf { it.isNotBlank() },
    )

private fun GenerationHistoryOutput.toTaskHistoryDetailExpiryUi(): TaskHistoryExpiryUi? {
    val remainingDays = expireDays?.takeIf { it.isNotBlank() }
    val expireTime = expireTime?.takeIf { it.isNotBlank() }
    return if (remainingDays != null || expireTime != null) {
        TaskHistoryExpiryUi(remainingDays = remainingDays, expireTime = expireTime)
    } else {
        null
    }
}

private fun String.toTaskHistoryDetailStatus(): TaskHistoryDetailStatus = when {
    isCompletedStatus() -> TaskHistoryDetailStatus.SUCCESS
    isFailedStatus() -> TaskHistoryDetailStatus.FAILED
    isCancelledStatus() -> TaskHistoryDetailStatus.CANCELED
    isBlank() -> TaskHistoryDetailStatus.UNKNOWN
    else -> TaskHistoryDetailStatus.IN_PROGRESS
}

private fun String.isCompletedStatus(): Boolean = lowercase() in setOf("success", "completed", "done")

private fun String.isFailedStatus(): Boolean =
    lowercase() in setOf("failed", "fail", "error")

private fun String.isCancelledStatus(): Boolean =
    lowercase() in setOf("canceled", "cancelled", "cancel")
