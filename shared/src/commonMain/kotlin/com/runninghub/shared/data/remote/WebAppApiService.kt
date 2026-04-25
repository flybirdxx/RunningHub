package com.runninghub.shared.data.remote

import com.runninghub.shared.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody

class WebAppApiService(private val client: HttpClient) {

    suspend fun getWebAppList(request: WebAppListRequest): BaseResponse<PageData<WebAppDto>> =
        client.post("webapp/list") { setBody(request) }.body()

    suspend fun getCarefullyChosenList(): BaseResponse<List<WebAppDto>> =
        client.post("webapp/carefullyChosenList") { setBody(emptyMap<String, String>()) }.body()

    suspend fun getWebAppUserList(request: Map<String, String>): BaseResponse<PageData<WebAppDto>> =
        client.post("webapp/user/list") { setBody(request) }.body()

    suspend fun getTagTree(request: TagTreeRequest = TagTreeRequest()): BaseResponse<List<TagDto>> =
        client.post("portal/tag/tree") { setBody(request) }.body()

    suspend fun getApiCallDemo(apiKey: String, webappId: String): BaseResponse<WebAppDetailDto> =
        client.get("webapp/apiCallDemo") {
            parameter("apiKey", apiKey)
            parameter("webappId", webappId)
        }.body()

    suspend fun getWebAppDetail(request: Map<String, String>): BaseResponse<WebAppDetailDto> =
        client.post("webapp/detail") { setBody(request) }.body()

    suspend fun runTask(request: TaskRunRequest): BaseResponse<TaskRunResponse> =
        client.post("${ApiConfig.BASE_URL}../task/openapi/ai-app/run") { setBody(request) }.body()

    suspend fun getTaskOutputs(request: TaskStatusRequest): BaseResponse<List<TaskOutputDto>> =
        client.post("${ApiConfig.BASE_URL}../task/openapi/outputs") { setBody(request) }.body()

    suspend fun getAccountStatus(request: AccountStatusRequest): BaseResponse<AccountStatusDto> =
        client.post("${ApiConfig.BASE_URL}../uc/openapi/accountStatus") { setBody(request) }.body()

    suspend fun getUserInfo(): BaseResponse<UserDto> =
        client.post("https://www.runninghub.cn/uc/getUserInfo") {
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun getUserDetail(referer: String, request: Map<String, String>): BaseResponse<UserDto> =
        client.post("https://www.runninghub.cn/uc/getUserInfo") {
            headers.append("Referer", referer)
            setBody(request)
        }.body()

    suspend fun isFollow(referer: String, request: Map<String, String>): BaseResponse<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/isFollow") {
            headers.append("Referer", referer)
            setBody(request)
        }.body()

    suspend fun followUser(referer: String, request: Map<String, String>): BaseResponse<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/followUser") {
            headers.append("Referer", referer)
            setBody(request)
        }.body()

    suspend fun unFollowUser(referer: String, request: Map<String, String>): BaseResponse<Boolean> =
        client.post("https://www.runninghub.cn/uc/follow/unFollowUser") {
            headers.append("Referer", referer)
            setBody(request)
        }.body()
}
