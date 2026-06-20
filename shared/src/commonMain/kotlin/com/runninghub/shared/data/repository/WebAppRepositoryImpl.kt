package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.*
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.WebAppRepository

/**
 * WebAppRepository 的 Data 层实现。
 *
 * 本类负责调用 RunningHub WebApp 相关接口，并在需要认证凭据的接口中通过 [CredentialStore]
 * 读取 API Key。这样 Presentation 层不再持有或透传敏感凭据，后续替换为 Android Keystore
 * 或 iOS Keychain 时只需要调整凭据实现。
 *
 * @param api RunningHub WebApp 网络接口。
 * @param credentialStore API Key 和其他凭据的读取边界。
 */
class WebAppRepositoryImpl(
    private val api: RunningHubApi,
    private val credentialStore: CredentialStore,
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

    override suspend fun getApiCallDemo(webappId: String): Result<AppDetail> = runCatching {
        val apiKey = requireApiKey()
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
        nodeInfoList: List<InputNode>,
        webhookUrl: String?,
        instanceType: String?
    ): Result<TaskResult> = runCatching {
        val apiKey = requireApiKey()
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

    override suspend fun getTaskOutputs(taskId: Long): Result<List<TaskOutput>> = runCatching {
        val apiKey = requireApiKey()
        val response = api.getTaskOutputs(TaskStatusRequest(taskId, apiKey))
        check(response.code == 0) { response.msg }
        response.data?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun uploadFile(
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): Result<UploadResult> = runCatching {
        val apiKey = requireApiKey()
        val response = api.uploadFile(apiKey, fileType, fileBytes, fileName)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getTaskHistory(
        pageNum: Int,
        pageSize: Int,
    ): Result<List<TaskHistoryItem>> = runCatching {
        val apiKey = requireApiKey()
        val response = api.getTaskHistory(
            mapOf("apiKey" to apiKey, "pageNum" to pageNum, "pageSize" to pageSize)
        )
        check(response.code == 0) { response.msg.ifEmpty { "Failed to load history" } }
        response.data?.records?.map { it.toDomain() } ?: throw IllegalStateException("Empty response data")
    }

    private suspend fun requireApiKey(): String {
        // 缺少 API Key 是明确的业务前置条件失败，不能用空字符串继续请求远端接口。
        return credentialStore.getApiKey()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("请先在设置中绑定 API Key")
    }
}
