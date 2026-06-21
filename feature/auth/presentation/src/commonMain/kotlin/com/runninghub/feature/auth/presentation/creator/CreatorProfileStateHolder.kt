package com.runninghub.feature.auth.presentation.creator

import com.runninghub.core.model.User
import com.runninghub.core.model.WebApp
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CREATOR_APP_PAGE_SIZE = 20

/**
 * 创作者主页可展示的稳定错误语义。
 *
 * 该类型属于 Auth Presentation 层，只描述页面可以理解的错误类别，不携带远端 `msg`、
 * 本地异常消息或敏感诊断信息。最终中文文案由应用壳通过 Compose Resources 映射。
 */
enum class CreatorProfileError {
    /**
     * 创作者公开资料主请求失败。
     *
     * 页面可以展示整页错误并允许用户重试；关注状态和作品列表属于增强信息，
     * 即使它们加载成功，也不能把底层异常消息透传给最终 UI。
     */
    LoadFailed,
}

/**
 * 创作者主页的完整可渲染状态。
 *
 * 该状态属于 Auth Presentation 层，因为创作者公开资料和关注关系由 Auth Domain 的
 * [UserRepository] 负责；作品列表通过 Discovery Domain 的 [WebAppCatalogRepository]
 * 读取。状态只保存可展示数据，不保存 Token、Cookie、API Key 或其他敏感凭据。
 *
 * @property isLoading 是否正在加载创作者资料、关注关系或作品列表。
 * `true` 表示页面应展示加载态或避免重复提交；`false` 表示当前没有由创作者主页发起的主要加载流程。
 * 该字段不代表全局会话恢复状态，也不代表作品列表分页是否正在加载更多。
 * @property user 当前创作者公开资料，来源于 [UserRepository.getUserDetail]。
 * `null` 表示尚未加载、目标用户不存在、接口失败或服务端未返回有效资料；不能用它判断当前登录用户身份。
 * @property apps 当前创作者发布的 WebApp 列表，来源于 [WebAppCatalogRepository.getUserAppList]。
 * 列表顺序保留服务端返回顺序；空集合表示尚未成功加载、该创作者没有公开作品或作品接口失败。
 * @property isFollowing 当前登录用户是否已关注该创作者。
 * `true` 表示已关注；`false` 表示未关注、尚未查询成功、查询失败或用户未登录。
 * @property error 页面级稳定错误语义。
 * `null` 表示当前没有待展示错误；非空通常来自创作者资料主请求失败，后续重新加载时会清空。
 * 关注关系或作品列表失败不会设置该字段，避免局部增强信息失败遮挡已可展示的主页内容。
 * 该字段不得保存远端 `msg` 或 [Throwable.message]，最终中文文案由应用壳资源映射。
 */
data class CreatorProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val apps: List<WebApp> = emptyList(),
    val isFollowing: Boolean = false,
    val error: CreatorProfileError? = null,
)

/**
 * 持有创作者主页状态并协调资料、关注关系和作品列表加载。
 *
 * 本类位于 Auth Presentation 层，只依赖 Auth/Discovery 的 Domain 契约，不依赖 Compose、
 * Voyager、Ktor、DataStore 或平台 SDK。应用壳负责传入与页面生命周期绑定的 [coroutineScope]，
 * 并在 UI 层处理导航、返回和作品详情跳转。
 *
 * @param userRepository 用户资料和关注关系仓库。
 * @param webAppRepository WebApp 公开目录仓库，用于读取该创作者发布的作品列表。
 * @param coroutineScope 与页面生命周期绑定的协程作用域，页面销毁时应取消尚未完成的请求。
 */
class CreatorProfileStateHolder(
    private val userRepository: UserRepository,
    private val webAppRepository: WebAppCatalogRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(CreatorProfileUiState())

    /**
     * 创作者主页只读状态流。
     *
     * UI 只能收集该状态并通过公开动作函数回传用户操作，不得直接修改内部 [MutableStateFlow]。
     */
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    /**
     * 加载指定创作者主页。
     *
     * 同一个 [userId] 已有用户资料时会跳过重复加载，避免页面重组或返回当前页面时重复请求。
     * 加载流程并发读取用户资料、关注关系和作品列表；任一局部失败不会阻断其他成功数据展示。
     *
     * @param userId 目标创作者 ID，来自路由参数或上游用户资料。空字符串会被忽略。
     */
    fun loadProfile(userId: String) {
        if (userId.isBlank()) return
        if (userId == currentUserId && _uiState.value.user != null) return
        currentUserId = userId

        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 三个请求互不依赖，并发启动可以减少首屏等待；后续逐个 fold，保证局部失败不吞掉其他成功结果。
            val userDeferred = async { userRepository.getUserDetail(userId) }
            val followDeferred = async { userRepository.isFollow(userId) }
            val appsDeferred = async {
                webAppRepository.getUserAppList(
                    userId = userId,
                    pageNum = 1,
                    pageSize = CREATOR_APP_PAGE_SIZE,
                )
            }

            userDeferred.await()
                .onSuccess { user -> _uiState.update { it.copy(user = user) } }
                .onFailure {
                    // 主资料失败时只输出稳定错误语义，避免远端 msg 或本地异常诊断直接进入 UI。
                    _uiState.update { it.copy(error = CreatorProfileError.LoadFailed) }
                }

            // 关注状态是增强信息，失败时保持默认 false，避免未登录或远端异常阻断公开主页展示。
            followDeferred.await()
                .onSuccess { following -> _uiState.update { it.copy(isFollowing = following) } }

            // 作品列表失败时保留已加载资料和关注状态，后续可由页面刷新入口单独重试。
            appsDeferred.await()
                .onSuccess { page -> _uiState.update { it.copy(apps = page.records) } }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 切换当前创作者的关注状态。
     *
     * 该动作依赖最近一次 [loadProfile] 设置的用户 ID；没有目标用户时直接忽略。
     * 成功后本地乐观更新粉丝数，避免等待下一次完整刷新才能反馈用户操作。
     */
    fun toggleFollow() {
        val userId = currentUserId.ifBlank { return }
        coroutineScope.launch {
            val isFollowing = _uiState.value.isFollowing
            val result = if (isFollowing) {
                userRepository.unFollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
            result.onSuccess {
                _uiState.update { state ->
                    val updatedUser = state.user?.let { user ->
                        val fans = user.fanCount.toIntOrNull() ?: 0
                        val newFans = if (isFollowing) {
                            (fans - 1).coerceAtLeast(0)
                        } else {
                            fans + 1
                        }
                        user.copy(fanCount = newFans.toString())
                    }
                    state.copy(isFollowing = !isFollowing, user = updatedUser)
                }
            }
        }
    }

    /**
     * 重新加载当前创作者作品列表。
     *
     * 该函数只刷新作品列表，不重新拉取用户资料或关注关系，用于后续分页、下拉刷新或局部重试入口复用。
     */
    fun loadUserApps() {
        val userId = currentUserId.ifBlank { return }
        coroutineScope.launch {
            webAppRepository.getUserAppList(
                userId = userId,
                pageNum = 1,
                pageSize = CREATOR_APP_PAGE_SIZE,
            ).onSuccess { page ->
                _uiState.update { it.copy(apps = page.records) }
            }
        }
    }
}
