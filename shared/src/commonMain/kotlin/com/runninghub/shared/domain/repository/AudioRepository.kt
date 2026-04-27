package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.AudioRequest
import com.runninghub.shared.domain.model.AudioTaskResult
import com.runninghub.shared.domain.model.AudioTaskStatus
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun convertTextToAudio(request: AudioRequest): Flow<AudioTaskStatus>
    suspend fun queryTask(taskId: String): Result<AudioTaskResult>
}
