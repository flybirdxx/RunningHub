package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.*

/**
 * WebApp 目录、详情和任务执行相关仓库。
 *
 * 公开接口不再要求 Presentation 层传入 API Key；需要凭据的任务执行、上传和输出查询由
 * Data 层实现通过凭据存储自行读取，避免 UI 层接触和转发敏感凭据。
 */
interface WebAppRepository {
    /** 分页读取 WebApp 列表，可按标签、关键词、排序和时间窗口筛选。 */
    suspend fun getAppList(
        pageNum: Int,
        pageSize: Int,
        tags: List<String> = emptyList(),
        keyword: String? = null,
        sort: String? = null,
        days: Int? = null
    ): Result<PageData<WebApp>>

    /** 读取运营精选 WebApp 列表。 */
    suspend fun getCarefullyChosenList(): Result<List<WebApp>>

    /** 读取定制专区 WebApp 列表。 */
    suspend fun getCustomMadeWebappList(tags: List<String> = emptyList()): Result<List<WebApp>>

    /** 分页读取指定用户发布的 WebApp。 */
    suspend fun getUserAppList(
        userId: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>>

    /** 读取 WebApp 标签树。 */
    suspend fun getTagTree(rang: String = "WEBAPP"): Result<List<Tag>>

    /** 读取公开 WebApp 详情；不需要 API Key。 */
    suspend fun getAppDetail(appId: String): Result<AppDetail>

    /**
     * 读取需要 API Key 的调用示例详情。
     *
     * 实现层负责获取 API Key；未绑定时返回失败结果，调用方据此展示绑定提示或降级页面。
     */
    suspend fun getApiCallDemo(webappId: String): Result<AppDetail>

    /** 按关键词分页搜索 WebApp。 */
    suspend fun searchApps(
        keyword: String,
        pageNum: Int,
        pageSize: Int
    ): Result<PageData<WebApp>>

    /**
     * 提交 WebApp 工作流任务。
     *
     * 实现层负责读取 API Key 并写入远程请求；调用方只提供页面输入节点。
     */
    suspend fun runTask(
        webappId: Long,
        nodeInfoList: List<InputNode>,
        webhookUrl: String? = null,
        instanceType: String? = null
    ): Result<TaskResult>

    /** 查询任务输出；实现层复用本地 API Key，避免调用方在轮询链路中保存凭据。 */
    suspend fun getTaskOutputs(taskId: Long): Result<List<TaskOutput>>

    /**
     * 分页读取当前 API Key 对应的任务历史。
     *
     * 该接口当前没有生产调用点，保留是为了兼容后续任务历史拆分。
     */
    suspend fun getTaskHistory(pageNum: Int, pageSize: Int): Result<List<TaskHistoryItem>>

    /**
     * 上传任务输入文件。
     *
     * 实现层负责读取 API Key；调用方只传文件类型、文件内容和展示文件名。
     */
    suspend fun uploadFile(
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): Result<UploadResult>
}
