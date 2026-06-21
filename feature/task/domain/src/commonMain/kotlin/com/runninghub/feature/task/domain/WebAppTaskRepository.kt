package com.runninghub.feature.task.domain

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.TaskOutput
import com.runninghub.core.model.TaskResult
import com.runninghub.core.model.UploadResult

/**
 * WebApp 任务执行仓库。
 *
 * 该接口属于 Task Feature Domain，只覆盖需要 API Key 的 WebApp 运行链路，包括读取可运行示例、
 * 上传输入文件、提交任务和轮询输出。公开目录与搜索能力应继续依赖 Discovery Domain 的目录仓库，
 * 避免详情页重新依赖包含目录、任务、上传和历史的宽接口。
 */
interface WebAppTaskRepository {
    /**
     * 读取需要 API Key 的可运行调用示例详情。
     *
     * @param webappId WebApp ID。
     * @return 成功时返回带调用参数的详情；未绑定 API Key 或网络失败时返回失败结果。
     */
    suspend fun getApiCallDemo(webappId: String): Result<AppDetail>

    /**
     * 提交 WebApp 工作流任务。
     *
     * @param webappId WebApp 数字 ID。
     * @param nodeInfoList 页面输入节点列表，Data 层会转换为远端 DTO。
     * @param webhookUrl 任务回调地址；为空表示不使用回调。
     * @param instanceType 运行实例类型；为空表示使用服务端默认配置。
     * @return 成功时返回任务 ID 和归一化提交状态。
     */
    suspend fun runTask(
        webappId: Long,
        nodeInfoList: List<InputNode>,
        webhookUrl: String? = null,
        instanceType: String? = null,
    ): Result<TaskResult>

    /**
     * 查询任务输出。
     *
     * @param taskId 远端任务 ID。
     * @return 成功时返回输出列表；任务尚未产出时可能为空列表。
     */
    suspend fun getTaskOutputs(taskId: Long): Result<List<TaskOutput>>

    /**
     * 上传任务输入文件。
     *
     * @param fileType MIME 类型。
     * @param fileBytes 文件内容字节。
     * @param fileName 展示文件名。
     * @return 成功时返回服务端文件信息。
     */
    suspend fun uploadFile(
        fileType: String,
        fileBytes: ByteArray,
        fileName: String,
    ): Result<UploadResult>
}
