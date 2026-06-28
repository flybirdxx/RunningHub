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
import com.runninghub.feature.task.data.remote.dto.BillingUsageWideDetailsRequestDto
import com.runninghub.feature.task.data.remote.dto.OpenApiCallLogDetailRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskBaseResponseDto
import com.runninghub.feature.task.data.remote.dto.TaskHistoryRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskRunRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskStatusRequestDto
import com.runninghub.feature.task.data.remote.dto.toHistoryDomain
import com.runninghub.feature.task.data.remote.dto.toDomain
import com.runninghub.feature.task.data.remote.dto.toDto
import com.runninghub.feature.task.domain.WebAppTaskHistoryRepository
import com.runninghub.feature.task.domain.WebAppTaskException
import com.runninghub.feature.task.domain.WebAppTaskIssue
import com.runninghub.feature.task.domain.WebAppTaskRepository
import com.runninghub.feature.task.domain.GenerationTaskDetail
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

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
     * 分页读取当前登录用户的控制台任务历史。
     *
     * Web 控制台“任务与账单”页使用 `/api/billing/usage/wideDetails` 承载所有任务记录，
     * 包括 AI 应用、模型 API、工作流、快捷创作以及尚未产出 output 的运行中任务。
     * 旧 `/api/output/v2/history` 只返回输出历史，不能作为 History 页的任务列表主数据源。
     */
    override suspend fun getTaskHistory(
        pageNum: Int,
        pageSize: Int,
    ): Result<List<TaskHistoryItem>> = runCatching {
        val safePage = pageNum.coerceAtLeast(1)
        val safePageSize = pageSize.coerceAtLeast(1)
        val requestedSize = (safePage * safePageSize).coerceAtMost(BILLING_HISTORY_MAX_PAGE_SIZE)
        val response = api.getBillingUsageWideDetails(
            billingHistoryRequest(size = requestedSize)
        )
        val billingItems = response.requireTaskData(
            failedIssue = WebAppTaskIssue.TaskHistoryFailed,
            missingIssue = WebAppTaskIssue.TaskHistoryMissing,
        ).records
            .drop((safePage - 1) * safePageSize)
            .take(safePageSize)
            .map { it.toHistoryDomain() }
        val outputSupplementsByTaskId = loadOutputHistorySupplements(size = requestedSize)
            .associateBy { it.taskId }

        billingItems
            .map { item -> item.mergeOutputSupplement(outputSupplementsByTaskId[item.taskId]) }
            .withChildOutputsAttachedToParents()
    }

    /**
     * 读取控制台任务详情。
     *
     * 详情接口使用登录态认证，不需要 API Key。返回的请求信息可能含有服务端保存的原始 API Key，
     * 因此必须经过 mapper 脱敏后才能进入领域模型。
     */
    override suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail> = runCatching {
        val response = api.getOpenApiCallLogDetail(OpenApiCallLogDetailRequestDto(taskId = taskId))
        response.requireTaskData(
            failedIssue = WebAppTaskIssue.TaskDetailFailed,
            missingIssue = WebAppTaskIssue.TaskDetailMissing,
        ).toDomain(taskId)
    }

    private fun billingHistoryRequest(size: Int): BillingUsageWideDetailsRequestDto {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.of(BILLING_HISTORY_TIME_ZONE))
            .date
        val startDate = today.minus(DatePeriod(days = BILLING_HISTORY_LOOKBACK_DAYS - 1))
        return BillingUsageWideDetailsRequestDto(
            startDateTime = "$startDate 00:00:00",
            endDateTime = "$today 23:59:59",
            size = size,
            includeStats = true,
            includeChildTasks = true,
        )
    }

    private suspend fun loadOutputHistorySupplements(size: Int): List<TaskHistoryItem> {
        val apiKey = credentialStore.getApiKey()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            api.getTaskHistory(
                TaskHistoryRequestDto(
                    apiKey = apiKey,
                    pageNum = 1,
                    pageSize = size,
                )
            ).requireTaskData(
                failedIssue = WebAppTaskIssue.TaskHistoryFailed,
                missingIssue = WebAppTaskIssue.TaskHistoryMissing,
            ).records.map { it.toDomain() }
        }.getOrElse { emptyList() }
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

private fun TaskHistoryItem.mergeOutputSupplement(supplement: TaskHistoryItem?): TaskHistoryItem =
    if (outputs.isEmpty() && supplement?.outputs?.isNotEmpty() == true) {
        copy(outputs = supplement.outputs)
    } else {
        this
    }

private fun List<TaskHistoryItem>.withChildOutputsAttachedToParents(): List<TaskHistoryItem> {
    val childOutputsByParentTaskId = filter { item ->
        !item.parentTaskId.isNullOrBlank() && item.outputs.isNotEmpty()
    }.groupBy { item -> item.parentTaskId }
        .mapValues { (_, children) -> children.flatMap { it.outputs } }

    return map { item ->
        val childOutputs = childOutputsByParentTaskId[item.taskId].orEmpty()
        if (item.outputs.isEmpty() && childOutputs.isNotEmpty()) {
            item.copy(outputs = childOutputs)
        } else {
            item
        }
    }
}

private const val BILLING_HISTORY_LOOKBACK_DAYS = 365
private const val BILLING_HISTORY_MAX_PAGE_SIZE = 200
private const val BILLING_HISTORY_TIME_ZONE = "Asia/Shanghai"
