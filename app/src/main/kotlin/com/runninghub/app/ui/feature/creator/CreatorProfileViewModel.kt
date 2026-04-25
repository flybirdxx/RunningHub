package com.runninghub.app.ui.feature.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.remote.model.AuthorDto
import com.runninghub.app.data.remote.model.WebAppDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import android.util.Log

private const val TAG = "CreatorProfileVM"





@HiltViewModel
class CreatorProfileViewModel @Inject constructor(
    private val webAppApi: WebAppApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorProfileUiState())
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20
    private var currentUserId: String? = null

    fun loadCreatorProfile(userId: String) {
        if (currentUserId == userId && _uiState.value.webAppList.isNotEmpty()) return
        
        currentUserId = userId
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, webAppList = emptyList()) }
            fetchData(userId, 1)
        }
    }

    fun loadMore() {
        val userId = currentUserId ?: return
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return

        viewModelScope.launch {
            fetchData(userId, currentPage + 1)
        }
    }

    private suspend fun fetchData(userId: String, page: Int) {
        try {
            // Unified Loading State: Only set loading true if it's a fresh load or explicit refresh
            if (page == 1) {
                // For pagination (page > 1), we don't block the whole UI, just show footer loader (handled by paging logic usually)
                // But here we are doing manual paging.
            }

            // Use coroutineScope for parallel execution
            coroutineScope {
                // 1. Fetch App List (Always needed for pagination)
                val listDeferred = async { 
                    val request = mapOf(
                        "userId" to userId,
                        "current" to page,
                        "size" to pageSize
                    )
                    webAppApi.getWebAppUserList(request)
                }

                // 2. Fetch User Profile & Follow Status (Only needed for first page/initial load)
                val profileDeferred = if (page == 1) async { 
                    val referer = "https://www.runninghub.cn/user-center/$userId"
                    webAppApi.getUserDetail(referer, mapOf("userId" to userId)) 
                } else null

                val followDeferred = if (page == 1) async { 
                    val referer = "https://www.runninghub.cn/user-center/$userId"
                    // Correct parameter name found via reverse engineering: "followId"
                    webAppApi.isFollow(referer, mapOf("followId" to userId)) 
                } else null

                // Await all results
                val listResponse = listDeferred.await()
                val profileResponse = profileDeferred?.await()
                val followResponse = followDeferred?.await()

                // Process Results
                
                // A. Follow Status
                if (followResponse != null && followResponse.code == 0) {
                    _uiState.update { it.copy(isFollowing = followResponse.data ?: false) }
                }

                // B. User Profile
                if (profileResponse != null && profileResponse.code == 0 && profileResponse.data != null) {
                    val userDto = profileResponse.data
                    val enrichedAuthor = AuthorDto(
                        id = userDto.id,
                        name = userDto.nickName,
                        avatar = userDto.headIcon,
                        intro = userDto.introduce,
                        fansCount = userDto.fanCount,
                        followCount = userDto.followCount,
                        likeCount = userDto.likeCount,
                        collectCount = userDto.collectCount,
                        bgImage = null
                    )
                    _uiState.update { it.copy(creatorInfo = enrichedAuthor) }
                }

                // C. App List
                if (listResponse.code == 0 && listResponse.data != null) {
                    val data = listResponse.data
                    val newItems = data.records ?: emptyList()
                    
                    // Fallback: If profile fetch failed or we are identifying via list
                    if (_uiState.value.creatorInfo?.intro == null && newItems.isNotEmpty()) {
                         val firstItem = newItems.first()
                         val author = firstItem.author
                         if (author != null) {
                             _uiState.update { current ->
                                 if (current.creatorInfo == null) current.copy(creatorInfo = author) else current
                             }
                         }
                    }

                    _uiState.update { current ->
                        current.copy(
                            isLoading = false, // Loading finishes here
                            webAppList = if (page == 1) newItems else current.webAppList + newItems,
                            hasMore = newItems.size >= pageSize
                        )
                    }
                    if (newItems.isNotEmpty()) {
                        currentPage = page
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = listResponse.msg ?: "Unknown error") }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
        }
    }
    fun toggleFollow() {
        val creatorId = _uiState.value.creatorInfo?.id ?: return
        val referer = "https://www.runninghub.cn/user-center/$creatorId"
        val isFollowing = _uiState.value.isFollowing
        
        Log.d(TAG, "toggleFollow: Clicked. Current isFollowing=$isFollowing, creatorId=$creatorId")

        viewModelScope.launch {
            try {
                // Correct parameter name found via reverse engineering: "followId"
                val request = mapOf("followId" to creatorId)
                val response = if (isFollowing) {
                    Log.d(TAG, "toggleFollow: Calling unFollowUser")
                    webAppApi.unFollowUser(referer, request)
                } else {
                    Log.d(TAG, "toggleFollow: Calling followUser")
                    webAppApi.followUser(referer, request)
                }
                
                Log.d(TAG, "toggleFollow: API Response. Code=${response.code}, Data=${response.data}, Msg=${response.msg}")

                if (response.code == 0) {
                    // Trust code=0 as success for both follow and unfollow
                    val newStatus = !isFollowing
                    Log.d(TAG, "toggleFollow: Success. Updating UI to newStatus=$newStatus")
                    
                    _uiState.update { 
                        it.copy(
                            isFollowing = newStatus,
                            creatorInfo = it.creatorInfo?.copy(
                                fansCount = (it.creatorInfo.fansCount?.toIntOrNull()?.let { count ->
                                    if (newStatus) count + 1 else (count - 1).coerceAtLeast(0)
                                } ?: 0).toString()
                            )
                        ) 
                    }
                } else {
                    Log.e(TAG, "toggleFollow: Failed with code ${response.code}")
                    _uiState.update { it.copy(error = response.msg ?: "Operation failed") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleFollow: Exception", e)
                e.printStackTrace()
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}

data class CreatorProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val creatorInfo: AuthorDto? = null,
    val webAppList: List<WebAppDto> = emptyList(),
    val hasMore: Boolean = false,
    val isFollowing: Boolean = false
)
