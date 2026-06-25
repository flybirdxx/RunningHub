package com.runninghub.feature.model.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.network.auth.markRunningHubAuthRetryAllowed
import com.runninghub.feature.model.data.remote.dto.BaseResponseDto
import com.runninghub.feature.model.data.remote.dto.LlmModelDto
import com.runninghub.feature.model.data.remote.dto.SkuDetailDto
import com.runninghub.feature.model.data.remote.dto.SkuDetailRequestDto
import com.runninghub.feature.model.data.remote.dto.SkuListPageDto
import com.runninghub.feature.model.data.remote.dto.SkuListRequestDto
import com.runninghub.feature.model.data.remote.dto.SkuTagDto
import com.runninghub.feature.model.data.remote.dto.SkuTagQueryRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * 标准模型目录接口的 Data 层访问入口。
 *
 * 本类只负责访问 SKU 与 LLM 模型目录端点，Repository 负责把 DTO 转换为业务模型，
 * 从而避免 Domain 层直接感知 RunningHub 的具体 URL 结构。
 *
 * @param client 已安装 RunningHub 默认配置和认证拦截器的 Ktor 客户端。
 */
class ModelCatalogApi(private val client: HttpClient) {
    /**
     * 拉取标准模型页顶部的模型分组标签。
     *
     * 页面使用 `parentId=4` 和一级 `levels=[1]` 区分 Seedance、Suno 等模型分组；
     * 这些分组后续会作为 `categoryTagIds` 查询条件，不与单个模型的能力类型混用。
     */
    suspend fun listStandardModelGroups(search: String = ""): BaseResponseDto<List<SkuTagDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("sku/tag/query")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(
                SkuTagQueryRequestDto(
                    parentId = STANDARD_MODEL_GROUP_PARENT_ID,
                    levels = listOf(STANDARD_MODEL_GROUP_LEVEL),
                    showType = STANDARD_MODEL_GROUP_SHOW_TYPE,
                    search = search,
                )
            )
        }.body()

    /**
     * 拉取标准模型 SKU 分页列表。
     *
     * SKU 目录属于 RunningHub `/api/` 业务接口，统一经 [RunningHubApiEnvironment.apiUrl] 生成地址；
     * 这样后续切换环境或拆分 core/network 时只需要调整网络环境定义。
     */
    suspend fun listStandardModels(request: SkuListRequestDto): BaseResponseDto<SkuListPageDto> =
        client.post(RunningHubApiEnvironment.apiUrl("sku/list")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 拉取单个标准模型 SKU 详情。
     *
     * 详情请求只暴露 SKU id，接口路径仍由 core/network 负责拼接，避免 Repository 或 Domain
     * 复制 RunningHub 的 `/api/sku/detail` 目录结构。
     */
    suspend fun getStandardModelDetail(id: String): BaseResponseDto<SkuDetailDto> =
        client.post(RunningHubApiEnvironment.apiUrl("sku/detail")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(SkuDetailRequestDto(id))
        }.body()

    /**
     * 拉取 LLM 模型目录。
     *
     * 该接口当前位于 Web 根路径下的 `llm/api` 分组，不属于通用 `/api/` 前缀，
     * 因此使用 [RunningHubApiEnvironment.webUrl] 保留真实服务端分组。
     */
    suspend fun listLlmModels(): BaseResponseDto<List<LlmModelDto>> =
        client.get(RunningHubApiEnvironment.webUrl("llm/api/models")).body()
}

private const val STANDARD_MODEL_GROUP_PARENT_ID = 4
private const val STANDARD_MODEL_GROUP_LEVEL = 1
private const val STANDARD_MODEL_GROUP_SHOW_TYPE = "tree"
