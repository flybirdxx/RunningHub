package com.runninghub.feature.audio.data.repository

import com.runninghub.core.network.NetworkErrorMapper
import com.runninghub.feature.audio.data.remote.api.AudioApi
import com.runninghub.feature.audio.data.remote.dto.MiniMaxAudioRequestDto
import com.runninghub.feature.audio.domain.AudioRepository
import com.runninghub.feature.audio.domain.AudioRequest
import com.runninghub.feature.audio.domain.AudioResult
import com.runninghub.feature.audio.domain.AudioTaskResult
import com.runninghub.feature.audio.domain.AudioTaskStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * 音频生成仓库的数据层实现。
 *
 * 本类负责把领域层的 [AudioRequest] 转换为 MiniMax 音频 DTO，提交远端任务并轮询结果。
 * 网络异常识别统一委托给 core/network 的 [NetworkErrorMapper]，避免各 Repository 复制
 * DNS、连接失败、超时等脆弱字符串判断；最终用户可见文案仍应由 Presentation 层决定。
 *
 * @param audioApi 音频生成和任务查询 API 封装。
 */
class AudioRepositoryImpl(
    private val audioApi: AudioApi
) : AudioRepository {

    /**
     * 提交文本转语音任务并轮询结果。
     *
     * Flow 会先发出 [AudioTaskStatus.Submitting]，提交成功后发出 [AudioTaskStatus.Running]，
     * 最终发出 [AudioTaskStatus.Success] 或 [AudioTaskStatus.Error] 后结束。轮询最多执行 60 次，
     * 每次间隔 1 秒；调用方取消收集时，协程会按 Flow 结构化取消规则停止后续轮询。
     *
     * @param request 音频生成参数，包含文本、音色和语速等配置。
     * @return 描述提交、运行、成功或失败状态的冷 Flow。
     */
    override fun convertTextToAudio(request: AudioRequest): Flow<AudioTaskStatus> = flow {
        emit(AudioTaskStatus.Submitting)

        try {
            val dto = MiniMaxAudioRequestDto(
                text = request.text,
                voiceId = request.voiceId,
                speed = request.speed,
                volume = request.volume,
                pitch = request.pitch,
                emotion = request.emotion
            )
            val submitResponse = audioApi.textToAudio(dto)

            if (submitResponse.status == "FAILED") {
                emit(AudioTaskStatus.Error(submitResponse.errorMessage ?: "Submission failed"))
                return@flow
            }

            val taskId = submitResponse.taskId
            emit(AudioTaskStatus.Running(taskId))

            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                val queryResponse = audioApi.queryTask(taskId)

                when (queryResponse.status) {
                    "SUCCESS" -> {
                        val resultUrl = queryResponse.results?.firstOrNull()?.url
                        if (resultUrl != null) {
                            emit(AudioTaskStatus.Success(resultUrl))
                        } else {
                            emit(AudioTaskStatus.Error("No result URL found"))
                        }
                        return@flow
                    }
                    "FAILED" -> {
                        emit(AudioTaskStatus.Error(queryResponse.errorMessage ?: "Task failed"))
                        return@flow
                    }
                    else -> {
                        delay(1000)
                        attempts++
                    }
                }
            }

            emit(AudioTaskStatus.Error("Task timed out"))
        } catch (e: CancellationException) {
            // Flow 收集方主动取消时必须继续向外传播取消信号，避免页面离开后被误写成任务失败状态。
            throw e
        } catch (e: Exception) {
            emit(AudioTaskStatus.Error(mapAudioErrorMessage(e)))
        }
    }

    /**
     * 查询已有音频任务状态。
     *
     * 该方法只做 DTO 到 Domain 结果的显式映射；调用方通过 [Result] 处理网络或解析异常。
     *
     * @param taskId 远端任务 ID。
     * @return 成功时返回任务状态和输出列表，失败时保留底层异常供调用方处理。
     */
    override suspend fun queryTask(taskId: String): Result<AudioTaskResult> = runCatching {
        val response = audioApi.queryTask(taskId)
        AudioTaskResult(
            taskId = response.taskId,
            status = response.status,
            errorCode = response.errorCode,
            errorMessage = response.errorMessage,
            results = response.results?.map {
                AudioResult(url = it.url, outputType = it.outputType, text = it.text)
            }
        )
    }

    /**
     * 将音频任务链路中的底层异常转换为仓库当前暴露的稳定错误信息。
     *
     * 当前 Repository API 仍以 [AudioTaskStatus.Error] 字符串承载失败状态，因此这里先复用
     * [NetworkErrorMapper] 消除各仓库的重复网络字符串判断；后续迁移到类型化错误后，Presentation
     * 层应接管最终文案映射。
     */
    private fun mapAudioErrorMessage(error: Throwable): String =
        if (NetworkErrorMapper.isNetworkError(error)) {
            "Network error occurred"
        } else {
            error.message ?: "Unknown error occurred"
        }
}
