package com.runninghub.app.ui.feature.login

import com.runninghub.core.network.ApiEnvironment
import com.runninghub.core.network.RunningHubApiEnvironment
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * iOS 平台验证码环境契约测试。
 *
 * WKWebView 通过 baseURL 解析 HTML 内的 `/tac` 静态资源和 `/uc` 校验接口；该地址必须跟随
 * 平台启动层注入的 Web 环境，避免 debug/staging 短信接口和验证码 token 来自不同站点。
 */
class SmsCaptchaEnvironmentTest {

    @AfterTest
    fun tearDown() {
        RunningHubApiEnvironment.resetToProduction()
    }

    @Test
    fun captchaBaseUrlFollowsConfiguredWebEnvironment() {
        RunningHubApiEnvironment.configure(
            ApiEnvironment(
                webBaseUrl = "https://captcha.ios.runninghub.test",
                apiBaseUrl = "https://captcha.ios.runninghub.test/api",
                userCenterBaseUrl = "https://captcha.ios.runninghub.test/uc",
                taskBaseUrl = "https://captcha.ios.runninghub.test/task/openapi",
                openApiV2BaseUrl = "https://captcha.ios.runninghub.test/openapi/v2",
                trustedAuthHosts = setOf("captcha.ios.runninghub.test"),
            ),
        )

        assertEquals(
            "https://captcha.ios.runninghub.test/",
            smsCaptchaBaseUrl(),
        )
    }
}
