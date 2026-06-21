package com.runninghub.feature.auth.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用级会话状态。
 *
 * 该状态属于 Auth Domain，只描述应用入口应该展示登录页还是主页面，不携带 Token、Cookie、
 * API Key 或最终用户可见错误文案。Presentation 层只能观察状态并执行导航。
 */
sealed interface SessionState {
    /** 正在从本地凭据恢复会话，根入口应暂缓创建业务导航栈。 */
    data object Restoring : SessionState

    /** 本地存在可用于恢复或继续访问接口的会话凭据。 */
    data object Authenticated : SessionState

    /** 当前没有可恢复会话，应用应展示登录入口或游客入口。 */
    data object Unauthenticated : SessionState

    /** 运行中的会话已被远端判定失效，应用应清空业务页面栈并回到登录页。 */
    data object Expired : SessionState
}

/**
 * 会话恢复所需的领域数据边界。
 *
 * 实现层可以读取 CredentialStore、Keychain 或其他安全存储，但调用方只关心
 * 本地是否存在可尝试恢复的会话，不直接读取任何敏感凭据。
 */
interface SessionRestoreRepository {
    /**
     * 判断本地是否存在可恢复会话。
     *
     * @return `true` 表示应用启动时可以进入主页面并由后续请求刷新会话；
     * `false` 表示应进入登录页或游客模式。
     */
    suspend fun hasRestorableSession(): Boolean
}

/**
 * 管理当前应用会话的运行时失效状态。
 *
 * 本类位于 Auth Domain 边界，向 Data 层提供会话失效通知入口，向 Presentation 层提供只读状态。
 * 这样 401 拦截器、令牌刷新和页面导航都依赖同一个可注入实例，避免全局 object 在测试之间
 * 保留旧状态，也避免 UI 无法订阅 Repository 内部的失效事件。
 *
 * 并发约束：
 * - [StateFlow] 只表达“当前会话是否已经失效”，不携带用户可见文案。
 * - 多个并发请求重复调用 [expire] 是幂等的，页面只需要处理 true 状态。
 * - 登录成功或进入新的有效会话后，调用方必须通过 [resetExpiration] 清除旧失效标记。
 */
class SessionManager(
    private val restoreRepository: SessionRestoreRepository? = null,
) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    private val _isExpired = MutableStateFlow(false)

    /**
     * 当前应用入口应遵循的会话状态。
     *
     * App 根层观察该状态来选择 Login 或 Main；业务页面不得直接写入状态。
     */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /**
     * 当前会话是否已经失效。
     *
     * Presentation 层应只订阅该只读流并执行导航或状态重置；不要从 UI 直接修改会话状态。
     */
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    /**
     * 从本地凭据恢复应用会话。
     *
     * 该方法只判断是否存在可恢复凭据，不在 Domain 层执行网络刷新；这样可以避免根 Composable
     * 直接调用 AuthRepository，同时把真正的令牌刷新继续交给网络拦截器和认证仓库。
     */
    suspend fun restore() {
        _state.value = SessionState.Restoring
        _isExpired.value = false
        _state.value = try {
            if (restoreRepository?.hasRestorableSession() == true) {
                SessionState.Authenticated
            } else {
                SessionState.Unauthenticated
            }
        } catch (_: Exception) {
            // 恢复阶段读取本地凭据失败时保守回到登录页，避免把未知状态误判为已认证。
            SessionState.Unauthenticated
        }
    }

    /**
     * 标记当前会话已经认证成功。
     *
     * 登录成功或令牌刷新成功后调用，确保根入口观察到已认证状态并切换到主页面。
     */
    fun markAuthenticated() {
        _state.value = SessionState.Authenticated
        _isExpired.value = false
    }

    /**
     * 标记当前没有可用会话。
     *
     * 主动注销或本地凭据被清理后调用，确保根入口和页面栈不再保留已登录状态。
     */
    fun logout() {
        _state.value = SessionState.Unauthenticated
        _isExpired.value = false
    }

    /**
     * 标记当前会话失效。
     *
     * 该函数由 401 拦截器或令牌刷新失败路径调用。它不清理本地凭证，避免网络层在未知上下文中
     * 执行破坏性操作；凭证清理仍由明确的登录、注销或后续 Session UseCase 负责。
     * 如果用户已经主动 logout 并进入未认证状态，迟到的 401 不得重新制造 Expired 状态，
     * 否则根导航会把一次已完成的注销误判为会话异常。
     */
    fun expire() {
        if (_state.value == SessionState.Unauthenticated) {
            _isExpired.value = false
            return
        }
        _state.value = SessionState.Expired
        _isExpired.value = true
    }

    /**
     * 清除会话失效标记。
     *
     * 登录成功、主动注销完成或应用确认进入登录页后可以调用该函数，防止旧的失效事件影响下一次会话。
     */
    fun resetExpiration() {
        if (_state.value == SessionState.Expired) {
            _state.value = SessionState.Unauthenticated
        }
        _isExpired.value = false
    }
}
