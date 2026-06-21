package com.runninghub.feature.auth.data.repository

import com.runninghub.core.storage.BalanceCache
import com.runninghub.feature.auth.domain.BalanceSnapshotRepository

/**
 * 用户余额快照仓库的数据层实现。
 *
 * 本类把领域层的“最近一次余额快照”读取委托给 [BalanceCache]，避免 composeApp
 * 的导航壳层直接依赖 storage 边界。快照只用于弱展示，实时账户状态仍应通过用户仓库查询。
 */
class BalanceSnapshotRepositoryImpl(
    private val balanceCache: BalanceCache,
) : BalanceSnapshotRepository {

    /**
     * 读取最近一次已缓存的金币余额。
     *
     * 返回值只用于导航壳层或弱展示场景，可能为空或过期；需要实时余额时应通过
     * [UserRepositoryImpl.getAccountStatus] 重新请求用户中心账户状态。
     */
    override suspend fun getLastKnownCoins(): String? =
        balanceCache.getLastKnownCoins()
}
