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

    /**
     * 读取控制台任务详情。
     *
     * @param taskId 服务端任务稳定标识。
     * @return 成功时返回任务详情，包括输出文件、基础信息、计费信息以及已脱敏请求/响应 JSON。
     */
    suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail>
}
