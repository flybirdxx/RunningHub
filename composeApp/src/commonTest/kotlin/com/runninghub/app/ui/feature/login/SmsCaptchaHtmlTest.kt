package com.runninghub.app.ui.feature.login

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 短信图形验证码 HTML 契约测试。
 *
 * 这些测试只锁定客户端 Web 容器包装层的兼容逻辑，不测试 RunningHub TAC 第三方脚本本身。
 * 目标是保证客户端按官网抓包到的 TAC 初始化路径工作，并在平台 Web 容器图片渲染失败时
 * 提供可恢复的重试入口，而不是让用户停留在白色外壳。
 */
class SmsCaptchaHtmlTest {
    /**
     * TAC 初始化应保持与官网一致。
     *
     * 2026-06-21 通过 Chrome DevTools MCP 重新抓包确认，官网页面直接加载 `/tac`
     * 资源并让 TAC 内部请求 `/uc/genCaptcha?type=ROTATE`，成功后再由 `/uc/checkCaptcha`
     * 返回 `validToken`。客户端不应提前预取并覆写 `requestCaptchaData`，否则容易和
     * TAC 内部状态不一致，表现为图形区域白屏。
     */
    @Test
    fun `captcha html delegates challenge loading to tac`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.bridge.onToken(token)",
            closeCallbackExpression = "window.bridge.onClose()",
        )

        assertTrue(html.contains("requestCaptchaDataUrl: '/uc/genCaptcha?type=ROTATE'"))
        assertTrue(html.contains("validCaptchaUrl: '/uc/checkCaptcha'"))
        assertTrue(html.contains("<link rel=\"stylesheet\" href=\"/tac/css/tac.css\">"))
        assertTrue(html.contains("script.src = '/tac/js/tac.min.js'"))
        assertTrue(html.contains("new window.TAC(config, style).init()"))
        assertTrue(html.contains("btnUrl: '/tac/images/huakuai.png'"))
        assertFalse(html.contains("https://www.runninghub.cn"))
        assertFalse(html.contains("config.requestCaptchaData = function"))
    }

    /**
     * TAC 校验成功后必须提取 token 并回调原生层。
     *
     * 用户已经看到绿色“验证成功”时，说明 `/uc/checkCaptcha` 已成功返回；
     * 此时 HTML 包装层必须从响应中取出 `validToken`，否则原生登录页不会继续重试 `/uc/sendSms`。
     */
    @Test
    fun `captcha success extracts token before native callback`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.bridge.onToken(token)",
            closeCallbackExpression = "window.bridge.onClose()",
        )

        assertTrue(html.contains("function extractValidToken(res)"))
        assertTrue(html.contains("res.data && res.data.validToken"))
        assertTrue(html.contains("typeof res.data === 'string'"))
        assertTrue(html.contains("var token = extractValidToken(res)"))
        assertTrue(html.contains("notifyNativeToken(token)"))
        assertTrue(html.contains("window.webkit.messageHandlers[bridgeName].postMessage('token:' + callbackToken)"))
        assertFalse(html.contains("tac.destroyWindow()"))
    }

    /**
     * 成功和关闭事件应能通过自定义 scheme 回到原生层。
     *
     * WebView / WKWebView 的 JS bridge 在不同平台上兼容性差异较大，
     * 这里要求 HTML 允许调用方注入 URL scheme 回调，以便原生层从导航拦截统一接收事件。
     */
    @Test
    fun `captcha html supports native callback expression injection`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.location.href = 'runninghub-sms-captcha://token?value=' + encodeURIComponent(token || '')",
            closeCallbackExpression = "window.location.href = 'runninghub-sms-captcha://close'",
        )

        assertTrue(html.contains("window[bridgeName] && typeof window[bridgeName].onToken === 'function'"))
        assertTrue(html.contains("window[bridgeName] && typeof window[bridgeName].onClose === 'function'"))
        assertTrue(html.contains("window.webkit.messageHandlers[bridgeName]"))
        assertTrue(html.contains("window.webkit.messageHandlers[bridgeName].postMessage('close')"))
        assertTrue(html.contains("runninghub-sms-captcha://token"))
        assertTrue(html.contains("runninghub-sms-captcha://close"))
    }

    /**
     * 平台 Web 容器无法渲染验证码图片时应展示重试入口。
     *
     * Android WebView 或 WKWebView 偶发资源解码失败时，TAC 脚本可能已经创建外壳但图片仍为空。
     * watchdog 只检查背景图和旋转图是否真实完成解码，失败时替换为可点击重试文案。
     */
    @Test
    fun `captcha html has render watchdog for blank image shell`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.bridge.onToken(token)",
            closeCallbackExpression = "window.bridge.onClose()",
        )

        assertTrue(html.contains("scheduleRenderWatchdog"))
        assertTrue(html.contains("tianai-captcha-slider-bg-img"))
        assertTrue(html.contains("tianai-captcha-slider-move-img"))
        assertTrue(html.contains("图形验证图片加载失败，请点击重试"))
    }

    /**
     * TAC 脚本加载失败或长时间无响应时必须进入可恢复降级状态。
     *
     * WKWebView 和 Android WebView 都可能因为网络、ATS/证书或 CDN 临时异常导致外部脚本没有执行；
     * HTML 包装层不能只停留在“准备图形验证”，必须显示明确失败文案并允许用户重试加载脚本。
     */
    @Test
    fun `captcha html reports script load failure and timeout`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.bridge.onToken(token)",
            closeCallbackExpression = "window.bridge.onClose()",
        )

        assertTrue(html.contains("function loadCaptchaScript()"))
        assertTrue(html.contains("script.onerror = function ()"))
        assertTrue(html.contains("window.__captchaScriptTimer = window.setTimeout"))
        assertTrue(html.contains("图形验证脚本加载失败，请点击重试"))
        assertTrue(html.contains("图形验证脚本加载超时，请点击重试"))
        assertTrue(html.contains("onclick=\"window.__loadSmsCaptchaScript()\""))
    }

    /**
     * 重试加载脚本前必须清理旧 script 和旧计时器。
     *
     * 用户多次打开验证码或点击重试时，旧的 TAC script / watchdog 若继续存活，
     * 可能晚于新实例回调并覆盖当前弹窗状态，表现为已关闭弹窗又收到旧 token。
     */
    @Test
    fun `captcha html clears stale script and timers before retry`() {
        val html = smsCaptchaHtml(
            tokenCallbackExpression = "window.bridge.onToken(token)",
            closeCallbackExpression = "window.bridge.onClose()",
        )

        assertTrue(html.contains("window.clearTimeout(window.__captchaScriptTimer)"))
        assertTrue(html.contains("window.clearTimeout(window.__captchaWatchdog)"))
        assertTrue(html.contains("var oldScript = document.getElementById('runninghub-tac-script')"))
        assertTrue(html.contains("oldScript.parentNode.removeChild(oldScript)"))
        assertTrue(html.contains("script.id = 'runninghub-tac-script'"))
        assertTrue(html.contains("window.__initSmsCaptcha = initCaptcha"))
        assertTrue(html.contains("window.__loadSmsCaptchaScript = loadCaptchaScript"))
    }
}
