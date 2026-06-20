package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.ModelInvocationRequest
import com.runninghub.shared.domain.model.ModelInvocationTask

/**
 * 标准模型调用仓库的领域接口。
 *
 * Presentation 层通过该接口提交模型任务、查询任务状态和上传媒体素材。
 * API Key 等敏感凭据由 Data 层实现从安全存储读取，调用方不得透传或持有凭据字符串。
 */
interface ModelInvocationRepository {
    /**
     * 提交标准模型调用任务。
     *
     * @param request 领域层模型调用请求，包含模型 ID、字段定义和值；具体 endpoint 由 Data 层解析。
     * @return 提交成功时返回远端任务；网络错误、响应格式错误或业务错误以 [Result.failure] 返回。
     */
    suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask>

    /**
     * 查询模型调用任务状态。
     *
     * @param taskId 远端任务 ID。
     * @return 查询成功时返回任务状态和输出；失败时保留底层错误供调用方映射为 UI 状态。
     */
    suspend fun queryTask(taskId: String): Result<ModelInvocationTask>

    /**
     * 上传模型调用所需的媒体素材。
     *
     * API Key 在 Data 层实现内部读取，避免凭据进入 Presentation 或 Domain 调用参数。
     *
     * @param fileBytes 文件二进制内容。
     * @param fileName 原始文件名，用于远端 multipart 元数据。
     * @param contentType 文件 MIME 类型，例如 `image/png` 或 `video/mp4`。
     * @return 上传成功时返回远端可访问 URL；缺少 API Key、网络失败或响应缺少 URL 时返回失败。
     */
    suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Result<String>
}
