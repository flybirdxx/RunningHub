package com.runninghub.feature.task.data.repository

import com.runninghub.core.common.MissingCredential
import com.runninghub.core.common.MissingCredentialException
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.TaskHistoryItem
import com.runninghub.core.model.TaskOutput
import com.runninghub.core.model.TaskResult
import com.runninghub.core.model.UploadResult
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.task.data.remote.api.WebAppTaskApi
import com.runninghub.feature.task.data.remote.dto.TaskBaseResponseDto
import com.runninghub.feature.task.data.remote.dto.TaskHistoryRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskRunRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskStatusRequestDto
import com.runninghub.feature.task.data.remote.dto.toDomain
import com.runninghub.feature.task.data.remote.dto.toDto
import com.runninghub.feature.task.domain.WebAppTaskHistoryRepository
import com.runninghub.feature.task.domain.WebAppTaskException
import com.runninghub.feature.task.domain.WebAppTaskIssue
import com.runninghub.feature.task.domain.WebAppTaskRepository

/**
 * WebApp 任务和上传仓库的 Task Data 实现。
 *
 * 本实现封装需要 API Key 的运行链路，并通过 [CredentialStore] 读取本地凭据。
 * Presentation 层只依赖 [WebAppTaskRepository] 或 [WebAppTaskHistoryRepository]，
 * 不持有 API Key，也不直接触达 Ktor API 或 DTO。
 *
 * @param api WebApp 任务远端 API。
 * @param credentialStore API Key 和其他凭据的读取边界。
 */
class WebAppTaskRepositoryImpl(
    private val api: WebAppTaskApi,
    private val credentialStore: CredentialStore,
) : WebAppTaskRepository,
    WebAppTaskHistoryRepository {
    /**
     * 读取需要 API Key 的调用示例详情。
     *
     * 缺少 API Key 时会在发起网络请求前失败，避免向远端提交空凭据。
     */
    override suspend fun getApiCallDemo(webappId: String): Result<AppDetail> = runCatching {
        val apiKey = requireApiKey()
        val response = api.getApiCallDemo(apiKey, webappId)
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.ApiCallDemoFailed,
            missingIssue = WebAppTaskIssue.ApiCallDemoMissing,
        ).toDomain()
    }

    /**
     * 提交 WebApp 任务。
     *
     * API Key 在 Data 层注入请求体，调用方只提供业务输入节点，避免凭据穿透到 UI 状态。
     */
    override suspend fun runTask(
        webappId: Long,
        nodeInfoList: List<InputNode>,
        webhookUrl: String?,
        instanceType: String?,
    ): Result<TaskResult> = runCatching {
        val apiKey = requireApiKey()
        val request = TaskRunRequestDto(
            webappId = webappId,
            apiKey = apiKey,
            nodeInfoList = nodeInfoList.map { it.toDto() },
            webhookUrl = webhookUrl,
            instanceType = instanceType,
        )
        val response = api.runTask(request)
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.RunTaskFailed,
            missingIssue = WebAppTaskIssue.RunTaskMissing,
        ).toDomain()
    }

    /**
     * 查询任务输出。
     *
     * 空输出数组是有效业务结果；成功响应缺少 data 才视为远端响应异常。
     */
    override suspend fun getTaskOutputs(taskId: Long): Result<List<TaskOutput>> = runCatching {
        val apiKey = requireApiKey()
        val response = api.getTaskOutputs(TaskStatusRequestDto(taskId, apiKey))
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.TaskOutputsFailed,
            missingIssue = WebAppTaskIssue.TaskOutputsMissing,
        ).map { it.toDomain() }
    }

    /**
     * 上传任务输入文件。
     *
     * 文件内容和 API Key 只在 Data 层发送到远端，不写入日志或异常消息。
     */
    override suspend fun uploadFile(
        fileType: String,
        fileBytes: ByteArray,
        fileName: String,
    ): Result<UploadResult> = runCatching {
        val apiKey = requireApiKey()
        val response = api.uploadFile(apiKey, fileType, fileBytes, fileName)
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.UploadFileFailed,
            missingIssue = WebAppTaskIssue.UploadFileMissing,
        ).toDomain()
    }

    /**
     * 分页读取当前 API Key 的 WebApp 任务历史。
     *
     * 历史接口仍是旧 `/api/output/v2/history`，Data 层负责把分页 records 映射为领域列表。
     */
    override suspend fun getTaskHistory(
        pageNum: Int,
        pageSize: Int,
    ): Result<List<TaskHistoryItem>> = runCatching {
        val apiKey = requireApiKey()
        val response = api.getTaskHistory(TaskHistoryRequestDto(apiKey = apiKey, pageNum = pageNum, pageSize = pageSize))
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.TaskHistoryFailed,
            missingIssue = WebAppTaskIssue.TaskHistoryMissing,
        ).records.map { it.toDomain() }
    }

    private suspend fun requireApiKey(): String {
        // 缺少 API Key 是明确的业务前置条件失败，不能用空字符串继续请求远端接口。
        return credentialStore.getApiKey()?.takeIf { it.isNotBlank() }
            ?: throw MissingCredentialException(MissingCredential.ApiKey)
    }

    private fun <T> TaskBaseResponseDto<T>.requireTaskData(
        failedIssue: WebAppTaskIssue,
        missingIssue: WebAppTaskIssue,
    ): T {
        // Task 接口统一在 Data 层校验服务端业务码和空响应，
        // 调用方只处理 Kotlin Result，不直接解析 DTO，也不能把远端 msg 当成最终展示文案。
        if (code != 0) {
            throw WebAppTaskException(failedIssue, code)
        }
        return data ?: throw WebAppTaskException(missingIssue)
    }
}
