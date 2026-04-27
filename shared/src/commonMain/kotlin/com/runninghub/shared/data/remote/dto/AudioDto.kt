package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiniMaxAudioRequestDto(
    @SerialName("text") val text: String,
    @SerialName("voice_id") val voiceId: String,
    @SerialName("pronunciation_dict") val pronunciationDict: List<String>? = null,
    @SerialName("speed") val speed: Float? = 1.0f,
    @SerialName("volume") val volume: Float? = 1.0f,
    @SerialName("pitch") val pitch: Int? = 0,
    @SerialName("enable_base64_output") val enableBase64Output: Boolean? = false,
    @SerialName("english_normalization") val englishNormalization: Boolean? = false,
    @SerialName("emotion") val emotion: String? = null
)

@Serializable
data class MiniMaxAudioResponseDto(
    @SerialName("taskId") val taskId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<AudioResultDto>? = null,
    @SerialName("clientId") val clientId: String? = null,
    @SerialName("promptTips") val promptTips: String? = null
)

@Serializable
data class TaskQueryResultDto(
    @SerialName("taskId") val taskId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<AudioResultDto>? = null,
    @SerialName("usage") val usage: TaskUsageDto? = null
)

@Serializable
data class AudioResultDto(
    @SerialName("url") val url: String? = null,
    @SerialName("outputType") val outputType: String? = null,
    @SerialName("text") val text: String? = null
)

@Serializable
data class TaskUsageDto(
    @SerialName("consumeMoney") val consumeMoney: String? = null,
    @SerialName("consumeCoins") val consumeCoins: String? = null,
    @SerialName("taskCostTime") val taskCostTime: String? = null,
    @SerialName("thirdPartyConsumeMoney") val thirdPartyConsumeMoney: String? = null
)

@Serializable
data class TaskQueryRequestDto(
    @SerialName("taskId") val taskId: String
)
