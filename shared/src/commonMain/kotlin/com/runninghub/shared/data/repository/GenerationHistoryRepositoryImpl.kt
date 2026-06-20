package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.GenerationHistoryItem
import com.runninghub.shared.domain.model.GenerationHistoryOutput
import com.runninghub.shared.domain.model.GenerationHistoryPage
import com.runninghub.shared.domain.model.GenerationHistorySource
import com.runninghub.shared.domain.repository.GenerationHistoryRepository
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryOutput
import com.runninghub.shared.domain.repository.QuickCreationHistoryPage

class GenerationHistoryRepositoryImpl(
    private val quickCreateRepository: QuickCreateRepository,
) : GenerationHistoryRepository {
    override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> =
        quickCreateRepository.listQuickCreationHistory(page, size).map { it.toGenerationHistoryPage() }

    override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> =
        quickCreateRepository.getQuickCreationHistoryDetail(outputId).map { it.toGenerationHistoryItem() }

    override suspend fun cancelTask(taskId: String): Result<Unit> =
        quickCreateRepository.cancelQuickCreationTask(taskId)
}

fun QuickCreationHistoryPage.toGenerationHistoryPage(): GenerationHistoryPage =
    GenerationHistoryPage(
        page = page,
        size = size,
        total = total,
        items = items.map { it.toGenerationHistoryItem() },
    )

fun QuickCreationHistoryItem.toGenerationHistoryItem(): GenerationHistoryItem =
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

fun QuickCreationHistoryOutput.toGenerationHistoryOutput(): GenerationHistoryOutput =
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
