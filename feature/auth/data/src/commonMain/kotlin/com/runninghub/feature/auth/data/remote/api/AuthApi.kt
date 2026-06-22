package com.runninghub.feature.auth.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.network.auth.markRunningHubAuthRetryAllowed
import com.runninghub.feature.auth.data.remote.dto.AccountStatusDto
import com.runninghub.feature.auth.data.remote.dto.AccountStatusRequestDto
import com.runninghub.feature.auth.data.remote.dto.AuthBaseResponseDto
import com.runninghub.feature.auth.data.remote.dto.LoginTokenDataDto
import com.runninghub.feature.auth.data.remote.dto.PwdLoginRequestDto
import com.runninghub.feature.auth.data.remote.dto.SmsCodeRequestDto
import com.runninghub.feature.auth.data.remote.dto.SmsLoginRequestDto
import com.runninghub.feature.auth.data.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

/**
 * Auth Data 模块的用户中心 API 封装。
 *
 * 本类只负责发送认证、用户资料和关注相关请求；业务错误分类、令牌持久化和会话状态变更
 * 均由 Repository 完成。路径统一通过 [RunningHubApiEnvironment] 生成，避免 Data 层散落完整域名。
 */
class AuthApi(
    private val client: HttpClient,
) {
    /** 使用手机号和 MD5 后的密码登录用户中心。 */
    suspend fun pwdLogin(request: PwdLoginRequestDto): AuthBaseResponseDto<LoginTokenDataDto?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("pwdLogin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** 请求用户中心发送短信验证码。 */
    suspend fun sendSmsCode(request: SmsCodeRequestDto): AuthBaseResponseDto<JsonObject?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("sendSms")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** 使用短信验证码登录用户中心。 */
    suspend fun smsLogin(request: SmsLoginRequestDto): AuthBaseResponseDto<LoginTokenDataDto?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("smsLogin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** 注销当前用户中心会话，失败时由 Repository 按 best-effort 策略处理。 */
    suspend fun logout(accessToken: String): AuthBaseResponseDto<JsonObject?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("logout")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(emptyMap<String, String>())
        }.body()

    /** 使用指定 access token 查询用户信息，通常用于登录成功后建立本地会话。 */
    suspend fun getUserInfoWithToken(
        accessToken: String,
        userId: String,
    ): AuthBaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(mapOf("userId" to userId))
        }.body()

    /** 查询当前 API Key 的账户余额和权益状态。 */
    suspend fun getAccountStatus(request: AccountStatusRequestDto): AuthBaseResponseDto<AccountStatusDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("openapi/accountStatus")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** 查询当前会话或指定用户的用户信息。 */
    suspend fun getUserInfo(params: Map<String, String> = emptyMap()): AuthBaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /** 按用户主页 Referer 查询用户详情。 */
    suspend fun getUserDetail(
        referer: String,
        params: Map<String, String>,
    ): AuthBaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /** 查询当前用户是否关注目标用户。 */
    suspend fun isFollow(
        referer: String,
        params: Map<String, String>,
    ): AuthBaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/isFollow")) {
            markRunningHubAuthRetryAllowed()
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /** 关注目标用户。 */
    suspend fun followUser(
        referer: String,
        params: Map<String, String>,
    ): AuthBaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/followUser")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /** 取消关注目标用户。 */
    suspend fun unFollowUser(
        referer: String,
        params: Map<String, String>,
    ): AuthBaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/unFollowUser")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()
}
