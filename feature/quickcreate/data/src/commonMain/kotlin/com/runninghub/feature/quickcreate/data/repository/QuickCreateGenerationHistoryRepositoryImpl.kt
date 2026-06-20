package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.shared.domain.model.GenerationHistoryItem
import com.runninghub.shared.domain.model.GenerationHistoryOutput
import com.runninghub.shared.domain.model.GenerationHistoryPage
import com.runninghub.shared.domain.model.GenerationHistorySource
import com.runninghub.shared.domain.repository.GenerationHistoryRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository

/**
 * 将快捷创作任务历史适配为通用生成历史仓库。
 *
 * 该实现位于 QuickCreate data 模块，只依赖快捷创作历史窄接口并完成 Domain 模型映射；
 * 它不读取项目元数据，也不直接访问远程 API，避免 shared 通用历史边界反向依赖 QuickCreate。
 *
 * @param quickCreationTaskHistoryRepository 快捷创作任务历史仓库，提供最近历史、详情和取消任务能力。
 */
class QuickCreateGenerationHistoryRepositoryImpl(
    private val quickCreationTaskHistoryRepository: QuickCreationTaskHistoryRepository,
) : GenerationHistoryRepository {
    /**
     * 拉取通用历史列表。
     *
     * 请求仍委托给 QuickCreate 历史窄仓库，本实现只负责把来源标记和输出字段转换为
     * shared 的通用历史模型，供历史 Tab 复用。
     */
    override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> =
        quickCreationTaskHistoryRepository.listQuickCreationHistory(page, size).map { it.toGenerationHistoryPage() }

    /**
     * 拉取单条历史详情。
     *
     * @param outputId QuickCreate 输出记录 ID，由通用历史详情页传入；底层仓库会按 QuickCreate
     * 服务端协议解析对应任务详情。
     */
    override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> =
        quickCreationTaskHistoryRepository.getQuickCreationHistoryDetail(outputId).map { it.toGenerationHistoryItem() }

    /**
     * 取消一个仍在执行中的 QuickCreate 任务。
     *
     * 取消能力来自 QuickCreate 任务历史仓库；通用历史接口只透传任务 ID，不感知具体远端 endpoint。
     */
    override suspend fun cancelTask(taskId: String): Result<Unit> =
        quickCreationTaskHistoryRepository.cancelQuickCreationTask(taskId)
}

/**
 * 将 QuickCreate 分页历史转换为通用历史分页。
 *
 * 分页数字和总数完全保留服务端返回值，避免通用历史页改变 QuickCreate 原有分页语义。
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
 * source 固定写为 [GenerationHistorySource.QUICK_CREATION]，让通用历史 UI 可以区分后续接入的
 * 标准模型、音频或其他来源。
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
 * 保留服务端已经归一化后的输出类型。
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
