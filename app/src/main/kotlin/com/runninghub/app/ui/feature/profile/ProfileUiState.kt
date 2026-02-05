package com.runninghub.app.ui.feature.profile

import com.runninghub.app.data.remote.model.AccountStatusDto
import com.runninghub.app.data.remote.model.UserDto

/**
 * [INPUT]: AccountStatusDto, Loading State, Error State
 * [OUTPUT]: ProfileUiState
 * [POS]: 个人中心页面的 UI 状态定义
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isBinding: Boolean = false, // Loading state for binding process
    val hasApiKey: Boolean = false, // Whether user is logged in (Cookie OR Key)
    val hasAppApiKey: Boolean = false, // Whether a specific API Key is set for Apps
    val error: String? = null,
    val bindError: String? = null, // Specific error for binding
    val accountStatus: AccountStatusDto? = null,
    val user: UserDto? = null
)
