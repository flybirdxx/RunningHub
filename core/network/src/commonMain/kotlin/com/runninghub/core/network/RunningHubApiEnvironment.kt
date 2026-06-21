package com.runninghub.core.network

/**
 * RunningHub API 环境配置。
 *
 * 本模型位于 core/network，用于描述平台启动层注入的远端环境。它只包含公开服务地址和
 * 认证头允许发送的主机白名单，不保存 Token、Cookie、API Key 或用户身份信息。
 *
 * @property webBaseUrl RunningHub Web 根地址，建议带末尾斜杠。
 * 该地址用于普通 Web 分组接口、Referer 请求头和站内页面链接；空字符串不是有效配置。
 * @property apiBaseUrl Web 业务 API 根地址，通常对应 `/api/` 分组。
 * Data 层目录、社区和部分快捷创作接口会基于该地址拼接相对 endpoint。
 * @property userCenterBaseUrl 用户中心 API 根地址，通常对应 `/uc/` 分组。
 * 登录、短信、用户信息和 token refresh 均从该地址派生；切换环境时必须与
 * [trustedAuthHosts] 同步，避免凭据发送到错误主机。
 * @property taskBaseUrl 任务 OpenAPI 根地址，通常对应 `/task/openapi/` 分组。
 * API Key 任务运行、输出和上传接口会基于该地址拼接路径。
 * @property openApiV2BaseUrl 模型调用 OpenAPI v2 根地址，通常对应 `/openapi/v2/` 分组。
 * 快捷创作模型直调和媒体上传会基于该地址拼接路径。
 * @property trustedAuthHosts 允许自动附加 Authorization 与 Cookie 的精确主机白名单。
 * 元素必须是不含协议、端口和路径的主机名；空集合表示所有请求都不得自动携带站内凭据。
 */
data class ApiEnvironment(
    val webBaseUrl: String,
    val apiBaseUrl: String,
    val userCenterBaseUrl: String,
    val taskBaseUrl: String,
    val openApiV2BaseUrl: String,
    val trustedAuthHosts: Set<String>,
)

/**
 * RunningHub API 环境地址门面。
 *
 * 本对象属于 core/network 的环境边界，集中维护 Web、用户中心、任务 OpenAPI 等根地址。
 * Data 层可以通过这些地址拼接远端 endpoint；Domain 和 Presentation 不应依赖这些网络细节。
 * 平台启动层通过 [configure] 注入实际环境；默认值为生产环境，便于单元测试和迁移期旧组合根继续运行。
 */
object RunningHubApiEnvironment {
    private var activeEnvironment: ApiEnvironment = production()

    /**
     * 允许自动附加站内认证头的精确主机白名单。
     *
     * 该集合只列出当前环境真实主机，不能使用 `contains` 或通配符匹配。
     * Authorization 与 Cookie 属于敏感凭据，若把 `evilrunninghub.cn` 或
     * `runninghub.cn.example.com` 误判为可信主机，会导致认证头被发送到错误服务端。
     */
    internal val TRUSTED_AUTH_HOSTS: Set<String>
        get() = activeEnvironment.trustedAuthHosts

    /**
     * RunningHub Web 站点 origin，不带末尾斜杠，适用于 Origin 请求头。
     *
     * 该值来自当前 [ApiEnvironment.webBaseUrl]，平台启动层切换环境后会立即影响新请求。
     */
    val WEB_ORIGIN: String
        get() = activeEnvironment.webBaseUrl.trimEnd('/')

    /**
     * RunningHub Web 根地址，带末尾斜杠，适用于 Referer 请求头和相对路径拼接。
     *
     * 空路径会保留根地址；调用方不应自行拼接协议或主机。
     */
    val WEB_BASE_URL: String
        get() = activeEnvironment.webBaseUrl

    /** Web 业务 API 根地址，带末尾斜杠，对应 `/api/` 下的目录、用户和历史接口。 */
    val API_BASE_URL: String
        get() = activeEnvironment.apiBaseUrl

    /** 用户中心 API 根地址，带末尾斜杠，对应登录、短信、用户信息和 token 刷新接口。 */
    val USER_CENTER_BASE_URL: String
        get() = activeEnvironment.userCenterBaseUrl

    /** 任务 OpenAPI 根地址，带末尾斜杠，对应 API Key 任务运行、输出和上传接口。 */
    val TASK_OPEN_API_BASE_URL: String
        get() = activeEnvironment.taskBaseUrl

    /** 模型调用 OpenAPI v2 根地址，带末尾斜杠，对应快捷创作和模型直调能力。 */
    val OPEN_API_V2_BASE_URL: String
        get() = activeEnvironment.openApiV2BaseUrl

    /** 用户中心 refresh token 接口完整地址，供独立 refresh 客户端使用。 */
    val TOKEN_REFRESH_URL: String
        get() = userCenterUrl("token/refresh")

    /**
     * 构造默认生产 API 环境。
     *
     * @return 当前线上 RunningHub 地址集合；调用方可以在平台启动层按构建类型替换为 staging 或 dev。
     */
    fun production(): ApiEnvironment = ApiEnvironment(
        webBaseUrl = "https://www.runninghub.cn/",
        apiBaseUrl = "https://www.runninghub.cn/api/",
        userCenterBaseUrl = "https://www.runninghub.cn/uc/",
        taskBaseUrl = "https://www.runninghub.cn/task/openapi/",
        openApiV2BaseUrl = "https://www.runninghub.cn/openapi/v2/",
        trustedAuthHosts = setOf("www.runninghub.cn"),
    )

    /**
     * 注入当前运行环境。
     *
     * 平台启动层必须在创建 HttpClient 或 Data API 之前调用本函数。该函数会规范化各根地址的
     * 末尾斜杠，并把认证主机转换为小写精确匹配集合；不会访问网络或读取任何本地凭据。
     *
     * @param environment 平台按构建类型选择的公开 API 环境。debug 可传入 dev/staging，
     * release 应传入 [production]，除非发布流水线显式指定其他环境。
     */
    fun configure(environment: ApiEnvironment) {
        activeEnvironment = environment.normalized()
    }

    /**
     * 恢复默认生产环境。
     *
     * 该函数主要用于单元测试在验证自定义环境后清理全局状态；生产代码通常只在启动时调用
     * [configure] 一次，不应在请求飞行期间切换环境。
     */
    fun resetToProduction() {
        configure(production())
    }

    /**
     * 拼接 RunningHub Web 根地址下的路径。
     *
     * @param path 站点内相对路径，可带或不带开头斜杠。
     * @return 基于 [WEB_BASE_URL] 的完整 URL。
     */
    fun webUrl(path: String): String = WEB_BASE_URL + path.trimStart('/')

    /**
     * 拼接 `/api/` 业务接口路径。
     *
     * @param path `/api/` 下的相对 endpoint，可带或不带开头斜杠。
     * @return 基于 [API_BASE_URL] 的完整 URL。
     */
    fun apiUrl(path: String): String = API_BASE_URL + path.trimStart('/')

    /**
     * 拼接用户中心接口路径。
     *
     * @param path `/uc/` 下的相对 endpoint，可带或不带开头斜杠。
     * @return 基于 [USER_CENTER_BASE_URL] 的完整 URL。
     */
    fun userCenterUrl(path: String): String = USER_CENTER_BASE_URL + path.trimStart('/')

    /**
     * 拼接任务 OpenAPI 接口路径。
     *
     * @param path `/task/openapi/` 下的相对 endpoint，可带或不带开头斜杠。
     * @return 基于 [TASK_OPEN_API_BASE_URL] 的完整 URL。
     */
    fun taskOpenApiUrl(path: String): String = TASK_OPEN_API_BASE_URL + path.trimStart('/')

    /**
     * 拼接 OpenAPI v2 模型接口路径。
     *
     * @param path `/openapi/v2/` 下的相对 endpoint，可带或不带开头斜杠。
     * @return 基于 [OPEN_API_V2_BASE_URL] 的完整 URL。
     */
    fun openApiV2Url(path: String): String = OPEN_API_V2_BASE_URL + path.trimStart('/')
}

/**
 * 判断请求主机是否允许携带 RunningHub 站内认证头。
 *
 * @param host Ktor URL 解析得到的主机名，不包含协议、端口或路径；空字符串表示请求尚未
 * 形成有效远端主机。
 * @return `true` 表示请求会发送到显式信任的 RunningHub 主机，可以附加 Authorization
 * 和 Cookie；`false` 表示必须视为第三方或未知主机，不得发送敏感认证头，也不得触发
 * RunningHub token refresh 流程。
 */
internal fun isTrustedRunningHubHost(host: String): Boolean =
    host.lowercase() in RunningHubApiEnvironment.TRUSTED_AUTH_HOSTS

private fun ApiEnvironment.normalized(): ApiEnvironment =
    copy(
        webBaseUrl = webBaseUrl.normalizedBaseUrl(),
        apiBaseUrl = apiBaseUrl.normalizedBaseUrl(),
        userCenterBaseUrl = userCenterBaseUrl.normalizedBaseUrl(),
        taskBaseUrl = taskBaseUrl.normalizedBaseUrl(),
        openApiV2BaseUrl = openApiV2BaseUrl.normalizedBaseUrl(),
        trustedAuthHosts = trustedAuthHosts
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet(),
    )

private fun String.normalizedBaseUrl(): String {
    val trimmed = trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}
