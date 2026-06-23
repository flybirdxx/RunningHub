package com.runninghub.feature.model.domain

/**
 * 标准模型和 LLM 模型目录的领域仓库契约。
 *
 * 接口位于 Model Domain，隔离 Presentation 与 RunningHub 目录 API、DTO 和 endpoint 缓存；
 * 当前实现由 `feature:model:data` 提供，调用方只依赖本契约。
 */
interface ModelCatalogRepository {
    /**
     * 分页查询标准模型列表。
     *
     * @param search 搜索关键字，空字符串表示不按关键字过滤。
     * @param page 页码，从 1 开始；小于 1 的值应由调用方拦截或归一化。
     * @param size 每页数量，单位为条；调用方应限制过大的值，避免远端压力。
     * @return 成功时返回模型摘要列表；空列表表示没有匹配模型或远端返回空页。
     */
    suspend fun listStandardModels(
        search: String = "",
        page: Int = 1,
        size: Int = 30,
    ): Result<List<ApiModelSummary>>

    /**
     * 只读取本地缓存中的标准模型列表。
     *
     * 该方法不会访问网络，适合页面首次打开时快速显示上一次成功同步的模型目录。
     * 空集合表示本地还没有该查询条件缓存，调用方可以再触发 [refreshStandardModels]。
     *
     * @param search 搜索关键字，必须与刷新时的查询条件一致。
     * @param page 页码，从 1 开始。
     * @param size 每页数量，单位为条。
     * @return 本地已脱敏的模型摘要列表；空集合表示缓存不存在或无法解析。
     */
    suspend fun getCachedStandardModels(
        search: String = "",
        page: Int = 1,
        size: Int = 30,
    ): List<ApiModelSummary> = emptyList()

    /**
     * 通过接口刷新标准模型列表并更新本地缓存。
     *
     * 该方法始终尝试请求 `/api/sku/list`，适合页面已经展示缓存后进行后台同步。
     * 失败时通过 [Result.failure] 返回，调用方可继续保留旧缓存。
     *
     * @param search 搜索关键字，空字符串表示默认标准模型目录。
     * @param page 页码，从 1 开始。
     * @param size 每页数量，单位为条。
     * @return 成功时返回最新远端模型摘要，并已写入本地缓存。
     */
    suspend fun refreshStandardModels(
        search: String = "",
        page: Int = 1,
        size: Int = 30,
    ): Result<List<ApiModelSummary>> =
        listStandardModels(search = search, page = page, size = size)

    /**
     * 查询标准模型详情。
     *
     * @param modelId 标准模型 SKU ID，必须来自目录列表或历史记录。
     * @return 成功时返回模型详情和字段定义；失败时通过 [Result.failure] 暴露。
     */
    suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail>

    /**
     * 查询可用 LLM 模型列表。
     *
     * @return 成功时返回 LLM 模型摘要；空列表表示远端暂无可用模型或旧接口未返回。
     */
    suspend fun listLlmModels(): Result<List<LlmModelSummary>>
}

/**
 * 标准模型目录仓库的结构化失败异常。
 *
 * Data 层用该异常表达目录接口和 endpoint 回源的稳定失败语义；[message] 只保留诊断码，
 * Presentation 不得把它作为最终用户可见文案展示。
 *
 * @property issue 标准模型目录的稳定失败语义。
 * @property remoteCode 服务端业务 code；`null` 表示失败来自响应缺字段或本地结构校验。
 */
class ModelCatalogException(
    val issue: ModelCatalogIssue,
    val remoteCode: Int? = null,
) : IllegalStateException(issue.diagnosticMessage(remoteCode))

/**
 * 标准模型目录仓库使用的稳定错误语义。
 *
 * 这些枚举值只用于上层按类型判断错误和日志分类，不携带服务端 `msg`、底层异常 message
 * 或最终中文 UI 文案。
 *
 * @property code 稳定诊断码，可用于测试断言和日志分类；不得作为最终 UI 文案。
 */
enum class ModelCatalogIssue(val code: String) {
    /**
     * 标准模型列表接口返回非成功业务 code。
     */
    StandardListLoadFailed("MODEL_CATALOG_STANDARD_LIST_LOAD_FAILED"),

    /**
     * 标准模型详情接口返回非成功业务 code。
     */
    StandardDetailLoadFailed("MODEL_CATALOG_STANDARD_DETAIL_LOAD_FAILED"),

    /**
     * 标准模型详情接口成功但响应缺少 data。
     */
    StandardDetailMissing("MODEL_CATALOG_STANDARD_DETAIL_MISSING"),

    /**
     * 标准模型详情响应缺少可调用 endpoint。
     */
    StandardEndpointMissing("MODEL_CATALOG_STANDARD_ENDPOINT_MISSING"),

    /**
     * LLM 模型目录接口返回非成功业务 code。
     */
    LlmListLoadFailed("MODEL_CATALOG_LLM_LIST_LOAD_FAILED"),
}

private fun ModelCatalogIssue.diagnosticMessage(remoteCode: Int?): String =
    if (remoteCode == null) code else "$code:$remoteCode"
