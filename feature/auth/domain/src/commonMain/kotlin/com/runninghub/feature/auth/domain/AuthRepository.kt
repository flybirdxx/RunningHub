package com.runninghub.feature.auth.domain

import com.runninghub.core.model.User

/**
 * 描述通用认证链路的稳定业务错误。
 *
 * 该类型用于密码登录、令牌恢复等非短信专属流程。它只表达错误语义，
 * 不包含最终用户可见中文文案；Presentation 层负责根据页面上下文映射提示。
 *
 * @property code 稳定错误码，用于测试、日志分类和 Presentation 分支判断。
 * @property serverMessage 服务端原始消息或客户端诊断信息，仅用于降级和排查。
 */
sealed class AuthError(
    val code: String,
    val serverMessage: String? = null,
) : RuntimeException(serverMessage ?: code) {
    /** 网络不可用、连接超时或 DNS 解析失败，调用方通常应保留用户输入以便重试。 */
    class Network : AuthError("NETWORK_UNAVAILABLE")

    /** 登录接口返回成功 code 但缺少 token data，调用方应视为认证失败并允许重试。 */
    class EmptyLoginResponse : AuthError("EMPTY_LOGIN_RESPONSE")

    /** 登录或刷新成功路径缺少可用 access token，调用方不得继续进入已认证会话。 */
    class MissingAccessToken : AuthError("MISSING_ACCESS_TOKEN")

    /** 本地 token 过期后刷新失败，调用方应进入会话过期或重新登录流程。 */
    class TokenRefreshFailed : AuthError("TOKEN_REFRESH_FAILED")

    /** 用户资料接口返回成功 code 但缺少 user data，调用方不得把会话标记为已认证。 */
    class EmptyUserResponse : AuthError("EMPTY_USER_RESPONSE")

    /**
     * 未被客户端稳定分类的认证失败。
     *
     * @param serverMessage 服务端原始消息或客户端诊断信息，不应直接视为最终 UI 文案。
     */
    class Unknown(serverMessage: String?) : AuthError("UNKNOWN_AUTH_ERROR", serverMessage)
}

/**
 * 描述短信登录链路的稳定业务错误。
 *
 * 该类型位于 Auth Domain 层，只表达服务端返回的错误语义，不携带最终用户可见中文文案。
 * Presentation 层应根据具体页面和资源体系把错误映射成展示文本，避免 Domain 反向依赖 UI 文案。
 *
 * @property code 稳定错误码，用于日志、测试和 Presentation 层分支判断。
 * 该值不包含敏感凭据，可以安全用于非用户可见的状态判断。
 * @property serverMessage 服务端原始消息，仅作为诊断信息保留。
 * `null` 表示客户端已能通过 [code] 完整表达错误语义；调用方不能把它视为最终 UI 文案。
 */
sealed class SmsError(
    val code: String,
    val serverMessage: String? = null,
) : RuntimeException(serverMessage ?: code) {
    /** 验证码错误，Presentation 层通常需要清空已输入验证码。 */
    class WrongCode : SmsError("SMS_CODE_INVALID")

    /** 验证码过期，Presentation 层通常需要停止倒计时并允许重新获取验证码。 */
    class CodeExpired : SmsError("SMS_CODE_EXPIRED")

    /** 手机号未绑定 RunningHub 账号。 */
    class AccountNotFound : SmsError("ACCOUNT_NOT_FOUND")

    /** 短时间内请求短信过于频繁。 */
    class RateLimited : SmsError("SMS_SEND_TOO_FREQUENT")

    /** 当日短信发送次数达到服务端限制。 */
    class DailyLimit : SmsError("SMS_DAILY_LIMIT")

    /**
     * 服务端要求先完成图形验证码。
     *
     * 当前网页端在发送短信前会完成 TAC 滑块校验，并把返回的 `validToken`
     * 作为 `sendSms` 请求的 `token` 字段提交。Presentation 层收到该错误后应打开
     * 图形验证码流程，拿到 token 后再重试发送短信。
     */
    class CaptchaRequired : SmsError("CAPTCHA_VERIFY_ERROR")

    /** 网络不可用、超时或 DNS 解析失败，调用方应保留用户输入以便重试。 */
    class Network : SmsError("NETWORK_UNAVAILABLE")

    /**
     * 服务端返回了未被客户端识别的登录错误。
     *
     * @param serverMessage 服务端原始错误消息，仅用于诊断或降级展示。
     */
    class Unknown(serverMessage: String?) : SmsError("UNKNOWN_AUTH_ERROR", serverMessage)
}

/**
 * Auth 功能的 Domain Repository 契约。
 *
 * 接口只描述登录、短信验证码、令牌刷新和当前会话查询能力；具体网络请求、
 * 凭证持久化和错误映射由 Data 层实现。调用方应通过 Result 处理业务失败，
 * 不应假设实现会抛出平台或网络库的具体异常。
 */
interface AuthRepository {
    /** 使用手机号和密码登录，成功时返回当前用户信息；失败时可能返回 [AuthError]。 */
    suspend fun login(phone: String, password: String): Result<User>

    /**
     * 请求发送短信验证码。
     *
     * @param phone 用户输入的中国大陆手机号，不包含 `+86` 前缀。
     * 调用方应先完成基础格式校验，避免空字符串或明显非法号码进入 Data 层。
     * @param captchaToken TAC 图形验证码返回的 `validToken`。
     * `null` 表示首次尝试或当前环境尚未取得验证码 token；服务端可能返回
     * [SmsError.CaptchaRequired] 要求调用方先完成图形验证后重试。
     * @return 发送请求成功被服务端接受时返回成功；失败时通过 [SmsError] 表达频控、
     * 图形验证、网络异常或验证码业务错误。
     */
    suspend fun sendSmsCode(phone: String, captchaToken: String? = null): Result<Unit>

    /** 使用手机号和短信验证码登录，成功后由实现层持久化访问令牌和刷新令牌。 */
    suspend fun smsLogin(phone: String, code: String): Result<User>

    /** 注销当前会话，并清理本地认证凭证。 */
    suspend fun logout()

    /** 返回本地是否存在可用于会话恢复的认证令牌。 */
    suspend fun isLoggedIn(): Boolean

    /** 在访问令牌过期或即将过期时刷新令牌，成功时返回可用访问令牌。 */
    suspend fun refreshTokenIfNeeded(): Result<String>

    /** 读取当前访问令牌；调用方不得把返回值写入日志或 UI。 */
    suspend fun getCurrentAuthToken(): String?

    /** 从当前令牌中解析用户 ID，解析失败时返回 null。 */
    suspend fun getCurrentUserId(): String?
}
