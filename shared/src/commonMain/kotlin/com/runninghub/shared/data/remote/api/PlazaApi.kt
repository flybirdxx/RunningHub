package com.runninghub.shared.data.remote.api

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

class PlazaApi(private val client: HttpClient) {
    companion object {
        private const val BASE_URL = "https://www.runninghub.cn"
    }

    suspend fun getCreationTags(): BaseResponseDto<List<PlazaTagDto>> =
        client.post("$BASE_URL/api/portal/tag/tree") {
            contentType(ContentType.Application.Json)
            setBody(PlazaTagTreeRequestDto())
        }.body()

    suspend fun listCreations(request: PlazaCreationListRequestDto): BaseResponseDto<PlazaCreationPageDto> =
        client.post("$BASE_URL/api/portal/creation/list") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun listShortCategories(): BaseResponseDto<List<PlazaShortCategoryDto>> =
        client.post("$BASE_URL/canvas/community/category/list") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun listShorts(request: PlazaShortListRequestDto): BaseResponseDto<PlazaShortPageDto> =
        client.post("$BASE_URL/canvas/community/composition/list") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
