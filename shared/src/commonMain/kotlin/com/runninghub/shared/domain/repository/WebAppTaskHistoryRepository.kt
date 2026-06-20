package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.TaskHistoryItem

/**
 * WebApp 任务历史仓库。
 *
 * 该接口保留旧 WebApp 历史接口的查询能力，并与目录、任务执行能力分离。当前生产历史页
 * 已经使用 [GenerationHistoryRepository]，因此本接口暂时只作为兼容期边界，避免后续调用点
 * 为了历史查询重新依赖目录或任务提交能力。
 */
interface WebAppTaskHistoryRepository {
    /**
     * 分页读取当前 API Key 对应的 WebApp 任务历史。
     *
     * @param pageNum 页码，从 1 开始。
     * @param pageSize 每页数量。
     * @return 成功时返回历史条目；未绑定 API Key 或网络失败时返回失败结果。
     */
    suspend fun getTaskHistory(pageNum: Int, pageSize: Int): Result<List<TaskHistoryItem>>
}
