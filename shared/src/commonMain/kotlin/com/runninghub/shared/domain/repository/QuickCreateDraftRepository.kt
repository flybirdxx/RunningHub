package com.runninghub.shared.domain.repository

/**
 * 快捷创作可恢复草稿的领域快照。
 *
 * 该模型只表达业务层需要长期保存的编辑恢复信息，不暴露 DataStore、JSON 字段兼容或
 * Presentation 的页面状态对象。上传素材、计费预览和任务状态都不进入草稿，避免恢复时复用
 * 已经过期的远程资源或价格结果。
 *
 * @property currentTab 保存草稿时所在的创作 Tab，当前使用 `IMAGE` 或 `VIDEO` 这类稳定字符串。
 * 未知值由 Presentation 根据非空提示词降级恢复，不在 Domain 层依赖 UI 枚举。
 * @property imagePrompt 图片创作提示词；空字符串表示图片编辑区没有可恢复内容。
 * @property videoPrompt 视频创作提示词；空字符串表示视频编辑区没有可恢复内容。
 */
data class QuickCreateDraftSnapshot(
    val currentTab: String = "IMAGE",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
)

/**
 * 快捷创作草稿的领域仓库边界。
 *
 * Presentation 通过该接口读取、保存和清理可恢复草稿，不直接了解底层存储格式。
 * Data 实现负责 JSON 序列化、旧草稿兼容、坏数据清理和空草稿降级，这样页面状态管理只处理
 * 已经验证过的领域快照。
 */
interface QuickCreateDraftRepository {
    /**
     * 读取一个可恢复草稿。
     *
     * @return 至少包含一个非空提示词时返回草稿快照；没有草稿、草稿为空或存储内容损坏时返回 null。
     * Data 实现可以在发现不可恢复草稿时清理底层存储，避免下次启动重复提示。
     */
    suspend fun getRestorableDraft(): QuickCreateDraftSnapshot?

    /**
     * 保存最新编辑草稿。
     *
     * @param snapshot 当前编辑区的最小可恢复快照。实现应在图片和视频提示词都为空时清理草稿，
     * 防止持久化无意义的空草稿。
     */
    suspend fun saveDraft(snapshot: QuickCreateDraftSnapshot)

    /**
     * 清理已保存草稿。
     *
     * 通常在用户放弃草稿、成功恢复草稿或任务提交进入远程队列后调用。
     */
    suspend fun clearDraft()
}
