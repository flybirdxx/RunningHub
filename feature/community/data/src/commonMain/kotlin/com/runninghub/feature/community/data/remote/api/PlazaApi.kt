package com.runninghub.feature.community.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.feature.community.data.remote.dto.CommunityBaseResponseDto
import com.runninghub.feature.community.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.feature.community.data.remote.dto.PlazaCreationPageDto
import com.runninghub.feature.community.data.remote.dto.PlazaShortCategoryDto
import com.runninghub.feature.community.data.remote.dto.PlazaShortListRequestDto
import com.runninghub.feature.community.data.remote.dto.PlazaShortPageDto
import com.runninghub.feature.community.data.remote.dto.PlazaTagDto
import com.runninghub.feature.community.data.remote.dto.PlazaTagTreeRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Community Data 模块的 Plaza 远端 API。
 *
 * 本类只保留 Plaza 相关 endpoint 和请求体组装，业务错误处理、分页降级和 DTO 映射由 Repository 完成。
 * API 路径统一通过 [RunningHubApiEnvironment] 拼接，避免 Domain 或 Presentation 了解远端地址结构。
 */
class PlazaApi(private val client: HttpClient) {
    /**
     * 拉取 Plaza 创作标签树。
     *
     * 标签树属于 `/api/portal` 分组，服务端返回树形结构，Repository 会压平成 Presentation 当前需要的列表。
     */
    suspend fun getCreationTags(): CommunityBaseResponseDto<List<PlazaTagDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/tag/tree")) {
            contentType(ContentType.Application.Json)
            setBody(PlazaTagTreeRequestDto())
        }.body()

    /**
     * 按分页和标签条件拉取 Plaza 创作列表。
     *
     * 排序、标签和分页参数由请求 DTO 承载；本函数不做缓存或去重，避免 API 层持有页面状态。
     */
    suspend fun listCreations(request: PlazaCreationListRequestDto): CommunityBaseResponseDto<PlazaCreationPageDto> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/creation/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 拉取 Plaza 短片分类。
     *
     * 短片分类接口挂在 Web 根路径的 `canvas/community` 分组，因此通过 webUrl 生成完整路径。
     */
    suspend fun listShortCategories(): CommunityBaseResponseDto<List<PlazaShortCategoryDto>> =
        client.post(RunningHubApiEnvironment.webUrl("canvas/community/category/list")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 按分类和分页拉取 Plaza 短片列表。
     *
     * 返回结果可能使用 `records` 或 `list` 字段，兼容逻辑在 DTO/mapper 层统一处理。
     */
    suspend fun listShorts(request: PlazaShortListRequestDto): CommunityBaseResponseDto<PlazaShortPageDto> =
        client.post(RunningHubApiEnvironment.webUrl("canvas/community/composition/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
