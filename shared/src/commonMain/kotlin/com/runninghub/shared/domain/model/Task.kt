package com.runninghub.shared.domain.model

data class TaskResult(
    val netWssUrl: String?,
    val taskId: Long,
    val clientId: String?,
    val taskStatus: String?,
    val promptTips: String?
)

data class TaskOutput(
    val fileUrl: String?,
    val fileName: String?,
    val fileType: String?,
    val failedReason: TaskFailedReason?
)

data class TaskFailedReason(
    val nodeName: String?,
    val exceptionMessage: String?,
    val traceback: String?
)

data class UploadResult(
    val fileName: String?,
    val fileType: String?
)

data class TaskHistoryItem(
    val taskId: String?,
    val outputs: List<TaskHistoryOutput>,
    val taskStatus: String?,
    val taskCostTime: String?,
    val createTime: String?,
    val taskName: String?,
    val webappId: String?,
)

data class TaskHistoryOutput(
    val id: String?,
    val outputName: String?,
    val outputType: String?,
    val fileUrl: String?,
    val filePreviewUrl: String?,
    val outputSize: String?,
    val expireDays: String?,
)

sealed class AudioTaskStatus {
    data object Submitting : AudioTaskStatus()
    data class Running(val taskId: String) : AudioTaskStatus()
    data class Success(val url: String) : AudioTaskStatus()
    data class Error(val message: String) : AudioTaskStatus()
}

data class AudioRequest(
    val text: String,
    val voiceId: String,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Int = 0,
    val emotion: String? = null
)

data class AudioResult(
    val url: String?,
    val outputType: String?,
    val text: String?
)

data class AudioTaskResult(
    val taskId: String,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val results: List<AudioResult>?
)
