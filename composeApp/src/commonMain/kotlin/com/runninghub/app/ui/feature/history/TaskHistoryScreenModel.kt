package com.runninghub.app.ui.feature.history

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryStateHolder
import com.runninghub.feature.task.presentation.TaskHistoryUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * History Voyager 页面对 Task Presentation 状态持有器的生命周期适配层。
 *
 * 真实列表加载、详情、取消、参数复用和轮询规则由 [TaskHistoryStateHolder] 承担；
 * 本类只负责把 Voyager 的 [screenModelScope] 传入并在页面销毁时释放轮询。
 *
 * @param generationHistoryRepository 历史任务仓库接口，由 Koin 从 Task Data 实现绑定注入。
 * @param enablePolling 是否启用 History 页面后台轮询。
 * `true` 表示存在运行中任务时按 Presentation 层策略刷新；`false` 主要用于测试或临时禁用。
 */
class TaskHistoryScreenModel(
    generationHistoryRepository: GenerationHistoryRepository,
    enablePolling: Boolean = true,
) : ScreenModel {
    private val stateHolder = TaskHistoryStateHolder(
        generationHistoryRepository = generationHistoryRepository,
        coroutineScope = screenModelScope,
        enablePolling = enablePolling,
    )

    /**
     * 历史页只读 UI 状态。
     *
     * UI 只能收集该状态并通过本类公开动作发送事件，避免应用壳重新实现 Task Presentation 的状态规则。
     */
    val uiState: StateFlow<TaskHistoryUiState> = stateHolder.uiState

    /**
     * 加载第一页历史记录。
     *
     * 该动作直接委托给 [TaskHistoryStateHolder]，保持 Voyager 适配层无业务分支。
     */
    fun loadHistory() {
        stateHolder.loadHistory()
    }

    /**
     * 切换历史任务筛选条件。
     *
     * @param filter 用户在 History 页面选择的筛选条件。
     */
    fun setFilter(filter: TaskHistoryFilter) {
        stateHolder.setFilter(filter)
    }

    /**
     * 加载指定输出的历史详情。
     *
     * @param outputId 列表项中第一项输出的稳定 ID。
     */
    fun selectOutput(outputId: String) {
        stateHolder.selectOutput(outputId)
    }

    /**
     * 准备复用指定历史任务参数。
     *
     * @param taskId 历史任务 ID。
     */
    fun prepareReuseParams(taskId: String) {
        stateHolder.prepareReuseParams(taskId)
    }

    /**
     * 准备失败任务的重试参数。
     *
     * @param taskId 历史任务 ID。
     */
    fun retryTask(taskId: String) {
        stateHolder.retryTask(taskId)
    }

    /**
     * 请求取消运行中的历史任务。
     *
     * @param taskId 历史任务 ID。
     */
    fun cancelTask(taskId: String) {
        stateHolder.cancelTask(taskId)
    }

    override fun onDispose() {
        stateHolder.dispose()
        super.onDispose()
    }
}
