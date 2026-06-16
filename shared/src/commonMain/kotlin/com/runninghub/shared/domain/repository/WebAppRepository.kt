package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.*

interface WebAppRepository {
    suspend fun getAppList(
        pageNum: Int,
        pageSize: Int,
        tags: List<String> = emptyList(),
        keyword: String? = null,
        sort: String? = null,
        days: Int? = null
    ): Result<PageData<WebApp>>

    suspend fun getCarefullyChosenList(): Result<List<WebApp>>

    suspend fun getCustomMadeWebappList(tags: List<String> = emptyList()): Result<List<WebApp>>

    suspend fun getUserAppList(
        userId: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>>

    suspend fun getTagTree(rang: String = "WEBAPP"): Result<List<Tag>>

    suspend fun getAppDetail(appId: String): Result<AppDetail>

    suspend fun getApiCallDemo(apiKey: String, webappId: String): Result<AppDetail>

    suspend fun searchApps(
        keyword: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>>

    suspend fun runTask(
        webappId: Long,
        apiKey: String,
        nodeInfoList: List<InputNode>,
        webhookUrl: String? = null,
        instanceType: String? = null
    ): Result<TaskResult>

    suspend fun getTaskOutputs(taskId: Long, apiKey: String): Result<List<TaskOutput>>

    suspend fun getTaskHistory(
        apiKey: String,
        pageNum: Int,
        pageSize: Int
    ): Result<List<TaskHistoryItem>>

    suspend fun uploadFile(
        apiKey: String,
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): Result<UploadResult>
}
