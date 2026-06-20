package com.runninghub.core.storage

/**
 * 用户余额快照缓存边界。
 *
 * 该接口只保存 UI 可快速展示的最近一次余额文本，不代表实时账户资产。
 * 把余额缓存从 SettingsRepository 拆出后，首页、认证注销和后续 Profile 流程可以依赖
 * 明确的缓存职责，而不再为了一个余额字段持有完整凭据与草稿存储接口。
 */
interface BalanceCache {
    /** 读取最近一次成功加载的余额文本；从未缓存或已清理时返回 null。 */
    suspend fun getLastKnownCoins(): String?

    /**
     * 保存最近一次成功加载的余额文本。
     *
     * @param coins 服务端返回并经过上层格式化的余额展示值；该值仅用于弱缓存，不参与计费判断。
     */
    suspend fun setLastKnownCoins(coins: String)

    /** 清理余额快照，通常在用户注销或会话失效后调用。 */
    suspend fun clearLastKnownCoins()
}
