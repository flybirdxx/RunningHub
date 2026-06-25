package com.runninghub.core.storage

/**
 * 快捷创作最近模型选择的非敏感存储边界。
 *
 * 这里只保存模型目录 UI 的稳定身份键，不保存完整模型字段、价格、接口参数或用户提示词。
 * 目录刷新后调用方必须用该身份键重新匹配当前接口返回的模型对象，避免沿用过期模型定义。
 */
interface QuickCreateModelSelectionStore {
    /** 读取最近一次用户主动选择的图片模型身份键；`null` 表示用户从未选择过图片模型。 */
    suspend fun getLastImageServiceModelIdentityKey(): String?

    /**
     * 保存最近一次用户主动选择的图片模型身份键。
     *
     * @param identityKey 当前模型目录中的 `bindingId|skuId` 身份键；空字符串不是有效值。
     */
    suspend fun saveLastImageServiceModelIdentityKey(identityKey: String)

    /** 读取最近一次用户主动选择的视频模型身份键；`null` 表示用户从未选择过视频模型。 */
    suspend fun getLastVideoServiceModelIdentityKey(): String?

    /**
     * 保存最近一次用户主动选择的视频模型身份键。
     *
     * @param identityKey 当前模型目录中的 `bindingId|skuId` 身份键；空字符串不是有效值。
     */
    suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String)
}
