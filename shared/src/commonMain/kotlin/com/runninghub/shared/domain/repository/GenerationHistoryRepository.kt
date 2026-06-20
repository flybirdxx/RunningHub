package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.GenerationHistoryItem
import com.runninghub.shared.domain.model.GenerationHistoryPage

interface GenerationHistoryRepository {
    suspend fun listHistory(page: Int = 1, size: Int = 20): Result<GenerationHistoryPage>

    suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem>

    suspend fun cancelTask(taskId: String): Result<Unit>
}
