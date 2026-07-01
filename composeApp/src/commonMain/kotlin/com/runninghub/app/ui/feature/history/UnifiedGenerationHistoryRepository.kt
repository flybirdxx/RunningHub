package com.runninghub.app.ui.feature.history

import com.runninghub.app.history.WebAppTaskHistoryOverlayStore
import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.model.TaskHistoryItem
import com.runninghub.core.model.TaskHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
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
        if (cachedItem?.source == GenerationHistorySource.QUICK_CREATION) {
            return quickCreateAdapter.getTaskDetail(taskId)
        }

        val webAppDetail = webAppTaskHistoryRepository.getTaskDetail(taskId)
        if (webAppDetail.isSuccess) {
            return if (cachedItem == null) {
                webAppDetail
            } else {
                webAppDetail.map { detail -> detail.withCachedListContext(cachedItem) }
            }
        }

        return if (cachedItem == null) {
            quickCreateAdapter.getTaskDetail(taskId)
        } else {
            Result.success(cachedItem.toFallbackTaskDetail())
        }
    }

    override suspend fun cancelTask(taskId: String): Result<Unit> {
        val remoteItem = lastRemoteTaskItems.firstOrNull { it.taskId == taskId }
        if (remoteItem != null && remoteItem.source != GenerationHistorySource.QUICK_CREATION) {
            return Result.failure(IllegalStateException("WEBAPP_HISTORY_CANCEL_UNAVAILABLE"))
        }
        return quickCreateAdapter.cancelTask(taskId)
    }
}

/**
 * 将 QuickCreate 历史仓库适配为当前历史页仍在使用的通用历史仓库。
 *
 * 该类与 [UnifiedGenerationHistoryRepository] 保持在同一个文件中，作为 composeApp 组合层唯一的
 * History 迁移兼容桥。它只连接 Task Domain 的统一历史契约和 QuickCreate Domain 的窄历史仓库，
 * 不直接访问 Data 实现；删除条件是 Task Data 能直接提供列表、详情、取消和参数快照完整能力的
 * [GenerationHistoryRepository]。
 *
 * @param quickCreationTaskHistoryRepository QuickCreate 领域历史仓库，负责最近历史、详情和取消任务。
 */
internal class QuickCreateGenerationHistoryRepositoryAdapter(
    private val quickCreationTaskHistoryRepository: QuickCreationTaskHistoryRepository,
) : GenerationHistoryRepository {
    /**
     * 拉取通用历史列表。
     *
     * 请求委托给 QuickCreate 历史窄仓库；本层只执行迁移期模型转换，不直接访问远程 API 或 Data 实现。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页条数，单位为条。
     * @return 通用历史分页；底层失败时保持 [Result.failure]，由历史页决定展示和重试策略。
     */
    override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> =
        quickCreationTaskHistoryRepository.listQuickCreationHistory(page, size).map { it.toGenerationHistoryPage() }

    /**
     * 拉取单条历史详情。
     *
     * @param outputId 历史输出稳定标识，来源于通用历史列表中的输出项。
     * @return 通用历史任务详情；失败时不吞掉异常，避免历史页丢失可重试信号。
     */
    override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> =
        quickCreationTaskHistoryRepository.getQuickCreationHistoryDetail(outputId).map { it.toGenerationHistoryItem() }

    /**
     * 按任务 ID 构造 QuickCreate 详情抽屉数据。
     *
     * QuickCreate 旧详情接口以 outputId 为入口；任务详情抽屉以 taskId 为入口，因此这里先读取最近历史页，
     * 找到对应任务后用列表项字段构造安全降级详情。
     */
    override suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail> =
        listHistory(page = 1, size = 50).mapCatching { page ->
            page.items.firstOrNull { it.taskId == taskId }?.toFallbackTaskDetail()
                ?: throw IllegalStateException("QUICK_CREATE_TASK_DETAIL_UNAVAILABLE")
        }

    /**
     * 取消一个仍在运行的 QuickCreate 任务。
     *
     * @param taskId 服务端任务稳定标识。
     * @return 取消结果；失败时保留原始错误，调用方负责提示和刷新策略。
     */
    override suspend fun cancelTask(taskId: String): Result<Unit> =
        quickCreationTaskHistoryRepository.cancelQuickCreationTask(taskId)
}

/**
 * 将 QuickCreate 分页历史转换为通用历史分页。
 *
 * 分页数字和总数完全保留服务端返回值，避免兼容桥改变 QuickCreate 原有分页语义。
 */
internal fun QuickCreationHistoryPage.toGenerationHistoryPage(): GenerationHistoryPage =
    GenerationHistoryPage(
        page = page,
        size = size,
        total = total,
        items = items.map { it.toGenerationHistoryItem() },
    )

/**
 * 将 QuickCreate 历史任务转换为通用历史任务。
 *
 * source 固定写为 [GenerationHistorySource.QUICK_CREATION]，让通用历史页能在迁移期区分
 * QuickCreate 与后续仍可能接入的标准模型、音频或其他来源。
 */
internal fun QuickCreationHistoryItem.toGenerationHistoryItem(): GenerationHistoryItem =
    GenerationHistoryItem(
        taskId = taskId,
        source = GenerationHistorySource.QUICK_CREATION,
        status = status,
        modelId = skuId,
        taskType = taskType,
        costAmount = cashAmount,
        costCurrency = cashCurrency,
        costTime = taskCostTime,
        params = params,
        outputs = outputs.map { it.toGenerationHistoryOutput() },
    )

/**
 * 将 QuickCreate 输出文件转换为通用历史输出。
 *
 * 输出 URL、缩略图、尺寸和过期信息均来自 QuickCreate 历史接口；此处不重新推断媒体类型，
 * 保留服务端已经归一化后的输出类型，避免兼容桥引入新的展示判断。
 */
internal fun QuickCreationHistoryOutput.toGenerationHistoryOutput(): GenerationHistoryOutput =
    GenerationHistoryOutput(
        outputId = outputId,
        url = url,
        type = type,
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        outputName = outputName,
        expireTime = expireTime,
        expireDays = expireDays,
    )

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
        outputs = outputs,
        requestParameters = params,
    )

private fun GenerationTaskDetail.withCachedListContext(item: GenerationHistoryItem): GenerationTaskDetail =
    copy(
        title = title ?: item.taskType ?: item.modelId,
        sourceLabel = sourceLabel?.takeUnless { it.equals("API", ignoreCase = true) } ?: item.source.key,
        outputs = if (outputs.isEmpty()) item.outputs else outputs,
    )

private fun Double.toHistoryAmountText(): String {
    val raw = toString()
    return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
}

private const val WEBAPP_OUTPUT_ID_PREFIX = "webapp:"
private const val RH_COIN_CURRENCY = "RHB"
