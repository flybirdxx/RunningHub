package com.runninghub.feature.task.domain

/**
 * 历史页可恢复 UI 快照。
 *
 * 当前只保存用户可见的本地筛选条件；历史任务数据仍由远端历史和本地 overlay 提供，避免复制服务端列表。
 */
data class TaskHistorySnapshot(
    val filter: String = "ALL",
)

/**
 * 历史页 UI 快照仓库。
 */
interface TaskHistorySnapshotRepository {
    /**
     * 读取最近一次历史页快照。
     */
    suspend fun getSnapshot(): TaskHistorySnapshot?

    /**
     * 保存最近一次历史页快照。
     */
    suspend fun saveSnapshot(snapshot: TaskHistorySnapshot)

    /**
     * 清理历史页快照。
     */
    suspend fun clearSnapshot()
}
