package com.runninghub.shared.data.repository

import com.runninghub.core.storage.BalanceCache
import com.runninghub.shared.domain.repository.BalanceSnapshotRepository

/**
 * 用户余额快照仓库的数据层实现。
 *
 * 本类把领域层的“最近一次余额快照”读取委托给 [BalanceCache]，避免 composeApp
 * 的导航壳层直接依赖 storage 边界。快照只用于弱展示，实时账户状态仍应通过用户仓库查询。
 *
 * @param balanceCache 余额文本的本地弱缓存。
 */
class BalanceSnapshotRepositoryImpl(
    private val balanceCache: BalanceCache,
) : BalanceSnapshotRepository {

    /**
     * 读取最近一次缓存的余额文本。
     *
     * 返回值不做格式化或兜底，避免 Data 层产生 UI 占位符；调用方应自行决定无缓存时展示什么。
     */
    override suspend fun getLastKnownCoins(): String? =
        balanceCache.getLastKnownCoins()
}
