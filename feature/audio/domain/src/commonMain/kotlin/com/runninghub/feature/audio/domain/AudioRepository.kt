package com.runninghub.feature.audio.domain

import kotlinx.coroutines.flow.Flow

/**
 * 音频生成领域仓库契约。
 *
 * 接口位于 Audio Domain，隔离 Presentation 与具体 MiniMax/RunningHub 远端 API。
 * 具体实现由 `feature:audio:data` 通过 Koin 绑定，调用方只依赖本领域契约即可。
 */
interface AudioRepository {
    /**
     * 提交文本转音频任务并持续发出任务状态。
     *
     * @param request 本次音频生成参数。调用方应在提交前完成必填、范围和权限校验。
     * @return 冷 Flow；开始收集后才提交远端任务。收集方取消时，Data 层必须停止后续轮询。
     */
    fun convertTextToAudio(request: AudioRequest): Flow<AudioTaskStatus>

    /**
     * 查询已有音频任务的远端状态和结果。
     *
     * @param taskId 远端任务 ID，必须来自提交响应或历史记录；空字符串应由调用方拦截。
     * @return 成功时返回任务领域结果；网络、解析或业务异常通过 [Result.failure] 暴露。
     */
    suspend fun queryTask(taskId: String): Result<AudioTaskResult>
}
