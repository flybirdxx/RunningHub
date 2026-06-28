package com.runninghub.feature.task.presentation

import kotlinx.coroutines.flow.Flow

/**
 * History 页面监听的任务历史刷新信号。
 *
 * 该端口只表达“任务历史可能已经变化”，不携带任务详情、凭据或远端错误。任务发起、完成或取消等来源
 * 通过应用组合根发出信号，History StateHolder 收到后自行按当前列表规则刷新。
 */
interface TaskHistoryInvalidationEvents {
    /** 每次收到一个元素表示历史列表需要静默刷新一次。 */
    val invalidations: Flow<Unit>
}

/**
 * 任务提交链路用于通知 History 列表刷新的轻量端口。
 */
fun interface TaskHistoryInvalidationNotifier {
    /** 通知 History 相关任务数据可能已经变化。 */
    fun notifyTaskHistoryInvalidated()
}
