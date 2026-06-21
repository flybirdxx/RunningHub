package com.runninghub.app.ui.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS 平台的短信图形验证码弹窗。
 *
 * 这里通过 WKWebView 复用 RunningHub 网页端 TAC 组件，保持与 Android 相同的预加载、
 * 图片解码和失败重试策略。WKWebView 只加载受控 HTML，并通过脚本消息回传短生命周期
 * `validToken`，不暴露本地会话、Cookie、Token 或 API Key。
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SmsCaptchaDialog(
    phone: String,
    onToken: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val messageHandler = remember(onToken, onDismiss) {
        SmsCaptchaMessageHandler(
            onToken = onToken,
            onDismiss = onDismiss,
        )
    }
    val webView = remember(messageHandler) {
        val userContentController = WKUserContentController()
        userContentController.addScriptMessageHandler(
            scriptMessageHandler = messageHandler,
            name = CAPTCHA_BRIDGE_NAME,
        )
        val configuration = WKWebViewConfiguration().apply {
            this.userContentController = userContentController
        }
        WKWebView(
            frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
            configuration = configuration,
        ).apply {
            opaque = false
            scrollView.scrollEnabled = false
            loadHTMLString(
                string = smsCaptchaHtml(
                    tokenCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://token?value=' + encodeURIComponent(token || '')",
                    closeCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://close'",
                ),
                baseURL = NSURL.URLWithString(CAPTCHA_BASE_URL),
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            // 弹窗销毁时移除脚本消息处理器，避免 WKWebView 持有过期的 Compose 回调。
            webView.configuration.userContentController.removeScriptMessageHandlerForName(CAPTCHA_BRIDGE_NAME)
            webView.stopLoading()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(360.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("完成图形验证")
                UIKitView(
                    factory = { webView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

/**
 * WKWebView 暴露给 TAC HTML 的最小脚本消息处理器。
 *
 * 处理器只识别 `token:` 和 `close` 两类消息，避免网页侧获得任意原生能力。
 * 当前优先使用 `window.webkit.messageHandlers` 回调原生层；自定义 scheme 仍作为 HTML 兜底保留，
 * 但不在此处直接依赖未稳定验证的导航拦截链路。
 */
@OptIn(ExperimentalForeignApi::class)
private class SmsCaptchaMessageHandler(
    private val onToken: (String?) -> Unit,
    private val onDismiss: () -> Unit,
) : NSObject(), WKScriptMessageHandlerProtocol {
    /**
     * 接收 TAC HTML 发出的脚本消息。
     *
     * `token:` 后缀为空表示服务端未返回有效 `validToken`，调用方会按验证失败处理；
     * `close` 表示用户点击 TAC 内部关闭按钮，需要退出当前弹窗。
     * WKWebView 传入的 body 在 Kotlin/Native 中可能表现为 NSString 或其他 Foundation 对象，
     * 因此这里统一使用 `toString()` 解析，避免字符串强转失败导致验证成功后没有后续动作。
     */
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        val message = didReceiveScriptMessage.body.toString()
        when {
            message == "close" -> dispatch_async(dispatch_get_main_queue()) {
                onDismiss()
            }
            message.startsWith("token:") -> {
                val token = message.removePrefix("token:").takeIf { it.isNotBlank() }
                dispatch_async(dispatch_get_main_queue()) {
                    onToken(token)
                }
            }
        }
    }
}
