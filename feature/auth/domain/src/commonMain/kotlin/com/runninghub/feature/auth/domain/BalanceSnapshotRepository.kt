package com.runninghub.feature.auth.domain

/**
 * 用户余额快照的领域读取边界。
 *
 * 该仓库属于 Auth Domain，只提供最近一次已知余额文本，用于应用壳层快速展示弱缓存。
 * 它不代表实时资产，也不能用于计费、余额校验或任务提交前的扣费判断。
 */
interface BalanceSnapshotRepository {
    /**
     * 读取最近一次成功加载并缓存的余额文本。
     *
     * @return 返回服务端账户状态曾经写入的余额展示值；`null` 表示当前没有可展示快照，
     * Presentation 层应使用自身占位符，而不是把 `null` 视为余额为 0。
     */
    suspend fun getLastKnownCoins(): String?
}
