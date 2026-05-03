package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.*
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.WebAppRepository

class WebAppRepositoryImpl(
    private val api: RunningHubApi
) : WebAppRepository {

    override suspend fun getAppList(
        pageNum: Int,
        pageSize: Int,
        tags: List<String>,
        keyword: String?,
        sort: String?,
        days: Int?
    ): Result<PageData<WebApp>> = runCatching {
        val response = api.getWebAppList(
            WebAppListRequest(
                pageNum = pageNum,
                pageSize = pageSize,
                tags = tags,
                keyword = keyword,
                sort = sort,
                days = days
            )
        )
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getCarefullyChosenList(): Result<List<WebApp>> = runCatching {
        val response = api.getCarefullyChosenList()
        check(response.code == 0) { response.msg }
        response.data?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> = runCatching {
        val response = api.getCustomMadeWebappList(CustomMadeWebappRequest(tags))
        check(response.code == 0) { response.msg }
        response.data?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getUserAppList(
        userId: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>> = runCatching {
        val params = mapOf(
            "userId" to userId,
            "pageNum" to pageNum.toString(),
            "pageSize" to pageSize.toString()
        )
        val response = api.getWebAppUserList(params)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getTagTree(rang: String): Result<List<Tag>> = runCatching {
        val response = api.getTagTree(TagTreeRequest(rang))
        check(response.code == 0) { response.msg }
        response.data?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getAppDetail(appId: String): Result<AppDetail> = runCatching {
        val response = api.getWebAppDetail(mapOf("webappId" to appId))
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getApiCallDemo(apiKey: String, webappId: String): Result<AppDetail> = runCatching {
        val response = api.getApiCallDemo(apiKey, webappId)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun searchApps(
        keyword: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>> = runCatching {
        val response = api.getWebAppList(
            WebAppListRequest(
                pageNum = pageNum,
                pageSize = pageSize,
                keyword = keyword
            )
        )
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun runTask(
        webappId: Long,
        apiKey: String,
        nodeInfoList: List<InputNode>,
        webhookUrl: String?,
        instanceType: String?
    ): Result<TaskResult> = runCatching {
        val request = TaskRunRequest(
            webappId = webappId,
            apiKey = apiKey,
            nodeInfoList = nodeInfoList.map { it.toDto() },
            webhookUrl = webhookUrl,
            instanceType = instanceType
        )
        val response = api.runTask(request)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getTaskOutputs(taskId: Long, apiKey: String): Result<List<TaskOutput>> = runCatching {
        val response = api.getTaskOutputs(TaskStatusRequest(taskId, apiKey))
        check(response.code == 0) { response.msg }
        response.data?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun uploadFile(
        apiKey: String,
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): Result<UploadResult> = runCatching {
        val response = api.uploadFile(apiKey, fileType, fileBytes, fileName)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getTaskHistory(
        apiKey: String,
        pageNum: Int,
        pageSize: Int,
    ): Result<List<TaskHistoryItem>> = runCatching {
        val response = api.getTaskHistory(
            mapOf("apiKey" to apiKey, "pageNum" to pageNum, "pageSize" to pageSize)
        )
        check(response.code == 0) { response.msg.ifEmpty { "Failed to load history" } }
        response.data?.records?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }
}
