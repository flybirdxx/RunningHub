package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作灵感模板的领域仓库边界。
 *
 * 本接口只暴露灵感标签、模板分页和模板详情能力，供灵感区域加载模板并把模板应用到编辑状态。
 * 历史、项目、生成、计费、上传和模型目录均由其他窄接口承担，避免灵感页面状态继续依赖完整创作能力。
 */
interface QuickCreationInspirationRepository {

    /**
     * 读取灵感模板标签。
     *
     * @return 当前可用于筛选模板的标签列表；空列表表示服务端没有标签或请求失败后调用方选择降级展示。
     * 失败原因通过 [Result.failure] 返回，由 Presentation 决定是否展示错误。
     */
    suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>>

    /**
     * 分页读取灵感模板列表。
     *
     * @param page 页码，从 1 开始；小于 1 的值应由调用方避免传入。
     * @param size 每页条数，单位为条；默认值保持与快捷创作灵感区域一致。
     * @param tagId 可选标签 ID；`null` 表示读取全部模板，不按标签筛选。
     * @return 模板分页结果；列表顺序保持服务端返回顺序，客户端不重新排序。
     */
    suspend fun getInspirationTemplates(
        page: Int = 1,
        size: Int = 20,
        tagId: String? = null,
    ): Result<QuickCreateInspirationTemplatePage>

    /**
     * 读取单个灵感模板详情。
     *
     * @param templateId 模板稳定标识，来源于模板列表；空字符串不应提交到仓库。
     * @return 模板详情，包含可回填到图片或视频编辑状态的 Prompt、参数和素材 URL。
     */
    suspend fun getInspirationTemplateDetail(templateId: String): Result<QuickCreateInspirationTemplateDetail>
}
