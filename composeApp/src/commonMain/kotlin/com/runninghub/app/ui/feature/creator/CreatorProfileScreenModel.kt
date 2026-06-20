package com.runninghub.app.ui.feature.creator

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.core.model.User
import com.runninghub.core.model.WebApp
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 创作者主页的完整可渲染状态。
 *
 * 该状态只保存公开资料、关注状态和作品列表，不保存 Token、Cookie、API Key 等敏感凭据。
 * 关注关系通过 [UserRepository] 查询和修改，作品列表通过 [WebAppCatalogRepository] 读取。
 *
 * @property isLoading 是否正在加载创作者资料、关注关系或作品列表。
 * `true` 表示页面应展示加载态；`false` 表示当前没有由本 ScreenModel 发起的主要加载流程。
 * @property user 当前创作者公开资料，来源于 [UserRepository.getUserDetail]。
 * `null` 表示尚未加载、目标用户不存在或加载失败。
 * @property apps 当前创作者发布的 WebApp 列表，来源于 [WebAppCatalogRepository.getUserAppList]。
 * 列表顺序保留服务端返回顺序；空列表表示尚未加载成功或该用户没有公开作品。
 * @property isFollowing 当前登录用户是否已关注该创作者。
 * `true` 表示已关注；`false` 表示未关注、尚未查询成功或用户未登录。
 * @property error 页面级错误提示。
 * `null` 表示当前没有待展示错误；非空通常来自用户资料加载失败，后续重新加载时会清空。
 */
data class CreatorProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val apps: List<WebApp> = emptyList(),
    val isFollowing: Boolean = false,
    val error: String? = null
)

/**
 * 创作者主页 ScreenModel。
 *
 * 本类组合用户资料仓库和 WebApp 目录仓库：用户资料负责关注关系，目录仓库只负责读取该创作者
 * 发布的 WebApp 列表。页面不需要上传、任务提交或任务历史能力。
 *
 * @param userRepository 用户资料仓库，用于读取创作者信息和关注状态。
 * @param webAppRepository WebApp 目录仓库，用于分页读取创作者发布的应用。
 */
class CreatorProfileScreenModel(
    private val userRepository: UserRepository,
    private val webAppRepository: WebAppCatalogRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(CreatorProfileUiState())

    /**
     * 创作者主页只读状态流。
     *
     * UI 只收集该状态并通过公开动作回传事件，不直接修改内部 MutableStateFlow。
     */
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    /**
     * 加载指定创作者主页。
     *
     * 同一个 [userId] 已有用户资料时会跳过重复加载，避免页面重组导致重复请求。
     * 加载流程并发读取用户资料、关注关系和作品列表；任一局部失败不会阻断其他成功数据展示。
     *
     * @param userId 目标创作者 ID，来自路由参数或上游用户资料。
     */
    fun loadProfile(userId: String) {
        if (userId == currentUserId && _uiState.value.user != null) return
        currentUserId = userId

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userDeferred = async { userRepository.getUserDetail(userId) }
            val followDeferred = async { userRepository.isFollow(userId) }
            val appsDeferred = async { webAppRepository.getUserAppList(userId, pageNum = 1, pageSize = 20) }

            userDeferred.await()
                .onSuccess { user -> _uiState.update { it.copy(user = user) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "加载失败") } }

            followDeferred.await()
                .onSuccess { following -> _uiState.update { it.copy(isFollowing = following) } }

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
        screenModelScope.launch {
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
                        val newFans = if (isFollowing) (fans - 1).coerceAtLeast(0) else fans + 1
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
     * 该函数只刷新作品列表，不重新拉取用户资料或关注关系，用于后续分页/刷新入口复用。
     */
    fun loadUserApps() {
        val userId = currentUserId.ifBlank { return }
        screenModelScope.launch {
            webAppRepository.getUserAppList(userId, pageNum = 1, pageSize = 20)
                .onSuccess { page -> _uiState.update { it.copy(apps = page.records) } }
        }
    }
}
