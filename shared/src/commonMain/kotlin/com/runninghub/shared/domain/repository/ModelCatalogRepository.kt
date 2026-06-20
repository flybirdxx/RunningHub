package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.ApiModelDetail
import com.runninghub.shared.domain.model.ApiModelSummary
import com.runninghub.shared.domain.model.LlmModelSummary

interface ModelCatalogRepository {
    suspend fun listStandardModels(
        search: String = "",
        page: Int = 1,
        size: Int = 30,
    ): Result<List<ApiModelSummary>>

    suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail>

    suspend fun listLlmModels(): Result<List<LlmModelSummary>>
}
