package com.runninghub.feature.task.domain

/**
 * 统一生成历史仓库。
 *
 * 该接口是历史页的 Task Feature Domain 契约。Presentation 只依赖该接口，不直接知道
 * QuickCreate 历史仓库、shared 兼容仓库或远端 API。迁移期可以由 composeApp 组合层提供唯一适配器；
 * 后续 Task Data 模块只有在同时覆盖列表、详情和取消能力后，才能替换该适配器成为正式实现。
 */
interface GenerationHistoryRepository {
    /**
     * 分页读取统一生成历史。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页条数，单位为条。
     * @return 成功时返回历史分页；失败时保留仓库错误，调用方负责展示和重试。
     */
    suspend fun listHistory(page: Int = 1, size: Int = 20): Result<GenerationHistoryPage>

    /**
     * 读取某个输出对应的历史详情。
     *
     * @param outputId 输出稳定标识，来自 [GenerationHistoryOutput.outputId]。
     * @return 成功时返回完整历史任务；失败时返回可重试错误。
     */
    suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem>

    /**
     * 取消仍在运行的任务。
     *
     * @param taskId 服务端任务稳定标识。
     * @return 取消请求结果；成功只表示服务端已接受取消或任务已进入取消流程。
     */
    suspend fun cancelTask(taskId: String): Result<Unit>
}
