package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作最近模型选择的领域仓库。
 *
 * 该仓库只保存用户主动选择过的模型身份键，用于下次进入快捷创作时恢复选择。
 * 模型名称、价格、字段定义仍以当前模型目录接口返回为准，避免持久化快照污染最新目录。
 */
interface QuickCreateModelSelectionRepository {
    /**
     * 读取最近一次图片模型选择。
     *
     * @return 当前用户上次主动选择的图片模型身份键；`null` 表示没有历史选择。
     */
    suspend fun getLastImageServiceModelIdentityKey(): String?

    /**
     * 保存最近一次图片模型选择。
     *
     * @param identityKey 模型目录 UI 身份键，通常为 `bindingId|skuId`。
     */
    suspend fun saveLastImageServiceModelIdentityKey(identityKey: String)

    /**
     * 读取最近一次视频模型选择。
     *
     * @return 当前用户上次主动选择的视频模型身份键；`null` 表示没有历史选择。
     */
    suspend fun getLastVideoServiceModelIdentityKey(): String?

    /**
     * 保存最近一次视频模型选择。
     *
     * @param identityKey 模型目录 UI 身份键，通常为 `bindingId|skuId`。
     */
    suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String)
}
