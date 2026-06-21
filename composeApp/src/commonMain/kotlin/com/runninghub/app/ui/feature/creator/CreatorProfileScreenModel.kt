package com.runninghub.app.ui.feature.creator

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.feature.auth.presentation.creator.CreatorProfileStateHolder
import com.runninghub.feature.auth.presentation.creator.CreatorProfileUiState
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * 创作者主页 ScreenModel。
 *
 * 创作者主页业务状态已迁入 Auth Presentation 层的 [CreatorProfileStateHolder]。
 * 本类留在 `composeApp`，只负责把 Voyager 的 [screenModelScope] 传给 StateHolder，
 * 并对外暴露页面继续使用的事件函数，避免应用壳重新持有跨 Feature 业务状态机。
 *
 * @param userRepository 用户资料仓库，用于读取创作者信息和关注状态。
 * @param webAppRepository WebApp 目录仓库，用于分页读取创作者发布的应用。
 */
class CreatorProfileScreenModel(
    private val userRepository: UserRepository,
    private val webAppRepository: WebAppCatalogRepository
) : ScreenModel {

    private val stateHolder = CreatorProfileStateHolder(
        userRepository = userRepository,
        webAppRepository = webAppRepository,
        coroutineScope = screenModelScope,
    )

    /**
     * 创作者主页只读状态流。
     *
     * UI 只收集该状态并通过公开动作回传事件，不直接修改 StateHolder 内部状态。
     */
    val uiState: StateFlow<CreatorProfileUiState> = stateHolder.uiState

    /**
     * 加载指定创作者主页。
     *
     * 实际状态转换委托给 [CreatorProfileStateHolder.loadProfile]，本门面只保留 Voyager 调用点。
     *
     * @param userId 目标创作者 ID，来自路由参数或上游用户资料。
     */
    fun loadProfile(userId: String) {
        stateHolder.loadProfile(userId)
    }

    /**
     * 切换当前创作者的关注状态。
     *
     * 实际关注/取消关注请求和本地粉丝数修正由 [CreatorProfileStateHolder.toggleFollow] 处理。
     */
    fun toggleFollow() {
        stateHolder.toggleFollow()
    }

    /**
     * 重新加载当前创作者作品列表。
     *
     * 实际作品列表刷新由 [CreatorProfileStateHolder.loadUserApps] 处理。
     */
    fun loadUserApps() {
        stateHolder.loadUserApps()
    }
}
