package com.runninghub.app.ui.feature.community

import cafe.adriel.voyager.core.model.ScreenModel
import com.runninghub.feature.community.presentation.CommunityStateHolder
import com.runninghub.feature.community.presentation.CommunityUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 社区工具页 ScreenModel 门面。
 *
 * 静态工具目录和页面状态已迁入 Community Presentation 层的 [CommunityStateHolder]。
 * 本类保留在 `composeApp`，只为 Voyager/Koin 提供 ScreenModel 入口，避免应用壳继续持有工具页状态模型。
 */
class CommunityScreenModel : ScreenModel {
    private val stateHolder = CommunityStateHolder()

    /**
     * 社区工具页只读状态流。
     *
     * UI 只收集该状态；工具点击后的真实导航仍由 `composeApp` 根据 route 处理。
     */
    val uiState: StateFlow<CommunityUiState> = stateHolder.uiState
}
