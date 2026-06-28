package com.runninghub.app.ui.feature.history

import com.runninghub.feature.detail.presentation.AppDetailSubmittedTask
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistorySource

/**
 * WebApp 本机任务历史补充缓存。
 *
 * 控制台任务宽表是 History 页的主数据源，但任务刚提交后服务端写入宽表可能存在短暂延迟。
 * 因此 App 需要把本机刚提交的 AI 应用任务暂存到内存列表；一旦宽表返回同一个 taskId，
 * 统一历史仓库会让服务端记录接管，避免长期展示本地兜底数据。
 */
class WebAppTaskHistoryOverlayStore {
    private val lock = Any()
    private var trackedItems: List<GenerationHistoryItem> = emptyList()

    /**
     * 记录详情页刚提交成功的 WebApp 任务。
     *
     * @param task 提交响应和详情页上下文组成的轻量任务快照，不包含 API Key、Cookie 或输入请求体。
     */
    fun trackSubmittedTask(task: AppDetailSubmittedTask) {
        upsert(
            GenerationHistoryItem(
                taskId = task.taskId,
                source = GenerationHistorySource.WEBAPP,
                status = task.status.ifBlank { "SUBMITTED" },
                modelId = task.webappId,
                taskType = task.taskName ?: task.webappId,
            )
        )
    }

    /**
     * 插入或更新一条补充历史。
     *
     * 新任务放在列表前面，使刚提交的任务在历史页首屏可见；相同 taskId 后续刷新只替换内容。
     */
    fun upsert(item: GenerationHistoryItem) {
        synchronized(lock) {
            trackedItems = listOf(item) + trackedItems.filterNot { it.taskId == item.taskId }
        }
    }

    /**
     * 读取当前补充历史快照。
     */
    fun items(): List<GenerationHistoryItem> =
        synchronized(lock) { trackedItems }
}
