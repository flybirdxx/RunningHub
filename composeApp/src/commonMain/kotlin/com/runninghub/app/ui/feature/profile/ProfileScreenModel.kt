package com.runninghub.app.ui.feature.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.ProfileCredentialRepository
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 个人中心页面状态。
 *
 * 该状态只承载个人中心的可渲染信息和弹窗开关，不保存 Token、Cookie、API Key
 * 或其他完整敏感凭据。凭据读写必须通过 [ProfileCredentialRepository] 进入领域端口。
 *
 * @property isLoading 是否正在加载用户资料、查询账户状态或执行凭据绑定流程。
 * `true` 表示页面应展示加载态并避免重复提交；`false` 表示当前没有由本 ScreenModel
 * 发起的进行中请求。该字段不代表全局会话恢复状态。
 * @property user 当前登录用户资料，来源于 [UserRepository.getUserInfo]。
 * `null` 表示尚未加载、未登录或加载失败；不能用它单独判断本地会话是否有效。
 * @property accountStatus API Key 对应的账户状态，来源于 [UserRepository.getAccountStatus]。
 * `null` 表示未绑定可用 API Key、查询失败或请求尚未完成；页面不应把 null 展示为余额为 0。
 * @property isLoggedIn 当前本地认证仓库判断到的会话可用性。
 * `true` 表示存在可尝试使用的登录凭据；`false` 表示应展示未登录态或引导登录。
 * @property error 等待页面展示的一次性错误提示。
 * `null` 表示当前没有错误；非空通常来自 Repository 失败消息或本地降级文案，刷新成功后会清空。
 * @property showApiKeyDialog 是否展示 API Key 绑定弹窗。
 * `true` 表示用户正在输入新的 API Key；`false` 表示弹窗关闭且不持有用户输入。
 * @property showCookieDialog 是否展示 Cookie 绑定弹窗。
 * `true` 表示用户正在输入 Cookie；`false` 表示弹窗关闭且不持有用户输入。
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

    /**
     * 个人中心页面只读状态流。
     *
     * UI 只能收集该状态并通过公开动作函数回传用户操作，不得直接修改内部 MutableStateFlow。
     */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshUserData()
    }

    /**
     * 重新加载个人中心数据。
     *
     * 该函数保留给页面生命周期或手动刷新入口使用，实际流程统一委托给 [refreshUserData]，
     * 确保登录态检查、用户资料加载和账户状态刷新使用同一套状态转换规则。
     */
    fun loadUserData() {
        refreshUserData()
    }

    /**
     * 刷新用户资料和 API Key 账户状态。
     *
     * 第一阶段先通过 [AuthRepository.isLoggedIn] 判断本地是否存在可用会话，避免未登录时继续请求用户资料。
     * 第二阶段加载用户资料并更新主体登录态；第三阶段尝试刷新账户状态，失败时保留用户资料展示，
     * 避免账户状态接口异常导致整个个人中心不可用。
     */
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

                // 账户状态是个人中心的增强信息，失败时不覆盖已加载的用户资料，避免局部接口异常扩大影响面。
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

    /**
     * 注销当前会话。
     *
     * 注销动作交给 [AuthRepository] 处理远程 best-effort 请求和本地凭据清理；
     * 本 ScreenModel 只在完成后清空个人中心状态，并通过 [onLoggedOut] 通知页面执行导航。
     *
     * @param onLoggedOut 注销完成后的导航回调，应只执行页面跳转，不应再次清理凭据。
     */
    fun logout(onLoggedOut: () -> Unit = {}) {
        screenModelScope.launch {
            authRepository.logout()
            _uiState.update { ProfileUiState(isLoading = false) }
            onLoggedOut()
        }
    }

    /**
     * 打开 API Key 绑定弹窗。
     *
     * 该函数只切换 UI 状态，不读取或预填充已保存 API Key，避免敏感凭据重新暴露到 Presentation。
     */
    fun showApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    /**
     * 关闭 API Key 绑定弹窗。
     *
     * 页面输入内容由 Composable 本地状态持有；关闭弹窗时不经过仓库，避免误保存未确认输入。
     */
    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    /**
     * 打开 Cookie 绑定弹窗。
     *
     * Cookie 只允许用户主动输入并提交，不从存储反向读取到页面，减少敏感信息泄露风险。
     */
    fun showCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = true) }
    }

    /**
     * 关闭 Cookie 绑定弹窗。
     *
     * 关闭动作只影响弹窗可见性，不会清理已经保存的 Cookie；清理凭据必须通过 [unbindApiKey]。
     */
    fun dismissCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = false) }
    }
}
