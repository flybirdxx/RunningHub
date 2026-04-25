package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun runTask(
        webappId: Long,
        apiKey: String,
        inputNodes: List<InputNode>
    ): AppResult<TaskResult>

    fun pollTaskOutputs(taskId: Long, apiKey: String): Flow<AppResult<TaskResult>>
}
