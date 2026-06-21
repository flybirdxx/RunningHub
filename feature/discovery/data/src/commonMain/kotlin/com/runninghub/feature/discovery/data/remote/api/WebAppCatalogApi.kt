package com.runninghub.feature.discovery.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.feature.discovery.data.remote.dto.CatalogTagDto
import com.runninghub.feature.discovery.data.remote.dto.CatalogTagTreeRequestDto
import com.runninghub.feature.discovery.data.remote.dto.CustomMadeWebappRequestDto
import com.runninghub.feature.discovery.data.remote.dto.DiscoveryBaseResponseDto
import com.runninghub.feature.discovery.data.remote.dto.DiscoveryPageDataDto
import com.runninghub.feature.discovery.data.remote.dto.WebAppCatalogDto
import com.runninghub.feature.discovery.data.remote.dto.WebAppDetailCatalogDto
import com.runninghub.feature.discovery.data.remote.dto.WebAppListRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Discovery Data 模块的 WebApp 目录远端 API。
 *
 * 本类只包含公开目录、搜索、标签树、用户发布列表和详情 endpoint。
 * 需要 API Key 的任务调用示例不放在这里，避免 Discovery Data 重新承担 Task Feature 职责。
 */
class WebAppCatalogApi(private val client: HttpClient) {
    /**
     * 查询 WebApp 目录分页列表。
     *
     * 搜索、分页、标签和排序参数都由请求 DTO 承载；业务码校验留给 Repository 统一处理。
     */
    suspend fun getWebAppList(
        request: WebAppListRequestDto,
    ): DiscoveryBaseResponseDto<DiscoveryPageDataDto<WebAppCatalogDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询运营精选 WebApp 列表。
     *
     * 服务端顺序包含运营权重，Repository 映射时必须保持原始顺序。
     */
    suspend fun getCarefullyChosenList(): DiscoveryBaseResponseDto<List<WebAppCatalogDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/carefullyChosenList")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 查询定制 WebApp 列表。
     *
     * 标签参数来自目录筛选，不在 API 层解释为空列表时的展示策略。
     */
    suspend fun getCustomMadeWebappList(
        request: CustomMadeWebappRequestDto,
    ): DiscoveryBaseResponseDto<List<WebAppCatalogDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/customMadeWebappList")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询指定用户发布的 WebApp 分页列表。
     *
     * params 保留 Map 以兼容服务端字段，但调用方只应传分页和 userId，不得传入凭据。
     */
    suspend fun getWebAppUserList(
        params: Map<String, String>,
    ): DiscoveryBaseResponseDto<DiscoveryPageDataDto<WebAppCatalogDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/user/list")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /**
     * 查询目录标签树。
     *
     * 标签树被首页筛选和搜索筛选复用，Data 层保持远端树形协议，mapper 再转换为 Domain 标签树。
     */
    suspend fun getTagTree(
        request: CatalogTagTreeRequestDto = CatalogTagTreeRequestDto(),
    ): DiscoveryBaseResponseDto<List<CatalogTagDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/tag/tree")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询 WebApp 公开详情。
     *
     * 该接口不需要 API Key；需要凭据的运行示例由 Task Data 的 API 保持管理。
     */
    suspend fun getWebAppDetail(
        params: Map<String, String>,
    ): DiscoveryBaseResponseDto<WebAppDetailCatalogDto> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/detail")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()
}
