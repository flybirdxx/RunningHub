package com.runninghub.app.ui.feature.login

/**
 * 短信图形验证码从 Web 容器回传给原生层的事件。
 *
 * 该类型只描述 TAC 弹窗内部的短生命周期回调，不承载手机号、短信验证码、Token、
 * Cookie 或其他认证状态。Android WebView 与 iOS WKWebView 都通过它统一解析自定义
 * scheme，避免两个平台对 token 为空、关闭按钮或未知导航产生不同语义。
 */
internal sealed interface SmsCaptchaCallback {
    /**
     * TAC 校验成功后的短信凭证事件。
     *
     * @property value 网页端 `/uc/checkCaptcha` 返回的 `validToken`。
     * `null` 表示网页侧触发了成功回调但没有提供有效凭证，调用方应保留验证码流程并提示重试；
     * 非空值只允许用于下一次 `/uc/sendSms` 请求，不得持久化或输出到日志。
     */
    data class Token(val value: String?) : SmsCaptchaCallback

    /**
     * 用户主动关闭验证码弹窗。
     *
     * 该事件只关闭验证码容器，不重试短信发送，也不清空登录页已有输入。
     */
    data object Close : SmsCaptchaCallback
}

/**
 * 解析 TAC HTML 通过自定义 scheme 发出的原生回调。
 *
 * @param rawUrl WebView/WKWebView 拦截到的完整导航 URL，可能是验证码回调，也可能是普通
 * HTTPS 资源请求。
 * @return 返回 [SmsCaptchaCallback] 表示应由原生层消费该导航；返回 `null` 表示不是验证码
 * 回调，平台 Web 容器应继续按普通资源或页面导航处理。
 */
internal fun parseSmsCaptchaCallbackUrl(rawUrl: String): SmsCaptchaCallback? {
    val prefix = "$CAPTCHA_CALLBACK_SCHEME://"
    if (!rawUrl.startsWith(prefix)) return null

    val remainder = rawUrl.removePrefix(prefix)
    val host = remainder.substringBefore('?').substringBefore('/')
    return when (host) {
        "token" -> SmsCaptchaCallback.Token(
            value = queryParameter(remainder, "value")?.takeIf { it.isNotBlank() },
        )
        "close" -> SmsCaptchaCallback.Close
        else -> null
    }
}

/**
 * 处理验证码自定义 scheme 回调。
 *
 * 平台层调用本函数后以返回值决定是否拦截当前导航。token 与关闭事件的实际业务动作仍由
 * Compose/ScreenModel 回调完成，解析层不直接修改页面状态。
 */
internal fun handleSmsCaptchaCallbackUrl(
    rawUrl: String,
    onToken: (String?) -> Unit,
    onClose: () -> Unit,
): Boolean {
    return when (val callback = parseSmsCaptchaCallbackUrl(rawUrl)) {
        is SmsCaptchaCallback.Token -> {
            onToken(callback.value)
            true
        }
        SmsCaptchaCallback.Close -> {
            onClose()
            true
        }
        null -> false
    }
}

private fun queryParameter(urlRemainder: String, name: String): String? {
    val query = urlRemainder.substringAfter('?', missingDelimiterValue = "")
    if (query.isEmpty()) return null

    return query
        .split('&')
        .firstNotNullOfOrNull { parameter ->
            val key = parameter.substringBefore('=')
            if (decodePercentEncoded(key) == name) {
                decodePercentEncoded(parameter.substringAfter('=', missingDelimiterValue = ""))
            } else {
                null
            }
        }
}

private fun decodePercentEncoded(value: String): String {
    val output = StringBuilder(value.length)
    val pendingBytes = mutableListOf<Byte>()

    fun flushBytes() {
        if (pendingBytes.isEmpty()) return
        output.append(pendingBytes.toByteArray().decodeToString())
        pendingBytes.clear()
    }

    var index = 0
    while (index < value.length) {
        val char = value[index]
        val high = value.getOrNull(index + 1)?.hexValue()
        val low = value.getOrNull(index + 2)?.hexValue()
        if (char == '%' && high != null && low != null) {
            pendingBytes += ((high shl 4) + low).toByte()
            index += 3
        } else {
            flushBytes()
            output.append(char)
            index += 1
        }
    }
    flushBytes()
    return output.toString()
}

private fun Char.hexValue(): Int? = when (this) {
    in '0'..'9' -> this - '0'
    in 'a'..'f' -> this - 'a' + 10
    in 'A'..'F' -> this - 'A' + 10
    else -> null
}
