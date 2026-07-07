package com.runninghub.feature.task.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.network.auth.markRunningHubAuthRetryAllowed
import com.runninghub.feature.task.data.remote.dto.BillingUsageWideDetailsDataDto
import com.runninghub.feature.task.data.remote.dto.BillingUsageWideDetailsRequestDto
import com.runninghub.feature.task.data.remote.dto.OpenApiCallLogDetailDataDto
import com.runninghub.feature.task.data.remote.dto.OpenApiCallLogDetailRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskBaseResponseDto
import com.runninghub.feature.task.data.remote.dto.TaskHistoryRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskHistoryItemDto
import com.runninghub.feature.task.data.remote.dto.TaskOutputDto
import com.runninghub.feature.task.data.remote.dto.TaskPageDataDto
import com.runninghub.feature.task.data.remote.dto.TaskRunRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskRunResponseDto
import com.runninghub.feature.task.data.remote.dto.TaskStatusRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskUploadResponseDto
import com.runninghub.feature.task.data.remote.dto.WebAppTaskDetailDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

/**
 * Task Data 模块的 WebApp 任务远端 API。
 *
 * 本类只包含需要 API Key 的运行链路：调用示例、提交任务、查询输出、上传文件和历史记录。
 * 公开目录、搜索和详情已由 Discovery Data 负责，避免 Task Data 反向承担目录职责。
 */
class WebAppTaskApi(private val client: HttpClient) {
    /**
     * 查询 WebApp 的 API 调用示例配置。
     *
     * API Key 只放入 `X-API-Key` 请求头，不写入日志或异常消息。
     */
    suspend fun getApiCallDemo(apiKey: String, webappId: String): TaskBaseResponseDto<WebAppTaskDetailDto> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/apiCallDemo")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiKey)
            setBody(mapOf("webappId" to webappId))
        }.body()

    /**
     * 通过任务 OpenAPI 提交 WebApp 任务。
     *
     * @param request 任务运行 DTO，包含应用标识、输入节点和 API Key。
     */
    suspend fun runTask(request: TaskRunRequestDto): TaskBaseResponseDto<TaskRunResponseDto> =
        client.post(RunningHubApiEnvironment.taskOpenApiUrl("ai-app/run")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询任务 OpenAPI 输出结果。
     *
     * @param request 任务状态请求，包含 taskId 和 API Key。
     */
    suspend fun getTaskOutputs(request: TaskStatusRequestDto): TaskBaseResponseDto<List<TaskOutputDto>> =
        client.post(RunningHubApiEnvironment.taskOpenApiUrl("outputs")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 上传 WebApp 任务所需的二进制文件。
     *
     * 文件内容通过 multipart form 发送，API Key 保持在请求体字段中以兼容现有服务端协议。
     * 本方法不记录文件内容或凭据，避免调试日志泄露敏感数据。
     */
    suspend fun uploadFile(
        apiKey: String,
        fileType: String,
        fileBytes: ByteArray,
        fileName: String,
    ): TaskBaseResponseDto<TaskUploadResponseDto> =
        client.submitFormWithBinaryData(
            url = RunningHubApiEnvironment.taskOpenApiUrl("upload"),
            formData = formData {
                append("apiKey", apiKey)
                append("fileType", fileType)
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, fileType)
                })
            },
        ).body()

    /**
     * 查询当前 API Key 的 WebApp 任务历史。
     *
     * @param request 历史查询参数，包含 API Key 和分页。
     */
    suspend fun getTaskHistory(
        request: TaskHistoryRequestDto,
    ): TaskBaseResponseDto<TaskPageDataDto<TaskHistoryItemDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("output/v2/history")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询控制台任务宽表。
     *
     * 该接口与 Web 控制台“任务与账单”页一致，返回所有来源任务的状态和费用记录，包括尚未产出
     * output 的运行中任务。请求依赖登录态认证拦截器，不在请求体中携带 API Key。
     */
    suspend fun getBillingUsageWideDetails(
        request: BillingUsageWideDetailsRequestDto,
    ): TaskBaseResponseDto<BillingUsageWideDetailsDataDto> =
        client.post(RunningHubApiEnvironment.apiUrl("billing/usage/wideDetails")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询控制台任务详情。
     *
     * 该接口与 Web 控制台详情抽屉一致，返回基础信息、计费信息、生成结果以及请求/响应详情。
     * 请求体只携带 taskId，认证由 HTTP 客户端统一注入，不允许把 API Key 混入请求体。
     */
    suspend fun getOpenApiCallLogDetail(
        request: OpenApiCallLogDetailRequestDto,
    ): TaskBaseResponseDto<OpenApiCallLogDetailDataDto> =
        client.post(RunningHubApiEnvironment.apiUrl("openapi/my/call/log/detail")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
