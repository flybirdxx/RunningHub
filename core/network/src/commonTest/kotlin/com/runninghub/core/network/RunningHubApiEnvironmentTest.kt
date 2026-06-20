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
}
