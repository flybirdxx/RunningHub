package com.runninghub.app.ui.feature.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.ProfileCredentialRepository
import com.runninghub.shared.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 个人中心页面状态。
 *
 * @property isLoading 是否正在加载用户资料或执行凭据绑定。
 * @property user 当前登录用户资料；未登录或加载失败时为 null。
 * @property accountStatus API Key 账户状态；没有绑定 API Key 或查询失败时为 null。
 * @property isLoggedIn 当前本地会话是否可用。
 * @property error 页面级错误提示。
 * @property showApiKeyDialog 是否展示 API Key 绑定弹窗。
 * @property showCookieDialog 是否展示 Cookie 绑定弹窗。
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val accountStatus: AccountStatus? = null,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val showApiKeyDialog: Boolean = false,
    val showCookieDialog: Boolean = false
)

/**
 * 个人中心 ScreenModel。
 *
 * 本类负责加载用户资料、绑定/解绑 API Key 与 Cookie，并把登录态变化映射为页面状态。
 * 凭据读写通过 [ProfileCredentialRepository] 表达为领域操作，避免 Presentation 直接依赖
 * CredentialStore、DataStore 或平台安全存储等 DataSource。
 *
 * @param userRepository 用户资料和账户状态仓库。
 * @param profileCredentialRepository 个人中心凭据绑定仓库，负责隐藏底层敏感存储实现。
 * @param authRepository 登录态和注销流程仓库。
 */
class ProfileScreenModel(
    private val userRepository: UserRepository,
    private val profileCredentialRepository: ProfileCredentialRepository,
    private val authRepository: AuthRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshUserData()
    }

    fun loadUserData() {
        refreshUserData()
    }

    fun refreshUserData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val loggedIn = authRepository.isLoggedIn()
            if (!loggedIn) {
                _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                return@launch
            }

            try {
                val userId = authRepository.getCurrentUserId()
                val userResult = userRepository.getUserInfo(userId)
                userResult.fold(
                    onSuccess = { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isLoggedIn = true,
                                error = null
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                error = e.message ?: "加载用户信息失败"
                            )
                        }
                    }
                )

                userRepository.getAccountStatus().onSuccess { status ->
                    _uiState.update { it.copy(accountStatus = status) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "网络请求失败")
                }
            }
        }
    }

    /**
     * 绑定用户在个人中心输入的 API Key。
     *
     * 空白输入会被忽略，不触发持久化和刷新；有效输入会关闭弹窗并进入加载状态，
     * 然后通过领域仓库保存凭据，再重新拉取用户资料和账户状态。
     */
    fun bindApiKey(key: String) {
        if (key.isBlank()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showApiKeyDialog = false) }
            // 绑定敏感凭据后立即刷新账户状态，确保页面展示的是新 API Key 对应的余额和账号能力。
            profileCredentialRepository.bindApiKey(key)
            refreshUserData()
        }
    }

    /**
     * 绑定用户在个人中心输入的 Cookie。
     *
     * Cookie 属于敏感凭据，ScreenModel 只负责接收用户动作并调用领域仓库；
     * 底层存储和后续请求头拼装均不在 Presentation 层处理。
     */
    fun bindCookie(cookie: String) {
        if (cookie.isBlank()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCookieDialog = false) }
            // Cookie 绑定用于兼容部分依赖 Web 会话的接口，刷新用户资料可以及时暴露绑定是否有效。
            profileCredentialRepository.bindCookie(cookie)
            refreshUserData()
        }
    }

    /**
     * 清理个人中心可管理的创作凭据。
     *
     * 解绑完成后重置为未登录的轻量状态，避免页面继续展示旧 API Key 对应的账户余额或能力信息。
     */
    fun unbindApiKey() {
        screenModelScope.launch {
            // 解绑需要同时清理 API Key、企业 Key 和 Cookie，避免旧凭据继续影响后续创作请求。
            profileCredentialRepository.clearCreationCredentials()
            _uiState.update { ProfileUiState(isLoading = false) }
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        screenModelScope.launch {
            authRepository.logout()
            _uiState.update { ProfileUiState(isLoading = false) }
            onLoggedOut()
        }
    }

    fun showApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun showCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = true) }
    }

    fun dismissCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = false) }
    }
}
