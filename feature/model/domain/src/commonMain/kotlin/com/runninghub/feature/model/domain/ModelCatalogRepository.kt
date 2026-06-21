package com.runninghub.feature.model.domain

/**
 * 标准模型和 LLM 模型目录的领域仓库契约。
 *
 * 接口位于 Model Domain，隔离 Presentation 与 RunningHub 目录 API、DTO 和 endpoint 缓存。
 * 当前实现仍暂存在 `shared` 兼容 Data 层，后续迁移到 `feature:model:data` 时调用方契约保持不变。
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
