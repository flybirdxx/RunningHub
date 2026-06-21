package com.runninghub.core.model

/**
 * WebApp 工作流任务提交结果。
 *
 * 该模型位于 core:model，只描述任务提交后业务侧需要继续轮询的标识和状态。
 * 远端返回的状态字符串会在 Data 层转换为 [TaskExecutionStatus]，避免 Presentation
 * 直接判断 API 协议值。
 *
 * @property netWssUrl 远端返回的 WebSocket 地址，当前页面轮询输出接口时暂未使用。
 * @property taskId RunningHub 任务 ID，用于后续查询输出。
 * @property clientId 远端客户端标识，保留用于兼容旧接口响应。
 * @property status 归一化后的任务执行状态；远端未返回时为空。
 * @property promptTips 远端提示信息，调用方可按业务需要展示或忽略。
 */
data class TaskResult(
    val netWssUrl: String?,
    val taskId: Long,
    val clientId: String?,
    val status: TaskExecutionStatus?,
    val promptTips: String?,
)

/**
 * 任务输出文件。
 *
 * @property fileUrl 输出文件地址。
 * @property fileName 输出文件名。
 * @property fileType 输出类型，例如 image、video 或 audio。
 * @property failedReason 单个输出节点失败时的原因；为空表示该输出没有失败信息。
 */
data class TaskOutput(
    val fileUrl: String?,
    val fileName: String?,
    val fileType: String?,
    val failedReason: TaskFailedReason?,
)

/**
 * 任务输出节点失败原因。
 *
 * @property nodeName 失败节点名称。
 * @property exceptionMessage 远端返回的异常摘要。
 * @property traceback 远端调试堆栈，仅用于问题排查，不应直接展示给普通用户。
 */
data class TaskFailedReason(
    val nodeName: String?,
    val exceptionMessage: String?,
    val traceback: String?,
)

/**
 * 上传任务输入文件后的远端文件信息。
 *
 * @property fileName 服务端保存后的文件名。
 * @property fileType 服务端识别的文件类型。
 */
data class UploadResult(
    val fileName: String?,
    val fileType: String?,
)

/**
 * 兼容期 WebApp 任务历史条目。
 *
 * 当前生产历史页已经迁移到快捷创作历史模型，该模型保留给 WebApp 旧历史接口。
 * 状态在 Data 层统一转换为 [TaskExecutionStatus]，调用方不再依赖远端 taskStatus 字段名。
 *
 * @property taskId 任务 ID。
 * @property outputs 历史输出文件列表。
 * @property status 归一化后的任务状态。
 * @property taskCostTime 远端返回的耗时文案。
 * @property createTime 创建时间文案。
 * @property taskName 任务名称。
 * @property webappId 来源 WebApp ID。
 */
data class TaskHistoryItem(
    val taskId: String?,
    val outputs: List<TaskHistoryOutput>,
    val status: TaskExecutionStatus?,
    val taskCostTime: String?,
    val createTime: String?,
    val taskName: String?,
    val webappId: String?,
)

/**
 * WebApp 历史输出文件。
 *
 * @property id 输出 ID。
 * @property outputName 输出名称。
 * @property outputType 输出类型。
 * @property fileUrl 原始文件地址。
 * @property filePreviewUrl 预览地址。
 * @property outputSize 输出尺寸或大小文案。
 * @property expireDays 剩余有效天数。
 */
data class TaskHistoryOutput(
    val id: String?,
    val outputName: String?,
    val outputType: String?,
    val fileUrl: String?,
    val filePreviewUrl: String?,
    val outputSize: String?,
    val expireDays: String?,
)

/**
 * 任务执行状态。
 *
 * Data 层将服务端 taskStatus/status 字符串转换为这些稳定业务状态。未知状态会保留原始值，
 * 这样既能避免 UI 直接依赖远端协议，又能在服务端新增状态时维持兼容展示和日志排查能力。
 *
 * @property rawValue 服务端原始状态值；标准状态使用规范化大写值，未知状态保留原始字符串。
 */
sealed class TaskExecutionStatus(val rawValue: String?) {
    /** 任务已经提交但尚未进入队列。 */
    data object Submitted : TaskExecutionStatus("SUBMITTED")

    /** 任务正在排队等待执行资源。 */
    data object Queued : TaskExecutionStatus("QUEUED")

    /** 任务正在执行。 */
    data object Running : TaskExecutionStatus("RUNNING")

    /** 任务已经成功完成。 */
    data object Success : TaskExecutionStatus("SUCCESS")

    /** 任务执行失败。 */
    data object Failed : TaskExecutionStatus("FAILED")

    /** 任务已经取消。 */
    data object Cancelled : TaskExecutionStatus("CANCELLED")

    /**
     * 服务端新增或暂未归类的状态。
     *
     * @property value 服务端原始状态；为空表示响应没有携带状态。
     */
    data class Unknown(val value: String?) : TaskExecutionStatus(value)

    companion object {
        /**
         * 根据远端状态字符串创建领域状态。
         *
         * @param raw 远端 taskStatus/status 字符串，允许为空。
         * @return 归一化领域状态；未知值返回 [Unknown] 并保留原始字符串。
         */
        fun fromRaw(raw: String?): TaskExecutionStatus {
            val normalized = raw?.trim()?.uppercase()
            return when (normalized) {
                "SUBMITTED", "SUBMITTING" -> Submitted
                "QUEUED", "QUEUEING", "QUEUING", "PENDING", "WAITING" -> Queued
                "RUNNING", "PROCESSING", "IN_PROGRESS" -> Running
                "SUCCESS", "COMPLETED", "DONE" -> Success
                "FAILED", "FAIL", "FAILURE", "ERROR" -> Failed
                "CANCELED", "CANCELLED" -> Cancelled
                else -> Unknown(raw)
            }
        }
    }
}

/**
 * 判断任务是否已经进入终态。
 *
 * @return 成功、失败或取消时返回 true；未知状态保持兼容，按非终态处理。
 */
fun TaskExecutionStatus.isTerminal(): Boolean =
    this is TaskExecutionStatus.Success ||
        this is TaskExecutionStatus.Failed ||
        this is TaskExecutionStatus.Cancelled

/**
 * 判断任务是否失败。
 *
 * @return 失败状态返回 true。
 */
fun TaskExecutionStatus.isFailed(): Boolean = this is TaskExecutionStatus.Failed

/**
 * 判断任务是否成功完成。
 *
 * @return 成功状态返回 true。
 */
fun TaskExecutionStatus.isSuccessful(): Boolean = this is TaskExecutionStatus.Success
