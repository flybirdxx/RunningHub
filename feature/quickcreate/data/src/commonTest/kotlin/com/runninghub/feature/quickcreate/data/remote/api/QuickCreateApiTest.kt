package com.runninghub.feature.quickcreate.data.remote.api

import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationCommitRequestDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationCreateRequestDto
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

class QuickCreateApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `quick create catalog api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )

        api.getQuickCreationCategories()
        api.getQuickCreationModels(listOf("image"))
        api.getQuickCreationModes("image")

        assertEquals(
            listOf(
                HttpMethod.Post to QuickCreateApi.QC_CATEGORIES,
                HttpMethod.Post to QuickCreateApi.QC_MODELS,
                HttpMethod.Post to QuickCreateApi.QC_CREATION_MODES,
            ),
            captured.map { it.method to it.path },
        )
    }

    @Test
    fun `quick create openapi query and upload use expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )

        api.queryTask("task-1")
        api.uploadMedia(
            apiKey = "api-key",
            fileBytes = byteArrayOf(1, 2, 3),
            fileName = "input.png",
            contentType = "image/png",
        )

        assertEquals(
            listOf(
                HttpMethod.Get to QuickCreateApi.TASK_QUERY,
                HttpMethod.Post to QuickCreateApi.MEDIA_UPLOAD,
            ),
            captured.map { it.method to it.path },
        )
        assertEquals("task-1", captured[0].taskId)
        assertEquals("Bearer api-key", captured[1].authorization)
    }

    @Test
    fun `quick create generation flow api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )
        val createRequest = QuickCreationCreateRequestDto(
            bindingId = "binding-1",
            categoryId = "image",
            skuId = "sku-1",
            params = emptyMap(),
        )

        api.previewQuickCreationFee(createRequest)
        api.prepareQuickCreation(createRequest)
        api.commitQuickCreation(
            QuickCreationCommitRequestDto(
                prepareToken = "prepare-token",
                createRequest = createRequest,
            ),
        )
        api.listQuickCreationTasks(page = 1, size = 10)

        assertEquals(
            listOf(
                HttpMethod.Post to QuickCreateApi.QC_FEE_PREVIEW,
                HttpMethod.Post to QuickCreateApi.QC_PREPARE,
                HttpMethod.Post to QuickCreateApi.QC_COMMIT,
                HttpMethod.Post to QuickCreateApi.QC_TASK_LIST,
            ),
            captured.map { it.method to it.path },
        )
    }

    @Test
    fun `quick create history api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )

        api.getQuickCreationTaskDetail(outputId = "output-1")
        api.cancelQuickCreationTask(taskId = "task id/1")

        assertEquals(
            listOf(
                HttpMethod.Post to QuickCreateApi.QC_TASK_DETAIL,
                HttpMethod.Post to QuickCreateApi.QC_TASK_CANCEL,
            ),
            captured.map { it.method to it.path },
        )
    }

    @Test
    fun `quick create project api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )

        api.listQuickCreationProjects(page = 1, size = 20)
        api.listQuickCreationProjectTasks(projectId = "project-1", page = 1, size = 10)
        api.createQuickCreationProject(name = "Project")
        api.renameQuickCreationProject(projectId = "project-1", name = "Renamed")
        api.deleteQuickCreationProject(projectId = "project-1")
        api.pinQuickCreationProject(projectId = "project-1", pinned = true)
        api.getQuickCreationProjectDetail(projectId = "project-1")

        assertEquals(
            listOf(
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_LIST,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_TASKS,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_CREATE,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_RENAME,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_DELETE,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_PIN,
                HttpMethod.Post to QuickCreateApi.QC_PROJECT_DETAIL,
            ),
            captured.map { it.method to it.path },
        )
    }

    @Test
    fun `quick create inspiration api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedQuickCreateRequest>()
        val api = QuickCreateApi(
            client = clientWithPathCapture(captured),
            json = json,
        )

        api.getQuickCreationInspirationTags()
        api.getQuickCreationInspirationTemplates(page = 1, size = 20, tagId = "tag-1")
        api.getQuickCreationInspirationTemplateDetail(templateId = "template-1")

        assertEquals(
            listOf(
                HttpMethod.Post to QuickCreateApi.QC_INSPIRATION_TAGS,
                HttpMethod.Post to QuickCreateApi.QC_INSPIRATION_TEMPLATES,
                HttpMethod.Post to QuickCreateApi.QC_INSPIRATION_TEMPLATE_DETAIL,
            ),
            captured.map { it.method to it.path },
        )
    }

    private fun clientWithPathCapture(
        captured: MutableList<CapturedQuickCreateRequest>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += CapturedQuickCreateRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    taskId = request.url.parameters["taskId"],
                    authorization = request.headers[HttpHeaders.Authorization],
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
            QuickCreateApi.QC_CATEGORIES -> """{"code":0,"msg":"success","data":[]}"""
            QuickCreateApi.QC_MODELS -> """{"code":0,"msg":"success","data":{"categoryMeta":[],"categories":{}}}"""
            QuickCreateApi.QC_CREATION_MODES -> """{"code":0,"msg":"success","data":[]}"""
            QuickCreateApi.TASK_QUERY -> """{"taskId":"task-1","status":"SUBMITTED"}"""
            QuickCreateApi.MEDIA_UPLOAD -> """{"fileName":"input.png","fileType":"image/png"}"""
            QuickCreateApi.QC_FEE_PREVIEW -> """{"code":0,"msg":"success","data":{"passed":true}}"""
            QuickCreateApi.QC_PREPARE -> """{"code":0,"msg":"success","data":{"prepareToken":"prepare-token","ttlSeconds":120}}"""
            QuickCreateApi.QC_COMMIT -> """{"code":0,"msg":"success","data":{"taskId":"task-1","taskStatus":"QUEUED"}}"""
            QuickCreateApi.QC_TASK_LIST -> """{"code":0,"msg":"success","data":{"page":1,"size":10,"total":0,"list":[]}}"""
            QuickCreateApi.QC_TASK_DETAIL -> """{"code":0,"msg":"success","data":{"taskId":"task-1","taskStatus":"SUCCESS"}}"""
            QuickCreateApi.QC_TASK_CANCEL -> """{"code":0,"msg":"success","data":true}"""
            QuickCreateApi.QC_PROJECT_LIST -> """{"code":0,"msg":"success","data":{"current":1,"size":20,"total":0,"records":[]}}"""
            QuickCreateApi.QC_PROJECT_TASKS -> """{"code":0,"msg":"success","data":{"page":1,"size":10,"total":0,"list":[]}}"""
            QuickCreateApi.QC_PROJECT_CREATE -> """{"code":0,"msg":"success","data":{"projectId":"project-1","name":"Project"}}"""
            QuickCreateApi.QC_PROJECT_RENAME -> """{"code":0,"msg":"success","data":true}"""
            QuickCreateApi.QC_PROJECT_DELETE -> """{"code":0,"msg":"success","data":true}"""
            QuickCreateApi.QC_PROJECT_PIN -> """{"code":0,"msg":"success","data":true}"""
            QuickCreateApi.QC_PROJECT_DETAIL -> """{"code":0,"msg":"success","data":{"projectId":"project-1","name":"Project"}}"""
            QuickCreateApi.QC_INSPIRATION_TAGS -> """{"code":0,"msg":"success","data":[]}"""
            QuickCreateApi.QC_INSPIRATION_TEMPLATES -> """{"code":0,"msg":"success","data":{"page":1,"size":20,"total":0,"list":[]}}"""
            QuickCreateApi.QC_INSPIRATION_TEMPLATE_DETAIL -> """{"code":0,"msg":"success","data":{"templateId":"template-1"}}"""
            else -> error("Unexpected path: $path")
        }

    private data class CapturedQuickCreateRequest(
        val method: HttpMethod,
        val path: String,
        val taskId: String?,
        val authorization: String?,
    )
}
