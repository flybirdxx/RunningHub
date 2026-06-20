package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.BaseResponseDto
import com.runninghub.shared.data.remote.dto.LlmModelDto
import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuDetailRequestDto
import com.runninghub.shared.data.remote.dto.SkuListPageDto
import com.runninghub.shared.data.remote.dto.SkuListRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ModelCatalogApi(private val client: HttpClient) {
    companion object {
        private const val BASE_URL = "https://www.runninghub.cn"
    }

    suspend fun listStandardModels(request: SkuListRequestDto): BaseResponseDto<SkuListPageDto> =
        client.post("$BASE_URL/api/sku/list") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getStandardModelDetail(id: String): BaseResponseDto<SkuDetailDto> =
        client.post("$BASE_URL/api/sku/detail") {
            contentType(ContentType.Application.Json)
            setBody(SkuDetailRequestDto(id))
        }.body()

    suspend fun listLlmModels(): BaseResponseDto<List<LlmModelDto>> =
        client.get("$BASE_URL/llm/api/models").body()
}
