package com.runninghub.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiniMaxAudioRequest(
    val text: String,
    @SerialName("voice_id") val voiceId: String,
    @SerialName("pronunciation_dict") val pronunciationDict: List<String>? = null,
    val speed: Float? = 1.0f,
    val volume: Float? = 1.0f,
    val pitch: Int? = 0,
    @SerialName("enable_base64_output") val enableBase64Output: Boolean? = false,
    @SerialName("english_normalization") val englishNormalization: Boolean? = false,
    val emotion: String? = null
)

@Serializable
data class MiniMaxAudioResponse(
    val taskId: String? = null,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val results: List<AudioResult>? = null,
    val clientId: String? = null,
    val promptTips: String? = null
)

@Serializable
data class TaskQueryResult(
    val taskId: String? = null,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val results: List<AudioResult>? = null,
    val usage: TaskUsage? = null
)

@Serializable
data class AudioResult(
    val url: String? = null,
    val outputType: String? = null,
    val text: String? = null
)

@Serializable
data class TaskUsage(
    val consumeMoney: String? = null,
    val consumeCoins: String? = null,
    val taskCostTime: String? = null,
    val thirdPartyConsumeMoney: String? = null
)

@Serializable
data class TaskQueryRequest(
    val taskId: String
)
