package com.runninghub.core.storage

/**
 * 标准模型目录的本地缓存端口。
 *
 * 本接口只保存 Data 层已经脱敏后的 JSON 快照，不接触 Cookie、Token、API Key、请求头或原始响应。
 * 当前平台实现复用应用级本地持久化存储；后续迁移到 SQLDelight 或其他数据库时，Data 层可继续
 * 通过本端口读写模型目录，避免 Presentation 依赖具体存储技术。
 */
interface ModelCatalogCacheStore {
    /**
     * 读取标准模型列表缓存。
     *
     * @param cacheKey 由调用方按搜索词、页码和页大小生成的稳定缓存键；空字符串不是有效键。
     * @return 已脱敏的列表 JSON；`null` 表示本地没有该查询条件的缓存。
     */
    suspend fun getStandardModelList(cacheKey: String): String?

    /**
     * 保存标准模型列表缓存。
     *
     * @param cacheKey 与读取时一致的稳定缓存键。
     * @param json 已脱敏的列表 JSON，不包含认证信息或远端错误消息。
     */
    suspend fun saveStandardModelList(cacheKey: String, json: String)

    /**
     * 读取标准模型详情缓存。
     *
     * @param modelId 标准模型 SKU ID；空字符串不是有效键。
     * @return 已脱敏的详情 JSON；`null` 表示本地没有该模型详情缓存。
     */
    suspend fun getStandardModelDetail(modelId: String): String?

    /**
     * 保存标准模型详情缓存。
     *
     * @param modelId 标准模型 SKU ID。
     * @param json 已脱敏的详情 JSON，不包含认证信息或远端错误消息。
     */
    suspend fun saveStandardModelDetail(modelId: String, json: String)
}
