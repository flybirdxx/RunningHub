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

/**
 * WebApp 任务仓库的结构化失败异常。
 *
 * Data 层用该异常表达任务提交、输出、上传和历史接口的稳定失败语义；[message] 只保留诊断码，
 * Presentation 不得把它作为最终用户可见文案展示。
 *
 * @property issue WebApp 任务仓库的稳定失败语义。
 * @property remoteCode 服务端业务 code；`null` 表示失败来自成功响应缺 data 等结构异常。
 */
class WebAppTaskException(
    val issue: WebAppTaskIssue,
    val remoteCode: Int? = null,
) : IllegalStateException(issue.diagnosticMessage(remoteCode))

/**
 * WebApp 任务仓库使用的稳定错误语义。
 *
 * 这些枚举值只用于上层按类型判断错误和日志分类，不携带服务端 `msg`、底层异常 message
 * 或最终中文 UI 文案。
 *
 * @property code 稳定诊断码，可用于测试断言和日志分类；不得作为最终 UI 文案。
 */
enum class WebAppTaskIssue(val code: String) {
    /** 调用示例详情接口返回非成功业务 code。 */
    ApiCallDemoFailed("WEB_APP_TASK_API_CALL_DEMO_FAILED"),

    /** 调用示例详情接口成功但响应缺少 data。 */
    ApiCallDemoMissing("WEB_APP_TASK_API_CALL_DEMO_MISSING"),

    /** 任务提交接口返回非成功业务 code。 */
    RunTaskFailed("WEB_APP_TASK_RUN_TASK_FAILED"),

    /** 任务提交接口成功但响应缺少 data。 */
    RunTaskMissing("WEB_APP_TASK_RUN_TASK_MISSING"),

    /** 任务输出接口返回非成功业务 code。 */
    TaskOutputsFailed("WEB_APP_TASK_OUTPUTS_FAILED"),

    /** 任务输出接口成功但响应缺少 data。 */
    TaskOutputsMissing("WEB_APP_TASK_OUTPUTS_MISSING"),

    /** 文件上传接口返回非成功业务 code。 */
    UploadFileFailed("WEB_APP_TASK_UPLOAD_FILE_FAILED"),

    /** 文件上传接口成功但响应缺少 data。 */
    UploadFileMissing("WEB_APP_TASK_UPLOAD_FILE_MISSING"),

    /** 任务历史接口返回非成功业务 code。 */
    TaskHistoryFailed("WEB_APP_TASK_HISTORY_FAILED"),

    /** 任务历史接口成功但响应缺少 data。 */
    TaskHistoryMissing("WEB_APP_TASK_HISTORY_MISSING"),

    /** 任务详情接口返回非成功业务 code。 */
    TaskDetailFailed("WEB_APP_TASK_DETAIL_FAILED"),

    /** 任务详情接口成功但响应缺少 data。 */
    TaskDetailMissing("WEB_APP_TASK_DETAIL_MISSING"),
}

private fun WebAppTaskIssue.diagnosticMessage(remoteCode: Int?): String =
    if (remoteCode == null) code else "$code:$remoteCode"
