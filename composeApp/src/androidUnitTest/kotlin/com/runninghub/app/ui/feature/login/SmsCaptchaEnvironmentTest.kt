package com.runninghub.app.ui.feature.login

import com.runninghub.core.network.ApiEnvironment
import com.runninghub.core.network.RunningHubApiEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android 平台验证码环境契约测试。
 *
 * 验证码 HTML 自身使用相对路径访问 TAC 资源和用户中心接口；真正决定同源主机的是平台
 * WebView 的 base URL。该测试锁定 base URL 必须跟随平台启动层注入的 [RunningHubApiEnvironment]，
 * 避免 debug/staging 登录请求使用测试环境而图形验证码仍访问生产站点。
 */
class SmsCaptchaEnvironmentTest {

    @AfterTest
    fun tearDown() {
        RunningHubApiEnvironment.resetToProduction()
    }

    @Test
    fun `captcha base url follows configured web environment`() {
        RunningHubApiEnvironment.configure(
            ApiEnvironment(
                webBaseUrl = "https://captcha.staging.runninghub.test",
                apiBaseUrl = "https://captcha.staging.runninghub.test/api",
                userCenterBaseUrl = "https://captcha.staging.runninghub.test/uc",
                taskBaseUrl = "https://captcha.staging.runninghub.test/task/openapi",
                openApiV2BaseUrl = "https://captcha.staging.runninghub.test/openapi/v2",
                trustedAuthHosts = setOf("captcha.staging.runninghub.test"),
            ),
        )

        assertEquals(
            "https://captcha.staging.runninghub.test/",
            smsCaptchaBaseUrl(),
        )
    }
}
