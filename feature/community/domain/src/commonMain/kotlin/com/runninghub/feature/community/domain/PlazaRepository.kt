package com.runninghub.feature.community.domain

/**
 * Plaza 社区内容仓库。
 *
 * 该接口属于 Community Feature Domain，封装广场创作内容、标签和短片列表查询。
 * Presentation 只依赖该领域契约；远端接口路径、DTO 和错误码映射由 Data 层处理。
 */
interface PlazaRepository {
    /**
     * 读取 Plaza 内容标签。
     *
     * @return 成功时返回可筛选标签列表；失败时保留仓库错误，调用方可展示降级内容。
     */
    suspend fun getTags(): Result<List<PlazaTag>>

    /**
     * 分页读取 Plaza 创作内容。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页条数。
     * @param sort 排序值，例如 RECOMMEND、HOT 或 LATEST。
     * @param tags 选中的标签 ID 列表。
     * @return 成功时返回创作内容分页。
     */
    suspend fun listCreations(
        page: Int = 1,
        size: Int = 30,
        sort: String = "RECOMMEND",
        tags: List<String> = emptyList(),
    ): Result<PlazaCreationPage>

    /**
     * 读取 Plaza 短片分类。
     *
     * @return 成功时返回短片分类列表。
     */
    suspend fun listShortCategories(): Result<List<PlazaShortCategory>>

    /**
     * 分页读取 Plaza 短片。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页条数。
     * @param categoryCode 分类代码；为空表示全部分类。
     * @return 成功时返回短片卡片列表。
     */
    suspend fun listShorts(
        page: Int = 1,
        size: Int = 30,
        categoryCode: String? = null,
    ): Result<List<PlazaShortCard>>
}
