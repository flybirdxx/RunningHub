package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.UiStateSnapshotStore
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionConversationSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionResultSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionSnapshotRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 快捷创作会话快照的数据层实现。
 *
 * 本实现复用 core/storage 的通用非敏感快照端口；具体 JSON schema 由 QuickCreate Data 层拥有，
 * 因此 Presentation 不需要理解 DataStore key 或序列化兼容规则。
 */
class QuickCreateSessionSnapshotRepositoryImpl(
    private val snapshotStore: UiStateSnapshotStore,
    private val json: Json,
) : QuickCreateSessionSnapshotRepository {
    override suspend fun getSnapshot(): QuickCreateSessionSnapshot? {
        val raw = snapshotStore.getSnapshot(KEY) ?: return null
        val dto = runCatching { json.decodeFromString<QuickCreateSessionSnapshotDto>(raw) }.getOrElse {
            snapshotStore.clearSnapshot(KEY)
            return null
        }
        val snapshot = dto.toDomain()
        return if (snapshot.hasRestorableContent) {
            snapshot
        } else {
            snapshotStore.clearSnapshot(KEY)
            null
        }
    }

    override suspend fun saveSnapshot(snapshot: QuickCreateSessionSnapshot) {
        if (snapshot.hasRestorableContent) {
            snapshotStore.saveSnapshot(KEY, json.encodeToString(snapshot.toDto()))
        } else {
            clearSnapshot()
        }
    }

    override suspend fun clearSnapshot() {
        snapshotStore.clearSnapshot(KEY)
    }

    private val QuickCreateSessionSnapshot.hasRestorableContent: Boolean
        get() = results.any { it.url.isNotBlank() } || conversationItems.isNotEmpty()

    private fun QuickCreateSessionSnapshot.toDto(): QuickCreateSessionSnapshotDto =
        QuickCreateSessionSnapshotDto(
            currentTab = currentTab,
            submittedPrompt = submittedPrompt,
            taskStatus = taskStatus,
            taskId = taskId,
            results = results.map { it.toDto() },
            conversationItems = conversationItems.map { it.toDto() },
        )

    private fun QuickCreateSessionSnapshotDto.toDomain(): QuickCreateSessionSnapshot =
        QuickCreateSessionSnapshot(
            currentTab = currentTab,
            submittedPrompt = submittedPrompt,
            taskStatus = taskStatus,
            taskId = taskId,
            results = results.mapNotNull { it.toDomainOrNull() },
            conversationItems = conversationItems.mapNotNull { it.toDomainOrNull() },
        )

    private fun QuickCreateSessionResultSnapshot.toDto(): QuickCreateSessionResultSnapshotDto =
        QuickCreateSessionResultSnapshotDto(
            url = url,
            type = type,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height,
            duration = duration,
        )

    private fun QuickCreateSessionResultSnapshotDto.toDomainOrNull(): QuickCreateSessionResultSnapshot? {
        if (url.isBlank()) return null
        return QuickCreateSessionResultSnapshot(
            url = url,
            type = type,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height,
            duration = duration,
        )
    }

    private fun QuickCreateSessionConversationSnapshot.toDto(): QuickCreateSessionConversationSnapshotDto =
        QuickCreateSessionConversationSnapshotDto(
            prompt = prompt,
            taskStatus = taskStatus,
            taskId = taskId,
            aspectRatio = aspectRatio,
            resolution = resolution,
            results = results.map { it.toDto() },
        )

    private fun QuickCreateSessionConversationSnapshotDto.toDomainOrNull(): QuickCreateSessionConversationSnapshot? {
        val restoredResults = results.mapNotNull { it.toDomainOrNull() }
        if (prompt.isBlank() && restoredResults.isEmpty() && taskId.isNullOrBlank()) return null
        return QuickCreateSessionConversationSnapshot(
            prompt = prompt,
            taskStatus = taskStatus,
            taskId = taskId,
            aspectRatio = aspectRatio,
            resolution = resolution,
            results = restoredResults,
        )
    }

    private companion object {
        private const val KEY = "quick_create_session_v1"
    }
}

@Serializable
private data class QuickCreateSessionSnapshotDto(
    val currentTab: String = "IMAGE",
    val submittedPrompt: String = "",
    val taskStatus: String = "IDLE",
    val taskId: String? = null,
    val results: List<QuickCreateSessionResultSnapshotDto> = emptyList(),
    val conversationItems: List<QuickCreateSessionConversationSnapshotDto> = emptyList(),
)

@Serializable
private data class QuickCreateSessionResultSnapshotDto(
    val url: String = "",
    val type: String = "",
    val mediaType: String = "IMAGE",
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

@Serializable
private data class QuickCreateSessionConversationSnapshotDto(
    val prompt: String = "",
    val taskStatus: String = "IDLE",
    val taskId: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val results: List<QuickCreateSessionResultSnapshotDto> = emptyList(),
)
