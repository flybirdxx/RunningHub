package com.runninghub.feature.model.data.repository

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.model.domain.ModelInvocationRepository
import com.runninghub.feature.model.domain.ModelInvocationIssueCode
import com.runninghub.feature.model.domain.ModelInvocationRequest
import com.runninghub.feature.model.domain.ModelInvocationTask
import com.runninghub.feature.model.data.remote.api.ModelCatalogApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
 *
 * @param client 已安装 RunningHub 默认配置和认证拦截器的 Ktor 客户端。
 * @param modelCatalogApi 标准模型目录 API，用于 endpoint 缓存缺失时按 modelId 回源查询详情。
 * @param credentialStore 凭据读取端口，上传接口需要从这里读取 API Key。
 * @param endpointRegistry 标准模型 endpoint 的 Data 层缓存，需与目录仓库共享。
 */
class ModelInvocationRepositoryImpl(
    private val client: HttpClient,
    private val modelCatalogApi: ModelCatalogApi,
    private val credentialStore: CredentialStore,
    private val endpointRegistry: ModelEndpointRegistry = ModelEndpointRegistry(),
) : ModelInvocationRepository {
    private val builder = ModelInvocationRequestBuilder()

    /**
     * 提交标准模型调用任务。
     *
     * endpoint 由 Data 层根据 modelId 解析，兼容旧模型数据中缺少 `/openapi/v2` 前缀的情况，
     * 避免调用方了解远端路径规则。
     */
    override suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask> =
        runCatching {
            val endpoint = resolveEndpoint(request.modelId).ensureOpenApiEndpoint()
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
     * 标准模型任务查询属于 OpenAPI v2 通用能力，不能再依赖 QuickCreate feature 的 Data API。
     * 这里仅解析标准模型页面需要展示的字段，其余响应细节保留在远端协议层。
     */
    override suspend fun queryTask(taskId: String): Result<ModelInvocationTask> =
        runCatching {
            val response = client.get(RunningHubApiEnvironment.openApiV2Url("query")) {
                parameter("taskId", taskId)
            }.body<JsonObject>()

            // OpenAPI 查询响应由服务端直接返回扁平 JSON；这里只取领域层需要的稳定字段，
            // 避免 shared 继续依赖 QuickCreate 专属 DTO。
            ModelInvocationTask(
                taskId = taskId,
                status = response.string("status"),
                errorCode = response.string("errorCode"),
                errorMessage = response.string("errorMessage"),
                resultUrls = response.resultUrls(),
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
                ?: error(ModelInvocationIssueCode.API_KEY_MISSING)
            val response = client.submitFormWithBinaryData(
                url = RunningHubApiEnvironment.openApiV2Url("media/upload/binary"),
                formData = formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, contentType)
                    })
                },
            ) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }.body<JsonObject>()

            // 服务端历史上可能返回 downloadUrl，也可能只返回 fileName；后者沿用旧客户端的 CDN
            // 拼接规则，确保标准模型上传在接口未完全统一时仍能得到可访问 URL。
            response.uploadUrl() ?: error(ModelInvocationIssueCode.MEDIA_UPLOAD_EMPTY_URL)
        }

    private suspend fun resolveEndpoint(modelId: String): String {
        endpointRegistry.get(modelId)?.let { return it }

        // 目录详情是 endpoint 的唯一远端来源；缓存未命中时在 Data 层回源，避免 Domain 请求携带路径。
        val response = modelCatalogApi.getStandardModelDetail(modelId)
        // endpoint 回源失败时只返回稳定错误和 code，不把服务端 msg 暴露到上层错误展示链路。
        check(response.code == 0) { "Model detail load failed: code=${response.code}" }
        val detail = checkNotNull(response.data) { "Model detail missing" }
        endpointRegistry.register(detail.id, detail.rhEndpoint)
        return detail.rhEndpoint.takeIf { it.isNotBlank() }
            ?: error("Model endpoint missing")
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

    private fun JsonObject.resultUrls(): List<String> =
        (this["results"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.string("url") }
            .orEmpty()

    private fun JsonObject.uploadUrl(): String? {
        val data = this["data"] as? JsonObject
        return data?.string("downloadUrl")
            ?: data?.string("fileName")?.let { "https://rh-images.xiaoyaoyou.com/$it" }
            ?: string("url")
    }

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
