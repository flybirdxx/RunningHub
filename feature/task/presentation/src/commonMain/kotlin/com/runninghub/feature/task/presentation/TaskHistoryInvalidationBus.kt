package com.runninghub.feature.task.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 任务历史刷新信号总线。
 *
 * 该实现位于 Task Presentation 层，统一承载任务提交、取消或终态变化后的 History 刷新信号。
 * 它不保存任务详情、不持久化事件，也不携带服务端错误；当 History 页面不可见时允许丢弃信号，
 * 下次进入页面仍由 [TaskHistoryStateHolder] 主动加载最新历史。
 */
class TaskHistoryInvalidationBus :
    TaskHistoryInvalidationEvents,
    TaskHistoryInvalidationNotifier {

    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 每次收到一个元素表示历史列表需要静默刷新一次。 */
    override val invalidations: Flow<Unit> = events.asSharedFlow()

    /**
     * 发出一次历史可能变化的轻量通知。
     *
     * 使用非挂起发送是为了让任务提交链路不被当前页面是否正在收集刷新信号阻塞。
     */
    override fun notifyTaskHistoryInvalidated() {
        events.tryEmit(Unit)
    }
}
