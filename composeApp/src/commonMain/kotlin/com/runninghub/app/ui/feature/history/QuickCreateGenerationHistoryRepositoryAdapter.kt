package com.runninghub.app.ui.feature.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryPage
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistorySource

/**
 * 将 QuickCreate 历史仓库适配为当前历史页仍在使用的通用历史仓库。
 *
 * 该类属于 composeApp 组合层的唯一迁移兼容桥：左侧连接 Task Domain 的统一历史契约，
 * 右侧只依赖 QuickCreate Domain 的窄历史仓库。它不得扩散到其他 composeApp 文件，也不得被
 * 搬进 Task Data 后通过跨 Feature 依赖伪装成正式实现；删除条件是 Task Data 能直接提供列表、
 * 详情、取消和参数快照完整能力的 [GenerationHistoryRepository]。
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
