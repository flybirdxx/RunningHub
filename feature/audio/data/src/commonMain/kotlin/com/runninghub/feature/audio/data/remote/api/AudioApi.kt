package com.runninghub.feature.audio.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.feature.audio.data.remote.dto.MiniMaxAudioRequestDto
import com.runninghub.feature.audio.data.remote.dto.MiniMaxAudioResponseDto
import com.runninghub.feature.audio.data.remote.dto.TaskQueryResultDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 音频生成相关 OpenAPI 接口的 Data 层访问入口。
 *
 * 当前音频能力仍复用 OpenAPI v2 路径，本类只处理网络调用和 DTO 解析，
 * 具体业务错误映射由上层 Repository 或调用方保持一致处理。
 */
class AudioApi(private val client: HttpClient) {

    /**
     * 提交 MiniMax 文本转音频任务。
     *
     * endpoint 通过 [RunningHubApiEnvironment.openApiV2Url] 统一拼接，避免 Data API 类各自维护
     * Web 根地址；调用方仍需在 Repository 层完成业务错误映射和领域模型转换。
     */
    suspend fun textToAudio(request: MiniMaxAudioRequestDto): MiniMaxAudioResponseDto =
        client.post(RunningHubApiEnvironment.openApiV2Url("rhart-audio/text-to-audio/speech-2.8-hd")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询音频任务执行状态和输出。
     *
     * 这里保持与快捷创作任务查询一致的 GET + query 参数形态；任务状态字段仍以 DTO 返回，
     * 由 Repository 统一处理轮询状态、失败原因和可展示文案。
     */
    suspend fun queryTask(taskId: String): TaskQueryResultDto =
        client.get(RunningHubApiEnvironment.openApiV2Url("query")) {
            parameter("taskId", taskId)
        }.body()
}
