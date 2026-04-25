package com.runninghub.app.data.remote.api

import com.runninghub.app.data.remote.model.BaseResponse
import com.runninghub.app.data.remote.model.PageData
import com.runninghub.app.data.remote.model.TagDto
import com.runninghub.app.data.remote.model.TagTreeRequest
import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.WebAppDto
import com.runninghub.app.data.remote.model.WebAppListRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.runninghub.app.data.remote.model.*

/**
 * [INPUT]: WebAppListRequest
 * [OUTPUT]: BaseResponse<PageData<WebAppDto>>
 * [POS]: 定义 WebApp 相关的网络请求契约
 */
interface WebAppApi {
    @POST("webapp/list")
    suspend fun getWebAppList(
        @Body request: WebAppListRequest
    ): BaseResponse<PageData<WebAppDto>>

    @POST("webapp/carefullyChosenList")
    suspend fun getCarefullyChosenList(
        @Body request: Map<String, String> = emptyMap()
    ): BaseResponse<List<WebAppDto>>

    @POST("webapp/user/list")
    suspend fun getWebAppUserList(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): BaseResponse<PageData<WebAppDto>>

    @POST("portal/tag/tree")
    suspend fun getTagTree(
        @Body request: TagTreeRequest = TagTreeRequest()
    ): BaseResponse<List<TagDto>>

    @GET("webapp/apiCallDemo")
    suspend fun getApiCallDemo(
        @Query("apiKey") apiKey: String,
        @Query("webappId") webappId: String
    ): BaseResponse<WebAppDetailDto>

    @POST("webapp/detail")
    suspend fun getWebAppDetail(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): BaseResponse<WebAppDetailDto>

    @POST("/task/openapi/ai-app/run")
    suspend fun runTask(
        @Body request: TaskRunRequest
    ): BaseResponse<TaskRunResponse>

    @POST("/task/openapi/outputs")
    suspend fun getTaskOutputs(
        @Body request: TaskStatusRequest
    ): BaseResponse<List<TaskOutputDto>>

    @Multipart
    @POST("/task/openapi/upload")
    suspend fun uploadFile(
        @Part("apiKey") apiKey: RequestBody,
        @Part("fileType") fileType: RequestBody,
        @Part file: MultipartBody.Part
    ): BaseResponse<UploadResponse>

    @POST("/uc/openapi/accountStatus")
    suspend fun getAccountStatus(
        @Body request: AccountStatusRequest
    ): BaseResponse<AccountStatusDto>

    @POST("https://www.runninghub.cn/uc/getUserInfo")
    suspend fun getUserInfo(
        @Body request: Map<String, String> = emptyMap()
    ): BaseResponse<UserDto>
    @POST("https://www.runninghub.cn/uc/getUserInfo")
    suspend fun getUserDetail(
        @retrofit2.http.Header("Referer") referer: String,
        @Body request: Map<String, String>
    ): BaseResponse<UserDto>
    @POST("https://www.runninghub.cn/uc/follow/isFollow")
    suspend fun isFollow(
        @retrofit2.http.Header("Referer") referer: String,
        @Body request: Map<String, String>
    ): BaseResponse<Boolean>

    @POST("https://www.runninghub.cn/uc/follow/followUser")
    suspend fun followUser(
        @retrofit2.http.Header("Referer") referer: String,
        @Body request: Map<String, String>
    ): BaseResponse<Boolean>

    @POST("https://www.runninghub.cn/uc/follow/unFollowUser")
    suspend fun unFollowUser(
        @retrofit2.http.Header("Referer") referer: String,
        @Body request: Map<String, String>
    ): BaseResponse<Boolean>
}
