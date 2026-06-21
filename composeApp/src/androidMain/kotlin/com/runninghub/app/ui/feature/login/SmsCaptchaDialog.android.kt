package com.runninghub.app.ui.feature.login

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

/**
 * Android 平台的短信图形验证码弹窗。
 *
 * 这里通过 WebView 复用 RunningHub 网页端 TAC 组件，避免客户端重新实现旋转验证码、
 * 轨迹采集和校验请求。WebView 仅加载受控 HTML，不暴露认证 token、Cookie 或 API Key。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun SmsCaptchaDialog(
    phone: String,
    onToken: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bridge = remember(onToken, onDismiss) {
        SmsCaptchaBridge(
            onVerifiedToken = onToken,
            onDismiss = onDismiss,
        )
    }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = SmsCaptchaWebViewClient(bridge)
            addJavascriptInterface(bridge, CAPTCHA_BRIDGE_NAME)
            // 使用 runninghub.cn 作为 baseUrl，使 TAC 脚本内的相对接口保持与网页端同源。
            loadDataWithBaseURL(
                CAPTCHA_BASE_URL,
                smsCaptchaHtml(
                    tokenCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://token?value=' + encodeURIComponent(token || '')",
                    closeCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://close'",
                ),
                "text/html",
                "UTF-8",
                null,
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            // 弹窗关闭时销毁 WebView，避免验证码脚本和 JS bridge 持有过期页面回调。
            webView.removeJavascriptInterface(CAPTCHA_BRIDGE_NAME)
            webView.destroy()
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
                AndroidView(
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
 * WebView 暴露给 TAC HTML 的最小 JS Bridge。
 *
 * Bridge 只接收验证码 token 和关闭事件，不允许网页读取本地认证状态或执行任意原生能力。
 */
private class SmsCaptchaBridge(
    private val onVerifiedToken: (String?) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * TAC 校验成功后把 `validToken` 回传给登录页。
     *
     * Android 的 JavaScript bridge 可能在 WebView 后台线程回调，此处切回主线程，
     * 避免直接从桥接线程修改 Compose 状态或启动 ScreenModel 业务流程。
     */
    @JavascriptInterface
    fun onToken(token: String?) {
        // JS 暴露方法也叫 onToken，回调字段必须使用不同名称，避免 lambda 内再次解析到本方法造成递归。
        mainHandler.post { onVerifiedToken(token) }
    }

    /**
     * TAC 内部关闭按钮触发时同步关闭 Compose 弹窗。
     *
     * 关闭动作同样切回主线程执行，保持与 Compose UI 状态更新的线程约束一致。
     */
    @JavascriptInterface
    fun onClose() {
        mainHandler.post { onDismiss() }
    }
}

/**
 * Android WebView 的导航拦截器。
 *
 * 验证页通过自定义 scheme 把 token 和关闭事件带回原生层；相比直接依赖 JS bridge，
 * 这种方式更容易观察、拦截，也能规避部分 WebView 线程和对象暴露差异。
 */
private class SmsCaptchaWebViewClient(
    private val bridge: SmsCaptchaBridge,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean = request?.url?.let(::handleCallbackUrl) ?: false

    @Suppress("OVERRIDE_DEPRECATION")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?,
    ): Boolean = url?.let { handleCallbackUrl(Uri.parse(it)) } ?: false

    private fun handleCallbackUrl(url: Uri): Boolean {
        if (url.scheme != CAPTCHA_CALLBACK_SCHEME) return false
        when (url.host) {
            "token" -> bridge.onToken(url.getQueryParameter("value"))
            "close" -> bridge.onClose()
        }
        return true
    }
}
