package com.runninghub.shared.data.repository

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

class ModelInvocationRepositoryImpl(
    private val client: HttpClient,
    private val quickCreateApi: QuickCreateApi,
) : ModelInvocationRepository {
    private val builder = ModelInvocationRequestBuilder()

    override suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask> =
        runCatching {
            val endpoint = request.endpoint.ensureOpenApiEndpoint()
            val response = client.post("${QuickCreateApi.BASE_URL}$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(builder.build(request.fields, request.values, request.webhookUrl).toJsonObject())
            }.body<JsonObject>()

            ModelInvocationTask(
                taskId = response.taskId(),
                status = response.string("status") ?: response.string("taskStatus"),
            )
        }

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

    override suspend fun uploadMedia(
        apiKey: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Result<String> =
        runCatching {
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
