package com.runninghub.app.ui.feature.login

import androidx.compose.runtime.Composable

/**
 * 短信图形验证码 Web 容器需要展示的本地化文案。
 *
 * 这些字符串由 composeApp 的 Compose Resources 在 commonMain 解析后传入平台 Web 容器；
 * HTML 包装层只负责安全转义和插入 TAC 配置，不在生产代码中硬编码最终中文 UI 文案。
 *
 * @property dialogTitle 弹窗标题。
 * @property dismissAction 关闭弹窗按钮文案。
 * @property preparing 验证码脚本或图片加载前的等待提示。
 * @property retryAction 失败状态下重新加载验证码的按钮文案。
 * @property loadFailedRetry 验证码容器或 TAC 脚本不可用且可重试时的提示。
 * @property imageLoadFailedRetry 验证码背景图或旋转图解码失败时的提示。
 * @property loadFailedLater 验证码脚本不存在或暂时无法初始化时的稍后重试提示。
 * @property rotateTitle TAC 旋转验证码标题。
 * @property captchaLoadFailed TAC 内部验证码加载失败提示。
 * @property verifyFailed TAC 校验失败提示。
 * @property verifySuccess TAC 校验成功提示。
 * @property tokenMissingRetry TAC 已成功但未返回短信凭证时的重试提示。
 * @property initFailedRetry TAC 初始化抛出异常时的重试提示。
 * @property scriptLoadFailedRetry 外部脚本加载失败时的重试提示。
 * @property scriptTimeoutRetry 外部脚本加载超时时的重试提示。
 */
data class SmsCaptchaCopy(
    val dialogTitle: String,
    val dismissAction: String,
    val preparing: String,
    val retryAction: String,
    val loadFailedRetry: String,
    val imageLoadFailedRetry: String,
    val loadFailedLater: String,
    val rotateTitle: String,
    val captchaLoadFailed: String,
    val verifyFailed: String,
    val verifySuccess: String,
    val tokenMissingRetry: String,
    val initFailedRetry: String,
    val scriptLoadFailedRetry: String,
    val scriptTimeoutRetry: String,
)

/**
 * 展示短信发送前的 TAC 图形验证码。
 *
 * 该组件位于 Presentation 层，但具体渲染依赖平台 Web 容器：Android 使用 WebView，
 * iOS 后续可替换为 WKWebView。commonMain 只暴露 token 回调，避免把平台类型泄漏到
 * 登录页状态或 ScreenModel。
 *
 * @param phone 当前请求发送短信的中国大陆手机号，仅用于平台容器展示上下文。
 * 组件不得把手机号写入日志；真正发送短信仍由 [LoginScreenModel] 在拿到 token 后完成。
 * @param copy 图形验证码弹窗和 Web 容器使用的本地化文案。
 * @param onToken TAC 校验成功后返回网页端 `validToken`。
 * token 属于短生命周期校验值，只能用于下一次 `/uc/sendSms` 请求。
 * @param onDismiss 用户主动关闭验证码容器时触发，调用方应退出弹窗但保留登录表单输入。
 */
@Composable
expect fun SmsCaptchaDialog(
    phone: String,
    copy: SmsCaptchaCopy,
    onToken: (String?) -> Unit,
    onDismiss: () -> Unit,
)
