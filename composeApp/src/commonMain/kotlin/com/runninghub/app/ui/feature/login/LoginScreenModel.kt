package com.runninghub.app.ui.feature.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.presentation.login.LoginStateHolder
import com.runninghub.feature.auth.presentation.login.LoginUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 登录页 Voyager ScreenModel 适配器。
 *
 * 本类只把 Voyager 生命周期作用域传入 [LoginStateHolder]，并保持历史 UI 入口的方法名稳定。
 * 登录校验、验证码 token 处理、错误文案映射和倒计时状态均由 Auth Presentation 层维护，
 * 避免 composeApp 继续承载可测试的认证状态机。
 *
 * @param authRepository 认证领域仓库，由 Koin 从数据层实现绑定到领域接口。
 */
class LoginScreenModel(
    private val authRepository: AuthRepository
) : ScreenModel {
    private val stateHolder = LoginStateHolder(
        authRepository = authRepository,
        coroutineScope = screenModelScope,
    )

    /**
     * 登录页面只读状态流。
     *
     * 状态来源于 [LoginStateHolder]，ScreenModel 不直接修改内部状态，保持应用壳只做框架适配。
     */
    val uiState: StateFlow<LoginUiState> = stateHolder.uiState

    /** 更新手机号输入，并清理旧错误，避免用户修正输入后仍看到过期提示。 */
    fun onPhoneChanged(phone: String) {
        stateHolder.onPhoneChanged(phone)
    }

    /** 更新短信验证码输入，仅保留数字并限制长度，避免无效字符进入登录请求。 */
    fun onSmsCodeChanged(code: String) {
        stateHolder.onSmsCodeChanged(code)
    }

    /**
     * 请求发送短信验证码。
     *
     * 本地先完成手机号校验和倒计时检查，避免无效请求进入远程短信发送链路。
     *
     * @param captchaToken 图形验证码容器回传的短生命周期 token；`null` 表示普通发送入口。
     */
    fun sendSmsCode(captchaToken: String? = null) {
        stateHolder.sendSmsCode(captchaToken)
    }

    /**
     * 处理图形验证码成功回传的 token。
     *
     * token 只参与下一次短信发送请求，不写入页面状态，避免短生命周期校验值被重组、
     * 日志或状态持久化误用。成功回调先关闭验证码弹窗状态，再重试短信发送；
     * 这样即使远程短信请求稍后失败，也不会让已经通过的 TAC 弹窗停留在前台。
     * 若 token 为空，保持验证码弹窗打开并提示用户重试。
     */
    fun onSmsCaptchaVerified(token: String?) {
        stateHolder.onSmsCaptchaVerified(token)
    }

    /** 用户关闭图形验证码时只退出弹窗，不清空手机号和验证码输入。 */
    fun dismissSmsCaptcha() {
        stateHolder.dismissSmsCaptcha()
    }

    /**
     * 使用短信验证码登录。
     *
     * 验证码错误会清空输入，过期会重置倒计时；网络错误保留用户输入，方便恢复网络后重试。
     */
    fun smsLogin() {
        stateHolder.smsLogin()
    }

    /** 更新密码输入，并清理旧错误提示。 */
    fun onPasswordChanged(password: String) {
        stateHolder.onPasswordChanged(password)
    }

    /** 切换短信/密码登录模式，模式切换不清空输入，避免误删用户已填写内容。 */
    fun toggleMode() {
        stateHolder.toggleMode()
    }

    /**
     * 使用手机号和密码登录。
     *
     * 该路径仍复用 AuthRepository 的认证契约；成功后只写入 SessionManager，由根 App 统一导航。
     */
    fun pwdLogin() {
        stateHolder.pwdLogin()
    }

    /** 清理当前错误提示，用于用户关闭提示或重新编辑输入后的状态恢复。 */
    fun clearError() {
        stateHolder.clearError()
    }
}
