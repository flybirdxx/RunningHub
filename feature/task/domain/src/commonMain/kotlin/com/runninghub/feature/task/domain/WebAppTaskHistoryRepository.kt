package com.runninghub.feature.task.domain

import com.runninghub.core.model.TaskHistoryItem

/**
 * WebApp 任务历史仓库。
 *
 * 该接口属于 Task Feature Domain，只覆盖当前 API Key 维度的 WebApp 任务历史查询。
 * 它与公开目录、任务提交和上传能力分离，避免历史页或兼容适配器重新依赖包含多种职责的宽仓库。
 */
interface WebAppTaskHistoryRepository {
    /**
     * 分页读取当前 API Key 对应的 WebApp 任务历史。
     *
     * @param pageNum 页码，从 1 开始。
     * @param pageSize 每页数量。
     * @return 成功时返回历史条目；未绑定 API Key、会话失效或网络失败时返回失败结果。
     */
    suspend fun getTaskHistory(pageNum: Int, pageSize: Int): Result<List<TaskHistoryItem>>
}
