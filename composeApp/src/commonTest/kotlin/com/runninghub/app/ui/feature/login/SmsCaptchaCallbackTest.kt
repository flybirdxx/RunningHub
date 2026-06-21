package com.runninghub.app.ui.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 短信图形验证码原生回调 URL 的契约测试。
 *
 * Android WebView 与 iOS WKWebView 都可能退回自定义 scheme 传递 token 或关闭事件；
 * 这些测试保证两个平台共享同一套解析语义，避免一端能关闭弹窗而另一端静默忽略。
 */
class SmsCaptchaCallbackTest {
    /**
     * token 回调必须从 `value` 查询参数中取值并按 `encodeURIComponent` 规则解码。
     *
     * TAC 返回的 `validToken` 只用于下一次短信发送请求；解析层只负责恢复原始字符串，
     * 不持久化、不打印，也不对 token 内容做业务校验。
     */
    @Test
    fun `token callback url extracts encoded token`() {
        val callback = parseSmsCaptchaCallbackUrl(
            "runninghub-sms-captcha://token?value=abc%2Fdef%2Bghi%3D",
        )

        assertEquals(SmsCaptchaCallback.Token("abc/def+ghi="), callback)
    }

    /**
     * 空 token 表示网页侧没有拿到有效短信凭证。
     *
     * 解析层保留该状态并交给 [LoginScreenModel] 决定如何提示用户重试，而不是伪造成功 token。
     */
    @Test
    fun `token callback url maps blank value to null token`() {
        val callback = parseSmsCaptchaCallbackUrl("runninghub-sms-captcha://token?value=")

        assertEquals(SmsCaptchaCallback.Token(null), callback)
    }

    /**
     * close 回调只表达用户主动关闭验证码容器。
     *
     * 它不能携带 token，也不应触发短信发送重试。
     */
    @Test
    fun `close callback url maps to close event`() {
        val callback = parseSmsCaptchaCallbackUrl("runninghub-sms-captcha://close")

        assertEquals(SmsCaptchaCallback.Close, callback)
    }

    /**
     * 非验证码 scheme 或未知 host 必须被忽略。
     *
     * 平台 Web 容器还会加载 TAC JS/CSS 和图片资源，解析函数不能错误拦截普通 HTTPS 导航。
     */
    @Test
    fun `unknown callback url is ignored`() {
        assertNull(parseSmsCaptchaCallbackUrl("https://www.runninghub.cn/tac/js/tac.min.js"))
        assertNull(parseSmsCaptchaCallbackUrl("runninghub-sms-captcha://unknown?value=abc"))
    }
}
