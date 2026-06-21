package com.runninghub.feature.community.data.repository

import com.runninghub.feature.community.data.remote.api.PlazaApi
import com.runninghub.feature.community.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.feature.community.data.remote.dto.PlazaShortListRequestDto
import com.runninghub.feature.community.data.remote.dto.flatten
import com.runninghub.feature.community.data.remote.dto.toDomain
import com.runninghub.feature.community.domain.PlazaCreationPage
import com.runninghub.feature.community.domain.PlazaRepository
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag

/**
 * Plaza Repository 的 Community Data 实现。
 *
 * 本实现封装广场创作、标签和短片接口，并把 DTO 映射为 Community Domain 模型。
 * 它位于 `feature:community:data`，不再依赖 shared；Presentation 只通过 [PlazaRepository] 访问。
 */
class PlazaRepositoryImpl(
    private val api: PlazaApi,
) : PlazaRepository {
    /**
     * 读取并压平 Plaza 标签树。
     *
     * 服务端返回树形标签，当前页面按平铺标签展示，因此 Data 层统一 flatten，避免 UI 依赖远端树结构。
     */
    override suspend fun getTags(): Result<List<PlazaTag>> =
        runCatching {
            val response = api.getCreationTags()
            check(response.code == 0) { response.msg.ifEmpty { "Plaza tags load failed" } }
            response.data.orEmpty().flatMap { it.flatten() }
        }

    /**
     * 分页读取 Plaza 创作内容。
     *
     * 服务端空数据会降级为空分页，保留请求页码，方便 ScreenModel 维持分页状态并停止继续加载。
     */
    override suspend fun listCreations(
        page: Int,
        size: Int,
        sort: String,
        tags: List<String>,
    ): Result<PlazaCreationPage> =
        runCatching {
            val response = api.listCreations(
                PlazaCreationListRequestDto(current = page, size = size, sort = sort, tags = tags)
            )
            check(response.code == 0) { response.msg.ifEmpty { "Plaza creations load failed" } }
            response.data?.toDomain() ?: PlazaCreationPage(page = page, total = 0, items = emptyList())
        }

    /**
     * 读取 Plaza 短片分类。
     *
     * 分类 code 缺失时由 mapper 降级为 id，确保旧接口数据仍能驱动后续短片分页请求。
     */
    override suspend fun listShortCategories(): Result<List<PlazaShortCategory>> =
        runCatching {
            val response = api.listShortCategories()
            check(response.code == 0) { response.msg.ifEmpty { "Short categories load failed" } }
            response.data.orEmpty().map { it.toDomain() }
        }

    /**
     * 分页读取 Plaza 短片列表。
     *
     * `categoryCode` 为 null 表示全部分类；短片分页结果当前由 ScreenModel 负责追加和去重。
     */
    override suspend fun listShorts(page: Int, size: Int, categoryCode: String?): Result<List<PlazaShortCard>> =
        runCatching {
            val response = api.listShorts(PlazaShortListRequestDto(page = page, size = size, categoryCode = categoryCode))
            check(response.code == 0) { response.msg.ifEmpty { "Short list load failed" } }
            response.data?.items.orEmpty().map { it.toDomain() }
        }
}
