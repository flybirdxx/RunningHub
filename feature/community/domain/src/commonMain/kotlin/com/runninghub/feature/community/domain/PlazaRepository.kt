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
     * @return 成功时返回短片分页。
     */
    suspend fun listShorts(
        page: Int = 1,
        size: Int = 30,
        categoryCode: String? = null,
    ): Result<PlazaShortPage>
}

/**
 * Plaza 仓库的结构化失败异常。
 *
 * Data 层用该异常表达广场标签、创作列表和短片接口的稳定失败语义；[message] 只保留诊断码，
 * Presentation 不得把它作为最终用户可见文案展示。
 *
 * @property issue Plaza 仓库的稳定失败语义。
 * @property remoteCode 服务端业务 code；`null` 表示失败来自响应缺字段或本地结构校验。
 */
class PlazaRepositoryException(
    val issue: PlazaRepositoryIssue,
    val remoteCode: Int? = null,
) : IllegalStateException(issue.diagnosticMessage(remoteCode))

/**
 * Plaza 仓库使用的稳定错误语义。
 *
 * 这些枚举值只用于上层按类型判断错误和日志分类，不携带服务端 `msg`、底层异常 message
 * 或最终中文 UI 文案。
 *
 * @property code 稳定诊断码，可用于测试断言和日志分类；不得作为最终 UI 文案。
 */
enum class PlazaRepositoryIssue(val code: String) {
    /** Plaza 标签树接口返回非成功业务 code。 */
    TagsLoadFailed("PLAZA_TAGS_LOAD_FAILED"),

    /** Plaza 创作分页接口返回非成功业务 code。 */
    CreationsLoadFailed("PLAZA_CREATIONS_LOAD_FAILED"),

    /** Plaza 短片分类接口返回非成功业务 code。 */
    ShortCategoriesLoadFailed("PLAZA_SHORT_CATEGORIES_LOAD_FAILED"),

    /** Plaza 短片分页接口返回非成功业务 code。 */
    ShortListLoadFailed("PLAZA_SHORT_LIST_LOAD_FAILED"),
}

private fun PlazaRepositoryIssue.diagnosticMessage(remoteCode: Int?): String =
    if (remoteCode == null) code else "$code:$remoteCode"
