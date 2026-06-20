package com.runninghub.shared.domain.usecase

import com.runninghub.shared.domain.repository.BalanceSnapshotRepository

/**
 * 读取应用壳层可展示的最近一次余额快照。
 *
 * 该 UseCase 位于 Domain 层，用于隔离 Presentation 与底层缓存实现。
 * 返回值只适合展示角标，不参与计费、余额不足判断或任务提交前校验。
 *
 * @param repository 余额快照领域仓库。
 */
class GetLastKnownBalanceUseCase(
    private val repository: BalanceSnapshotRepository,
) {

    /**
     * 获取最近一次已知余额文本。
     *
     * @return 有缓存时返回缓存文本；没有缓存或已清理时返回 `null`，由调用方决定展示占位符。
     */
    suspend operator fun invoke(): String? =
        repository.getLastKnownCoins()
}
