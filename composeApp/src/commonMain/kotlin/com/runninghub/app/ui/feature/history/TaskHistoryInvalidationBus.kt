package com.runninghub.app.ui.feature.history

import com.runninghub.feature.task.presentation.TaskHistoryInvalidationEvents
import com.runninghub.feature.task.presentation.TaskHistoryInvalidationNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 应用内任务历史刷新信号总线。
 *
 * 该实现只驻留在 composeApp 组合层，用于连接各创作入口和 History 页面。它不持久化事件，也不携带任务
 * 详情；History 页面不可见时丢弃信号，下一次进入页面仍会主动加载最新历史。
 */
internal class TaskHistoryInvalidationBus :
    TaskHistoryInvalidationEvents,
    TaskHistoryInvalidationNotifier {

    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val invalidations: Flow<Unit> = events.asSharedFlow()

    override fun notifyTaskHistoryInvalidated() {
        events.tryEmit(Unit)
    }
}
