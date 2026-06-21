package com.runninghub.feature.discovery.domain

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.PageData
import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp

/**
 * WebApp 目录和搜索仓库。
 *
 * 该接口位于 discovery feature 的 Domain 层，只覆盖不需要提交任务、上传文件或轮询输出的
 * 公开目录能力，用于 Discovery、Search、详情页公开信息和创作者主页作品列表等调用点。
 * 任务执行、媒体上传和历史查询继续留在更窄仓库，避免 Presentation 重新依赖目录、任务和
 * 历史混在一起的宽接口。
 */
interface WebAppCatalogRepository {
    /**
     * 分页读取 WebApp 列表。
     *
     * @param query 稳定目录查询模型，包含分页、标签、关键词和排序语义。
     * @return 成功时返回分页目录数据；网络异常、响应码错误或响应体缺失以 [Result.failure] 返回，
     * 失败类型应优先使用 [CatalogError]，便于 Presentation 做稳定文案映射。
     */
    suspend fun getAppList(
        query: CatalogQuery,
    ): Result<PageData<WebApp>>

    /**
     * 读取运营精选 WebApp 列表。
     *
     * @return 成功时返回服务端按运营权重排序的 WebApp 列表；空列表表示当前没有精选内容。
     */
    suspend fun getCarefullyChosenList(): Result<List<WebApp>>

    /**
     * 读取定制专区 WebApp 列表。
     *
     * @param tags 标签 ID 列表；空列表表示读取默认定制列表，顺序按服务端筛选规则解释。
     * @return 成功时返回定制专区 WebApp；网络或业务错误以 [Result.failure] 返回。
     */
    suspend fun getCustomMadeWebappList(tags: List<String> = emptyList()): Result<List<WebApp>>

    /**
     * 分页读取指定用户发布的 WebApp。
     *
     * @param userId 创作者用户 ID，来自用户资料或导航参数；空字符串应由调用方提前拦截。
     * @param pageNum 页码，从 1 开始。
     * @param pageSize 每页数量，单位为条。
     * @return 成功时返回创作者作品分页；用户不存在或网络失败以 [Result.failure] 返回。
     */
    suspend fun getUserAppList(
        userId: String,
        pageNum: Int,
        pageSize: Int,
    ): Result<PageData<WebApp>>

    /**
     * 读取 WebApp 标签树。
     *
     * @param range 标签业务范围，默认读取 WebApp 标签；Data 层负责把该值映射为远端 `rang` 参数。
     * @return 成功时返回标签树；空列表表示当前没有可用标签。
     */
    suspend fun getTagTree(range: CatalogTagRange = CatalogTagRange.WEB_APP): Result<List<Tag>>

    /**
     * 读取公开 WebApp 详情。
     *
     * 该详情不需要 API Key；可运行调用示例详情属于任务执行仓库边界，避免公开详情页持有敏感凭据能力。
     *
     * @param appId WebApp ID，来源于列表项或导航参数。
     * @return 成功时返回公开详情；ID 无效、网络失败或响应体缺失以 [Result.failure] 返回。
     */
    suspend fun getAppDetail(appId: String): Result<AppDetail>

    /**
     * 按关键词分页搜索 WebApp。
     *
     * @param keyword 搜索关键词，调用方应传入去除首尾空白后的内容；空字符串不应进入仓库层。
     * @param pageNum 页码，从 1 开始。
     * @param pageSize 每页数量，单位为条。
     * @return 成功时返回搜索结果分页；无匹配内容时 records 为空但仍属于成功结果。
     */
    suspend fun searchApps(
        keyword: String,
        pageNum: Int,
        pageSize: Int,
    ): Result<PageData<WebApp>>
}
