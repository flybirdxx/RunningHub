/**
 * [INPUT]: 依赖 Screen, DiscoveryScreen, NavHost, NavController
 * [OUTPUT]: 对外提供 AppNavigation 主导航图
 * [POS]: 业务流转的核心中枢，定义了全局路由跳转规则
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.feature.discovery.DiscoveryScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryViewModel
import com.runninghub.app.ui.feature.discovery.DiscoveryBottomNav
import com.runninghub.app.ui.component.FabMenuOverlay
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.detail.AppDetailViewModel
import com.runninghub.app.ui.feature.profile.ProfileScreen
import com.runninghub.app.ui.feature.profile.ProfileViewModel
import com.runninghub.app.ui.feature.profile.HistoryViewModel
import com.runninghub.app.ui.feature.community.tools.AudioGenerationScreen
import com.runninghub.app.ui.feature.community.tools.AudioGenerationViewModel
import com.runninghub.app.ui.feature.profile.TaskHistoryScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Discovery.route
    ) {
        composable(
            route = Screen.Discovery.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            val viewModel: DiscoveryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            DiscoveryScreen(
                uiState = uiState,
                navController = navController,
                onCategorySelected = viewModel::selectCategory,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore
            )
        }
        
        composable(
            route = Screen.AppDetail.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
            val viewModel: AppDetailViewModel = hiltViewModel()
            AppDetailScreen(
                appId = appId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAuthorClick = { userId -> navController.navigate(Screen.CreatorProfile.createRoute(userId)) }
            )
        }

        composable(
            route = Screen.CreatorProfile.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            com.runninghub.app.ui.feature.creator.CreatorProfileScreen(
                userId = userId,
                navController = navController
            )
        }

        composable(Screen.Search.route) {
            com.runninghub.app.ui.feature.search.SearchScreen(navController)
        }

        composable(Screen.Community.route) {
            com.runninghub.app.ui.feature.community.CommunityScreen(navController)
        }

        composable(
            route = Screen.Profile.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                uiState = uiState,
                navController = navController,
                onRefresh = viewModel::refresh,
                onNavigateToHistory = { navController.navigate(Screen.TaskHistory.route) },
                onBindApiKey = viewModel::bindApiKey,
                onBindEnterpriseApiKey = viewModel::bindEnterpriseApiKey,
                onUnbindApiKey = viewModel::unbindApiKey
            )
        }

        composable(
            route = Screen.TaskHistory.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
             popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            val viewModel: HistoryViewModel = hiltViewModel()
            TaskHistoryScreen(
                historyManager = viewModel.historyManager,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.UiInspector.route) {
            com.runninghub.app.ui.feature.community.tools.UiInspectorScreen(navController)
        }
        
        composable(Screen.SecretDecode.route) {
            com.runninghub.app.ui.feature.community.tools.SecretDecodeScreen(navController)
        }

        composable(Screen.AudioGeneration.route) {
            val audioViewModel = hiltViewModel<AudioGenerationViewModel>()
            AudioGenerationScreen(
                viewModel = audioViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderPage(title: String, navController: NavHostController) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { 
                DiscoveryBottomNav(
                    navController = navController,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = !isMenuExpanded }
                ) 
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(title)
            }
        }
        
        FabMenuOverlay(
            isVisible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            onMenuItemClick = { title ->
                if (title == "音频处理 API") {
                    navController.navigate(Screen.AudioGeneration.route)
                }
            }
        )
    }
}
