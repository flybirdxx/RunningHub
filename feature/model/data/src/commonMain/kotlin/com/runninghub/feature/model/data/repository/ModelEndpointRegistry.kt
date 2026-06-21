package com.runninghub.feature.model.data.repository

/**
 * 标准模型 endpoint 的 Data 层缓存。
 *
 * RunningHub SKU 目录会返回模型调用所需的 `rhEndpoint`，但该字段属于远端路由细节，
 * 不应继续暴露到 Domain 模型。目录仓库在读取列表或详情时把 endpoint 登记到这里，
 * 调用仓库提交任务时再通过 modelId 解析实际路径。
 *
 * 并发约束：
 * - 当前实现只保存少量字符串映射，重复登记同一 modelId 是幂等操作。
 * - 如果后续出现后台预取和多页面高并发写入，再把该缓存替换为带 Mutex 的实现。
 * - 缓存缺失时调用仓库会回源查询 SKU 详情，避免依赖调用方必须先加载目录。
 */
class ModelEndpointRegistry {
    private val endpointsByModelId = mutableMapOf<String, String>()

    /**
     * 登记模型调用 endpoint。
     *
     * @param modelId SKU 稳定标识，空字符串不会写入缓存。
     * @param endpoint 远端返回的模型调用路径，空字符串或 null 不会写入缓存。
     */
    fun register(modelId: String, endpoint: String?) {
        val normalizedEndpoint = endpoint?.takeIf { it.isNotBlank() } ?: return
        if (modelId.isBlank()) return
        endpointsByModelId[modelId] = normalizedEndpoint
    }

    /**
     * 按模型 ID 读取已登记 endpoint。
     *
     * @param modelId SKU 稳定标识。
     * @return 已登记的远端调用路径；没有缓存时返回 null，调用方应回源查询详情或返回业务错误。
     */
    fun get(modelId: String): String? =
        endpointsByModelId[modelId]
}
