package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作计费预览的领域仓库边界。
 *
 * 本接口只负责根据已构建好的图片或视频生成请求获取远端价格预览。它不暴露生成、上传、历史或项目能力，
 * 使 Presentation 层的计费 Interactor 可以按最小权限依赖远端能力。余额不足属于有效业务结果，
 * 网络、认证或响应格式异常通过 [Result.failure] 返回给调用方统一映射。
 */
interface QuickCreationFeePreviewRepository {

    /**
     * 获取图片快捷创作的价格预览。
     *
     * @param request 已完成字段校验和媒体上传映射的图片生成请求。
     * @return 计费成功时返回价格与余额判断；失败时返回远端或网络异常。
     */
    suspend fun previewImageQuickCreationFee(request: ImageGenerationRequest): Result<QuickCreationFeePreview>

    /**
     * 获取视频快捷创作的价格预览。
     *
     * @param request 已完成字段校验和媒体上传映射的视频生成请求。
     * @return 计费成功时返回价格与余额判断；失败时返回远端或网络异常。
     */
    suspend fun previewVideoQuickCreationFee(request: VideoGenerationRequest): Result<QuickCreationFeePreview>
}
