package com.runninghub.shared.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.shared.data.remote.dto.BaseResponseDto
import com.runninghub.shared.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.shared.data.remote.dto.PlazaCreationPageDto
import com.runninghub.shared.data.remote.dto.PlazaShortCategoryDto
import com.runninghub.shared.data.remote.dto.PlazaShortListRequestDto
import com.runninghub.shared.data.remote.dto.PlazaShortPageDto
import com.runninghub.shared.data.remote.dto.PlazaTagDto
import com.runninghub.shared.data.remote.dto.PlazaTagTreeRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 广场与社区内容接口的 Data 层访问入口。
 *
 * 本类保留远端标签、创作列表和短内容列表的路径拼接，Repository 负责统一转换为领域模型，
 * 避免 Presentation 直接了解服务端接口分组。
 */
class PlazaApi(private val client: HttpClient) {
    /**
     * 拉取广场创作标签树。
     *
     * 标签树属于 `/api/portal` 业务接口，统一通过 [RunningHubApiEnvironment.apiUrl] 拼接，
     * 保证广场数据源不再各自维护 Web origin。
     */
    suspend fun getCreationTags(): BaseResponseDto<List<PlazaTagDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/tag/tree")) {
            contentType(ContentType.Application.Json)
            setBody(PlazaTagTreeRequestDto())
        }.body()

    /**
     * 按分页和标签条件拉取广场创作列表。
     *
     * 服务端列表排序和游标语义由请求 DTO 承载；本 API 只负责命中 `/api/portal` endpoint，
     * 分页合并、去重和领域模型转换由 Repository 处理。
     */
    suspend fun listCreations(request: PlazaCreationListRequestDto): BaseResponseDto<PlazaCreationPageDto> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/creation/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 拉取社区短内容分类。
     *
     * 社区接口当前挂在 Web 根路径下的 `canvas/community` 分组，不使用 `/api/` 前缀，
     * 因此通过 [RunningHubApiEnvironment.webUrl] 表达真实服务端归属。
     */
    suspend fun listShortCategories(): BaseResponseDto<List<PlazaShortCategoryDto>> =
        client.post(RunningHubApiEnvironment.webUrl("canvas/community/category/list")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 按分类和分页拉取社区短内容列表。
     *
     * 该接口与短内容分类同属 `canvas/community` 分组，仍通过 Web 根路径拼接；
     * Repository 负责把服务端列表字段兼容为稳定的领域分页结果。
     */
    suspend fun listShorts(request: PlazaShortListRequestDto): BaseResponseDto<PlazaShortPageDto> =
        client.post(RunningHubApiEnvironment.webUrl("canvas/community/composition/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
