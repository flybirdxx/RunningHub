package com.runninghub.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class RunningHubApiEnvironmentTest {
    @Test
    fun `environment exposes stable runninghub base urls`() {
        assertEquals("https://www.runninghub.cn", RunningHubApiEnvironment.WEB_ORIGIN)
        assertEquals("https://www.runninghub.cn/", RunningHubApiEnvironment.WEB_BASE_URL)
        assertEquals("https://www.runninghub.cn/api/", RunningHubApiEnvironment.API_BASE_URL)
        assertEquals("https://www.runninghub.cn/uc/", RunningHubApiEnvironment.USER_CENTER_BASE_URL)
        assertEquals("https://www.runninghub.cn/uc/token/refresh", RunningHubApiEnvironment.TOKEN_REFRESH_URL)
    }

    @Test
    fun `url builders normalize leading slash`() {
        assertEquals(
            "https://www.runninghub.cn/api/webapp/list",
            RunningHubApiEnvironment.apiUrl("/webapp/list"),
        )
        assertEquals(
            "https://www.runninghub.cn/uc/openapi/accountStatus",
            RunningHubApiEnvironment.userCenterUrl("openapi/accountStatus"),
        )
        assertEquals(
            "https://www.runninghub.cn/uc/follow/isFollow",
            RunningHubApiEnvironment.userCenterUrl("/follow/isFollow"),
        )
        assertEquals(
            "https://www.runninghub.cn/task/openapi/upload",
            RunningHubApiEnvironment.taskOpenApiUrl("upload"),
        )
        assertEquals(
            "https://www.runninghub.cn/task/openapi/ai-app/run",
            RunningHubApiEnvironment.taskOpenApiUrl("/ai-app/run"),
        )
        assertEquals(
            "https://www.runninghub.cn/openapi/v2/query",
            RunningHubApiEnvironment.openApiV2Url("/query"),
        )
        assertEquals(
            "https://www.runninghub.cn/profile/user-1",
            RunningHubApiEnvironment.webUrl("profile/user-1"),
        )
    }

    @Test
    fun `trusted auth host check only accepts exact production host`() {
        assertEquals(true, isTrustedRunningHubHost("www.runninghub.cn"))
        assertEquals(true, isTrustedRunningHubHost("WWW.RUNNINGHUB.CN"))
        assertEquals(false, isTrustedRunningHubHost("runninghub.cn"))
        assertEquals(false, isTrustedRunningHubHost("evilrunninghub.cn"))
        assertEquals(false, isTrustedRunningHubHost("runninghub.cn.example.com"))
        assertEquals(false, isTrustedRunningHubHost(""))
    }

    @Test
    fun `platform startup can configure api environment and trusted auth hosts`() {
        val staging = ApiEnvironment(
            webBaseUrl = "https://staging.runninghub.test/",
            apiBaseUrl = "https://api.staging.runninghub.test/api/",
            userCenterBaseUrl = "https://auth.staging.runninghub.test/uc/",
            taskBaseUrl = "https://task.staging.runninghub.test/task/openapi/",
            openApiV2BaseUrl = "https://openapi.staging.runninghub.test/openapi/v2/",
            trustedAuthHosts = setOf(
                "auth.staging.runninghub.test",
                "api.staging.runninghub.test",
            ),
        )

        try {
            RunningHubApiEnvironment.configure(staging)

            assertEquals("https://staging.runninghub.test", RunningHubApiEnvironment.WEB_ORIGIN)
            assertEquals("https://api.staging.runninghub.test/api/webapp/list", RunningHubApiEnvironment.apiUrl("webapp/list"))
            assertEquals("https://auth.staging.runninghub.test/uc/token/refresh", RunningHubApiEnvironment.TOKEN_REFRESH_URL)
            assertEquals(true, isTrustedRunningHubHost("AUTH.STAGING.RUNNINGHUB.TEST"))
            assertEquals(false, isTrustedRunningHubHost("www.runninghub.cn"))
        } finally {
            RunningHubApiEnvironment.resetToProduction()
        }
    }
}
