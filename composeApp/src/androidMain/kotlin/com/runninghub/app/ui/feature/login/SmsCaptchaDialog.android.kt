package com.runninghub.app.ui.feature.login

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.core.network.RunningHubApiEnvironment

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
    copy: SmsCaptchaCopy,
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
            // 使用当前运行环境作为 baseUrl，使 TAC 脚本内的相对接口和短信发送接口保持同源。
            // Android 侧不注册 addJavascriptInterface，避免把原生对象暴露给网页；
            // token 和关闭事件统一走自定义 scheme，由 WebViewClient 在原生层拦截。
            loadDataWithBaseURL(
                smsCaptchaBaseUrl(),
                smsCaptchaHtml(
                    tokenCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://token?value=' + encodeURIComponent(token || '')",
                    closeCallbackExpression = "window.location.href = '$CAPTCHA_CALLBACK_SCHEME://close'",
                    copy = copy,
                ),
                "text/html",
                "UTF-8",
                null,
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            // 弹窗关闭时销毁 WebView，避免验证码脚本持有过期页面回调。
            bridge.dispose()
            webView.destroy()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(360.dp),
            shape = RoundedCornerShape(RhTheme.shapes.lg),
            color = RhTheme.colors.surfaceElevated,
        ) {
            Column(
                modifier = Modifier.padding(RhSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                Text(
                    text = copy.dialogTitle,
                    style = RhTypography.sectionTitle,
                    color = RhTheme.colors.textPrimary,
                )
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
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = RhTheme.colors.textSecondary,
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Text(text = copy.dismissAction, style = RhTypography.button)
                    }
                }
            }
        }
    }
}

/**
 * 返回 Android WebView 加载验证码 HTML 时使用的同源根地址。
 *
 * 地址来自平台启动层注入的 [RunningHubApiEnvironment]，而不是固定生产域名；
 * debug/staging 环境下验证码 token 必须由同一个用户中心环境签发，才能被后续发送短信接口接受。
 */
internal actual fun smsCaptchaBaseUrl(): String =
    RunningHubApiEnvironment.WEB_BASE_URL

/**
 * WebView 验证码回调适配器。
 *
 * 本对象不通过 `addJavascriptInterface` 暴露给网页，只由 [SmsCaptchaWebViewClient]
 * 在拦截自定义 scheme 后调用。这样既复用同一套 token/关闭回调，又避免网页获得任意原生对象入口。
 */
private class SmsCaptchaBridge(
    private val onVerifiedToken: (String?) -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isActive = true

    /**
     * 让已经关闭的验证码容器失效。
     *
     * WebView 的 scheme 拦截回调会异步切回主线程；弹窗关闭后必须同时清理已排队的回调，
     * 避免旧验证码实例在用户取消或重新打开后继续触发短信发送。
     */
    fun dispose() {
        isActive = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     * TAC 校验成功后把 `validToken` 回传给登录页。
     *
     * WebViewClient 拦截回调 URL 后可能不处于 Compose 状态更新期；这里统一切回主线程，
     * 避免直接从 WebView 回调栈修改 Compose 状态或启动 ScreenModel 业务流程。
     */
    fun onToken(token: String?) {
        if (!isActive) return
        mainHandler.post {
            if (isActive) {
                onVerifiedToken(token)
            }
        }
    }

    /**
     * TAC 内部关闭按钮触发时同步关闭 Compose 弹窗。
     *
     * 关闭动作同样切回主线程执行，保持与 Compose UI 状态更新的线程约束一致。
     */
    fun onClose() {
        if (!isActive) return
        mainHandler.post {
            if (isActive) {
                onDismiss()
            }
        }
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
    ): Boolean = request?.url?.toString()?.let(::handleCallbackUrl) ?: false

    @Suppress("OVERRIDE_DEPRECATION")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?,
    ): Boolean = url?.let(::handleCallbackUrl) ?: false

    private fun handleCallbackUrl(url: String): Boolean =
        handleSmsCaptchaCallbackUrl(
            rawUrl = url,
            onToken = bridge::onToken,
            onClose = bridge::onClose,
        )
}
