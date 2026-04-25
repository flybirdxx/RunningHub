package com.runninghub.shared.data.repository

import com.runninghub.shared.data.model.InputNodeDto
import com.runninghub.shared.data.model.TaskRunRequest
import com.runninghub.shared.data.model.TaskStatusRequest
import com.runninghub.shared.data.remote.WebAppApiService
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TaskRepositoryImpl(
    private val api: WebAppApiService
) : TaskRepository {

    override suspend fun runTask(
        webappId: Long,
        apiKey: String,
        inputNodes: List<InputNode>
    ): AppResult<TaskResult> = try {
        val request = TaskRunRequest(
            webappId = webappId,
            apiKey = apiKey,
            nodeInfoList = inputNodes.map { node ->
                InputNodeDto(
                    nodeId = node.nodeId,
                    nodeName = node.nodeName,
                    fieldName = node.fieldName,
                    fieldValue = node.fieldValue,
                    fieldType = node.fieldType,
                    description = node.description
                )
            }
        )
        val response = api.runTask(request)
        if (response.code == 0 && response.data != null) {
            AppResult.Success(
                TaskResult(
                    taskId = response.data.taskId,
                    status = parseTaskStatus(response.data.taskStatus)
                )
            )
        } else {
            AppResult.Error(response.msg)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error")
    }

    override fun pollTaskOutputs(taskId: Long, apiKey: String): Flow<AppResult<TaskResult>> = flow {
        emit(AppResult.Loading)
        var attempts = 0
        val maxAttempts = 120

        while (attempts < maxAttempts) {
            try {
                val response = api.getTaskOutputs(TaskStatusRequest(taskId, apiKey))
                if (response.code == 0 && response.data != null) {
                    val outputs = response.data.mapNotNull { dto ->
                        if (dto.fileUrl != null) {
                            TaskOutput(
                                fileUrl = dto.fileUrl,
                                fileName = dto.fileName,
                                fileType = dto.fileType
                            )
                        } else null
                    }
                    if (outputs.isNotEmpty()) {
                        emit(AppResult.Success(TaskResult(taskId, TaskStatus.SUCCESS, outputs)))
                        return@flow
                    }
                    val failedReason = response.data.firstOrNull()?.failedReason
                    if (failedReason != null) {
                        emit(AppResult.Success(
                            TaskResult(taskId, TaskStatus.FAILED, errorMessage = failedReason.exception_message)
                        ))
                        return@flow
                    }
                }
                delay(2000)
                attempts++
            } catch (e: Exception) {
                emit(AppResult.Error(e.message ?: "Polling error"))
                return@flow
            }
        }
        emit(AppResult.Error("Task timed out"))
    }

    private fun parseTaskStatus(status: String?): TaskStatus = when (status?.uppercase()) {
        "SUCCESS", "COMPLETED" -> TaskStatus.SUCCESS
        "FAILED", "ERROR" -> TaskStatus.FAILED
        "RUNNING", "PROCESSING" -> TaskStatus.RUNNING
        "PENDING", "QUEUED" -> TaskStatus.PENDING
        else -> TaskStatus.UNKNOWN
    }
}
