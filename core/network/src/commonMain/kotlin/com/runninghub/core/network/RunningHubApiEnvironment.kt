package com.runninghub.core.network

/**
 * RunningHub 线上 API 环境地址集合。
 *
 * 本对象属于 core/network 的环境边界，集中维护 Web、用户中心、任务 OpenAPI 等根地址。
 * Data 层可以通过这些地址拼接远端 endpoint；Domain 和 Presentation 不应依赖这些网络细节。
 * 这里的值均为公开服务地址，不包含 Token、Cookie、API Key 或其他敏感信息。
 */
object RunningHubApiEnvironment {
    /** RunningHub 主域名标识，用于判断请求是否需要自动附加站内认证头。 */
    const val HOST_MARKER = "runninghub.cn"

    /** RunningHub Web 站点 origin，不带末尾斜杠，适用于 Origin 请求头。 */
    const val WEB_ORIGIN = "https://www.runninghub.cn"

    /** RunningHub Web 根地址，带末尾斜杠，适用于 Referer 请求头和相对路径拼接。 */
    const val WEB_BASE_URL = "$WEB_ORIGIN/"

    /** Web 业务 API 根地址，带末尾斜杠，对应 `/api/` 下的目录、用户和历史接口。 */
    const val API_BASE_URL = "${WEB_BASE_URL}api/"

    /** 用户中心 API 根地址，带末尾斜杠，对应登录、短信、用户信息和 token 刷新接口。 */
    const val USER_CENTER_BASE_URL = "${WEB_BASE_URL}uc/"

    /** 任务 OpenAPI 根地址，带末尾斜杠，对应 API Key 任务运行、输出和上传接口。 */
    const val TASK_OPEN_API_BASE_URL = "${WEB_BASE_URL}task/openapi/"

    /** 模型调用 OpenAPI v2 根地址，带末尾斜杠，对应快捷创作和模型直调能力。 */
    const val OPEN_API_V2_BASE_URL = "${WEB_BASE_URL}openapi/v2/"

    /** 用户中心 refresh token 接口完整地址，供独立 refresh 客户端使用。 */
    const val TOKEN_REFRESH_URL = "${USER_CENTER_BASE_URL}token/refresh"

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
