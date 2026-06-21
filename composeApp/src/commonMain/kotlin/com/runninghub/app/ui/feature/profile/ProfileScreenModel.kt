package com.runninghub.app.ui.feature.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.ProfileCredentialRepository
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.feature.auth.presentation.profile.ProfileStateHolder
import com.runninghub.feature.auth.presentation.profile.ProfileUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Profile Voyager 页面对 Auth Presentation 状态持有器的生命周期适配层。
 *
 * 用户资料刷新、账户状态查询、创作凭据绑定/清理和注销状态转换由 [ProfileStateHolder] 承担；
 * 本类只负责把 Voyager 的 [screenModelScope] 传入并为 Koin/Voyager 暴露原有 ScreenModel 类型。
 *
 * @param userRepository 用户资料和账户状态仓库。
 * @param profileCredentialRepository 个人中心凭据绑定仓库。
 * @param authRepository 登录态和注销流程仓库。
 */
class ProfileScreenModel(
    userRepository: UserRepository,
    profileCredentialRepository: ProfileCredentialRepository,
    authRepository: AuthRepository,
) : ScreenModel {
    private val stateHolder = ProfileStateHolder(
        userRepository = userRepository,
        profileCredentialRepository = profileCredentialRepository,
        authRepository = authRepository,
        coroutineScope = screenModelScope,
    )

    /**
     * 个人中心只读 UI 状态。
     *
     * UI 只能收集该状态并通过本类公开动作发送事件，避免 composeApp 重新实现 Auth Presentation 状态规则。
     */
    val uiState: StateFlow<ProfileUiState> = stateHolder.uiState

    /**
     * 重新加载个人中心数据。
     *
     * 该动作直接委托给 [ProfileStateHolder]，保持应用壳无业务分支。
     */
    fun loadUserData() {
        stateHolder.loadUserData()
    }

    /**
     * 刷新用户资料和账户状态。
     */
    fun refreshUserData() {
        stateHolder.refreshUserData()
    }

    /**
     * 绑定用户输入的 API Key。
     *
     * @param key 用户手动输入的 API Key；空白输入由 Presentation 状态持有器忽略。
     */
    fun bindApiKey(key: String) {
        stateHolder.bindApiKey(key)
    }

    /**
     * 绑定用户输入的 Cookie。
     *
     * @param cookie 用户手动输入的 Cookie；该值不会回读到 UI。
     */
    fun bindCookie(cookie: String) {
        stateHolder.bindCookie(cookie)
    }

    /**
     * 清理个人中心可管理的创作凭据。
     */
    fun unbindApiKey() {
        stateHolder.unbindApiKey()
    }

    /**
     * 注销当前会话。
     *
     * 根 App 仍通过 SessionManager 统一清理业务页面栈，本适配层不直接执行导航。
     */
    fun logout() {
        stateHolder.logout()
    }

    /**
     * 打开 API Key 绑定弹窗。
     */
    fun showApiKeyDialog() {
        stateHolder.showApiKeyDialog()
    }

    /**
     * 关闭 API Key 绑定弹窗。
     */
    fun dismissApiKeyDialog() {
        stateHolder.dismissApiKeyDialog()
    }

    /**
     * 打开 Cookie 绑定弹窗。
     */
    fun showCookieDialog() {
        stateHolder.showCookieDialog()
    }

    /**
     * 关闭 Cookie 绑定弹窗。
     */
    fun dismissCookieDialog() {
        stateHolder.dismissCookieDialog()
    }
}
