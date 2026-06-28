package com.runninghub.feature.task.data.remote.api

import com.runninghub.feature.task.data.remote.dto.BillingUsageWideDetailsRequestDto
import com.runninghub.feature.task.data.remote.dto.OpenApiCallLogDetailRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskHistoryRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskRunRequestDto
import com.runninghub.feature.task.data.remote.dto.TaskStatusRequestDto
import com.runninghub.feature.task.data.remote.dto.WebAppTaskInputNodeDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WebApp Task API 路径与认证头测试。
 *
 * 迁移到 `feature:task:data` 后仍需固定真实 endpoint，避免详情页运行链路误走旧 shared API。
 */
class WebAppTaskApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * WebApp Task API 应调用预期的服务端路径。
     */
    @Test
    fun `webapp task api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = WebAppTaskApi(clientWithCapture(captured))

        api.getApiCallDemo(apiKey = "api-key", webappId = "100")
        api.runTask(
            TaskRunRequestDto(
                webappId = 100L,
                apiKey = "api-key",
                nodeInfoList = listOf(WebAppTaskInputNodeDto(nodeId = "1")),
            )
        )
        api.getTaskOutputs(TaskStatusRequestDto(taskId = 200L, apiKey = "api-key"))
        api.uploadFile(
            apiKey = "api-key",
            fileType = "image",
            fileBytes = byteArrayOf(1, 2, 3),
            fileName = "input.png",
        )
        api.getTaskHistory(TaskHistoryRequestDto(apiKey = "api-key", pageNum = 1, pageSize = 10))
        api.getBillingUsageWideDetails(
            BillingUsageWideDetailsRequestDto(
                startDateTime = "2026-06-22 00:00:00",
                endDateTime = "2026-06-28 23:59:59",
                size = 20,
            )
        )
        api.getOpenApiCallLogDetail(OpenApiCallLogDetailRequestDto(taskId = "2071208241819508737"))

        assertEquals(
            listOf(
                HttpMethod.Post to "/api/webapp/apiCallDemo",
                HttpMethod.Post to "/task/openapi/ai-app/run",
                HttpMethod.Post to "/task/openapi/outputs",
                HttpMethod.Post to "/task/openapi/upload",
                HttpMethod.Post to "/api/output/v2/history",
                HttpMethod.Post to "/api/billing/usage/wideDetails",
                HttpMethod.Post to "/api/openapi/my/call/log/detail",
            ),
            captured.map { it.method to it.path },
        )
        assertEquals("api-key", captured.first().apiKey)
    }

    private fun clientWithCapture(
        captured: MutableList<CapturedRequest>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += CapturedRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    apiKey = request.headers["X-API-Key"],
                )
                respond(
                    content = responseFor(request.url.encodedPath),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    private fun responseFor(path: String): String =
        when (path) {
            "/api/webapp/apiCallDemo" -> """{"code":0,"msg":"success","data":{"id":"100","name":"Demo"}}"""
            "/task/openapi/ai-app/run" -> """{"code":0,"msg":"success","data":{"taskId":123,"taskStatus":"SUBMITTED"}}"""
            "/task/openapi/outputs" -> """{"code":0,"msg":"success","data":[]}"""
            "/task/openapi/upload" -> """{"code":0,"msg":"success","data":{"fileName":"input.png","fileType":"image"}}"""
            "/api/output/v2/history" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            "/api/billing/usage/wideDetails" -> """{"code":0,"msg":"success","data":{"records":[],"hasNext":false}}"""
            "/api/openapi/my/call/log/detail" -> """{"code":0,"msg":"success","data":{"basicInfo":{"taskId":"2071208241819508737"},"list":[]}}"""
            else -> error("Unexpected path: $path")
        }

    private data class CapturedRequest(
        val method: HttpMethod,
        val path: String,
        val apiKey: String?,
    )
}
