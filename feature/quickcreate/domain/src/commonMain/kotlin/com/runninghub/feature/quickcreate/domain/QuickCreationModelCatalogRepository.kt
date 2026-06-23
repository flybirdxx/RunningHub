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

    /**
     * 判断本地是否已有该类别的模型目录缓存。
     *
     * Presentation 只用它决定是否需要在展示缓存后后台刷新；具体缓存介质由 Data 层隐藏。
     * 默认返回 `false`，使测试替身和未实现缓存的数据源保持单阶段加载行为。
     *
     * @param kind 图片或视频服务类别。
     * @return `true` 表示 [getModels] 有机会直接返回本地缓存；`false` 表示当前没有可用缓存。
     */
    suspend fun hasCachedModels(kind: QuickCreationServiceKind): Boolean = false

    /**
     * 通过接口刷新快捷创作服务模型目录。
     *
     * 默认实现沿用 [getModels]，具体 Data 实现可以改为强制同步远端并更新本地缓存。
     * 调用方通常先调用 [getModels] 读取本地可展示目录，再在后台调用本方法刷新最新参数。
     *
     * @param kind 图片或视频服务类别。
     * @return 该类别下最新可用服务模型；失败时调用方应保留旧缓存展示。
     */
    suspend fun refreshModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
        getModels(kind)
}
