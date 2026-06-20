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

class ModelCatalogRepositoryImpl(
    private val api: ModelCatalogApi,
    json: Json,
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
            response.data?.items.orEmpty().map { it.toDomain() }
        }

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> =
        runCatching {
            val response = api.getStandardModelDetail(modelId)
            check(response.code == 0) { response.msg.ifEmpty { "Model detail load failed" } }
            checkNotNull(response.data) { "Model detail missing" }.toDomain(fieldMapper)
        }

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        runCatching {
            val response = api.listLlmModels()
            check(response.code == 0) { response.msg.ifEmpty { "LLM model list load failed" } }
            response.data.orEmpty().map { it.toDomain() }
        }
}

fun SkuSummaryDto.toDomain(): ApiModelSummary =
    ApiModelSummary(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        endpoint = rhEndpoint,
        priceSummary = priceSummary ?: price,
    )

fun SkuDetailDto.toDomain(fieldMapper: ApiModelFieldMapper): ApiModelDetail =
    ApiModelDetail(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        endpoint = rhEndpoint,
        priceSummary = priceSummary ?: price,
        queueSize = queueSize,
        concurrencyLimit = concurrencyLimit,
        fields = fieldMapper.parse(inputConfigJson),
        rawInputConfigJson = inputConfigJson,
    )

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



