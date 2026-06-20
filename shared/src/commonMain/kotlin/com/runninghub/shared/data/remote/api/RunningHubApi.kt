package com.runninghub.shared.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.shared.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

/**
 * RunningHub Web 与用户中心接口的 Data 层访问入口。
 *
 * 本类只负责把 DTO 请求发送到远端 API，并保留当前服务端路径兼容逻辑；
 * 业务错误映射和 Domain 模型转换由 Repository 层处理。
 */
class RunningHubApi(private val client: HttpClient) {

    /**
     * 使用手机号和密码登录用户中心。
     *
     * 登录接口属于 `/uc/` 用户中心分组，路径统一由 [RunningHubApiEnvironment.userCenterUrl]
     * 生成；本方法只返回 token DTO，不在 Data API 层生成最终登录错误文案。
     */
    suspend fun pwdLogin(request: PwdLoginRequest): BaseResponseDto<LoginTokenData?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("pwdLogin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 使用 refresh token 换取新的登录 token。
     *
     * refresh token 仅通过 Authorization 请求头发送，本方法不记录 token 内容；
     * 并发刷新和会话失效处理由上层认证组件负责。
     */
    suspend fun tokenRefresh(refreshToken: String): BaseResponseDto<LoginTokenData?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("token/refresh")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $refreshToken")
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 请求用户中心发送短信验证码。
     *
     * 该接口只触发远端发送动作，验证码倒计时、频控提示和可展示错误由 Presentation
     * 通过 Repository 返回结果自行处理。
     */
    suspend fun sendSmsCode(request: SmsCodeRequest): BaseResponseDto<JsonObject?> {
        val response = client.post(RunningHubApiEnvironment.userCenterUrl("sendSms")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        println("[sendSmsCode] HTTP ${response.status.value}")
        return response.body()
    }

    /**
     * 使用短信验证码登录用户中心。
     *
     * 短信验证码属于短生命周期凭据，本方法只负责提交 DTO；验证码清空、
     * 失败重试和最终文案由登录 ScreenModel 处理。
     */
    suspend fun smsLogin(request: SmsLoginRequest): BaseResponseDto<LoginTokenData?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("smsLogin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 注销当前用户中心会话。
     *
     * access token 仅作为 Authorization 请求头发送；本方法不清理本地凭据，
     * 本地会话状态由 Repository 在远端调用完成后统一更新。
     */
    suspend fun logout(accessToken: String): BaseResponseDto<JsonObject?> =
        client.post(RunningHubApiEnvironment.userCenterUrl("logout")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 使用指定 access token 查询用户中心用户信息。
     *
     * 该方法用于登录后拉取用户资料并建立本地会话；如果远端返回空数据，
     * Repository 负责转换为认证失败或会话失效。
     */
    suspend fun getUserInfoWithToken(
        accessToken: String,
        userId: String
    ): BaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(mapOf("userId" to userId))
        }.body()

    /**
     * 查询 WebApp 目录分页列表。
     *
     * WebApp 目录属于 `/api/` 业务分组，路径统一经 [RunningHubApiEnvironment.apiUrl] 生成；
     * 搜索、分页和标签语义由请求 DTO 承载。
     */
    suspend fun getWebAppList(request: WebAppListRequest): BaseResponseDto<PageDataDto<WebAppDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/list")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询运营精选 WebApp 列表。
     *
     * 服务端返回顺序包含运营配置权重，Repository 映射时应保持原始顺序。
     */
    suspend fun getCarefullyChosenList(): BaseResponseDto<List<WebAppDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/carefullyChosenList")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 查询定制 WebApp 列表。
     *
     * 该接口仍使用 WebApp 业务分组，Data API 只提交请求参数；
     * 是否展示为空态或推荐内容由 Repository 和 Presentation 决定。
     */
    suspend fun getCustomMadeWebappList(request: CustomMadeWebappRequest): BaseResponseDto<List<WebAppDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/customMadeWebappList")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询指定用户发布的 WebApp 分页列表。
     *
     * params 保留为 Map 以兼容当前服务端参数集，调用方应避免放入 token、cookie
     * 或其他敏感值。
     */
    suspend fun getWebAppUserList(params: Map<String, String>): BaseResponseDto<PageDataDto<WebAppDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/user/list")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /**
     * 查询门户标签树。
     *
     * 标签树接口被首页目录和广场等多个数据流复用，统一保留在 `/api/portal` 分组；
     * 具体筛选范围由 [TagTreeRequest] 决定。
     */
    suspend fun getTagTree(request: TagTreeRequest = TagTreeRequest()): BaseResponseDto<List<TagDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("portal/tag/tree")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询 WebApp 的 API 调用示例配置。
     *
     * API Key 只放入 `X-API-Key` 请求头，不写入日志或异常消息；返回 DTO 仍需由
     * Repository 转换为领域详情。
     */
    suspend fun getApiCallDemo(apiKey: String, webappId: String): BaseResponseDto<WebAppDetailDto> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/apiCallDemo")) {
            contentType(ContentType.Application.Json)
            header("X-API-Key", apiKey)
            setBody(mapOf("webappId" to webappId))
        }.body()

    /**
     * 查询 WebApp 详情。
     *
     * params 维持 Map 形态以兼容服务端历史字段，Repository 负责校验必要字段并映射详情模型。
     */
    suspend fun getWebAppDetail(params: Map<String, String>): BaseResponseDto<WebAppDetailDto> =
        client.post(RunningHubApiEnvironment.apiUrl("webapp/detail")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /**
     * 通过任务 OpenAPI 提交 WebApp 任务。
     *
     * 该接口属于 API Key 任务链路，路径统一由 core/network 的环境对象生成，
     * 避免 Data 层散落完整域名导致环境切换或测试 Mock 规则漂移。
     *
     * @param request 任务运行 DTO，包含应用标识、参数和 API Key。
     * @return 服务端基础响应，调用方负责校验 code 并映射为 Domain 任务。
     */
    suspend fun runTask(request: TaskRunRequest): BaseResponseDto<TaskRunResponseDto> =
        client.post(RunningHubApiEnvironment.taskOpenApiUrl("ai-app/run")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询任务 OpenAPI 输出结果。
     *
     * @param request 任务状态请求，通常包含 taskId 和 API Key。
     * @return 远端输出 DTO 列表；空结果和业务错误由 Repository 继续处理。
     */
    suspend fun getTaskOutputs(request: TaskStatusRequest): BaseResponseDto<List<TaskOutputDto>> =
        client.post(RunningHubApiEnvironment.taskOpenApiUrl("outputs")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 上传 WebApp 任务所需的二进制文件。
     *
     * 文件内容通过 multipart form 发送，API Key 保持在请求体字段中以兼容现有服务端协议。
     * 本方法不记录文件内容或凭据，避免调试日志泄露敏感数据。
     *
     * @param apiKey 调用任务 OpenAPI 使用的 API Key。
     * @param fileType 服务端要求的文件类型标识。
     * @param fileBytes 文件二进制内容。
     * @param fileName 上传文件名，仅用于 multipart disposition。
     * @return 上传响应 DTO，调用方负责校验 URL 是否存在。
     */
    suspend fun uploadFile(
        apiKey: String,
        fileType: String,
        fileBytes: ByteArray,
        fileName: String
    ): BaseResponseDto<UploadResponseDto> =
        client.submitFormWithBinaryData(
            url = RunningHubApiEnvironment.taskOpenApiUrl("upload"),
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
        client.post(RunningHubApiEnvironment.apiUrl("output/v2/history")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /**
     * 查询当前 API Key 的账户余额和权益状态。
     *
     * @param request 账户状态请求，包含 API Key。
     * @return 账户状态 DTO；余额不足等业务状态由 Repository 映射给 Domain。
     */
    suspend fun getAccountStatus(request: AccountStatusRequest): BaseResponseDto<AccountStatusDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("openapi/accountStatus")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询当前会话或指定用户的用户信息。
     *
     * @param params 可选用户参数；为空时按当前登录会话查询。
     * @return 用户信息 DTO；空数据和业务错误由 Repository 转换。
     */
    suspend fun getUserInfo(params: Map<String, String> = emptyMap()): BaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()

    /**
     * 按用户主页上下文查询用户详情。
     *
     * 部分用户中心接口依赖 Referer 判断来源页面，因此 Referer 由 Repository 根据用户主页生成，
     * 本方法只负责把该上下文转发到远端接口。
     *
     * @param referer 用户主页 Referer。
     * @param params 用户查询参数。
     * @return 用户详情 DTO。
     */
    suspend fun getUserDetail(
        referer: String,
        params: Map<String, String>
    ): BaseResponseDto<UserDto> =
        client.post(RunningHubApiEnvironment.userCenterUrl("getUserInfo")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /**
     * 查询当前用户是否关注目标用户。
     *
     * @param referer 目标用户主页 Referer，用于兼容用户中心关注接口的 Web 来源校验。
     * @param params 关注查询参数，包含 followId。
     * @return true 表示已关注；缺失数据由 Repository 按 false 处理。
     */
    suspend fun isFollow(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/isFollow")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /**
     * 关注目标用户。
     *
     * @param referer 目标用户主页 Referer，用于兼容用户中心关注接口的 Web 来源校验。
     * @param params 关注参数，包含 followId。
     * @return true 表示服务端接受关注操作。
     */
    suspend fun followUser(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/followUser")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()

    /**
     * 取消关注目标用户。
     *
     * @param referer 目标用户主页 Referer，用于兼容用户中心关注接口的 Web 来源校验。
     * @param params 取消关注参数，包含 followId。
     * @return true 表示服务端接受取消关注操作。
     */
    suspend fun unFollowUser(referer: String, params: Map<String, String>): BaseResponseDto<Boolean> =
        client.post(RunningHubApiEnvironment.userCenterUrl("follow/unFollowUser")) {
            contentType(ContentType.Application.Json)
            header("Referer", referer)
            setBody(params)
        }.body()
}
