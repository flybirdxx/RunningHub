package com.runninghub.core.storage

/**
 * 快捷创作草稿存储边界。
 *
 * 草稿属于非敏感的编辑恢复数据，和认证凭据、余额缓存使用不同职责接口。
 * 该接口只保存序列化后的草稿字符串，草稿结构解析、兼容策略和坏数据清理由 quickcreate Data
 * 层的草稿 Repository 负责，避免存储层和 Presentation 层了解彼此的模型细节。
 */
interface QuickCreateDraftStore {
    /** 读取已保存的快捷创作草稿 JSON；没有草稿时返回 null。 */
    suspend fun getQuickCreateDraft(): String?

    /**
     * 保存快捷创作草稿 JSON。
     *
     * @param json 由快捷创作草稿 Data Repository 生成的稳定 JSON 字符串。
     */
    suspend fun saveQuickCreateDraft(json: String)

    /** 清理快捷创作草稿，通常在恢复、放弃或提交任务后调用。 */
    suspend fun clearQuickCreateDraft()
}
