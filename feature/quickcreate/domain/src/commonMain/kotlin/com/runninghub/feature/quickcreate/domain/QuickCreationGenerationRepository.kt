package com.runninghub.feature.quickcreate.domain

import kotlinx.coroutines.flow.Flow

/**
 * 快捷创作任务提交的领域仓库边界。
 *
 * 本接口只负责把已构造好的图片或视频生成请求提交到远端，并返回任务状态流。
 * 计费预览、媒体上传、模型目录、历史、项目和灵感模板属于其他业务边界，调用方不应为了提交任务
 * 继续依赖完整创作能力。
 */
interface QuickCreationGenerationRepository {

    /**
     * 提交图片快捷创作任务并观察远端状态。
     *
     * @param request 已完成字段校验、素材上传和计费拦截后的图片生成请求。
     * 请求中不得包含本地 URI；所有媒体字段都应在提交前转换为远端 URL。
     * @return 任务状态流；通常依次发出提交中、排队、运行和成功/失败状态。
     * 网络错误、认证失败或服务端业务错误会映射为 [QuickCreateTaskStatus.Error]。
     */
    fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus>

    /**
     * 提交视频快捷创作任务并观察远端状态。
     *
     * @param request 已完成字段校验、素材上传和计费拦截后的视频生成请求。
     * 请求中首帧、尾帧、参考视频和动态字段素材都必须是远端 URL 或空值。
     * @return 任务状态流；调用方负责在页面生命周期结束时停止收集，避免继续回写失效状态。
     */
    fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus>
}
