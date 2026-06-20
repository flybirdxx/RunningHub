package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作服务模型目录的领域仓库边界。
 *
 * 本接口只暴露图片/视频快捷创作模型目录读取能力，供 Presentation 层的目录加载和模型选择流程使用。
 * 上传、计费、生成、历史、项目和灵感模板属于其他业务能力，不应因为加载模型目录而被一并暴露给调用方。
 * 当前实现仍复用既有 QuickCreate 数据实现承载，后续可以独立迁移到 quickcreate data 的目录子边界。
 */
interface QuickCreationModelCatalogRepository {

    /**
     * 按业务类别读取快捷创作服务模型目录。
     *
     * @param kind 图片或视频服务类别；调用方不需要知道远端 categoryId 或接口路径。
     * @return 该类别下可用的服务模型；网络错误、认证失败或响应异常通过 [Result.failure] 返回。
     */
    suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>>
}
