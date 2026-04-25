package com.runninghub.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [INPUT]: Audio generation parameters
 * [OUTPUT]: MiniMax Audio Request Data
 * [POS]: Define parameters for speech-2.8-hd API
 */
data class MiniMaxAudioRequest(
    val text: String,
    @SerializedName("voice_id") val voiceId: String,
    @SerializedName("pronunciation_dict") val pronunciationDict: List<String>? = null,
    val speed: Float? = 1.0f,
    val volume: Float? = 1.0f,
    val pitch: Int? = 0,
    @SerializedName("enable_base64_output") val enableBase64Output: Boolean? = false,
    @SerializedName("english_normalization") val englishNormalization: Boolean? = false,
    val emotion: String? = null // happy, sad, angry, fearful, disgusted, surprised, neutral
)

data class MiniMaxAudioResponse(
    val taskId: String,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val results: List<AudioResult>? = null,
    val clientId: String?,
    val promptTips: String?
)

data class TaskQueryResult(
    val taskId: String,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val results: List<AudioResult>? = null,
    val usage: TaskUsage? = null
)

data class AudioResult(
    val url: String?,
    val outputType: String?,
    val text: String?
)

data class TaskUsage(
    val consumeMoney: String?,
    val consumeCoins: String?,
    val taskCostTime: String?,
    val thirdPartyConsumeMoney: String?
)

data class TaskQueryRequest(
    val taskId: String
)
