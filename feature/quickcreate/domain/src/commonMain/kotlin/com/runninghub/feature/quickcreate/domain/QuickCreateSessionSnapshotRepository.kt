package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作页面可恢复会话快照。
 *
 * 该快照只保存页面重进后需要继续展示的非敏感结果状态，例如已生成输出和终态会话。
 * 编辑中的 Prompt 仍由 [QuickCreateDraftRepository] 负责；上传素材、计费预览、请求体和远端错误原文不进入本模型。
 */
data class QuickCreateSessionSnapshot(
    val currentTab: String = "IMAGE",
    val submittedPrompt: String = "",
    val taskStatus: String = "IDLE",
    val taskId: String? = null,
    val results: List<QuickCreateSessionResultSnapshot> = emptyList(),
    val conversationItems: List<QuickCreateSessionConversationSnapshot> = emptyList(),
)

/**
 * 快捷创作会话中的单个输出结果快照。
 */
data class QuickCreateSessionResultSnapshot(
    val url: String,
    val type: String,
    val mediaType: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

/**
 * 快捷创作会话中的单次提交快照。
 */
data class QuickCreateSessionConversationSnapshot(
    val prompt: String,
    val taskStatus: String,
    val taskId: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val results: List<QuickCreateSessionResultSnapshot> = emptyList(),
)

/**
 * 快捷创作会话快照仓库。
 *
 * Data 实现负责 JSON 兼容和坏数据清理；Presentation 只读写领域快照，不直接接触底层存储格式。
 */
interface QuickCreateSessionSnapshotRepository {
    /**
     * 读取最近一次可恢复会话快照。
     */
    suspend fun getSnapshot(): QuickCreateSessionSnapshot?

    /**
     * 保存最近一次可恢复会话快照。
     */
    suspend fun saveSnapshot(snapshot: QuickCreateSessionSnapshot)

    /**
     * 清理已保存的会话快照。
     */
    suspend fun clearSnapshot()
}
