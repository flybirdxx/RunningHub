package com.runninghub.feature.discovery.data.repository

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.PageData
import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.data.remote.api.WebAppCatalogApi
import com.runninghub.feature.discovery.data.remote.dto.CatalogTagTreeRequestDto
import com.runninghub.feature.discovery.data.remote.dto.CustomMadeWebappRequestDto
import com.runninghub.feature.discovery.data.remote.dto.DiscoveryBaseResponseDto
import com.runninghub.feature.discovery.data.remote.dto.WebAppListRequestDto
import com.runninghub.feature.discovery.data.remote.dto.toDomain
import com.runninghub.feature.discovery.domain.CatalogError
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.domain.CatalogTagRange
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository

/**
 * WebApp 目录仓库的 Discovery Data 实现。
 *
 * 本实现承接公开目录、精选、定制列表、用户发布列表、标签树、公开详情和搜索能力。
 * 它位于 `feature:discovery:data`，通过 Discovery Domain 的 [WebAppCatalogRepository] 暴露，
 * 避免 composeApp 或 Presentation 继续依赖 shared 中的万能 WebApp 数据实现。
 *
 * @param api WebApp 目录远端 API。
 */
class WebAppCatalogRepositoryImpl(
    private val api: WebAppCatalogApi,
) : WebAppCatalogRepository {
    /**
     * 分页读取 WebApp 目录。
     *
     * 排序和标签范围在 Data 层转换为服务端协议值，Domain 保持平台无关的枚举语义。
     */
    override suspend fun getAppList(
        query: CatalogQuery,
    ): Result<PageData<WebApp>> = catalogResult("getAppList") {
        val response = api.getWebAppList(
            WebAppListRequestDto(
                pageNum = query.pageNum,
                pageSize = query.pageSize,
                tags = query.tagIds,
                keyword = query.keyword,
                sort = query.sort?.toApiValue(),
                days = query.sort?.heatWindowDays,
            )
        )
        response.requireCatalogData("getAppList").toDomain()
    }

    /**
     * 读取运营精选 WebApp。
     *
     * 返回顺序由运营配置决定，客户端只做 DTO 到 Domain 的映射，不重新排序。
     */
    override suspend fun getCarefullyChosenList(): Result<List<WebApp>> =
        catalogResult("getCarefullyChosenList") {
            val response = api.getCarefullyChosenList()
            response.requireCatalogData("getCarefullyChosenList").map { it.toDomain() }
        }

    /**
     * 根据定制标签读取 WebApp 列表。
     *
     * 空标签列表表示使用服务端默认定制推荐，具体空态展示由 Presentation 决定。
     */
    override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> =
        catalogResult("getCustomMadeWebappList") {
            val response = api.getCustomMadeWebappList(CustomMadeWebappRequestDto(tags))
            response.requireCatalogData("getCustomMadeWebappList").map { it.toDomain() }
        }

    /**
     * 读取用户主页发布的 WebApp。
     *
     * 参数只包含 userId 和分页，避免把认证凭据混入公开目录接口。
     */
    override suspend fun getUserAppList(
        userId: String,
        pageNum: Int,
        pageSize: Int,
    ): Result<PageData<WebApp>> = catalogResult("getUserAppList") {
        val params = mapOf(
            "userId" to userId,
            "pageNum" to pageNum.toString(),
            "pageSize" to pageSize.toString(),
        )
        val response = api.getWebAppUserList(params)
        response.requireCatalogData("getUserAppList").toDomain()
    }

    /**
     * 读取目录标签树。
     *
     * 标签范围由 Domain 枚举转换为服务端 `rang` 字段，mapper 会限制递归深度。
     */
    override suspend fun getTagTree(range: CatalogTagRange): Result<List<Tag>> =
        catalogResult("getTagTree") {
            val response = api.getTagTree(CatalogTagTreeRequestDto(range.toApiValue()))
            response.requireCatalogData("getTagTree").map { it.toDomain() }
        }

    /**
     * 读取公开 WebApp 详情。
     *
     * 该方法不读取 API Key；运行示例和任务提交仍由 Task Feature 的仓库负责。
     */
    override suspend fun getAppDetail(appId: String): Result<AppDetail> =
        catalogResult("getAppDetail") {
            val response = api.getWebAppDetail(mapOf("webappId" to appId))
            response.requireCatalogData("getAppDetail").toDomain()
        }

    /**
     * 按关键词搜索 WebApp。
     *
     * 搜索复用目录列表接口，保证错误映射和分页模型与首页目录一致。
     */
    override suspend fun searchApps(
        keyword: String,
        pageNum: Int,
        pageSize: Int,
    ): Result<PageData<WebApp>> = catalogResult("searchApps") {
        val response = api.getWebAppList(
            WebAppListRequestDto(
                pageNum = pageNum,
                pageSize = pageSize,
                keyword = keyword,
            )
        )
        response.requireCatalogData("searchApps").toDomain()
    }

    private inline fun <T> catalogResult(
        operation: String,
        block: () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (error: CatalogError) {
        Result.failure(error)
    } catch (throwable: Throwable) {
        Result.failure(CatalogError.Unexpected(operation = operation, original = throwable))
    }

    private fun <T> DiscoveryBaseResponseDto<T>.requireCatalogData(operation: String): T {
        // 目录接口统一在 Data 层把服务端业务码和空响应转换为 Domain 错误，
        // Presentation 只按稳定错误类型展示文案，不直接依赖服务端 msg。
        if (code != 0) {
            throw CatalogError.Remote(code = code, serverMessage = msg)
        }
        return data ?: throw CatalogError.EmptyResponse(operation)
    }

    private fun CatalogSort.toApiValue(): String = when (this) {
        CatalogSort.RECOMMEND -> "RECOMMEND"
        CatalogSort.REPUTATION -> "REPUTATION"
        CatalogSort.HOTTEST -> "HOTTEST"
        CatalogSort.NEWEST -> "NEWEST"
    }

    private fun CatalogTagRange.toApiValue(): String = when (this) {
        CatalogTagRange.WEB_APP -> "WEBAPP"
    }
}
