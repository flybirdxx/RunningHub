package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作任务历史的领域仓库边界。
 *
 * 本接口只承载最近历史、项目任务列表、历史详情和取消任务能力。项目本身的创建、重命名、
 * 删除、置顶和详情由 [QuickCreationProjectRepository] 承担，避免历史轮询与项目元数据管理
 * 再次聚合成万能仓库。
 */
interface QuickCreationTaskHistoryRepository {

    /**
     * 分页读取最近快捷创作历史。
     *
     * @param page 页码，从 1 开始；调用方不应传入小于 1 的值。
     * @param size 每页历史数量，单位为条；默认值保持与快捷创作历史区域一致。
     * @return 历史分页结果；失败时通过 [Result.failure] 返回，由 Presentation 决定是否保留旧列表。
     */
    suspend fun listQuickCreationHistory(page: Int = 1, size: Int = 10): Result<QuickCreationHistoryPage>

    /**
     * 读取单个快捷创作历史输出详情。
     *
     * @param outputId 历史输出项稳定标识，来源于历史列表中的输出集合；空字符串不应提交。
     * @return 历史任务详情，包含输出地址、参数和任务状态；失败时调用方应关闭加载态并展示可重试错误。
     */
    suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem>

    /**
     * 取消仍处于非终态的快捷创作任务。
     *
     * @param taskId 服务端任务稳定标识；调用方应先根据历史状态确认任务仍允许取消。
     * @return 成功时返回 [Unit]；失败时调用方应保留原任务状态并提示取消失败。
     */
    suspend fun cancelQuickCreationTask(taskId: String): Result<Unit>

    /**
     * 分页读取指定项目下的快捷创作任务。
     *
     * 该方法保留在历史边界中，因为返回值和刷新策略与最近历史一致，并由历史区域负责轮询和详情读取。
     *
     * @param projectId 项目稳定标识，来源于项目列表；空字符串不应提交。
     * @param page 页码，从 1 开始；调用方不应传入小于 1 的值。
     * @param size 每页任务数量，单位为条；默认值保持与快捷创作历史区域一致。
     * @return 项目任务分页结果；失败时调用方应保留当前项目筛选与旧任务列表。
     */
    suspend fun listQuickCreationProjectTasks(
        projectId: String,
        page: Int = 1,
        size: Int = 10,
    ): Result<QuickCreationHistoryPage>
}
