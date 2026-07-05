package com.runninghub.core.storage

/**
 * 非敏感 UI 状态快照的原始字符串存储端口。
 *
 * 调用方拥有具体字符串结构和兼容策略；本接口只提供按稳定 key 读写字符串的能力。
 * 认证凭据、Cookie、Token、API Key、验证码和未脱敏请求体不得通过本接口保存。
 */
interface UiStateSnapshotStore {
    /**
     * 读取指定 UI 状态快照。
     *
     * @param key 调用方定义的稳定快照 key。
     * @return 已保存的原始字符串；不存在时返回 null。
     */
    suspend fun getSnapshot(key: String): String?

    /**
     * 保存指定 UI 状态快照。
     *
     * @param key 调用方定义的稳定快照 key。
     * @param snapshot 调用方已经序列化好的非敏感字符串。
     */
    suspend fun saveSnapshot(key: String, snapshot: String)

    /**
     * 清理指定 UI 状态快照。
     *
     * @param key 调用方定义的稳定快照 key。
     */
    suspend fun clearSnapshot(key: String)
}
