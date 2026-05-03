package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

class RunningHubApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "https://www.runninghub.cn/api/"
        const val UC_BASE_URL = "https://www.runninghub.cn/uc/"
    }

    suspend fun pwdLogin(request: PwdLoginRequest): BaseResponseDto<LoginTokenData?> =
        client.post("${UC_BASE_URL}pwdLogin") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun tokenRefresh(refreshToken: String): BaseResponseDto<LoginTokenData?> =
        client.post("${UC_BASE_URL}token/refresh") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $refreshToken")
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun sendSmsCode(request: SmsCodeRequest): BaseResponseDto<JsonObject?> {
        val response = client.post("${UC_BASE_URL}sendSms") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("[sendSmsCode] HTTP ${response.status.value}")
        return response.body()
    }

    suspend fun smsLogin(request: SmsLoginRequest): BaseResponseDto<LoginTokenData?> =
        client.post("${UC_BASE_URL}smsLogin") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun logout(accessToken: String): BaseResponseDto<JsonObject?> =
        client.post("${UC_BASE_URL}logout") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun getUserInfoWithToken(
        accessToken: String,
        userId: String
    ): BaseResponseDto<UserDto> =
        client.post("${UC_BASE_URL}getUserInfo") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(mapOf("userId" to userId))
        }.body()

    suspend fun getWebAppList(request: WebAppListRequest): BaseResponseDto<PageDataDto<WebAppDto>> =
        client.post("${BASE_URL}webapp/list") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getCarefullyChosenList(): BaseResponseDto<List<WebAppDto>> =
        client.post("${BASE_URL}webapp/carefullyChosenList") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun getCustomMadeWebappList(request: CustomMadeWebappRequest): BaseResponseDto<List<WebAppDto>> =
        client.post("${BASE_URL}webapp/customMadeWebappList") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getWebAppUserList(params: Map<String, String>): BaseResponseDto<PageDataDto<WebAppDto>> =
        client.post("${BASE_URL}webapp/user/list") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    suspend fun getTagTree(request: TagTreeRequest = TagTreeRequest()): BaseResponseDto<List<TagDto>> =
        client.post("${BASE_URL}portal/tag/tree") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getApiCallDemo(apiKey: String, webappId: String): BaseResponseDto<WebAppDetailDto> =
        client.post("${BASE_URL}webapp/apiCallDemo") {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiKey)
            setBody(mapOf("webappId" to webappId))
        }.body()

    suspend fun getWebAppDetail(params: Map<String, String>): BaseResponseDto<WebAppDetailDto> =
        client.post("${BASE_URL}webapp/detail") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    suspend fun runTask(request: TaskRunRequest): BaseResponseDto<TaskRunResponseDto> =
        client.post("https://www.runninghub.cn/task/openapi/ai-app/run") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getTaskOutputs(request: TaskStatusRequest): BaseResponseDto<List<TaskOutputDto>> =
        client.post("https://www.runninghub.cn/task/openapi/outputs") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun uploadFile(
        apiKey: String,
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): BaseResponseDto<UploadResponseDto> =
        client.submitFormWithBinaryData(
            url = "https://www.runninghub.cn/task/openapi/upload",
            formData = formData {
                append("apiKey", apiKey)
                append("fileType", fileType)
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ).body()

    suspend fun getTaskHistory(
        params: Map<String, Any>
    ): BaseResponseDto<PageDataDto<TaskHistoryItemDto>> =
        client.post("${BASE_URL}output/v2/history") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    suspend fun getAccountStatus(request: AccountStatusRequest): BaseResponseDto<AccountStatusDto> =
        client.post("https://www.runninghub.cn/uc/openapi/accountStatus") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getUserInfo(params: Map<String, String> = emptyMap()): BaseResponseDto<UserDto> =
        client.post("https://www.runninghub.cn/uc/getUserInfo") {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    suspend fun getUserDetail(
        referer: String,
        params: Map<String, String>
    ): BaseResponseDto<UserDto> =
        client.post("https://www.runninghub.cn/uc/getUserInfo") {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    suspend fun isFollow(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/isFollow") {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    suspend fun followUser(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/followUser") {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    suspend fun unFollowUser(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/unFollowUser") {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()
}
