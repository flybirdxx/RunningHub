package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.ModelCatalogApi
import com.runninghub.shared.data.remote.dto.LlmModelDto
import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuListRequestDto
import com.runninghub.shared.data.remote.dto.SkuSummaryDto
import com.runninghub.shared.domain.model.ApiModelDetail
import com.runninghub.shared.domain.model.ApiModelSummary
import com.runninghub.shared.domain.model.LlmModelSummary
import com.runninghub.shared.domain.repository.ModelCatalogRepository
import kotlinx.serialization.json.Json

/**
 * 标准模型目录仓库的数据层实现。
 *
 * 本类负责调用 SKU/LLM 目录 API，并把 DTO 映射为不含 endpoint 的领域模型。
 * SKU endpoint 会登记到 [ModelEndpointRegistry]，供模型调用仓库在提交任务时按 modelId 解析，
 * 避免远端路由继续泄漏到 Domain 或 Presentation。
 *
 * @param api 标准模型目录 API。
 * @param json 解析服务端 inputConfig JSON 的序列化配置。
 * @param endpointRegistry 标准模型 endpoint 的 Data 层缓存，需与调用仓库共享同一个实例。
 */
class ModelCatalogRepositoryImpl(
    private val api: ModelCatalogApi,
    json: Json,
    private val endpointRegistry: ModelEndpointRegistry = ModelEndpointRegistry(),
) : ModelCatalogRepository {
    private val fieldMapper = ApiModelFieldMapper(json)

    override suspend fun listStandardModels(search: String, page: Int, size: Int): Result<List<ApiModelSummary>> =
        runCatching {
            val response = api.listStandardModels(
                SkuListRequestDto(search = search, pageNum = page, pageSize = size)
            )
            if (response.code != 0) {
                error(response.msg.ifEmpty { "Model list load failed: code=${response.code}" })
            }
            response.data?.items.orEmpty().map { dto ->
                endpointRegistry.register(dto.id, dto.rhEndpoint)
                dto.toDomain()
            }
        }

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> =
        runCatching {
            val response = api.getStandardModelDetail(modelId)
            check(response.code == 0) { response.msg.ifEmpty { "Model detail load failed" } }
            val detail = checkNotNull(response.data) { "Model detail missing" }
            endpointRegistry.register(detail.id, detail.rhEndpoint)
            detail.toDomain(fieldMapper)
        }

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        runCatching {
            val response = api.listLlmModels()
            check(response.code == 0) { response.msg.ifEmpty { "LLM model list load failed" } }
            response.data.orEmpty().map { it.toDomain() }
        }
}

/**
 * 把 SKU 列表 DTO 映射为领域摘要。
 *
 * endpoint 不进入返回值；调用方需要提交任务时只能使用 id，由 Data 层再解析实际路由。
 */
fun SkuSummaryDto.toDomain(): ApiModelSummary =
    ApiModelSummary(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        priceSummary = priceSummary ?: price,
    )

/**
 * 把 SKU 详情 DTO 映射为领域详情。
 *
 * inputConfigJson 只在 Data 层解析为结构化字段，原始 JSON 不再暴露给 Domain，避免上层依赖服务端表单格式。
 */
fun SkuDetailDto.toDomain(fieldMapper: ApiModelFieldMapper): ApiModelDetail =
    ApiModelDetail(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        priceSummary = priceSummary ?: price,
        queueSize = queueSize,
        concurrencyLimit = concurrencyLimit,
        fields = fieldMapper.parse(inputConfigJson),
    )

/**
 * 把 LLM 目录 DTO 映射为领域摘要。
 */
fun LlmModelDto.toDomain(): LlmModelSummary =
    LlmModelSummary(
        modelKey = modelKey,
        provider = provider,
        version = version,
        contextLength = contextLength,
        capabilities = capabilities,
        inputPrice = inputPrice,
        outputPrice = outputPrice,
    )



