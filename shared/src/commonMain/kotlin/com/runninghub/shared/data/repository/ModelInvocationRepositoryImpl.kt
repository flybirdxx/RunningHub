package com.runninghub.shared.data.repository

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.domain.model.ModelInvocationRequest
import com.runninghub.shared.domain.model.ModelInvocationTask
import com.runninghub.shared.domain.repository.ModelInvocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 标准模型调用仓库的数据层实现。
 *
 * 本类负责把领域请求转换为 RunningHub OpenAPI 调用，并在媒体上传时从 [CredentialStore]
 * 读取 API Key。这样可以避免 Presentation 层直接持有敏感凭据，同时复用 Data 层统一的
 * 网络错误和响应格式错误映射。
 */
class ModelInvocationRepositoryImpl(
    private val client: HttpClient,
    private val quickCreateApi: QuickCreateApi,
    private val credentialStore: CredentialStore,
) : ModelInvocationRepository {
    private val builder = ModelInvocationRequestBuilder()

    /**
     * 提交标准模型调用任务。
     *
     * endpoint 兼容旧模型数据中缺少 `/openapi/v2` 前缀的情况，避免调用方了解远端路径规则。
     */
    override suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask> =
        runCatching {
            val endpoint = request.endpoint.ensureOpenApiEndpoint()
            val response = client.post(RunningHubApiEnvironment.webUrl(endpoint)) {
                contentType(ContentType.Application.Json)
                setBody(builder.build(request.fields, request.values, request.webhookUrl).toJsonObject())
            }.body<JsonObject>()

            ModelInvocationTask(
                taskId = response.taskId(),
                status = response.string("status") ?: response.string("taskStatus"),
            )
        }

    /**
     * 查询模型调用任务状态。
     *
     * QuickCreateApi 统一承载 openapi 任务查询，当前实现只做 DTO 到领域任务状态的显式映射。
     */
    override suspend fun queryTask(taskId: String): Result<ModelInvocationTask> =
        runCatching {
            val response = quickCreateApi.queryTask(taskId)
            ModelInvocationTask(
                taskId = taskId,
                status = response.status,
                errorCode = response.errorCode,
                errorMessage = response.errorMessage,
                resultUrls = response.results?.map { it.url }.orEmpty(),
            )
        }

    /**
     * 上传模型调用媒体素材。
     *
     * 上传前先读取本地 API Key；缺失时直接返回失败，避免产生无效网络请求或在日志中暴露空凭据。
     */
    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Result<String> =
        runCatching {
            val apiKey = credentialStore.getApiKey()?.takeIf { it.isNotBlank() }
                ?: error("请先在设置中绑定 API Key")
            val response = quickCreateApi.uploadMedia(apiKey, fileBytes, fileName, contentType)
            response.url ?: error("Upload response missing URL")
        }

    private fun String.ensureOpenApiEndpoint(): String =
        if (startsWith("/openapi/v2/")) this else "/openapi/v2${if (startsWith("/")) this else "/$this"}"

    private fun JsonObject.taskId(): String =
        string("taskId")
            ?: string("id")
            ?: (this["data"] as? JsonPrimitive)?.contentOrNull
            ?: (this["data"] as? JsonObject)?.string("taskId")
            ?: ""

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun Map<String, Any>.toJsonObject(): JsonObject =
        JsonObject(mapValues { (_, value) -> value.toJsonElement() })

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is List<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }
}
