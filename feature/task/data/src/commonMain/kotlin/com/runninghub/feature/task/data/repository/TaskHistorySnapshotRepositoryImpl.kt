package com.runninghub.feature.task.data.repository

import com.runninghub.core.storage.UiStateSnapshotStore
import com.runninghub.feature.task.domain.TaskHistorySnapshot
import com.runninghub.feature.task.domain.TaskHistorySnapshotRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 历史页 UI 快照的数据层实现。
 */
class TaskHistorySnapshotRepositoryImpl(
    private val snapshotStore: UiStateSnapshotStore,
    private val json: Json,
) : TaskHistorySnapshotRepository {
    override suspend fun getSnapshot(): TaskHistorySnapshot? {
        val raw = snapshotStore.getSnapshot(KEY) ?: return null
        return runCatching { raw.toSnapshot() }.getOrElse {
            snapshotStore.clearSnapshot(KEY)
            null
        }
    }

    override suspend fun saveSnapshot(snapshot: TaskHistorySnapshot) {
        if (snapshot.filter.isBlank()) {
            clearSnapshot()
        } else {
            snapshotStore.saveSnapshot(KEY, snapshot.toJsonString())
        }
    }

    override suspend fun clearSnapshot() {
        snapshotStore.clearSnapshot(KEY)
    }

    private fun String.toSnapshot(): TaskHistorySnapshot {
        val element = json.parseToJsonElement(this).jsonObject
        return TaskHistorySnapshot(
            filter = element["filter"]?.jsonPrimitive?.contentOrNull ?: "ALL",
        )
    }

    private fun TaskHistorySnapshot.toJsonString(): String =
        buildJsonObject {
            put("filter", filter)
        }.toString()

    private companion object {
        private const val KEY = "task_history_ui_v1"
    }
}
