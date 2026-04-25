package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.AppResult
import kotlinx.coroutines.flow.Flow

data class AudioTask(
    val taskId: String,
    val status: String,
    val audioUrl: String? = null,
    val errorMessage: String? = null
)

interface AudioRepository {
    suspend fun generateAudio(
        text: String,
        voiceId: String,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
        pitch: Int = 0,
        emotion: String? = null
    ): AppResult<AudioTask>

    fun pollAudioTask(taskId: String): Flow<AppResult<AudioTask>>
}
