package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 快捷创作草稿仓库的数据层实现。
 *
 * 该实现复用 core/storage 的原始字符串存储能力，并把 JSON 结构解析、旧数据兼容和坏草稿清理
 * 封装在 Data 层。Presentation 只接收 [QuickCreateDraftSnapshot]，不会再直接处理持久化格式。
 *
 * @param draftStore 非敏感草稿的底层字符串存储端口，当前由 SettingsRepositoryImpl 提供。
 * @param json 跨 shared 模块复用的 JSON 配置，用于解析旧草稿时兼容未知字段。
 */
class QuickCreateDraftRepositoryImpl(
    private val draftStore: QuickCreateDraftStore,
    private val json: Json,
) : QuickCreateDraftRepository {
    /**
     * 读取并校验一个可恢复草稿。
     *
     * 存储中可能残留旧版本 JSON、空草稿或用户升级前写入的损坏内容；这些数据如果直接暴露给
     * Presentation 会导致页面反复展示不可恢复入口，因此在这里统一清理。
     */
    override suspend fun getRestorableDraft(): QuickCreateDraftSnapshot? {
        val raw = draftStore.getQuickCreateDraft()
        if (raw.isNullOrEmpty()) return null

        val snapshot = runCatching { raw.toDraftSnapshot() }.getOrElse {
            draftStore.clearQuickCreateDraft()
            return null
        }

        return if (snapshot.hasPromptContent) {
            snapshot
        } else {
            draftStore.clearQuickCreateDraft()
            null
        }
    }

    /**
     * 保存最新可恢复草稿。
     *
     * 图片和视频提示词都为空时没有恢复价值，直接清理底层存储；这样页面清空输入后不会在下次进入时
     * 看到一个空的草稿提示。
     */
    override suspend fun saveDraft(snapshot: QuickCreateDraftSnapshot) {
        if (snapshot.hasPromptContent) {
            draftStore.saveQuickCreateDraft(snapshot.toJsonString())
        } else {
            draftStore.clearQuickCreateDraft()
        }
    }

    /**
     * 清理已保存草稿。
     */
    override suspend fun clearDraft() {
        draftStore.clearQuickCreateDraft()
    }

    private val QuickCreateDraftSnapshot.hasPromptContent: Boolean
        get() = imagePrompt.isNotBlank() || videoPrompt.isNotBlank()

    private fun String.toDraftSnapshot(): QuickCreateDraftSnapshot {
        val element = json.parseToJsonElement(this).jsonObject
        return QuickCreateDraftSnapshot(
            currentTab = element["currentTab"]?.jsonPrimitive?.contentOrNull ?: "IMAGE",
            imagePrompt = element["imagePrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            videoPrompt = element["videoPrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }

    private fun QuickCreateDraftSnapshot.toJsonString(): String =
        buildJsonObject {
            put("currentTab", currentTab)
            put("imagePrompt", imagePrompt)
            put("videoPrompt", videoPrompt)
        }.toString()
}
