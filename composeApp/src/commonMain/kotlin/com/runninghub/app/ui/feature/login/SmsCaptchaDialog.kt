package com.runninghub.app.ui.feature.login

import androidx.compose.runtime.Composable

/**
 * 展示短信发送前的 TAC 图形验证码。
 *
 * 该组件位于 Presentation 层，但具体渲染依赖平台 Web 容器：Android 使用 WebView，
 * iOS 后续可替换为 WKWebView。commonMain 只暴露 token 回调，避免把平台类型泄漏到
 * 登录页状态或 ScreenModel。
 *
 * @param phone 当前请求发送短信的中国大陆手机号，仅用于平台容器展示上下文。
 * 组件不得把手机号写入日志；真正发送短信仍由 [LoginScreenModel] 在拿到 token 后完成。
 * @param onToken TAC 校验成功后返回网页端 `validToken`。
 * token 属于短生命周期校验值，只能用于下一次 `/uc/sendSms` 请求。
 * @param onDismiss 用户主动关闭验证码容器时触发，调用方应退出弹窗但保留登录表单输入。
 */
@Composable
expect fun SmsCaptchaDialog(
    phone: String,
    onToken: (String?) -> Unit,
    onDismiss: () -> Unit,
)
