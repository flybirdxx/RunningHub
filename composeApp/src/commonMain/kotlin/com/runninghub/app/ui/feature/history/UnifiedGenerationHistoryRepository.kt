package com.runninghub.app.ui.feature.history

import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.model.TaskHistoryItem
import com.runninghub.core.model.TaskHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryPage
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistorySource
import com.runninghub.feature.task.domain.GenerationTaskDetail
import com.runninghub.feature.task.domain.WebAppTaskHistoryRepository

/**
 * 迁移期统一历史仓库。
 *
 * History 页面需要展示当前已接入的多种任务来源。QuickCreate 源提供列表、详情和取消能力；
 * 控制台任务宽表提供 AI 应用、模型 API、工作流等统一列表。因此这里聚合两者的列表，
 * 详情和取消仍按来源能力处理。
 */
internal class UnifiedGenerationHistoryRepository(
    quickCreationTaskHistoryRepository: QuickCreationTaskHistoryRepository,
    private val webAppTaskHistoryRepository: WebAppTaskHistoryRepository,
    private val webAppTaskHistoryOverlayStore: WebAppTaskHistoryOverlayStore = WebAppTaskHistoryOverlayStore(),
) : GenerationHistoryRepository {
    private val quickCreateAdapter = QuickCreateGenerationHistoryRepositoryAdapter(quickCreationTaskHistoryRepository)
    private var lastRemoteTaskItems: List<GenerationHistoryItem> = emptyList()
    private var lastMergedItems: List<GenerationHistoryItem> = emptyList()

    override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> {
        val quickResult = quickCreateAdapter.listHistory(page = page, size = size)
        val webAppResult = webAppTaskHistoryRepository.getTaskHistory(pageNum = page, pageSize = size)

        if (quickResult.isFailure && webAppResult.isFailure) {
            return Result.failure(quickResult.exceptionOrNull() ?: webAppResult.exceptionOrNull() ?: IllegalStateException())
        }

        val quickPage = quickResult.getOrNull()
        val remoteTaskItems = webAppResult.getOrNull()
            ?.mapNotNull { it.toGenerationHistoryItem() }
            .orEmpty()
        val quickItems = quickPage?.items.orEmpty()
        val quickItemsByTaskId = quickItems.associateBy { it.taskId }
        val mergedRemoteItems = remoteTaskItems.map { item ->
            item.mergeSupplement(quickItemsByTaskId[item.taskId])
        }
        val overlayItems = webAppTaskHistoryOverlayStore.items()
            .filterNot { overlay -> mergedRemoteItems.any { it.taskId == overlay.taskId } }
        lastRemoteTaskItems = mergedRemoteItems

        val mergedItems = (mergedRemoteItems + overlayItems + quickItems)
            .distinctBy { item -> item.taskId }
        lastMergedItems = mergedItems

        return Result.success(
            GenerationHistoryPage(
                page = page,
                size = size,
                total = mergedItems.size,
                items = mergedItems,
            )
        )
    }

    override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> {
        if (outputId.startsWith(WEBAPP_OUTPUT_ID_PREFIX)) {
            val item = lastRemoteTaskItems.firstOrNull { historyItem ->
                historyItem.outputs.any { output -> output.outputId == outputId }
            }
            return item?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("WEBAPP_HISTORY_DETAIL_UNAVAILABLE"))
        }
        return quickCreateAdapter.getHistoryDetail(outputId)
    }

    override suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail> {
        val cachedItem = lastMergedItems.firstOrNull { it.taskId == taskId }
        if (cachedItem == null || cachedItem.source == GenerationHistorySource.QUICK_CREATION) {
            return quickCreateAdapter.getTaskDetail(taskId)
        }

        return webAppTaskHistoryRepository.getTaskDetail(taskId)
            .map { detail -> detail.withCachedListContext(cachedItem) }
            .recoverCatching { cachedItem.toFallbackTaskDetail() }
    }

    override suspend fun cancelTask(taskId: String): Result<Unit> {
        val remoteItem = lastRemoteTaskItems.firstOrNull { it.taskId == taskId }
        if (remoteItem != null && remoteItem.source != GenerationHistorySource.QUICK_CREATION) {
            return Result.failure(IllegalStateException("WEBAPP_HISTORY_CANCEL_UNAVAILABLE"))
        }
        return quickCreateAdapter.cancelTask(taskId)
    }
}

private fun TaskHistoryItem.toGenerationHistoryItem(): GenerationHistoryItem? {
    val resolvedTaskId = taskId?.takeIf { it.isNotBlank() } ?: return null
    val positiveMoneyAmount = moneyAmount?.takeIf { it > 0.0 }
    val positiveCoinAmount = coinAmount?.takeIf { it > 0.0 }
    return GenerationHistoryItem(
        taskId = resolvedTaskId,
        source = toGenerationHistorySource(),
        status = status.toHistoryStatus(),
        modelId = webappId,
        taskType = taskName ?: taskCategoryDisplay ?: webappId,
        costAmount = positiveMoneyAmount ?: positiveCoinAmount ?: 0.0,
        costCurrency = currency?.takeIf { positiveMoneyAmount != null }
            ?: positiveCoinAmount?.let { RH_COIN_CURRENCY },
        costTime = taskCostTime,
        outputs = outputs.mapNotNull { it.toGenerationHistoryOutput(resolvedTaskId) },
    )
}

private fun GenerationHistoryItem.mergeSupplement(supplement: GenerationHistoryItem?): GenerationHistoryItem =
    if (supplement == null) {
        this
    } else {
        copy(
            modelId = modelId ?: supplement.modelId,
            taskType = taskType ?: supplement.taskType,
            params = if (params.isEmpty()) supplement.params else params,
            outputs = if (outputs.isEmpty()) supplement.outputs else outputs,
        )
    }

private fun TaskHistoryItem.toGenerationHistorySource(): GenerationHistorySource {
    val sourceText = listOfNotNull(taskCategoryCode, taskCategoryDisplay)
        .joinToString(separator = "|")
        .uppercase()
    return when {
        "FAST_CREATE" in sourceText -> GenerationHistorySource.QUICK_CREATION
        "SKU_EXTERNAL_API" in sourceText || "模型" in sourceText -> GenerationHistorySource.STANDARD_MODEL
        "WORKFLOW" in sourceText || "工作流" in sourceText -> GenerationHistorySource.WORKFLOW
        "WEBAPP" in sourceText || "AI应用" in sourceText -> GenerationHistorySource.WEBAPP
        else -> GenerationHistorySource.WEBAPP
    }
}

private fun TaskHistoryOutput.toGenerationHistoryOutput(taskId: String): GenerationHistoryOutput? {
    val resolvedUrl = fileUrl?.takeIf { it.isNotBlank() }
        ?: filePreviewUrl?.takeIf { it.isNotBlank() }
        ?: return null
    val outputIdentity = id?.takeIf { it.isNotBlank() }
        ?: fileUrl?.takeIf { it.isNotBlank() }
        ?: filePreviewUrl?.takeIf { it.isNotBlank() }
        ?: outputName?.takeIf { it.isNotBlank() }
        ?: return null
    return GenerationHistoryOutput(
        outputId = "$WEBAPP_OUTPUT_ID_PREFIX$taskId:$outputIdentity",
        url = resolvedUrl,
        type = outputType?.takeIf { it.isNotBlank() } ?: resolvedUrl.toHistoryOutputType(),
        thumbnailUrl = filePreviewUrl?.takeIf { it.isNotBlank() } ?: resolvedUrl,
        outputName = outputName,
        expireDays = expireDays,
    )
}

private fun TaskExecutionStatus?.toHistoryStatus(): String = when (this) {
    TaskExecutionStatus.Submitted -> "SUBMITTED"
    TaskExecutionStatus.Queued -> "QUEUED"
    TaskExecutionStatus.Running -> "RUNNING"
    TaskExecutionStatus.Success -> "SUCCESS"
    TaskExecutionStatus.Failed -> "FAILED"
    TaskExecutionStatus.Cancelled -> "CANCELLED"
    is TaskExecutionStatus.Unknown -> rawValue ?: "UNKNOWN"
    null -> "UNKNOWN"
}

private fun String.toHistoryOutputType(): String {
    val normalized = substringBefore('?').substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return normalized.takeIf { it.isNotBlank() } ?: "file"
}

internal fun GenerationHistoryItem.toFallbackTaskDetail(): GenerationTaskDetail =
    GenerationTaskDetail(
        taskId = taskId,
        title = taskType ?: modelId,
        sourceLabel = source.key,
        status = status,
        duration = costTime,
        rhCoins = costCurrency?.takeIf { it == RH_COIN_CURRENCY }?.let { costAmount.toHistoryAmountText() },
        finalAmount = costCurrency?.takeIf { it != RH_COIN_CURRENCY }?.let { "${costAmount.toHistoryAmountText()} $it" },
        outputs = outputs,
    )

private fun GenerationTaskDetail.withCachedListContext(item: GenerationHistoryItem): GenerationTaskDetail =
    copy(
        title = title ?: item.taskType ?: item.modelId,
        sourceLabel = sourceLabel?.takeUnless { it.equals("API", ignoreCase = true) } ?: item.source.key,
        duration = duration ?: item.costTime,
        rhCoins = rhCoins ?: item.costCurrency?.takeIf { it == RH_COIN_CURRENCY }?.let { item.costAmount.toHistoryAmountText() },
        finalAmount = finalAmount
            ?: item.costCurrency?.takeIf { it != RH_COIN_CURRENCY }?.let { "${item.costAmount.toHistoryAmountText()} $it" },
        outputs = if (outputs.isEmpty()) item.outputs else outputs,
    )

private fun Double.toHistoryAmountText(): String {
    val raw = toString()
    return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
}

private const val WEBAPP_OUTPUT_ID_PREFIX = "webapp:"
private const val RH_COIN_CURRENCY = "RHB"
