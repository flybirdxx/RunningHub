package com.runninghub.shared.domain.model

/**
 * 旧音频任务状态。
 *
 * Audio 能力尚未进入独立 Feature，本模型暂留在 shared 兼容模块；WebApp 任务模型已迁移到 core:model。
 */
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
