package com.runninghub.feature.auth.presentation.login

import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.SmsError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录页面状态。
 *
 * 该状态属于 Auth Presentation 层，只保存页面渲染和交互所需的信息，
 * 不直接暴露 Token、Cookie 或 API Key。登录成功后的用户信息用于页面跳转前的即时展示，
 * 持久会话由 [AuthRepository] 的实现层维护。
 *
 * @property phone 用户当前输入的中国大陆手机号，不包含界面固定展示的 `+86` 前缀。
 * 当前校验规则为至少 11 位且全部为数字；空字符串表示用户尚未输入。
 * @property smsCode 用户当前输入的短信验证码。StateHolder 只保留数字并限制最多 6 位；
 * 空字符串表示尚未输入，或验证码错误后已按业务规则清空。
 * @property password 用户当前输入的明文密码，仅用于本次密码登录请求。
 * 空字符串表示尚未输入；该值不得持久化、打印到日志或写入错误报告。
 * @property isSmsMode 当前登录方式。`true` 表示短信验证码登录；`false` 表示密码登录。
 * @property isLoading 是否正在提交登录请求。`true` 时登录按钮应禁用；
 * `false` 表示当前没有进行中的登录请求。该字段不代表发送验证码流程。
 * @property isSendingCode 是否正在请求发送短信验证码。`true` 时发送按钮应禁用；
 * 请求成功或失败后恢复为 `false`。
 * @property countdownSeconds 再次发送验证码前的剩余等待秒数，单位为秒。
 * `0` 表示允许重新发送；大于 `0` 时发送按钮保持禁用，不允许出现负数。
 * @property error 等待页面展示的一次性稳定错误原因。
 * `null` 表示当前没有待展示错误；非空时由 composeApp 映射最终文案，
 * 并在用户处理后调用 [LoginStateHolder.clearError] 清理。
 * @property requiresSmsCaptcha 是否需要展示短信发送前的图形验证码。
 * `true` 表示用户中心拒绝了无 token 的短信发送请求，页面应打开 TAC 验证容器；
 * `false` 表示当前可以直接展示普通登录表单。该状态只控制 UI，不保存验证码 token。
 * @property user 登录成功后服务端返回并映射得到的当前用户信息。
 * 登录前以及登录失败时为 `null`；根导航不得依赖此字段，只能观察 SessionManager。
 */
data class LoginUiState(
    val phone: String = "",
    val smsCode: String = "",
    val password: String = "",
    val isSmsMode: Boolean = true,
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val error: LoginErrorText? = null,
    val requiresSmsCaptcha: Boolean = false,
    val user: User? = null,
)

/**
 * 登录页错误提示的稳定语义。
 *
 * Auth Presentation 层只保存错误原因，不直接生成最终中文文案；
 * composeApp 负责将这些原因映射到 Compose Resources。这样可以避免远端 `msg`、
 * 底层异常 `message` 或本地调试文本进入页面状态和 Snackbar。
 */
enum class LoginErrorText {
    /** 手机号为空，用户尚未输入登录手机号。 */
    PhoneRequired,

    /** 手机号不满足当前大陆手机号长度和数字校验规则。 */
    InvalidPhone,

    /** 短信验证码为空，用户尚未输入验证码。 */
    SmsCodeRequired,

    /** 短信验证码长度不足，暂不允许提交短信登录请求。 */
    SmsCodeIncomplete,

    /** 密码为空，用户尚未输入密码登录凭据。 */
    PasswordRequired,

    /** 图形验证码回调缺少有效 token，需要用户重新完成验证。 */
    CaptchaInvalid,

    /** 服务端判定短信验证码错误，页面会同步清空验证码输入。 */
    WrongSmsCode,

    /** 服务端判定短信验证码已过期，页面会同步重置发送倒计时。 */
    SmsCodeExpired,

    /** 服务端判定手机号未注册 RunningHub 账号。 */
    AccountNotFound,

    /** 服务端判定短信发送过于频繁，需要用户稍后重试。 */
    SmsRateLimited,

    /** 服务端判定当天短信发送次数已达上限。 */
    SmsDailyLimit,

    /** 服务端要求先完成图形验证码后才能继续发送短信。 */
    CaptchaRequired,

    /** 网络连接失败或请求无法到达服务端。 */
    Network,

    /** 未知认证失败兜底，不暴露远端消息或底层异常详情。 */
    LoginFailed,
}

/**
 * 持有登录页状态并协调短信验证码、短信登录和密码登录流程。
 *
 * 本类位于 Auth Presentation 层，依赖 Auth Domain 的仓库契约，不依赖 Compose、Voyager、
 * WebView 或平台验证码容器。应用壳负责传入页面生命周期绑定的 [coroutineScope]，
 * 平台验证码 UI 只需要在拿到 token 后调用 [onSmsCaptchaVerified]。
 *
 * 并发约束：
 * - 发送验证码期间通过 [LoginUiState.isSendingCode] 阻止重复反馈。
 * - 倒计时使用外部传入的 [coroutineScope] 绑定页面生命周期。
 * - 短信登录失败时根据错误语义决定是否清空验证码或重置倒计时。
 *
 * @param authRepository 认证领域仓库，负责隐藏远程接口、会话写入和错误语义映射。
 * @param coroutineScope 与页面或测试生命周期绑定的协程作用域。
 */
class LoginStateHolder(
    private val authRepository: AuthRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(LoginUiState())

    /**
     * 登录页面只读状态流。
     *
     * UI 只能收集该状态并通过公开动作函数回传用户操作，不得直接修改内部 [MutableStateFlow]。
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** 更新手机号输入，并清理旧错误，避免用户修正输入后仍看到过期提示。 */
    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, error = null) }
    }

    /** 更新短信验证码输入，仅保留数字并限制长度，避免无效字符进入登录请求。 */
    fun onSmsCodeChanged(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(smsCode = filtered, error = null) }
    }

    /**
     * 请求发送短信验证码。
     *
     * 本地先完成手机号校验和倒计时检查，避免无效请求进入远程短信发送链路。
     *
     * @param captchaToken 图形验证码容器回传的短生命周期 token；`null` 表示普通发送入口。
     */
    fun sendSmsCode(captchaToken: String? = null) {
        val phone = _uiState.value.phone
        if (phone.isBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.PhoneRequired) }
            return
        }
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(error = LoginErrorText.InvalidPhone) }
            return
        }
        if (_uiState.value.countdownSeconds > 0) return

        coroutineScope.launch {
            _uiState.update {
                it.copy(
                    isSendingCode = true,
                    error = null,
                    requiresSmsCaptcha = false,
                )
            }
            authRepository.sendSmsCode(phone, captchaToken)
                .onSuccess {
                    _uiState.update { it.copy(isSendingCode = false, countdownSeconds = 60) }
                    startCountdown()
                }
                .onFailure { e ->
                    val captchaRequired = e is SmsError.CaptchaRequired
                    _uiState.update {
                        it.copy(
                            isSendingCode = false,
                            error = e.toLoginErrorText(),
                            // 用户中心当前要求先完成 TAC 图形验证，页面拿到 token 后会重试本次发送。
                            requiresSmsCaptcha = captchaRequired,
                        )
                    }
                }
        }
    }

    /**
     * 处理图形验证码成功回传的 token。
     *
     * token 只参与下一次短信发送请求，不写入页面状态，避免短生命周期校验值被重组、
     * 日志或状态持久化误用。成功回调先关闭验证码弹窗状态，再重试短信发送；
     * 这样即使远程短信请求稍后失败，也不会让已经通过的 TAC 弹窗停留在前台。
     * 若 token 为空，保持验证码弹窗打开并提示用户重试。
     *
     * @param token 图形验证码服务回传的发送短信授权 token；空白值视为验证失败。
     */
    fun onSmsCaptchaVerified(token: String?) {
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.CaptchaInvalid) }
            return
        }
        _uiState.update { it.copy(requiresSmsCaptcha = false, error = null) }
        sendSmsCode(captchaToken = token)
    }

    /** 用户关闭图形验证码时只退出弹窗，不清空手机号和验证码输入。 */
    fun dismissSmsCaptcha() {
        _uiState.update { it.copy(requiresSmsCaptcha = false) }
    }

    /**
     * 使用短信验证码登录。
     *
     * 验证码错误会清空输入，过期会重置倒计时；网络错误保留用户输入，方便恢复网络后重试。
     */
    fun smsLogin() {
        val state = _uiState.value
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.PhoneRequired) }
            return
        }
        if (!isValidPhone(state.phone)) {
            _uiState.update { it.copy(error = LoginErrorText.InvalidPhone) }
            return
        }
        if (state.smsCode.isBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.SmsCodeRequired) }
            return
        }
        if (state.smsCode.length < 4) {
            _uiState.update { it.copy(error = LoginErrorText.SmsCodeIncomplete) }
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.smsLogin(state.phone, state.smsCode)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(isLoading = false, user = user)
                    }
                }
                .onFailure { e ->
                    val codeExpired = e is SmsError.CodeExpired
                    val wrongCode = e is SmsError.WrongCode
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.toLoginErrorText(),
                            // 错码清空输入，过期和网络失败保留输入，减少用户重复输入成本。
                            smsCode = if (wrongCode) "" else it.smsCode,
                            countdownSeconds = if (codeExpired) 0 else it.countdownSeconds,
                        )
                    }
                }
        }
    }

    /** 更新密码输入，并清理旧错误提示。 */
    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    /** 切换短信/密码登录模式，模式切换不清空输入，避免误删用户已填写内容。 */
    fun toggleMode() {
        _uiState.update { it.copy(isSmsMode = !it.isSmsMode, error = null) }
    }

    /**
     * 使用手机号和密码登录。
     *
     * 该路径仍复用 [AuthRepository] 的认证契约；成功后由仓库实现写入 SessionManager，
     * 根 App 统一观察会话状态并完成导航。
     */
    fun pwdLogin() {
        val state = _uiState.value
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.PhoneRequired) }
            return
        }
        if (!isValidPhone(state.phone)) {
            _uiState.update { it.copy(error = LoginErrorText.InvalidPhone) }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = LoginErrorText.PasswordRequired) }
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(state.phone, state.password)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(isLoading = false, user = user)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.toLoginErrorText(),
                        )
                    }
                }
        }
    }

    /** 清理当前错误提示，用于用户关闭提示或重新编辑输入后的状态恢复。 */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun startCountdown() {
        coroutineScope.launch {
            // 倒计时只由发送验证码成功后启动，避免失败请求也锁住重新发送入口。
            while (_uiState.value.countdownSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(countdownSeconds = it.countdownSeconds - 1) }
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 11 && phone.all { it.isDigit() }
    }

    private fun Throwable.toLoginErrorText(): LoginErrorText {
        return when (this) {
            is SmsError.WrongCode -> LoginErrorText.WrongSmsCode
            is SmsError.CodeExpired -> LoginErrorText.SmsCodeExpired
            is SmsError.AccountNotFound -> LoginErrorText.AccountNotFound
            is SmsError.RateLimited -> LoginErrorText.SmsRateLimited
            is SmsError.DailyLimit -> LoginErrorText.SmsDailyLimit
            is SmsError.CaptchaRequired -> LoginErrorText.CaptchaRequired
            is SmsError.Network -> LoginErrorText.Network
            is SmsError.Unknown -> LoginErrorText.LoginFailed
            is AuthError.Network -> LoginErrorText.Network
            is AuthError.Unknown -> LoginErrorText.LoginFailed
            // 未知异常可能来自远端 msg、HTTP 诊断或底层 SDK 文本，不能进入 Snackbar。
            else -> LoginErrorText.LoginFailed
        }
    }
}
