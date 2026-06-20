package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.ModelInvocationRequest
import com.runninghub.shared.domain.model.ModelInvocationTask

interface ModelInvocationRepository {
    suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask>

    suspend fun queryTask(taskId: String): Result<ModelInvocationTask>

    suspend fun uploadMedia(
        apiKey: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Result<String>
}
