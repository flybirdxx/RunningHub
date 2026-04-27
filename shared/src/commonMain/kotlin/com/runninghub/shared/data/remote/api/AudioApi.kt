package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.MiniMaxAudioRequestDto
import com.runninghub.shared.data.remote.dto.MiniMaxAudioResponseDto
import com.runninghub.shared.data.remote.dto.TaskQueryRequestDto
import com.runninghub.shared.data.remote.dto.TaskQueryResultDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AudioApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "https://www.runninghub.cn/"
    }

    suspend fun textToAudio(request: MiniMaxAudioRequestDto): MiniMaxAudioResponseDto =
        client.post("${BASE_URL}openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun queryTask(request: TaskQueryRequestDto): TaskQueryResultDto =
        client.post("${BASE_URL}openapi/v2/query") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
