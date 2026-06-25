package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.QuickCreateModelSelectionStore
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository

/**
 * 快捷创作最近模型选择仓库的数据层实现。
 *
 * 实现只转发非敏感身份键读写，持久化介质由 core/storage 的 [QuickCreateModelSelectionStore] 隔离。
 * Presentation 恢复选择时仍必须重新匹配当前模型目录，不能把该键当作完整模型快照使用。
 */
class QuickCreateModelSelectionRepositoryImpl(
    private val selectionStore: QuickCreateModelSelectionStore,
) : QuickCreateModelSelectionRepository {
    override suspend fun getLastImageServiceModelIdentityKey(): String? =
        selectionStore.getLastImageServiceModelIdentityKey()?.takeIf { it.isNotBlank() }

    override suspend fun saveLastImageServiceModelIdentityKey(identityKey: String) {
        if (identityKey.isBlank()) return
        selectionStore.saveLastImageServiceModelIdentityKey(identityKey)
    }

    override suspend fun getLastVideoServiceModelIdentityKey(): String? =
        selectionStore.getLastVideoServiceModelIdentityKey()?.takeIf { it.isNotBlank() }

    override suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String) {
        if (identityKey.isBlank()) return
        selectionStore.saveLastVideoServiceModelIdentityKey(identityKey)
    }
}
