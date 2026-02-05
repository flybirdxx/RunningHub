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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.feature.discovery.DiscoveryScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryViewModel
import com.runninghub.app.ui.feature.discovery.DiscoveryBottomNav
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.detail.AppDetailViewModel
import com.runninghub.app.ui.feature.profile.ProfileScreen
import com.runninghub.app.ui.feature.profile.ProfileViewModel
import com.runninghub.app.ui.feature.profile.HistoryViewModel
import com.runninghub.app.ui.feature.profile.TaskHistoryScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Discovery.route
    ) {
        composable(Screen.Discovery.route) {
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
        
        composable(Screen.AppDetail.route) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
            val viewModel: AppDetailViewModel = hiltViewModel()
            AppDetailScreen(
                appId = appId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            PlaceholderPage("搜索界面 (开发中)", navController)
        }

        composable(Screen.Community.route) {
            PlaceholderPage("动态界面 (开发中)", navController)
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                uiState = uiState,
                navController = navController,
                onRefresh = viewModel::refresh,
                onNavigateToHistory = { navController.navigate(Screen.TaskHistory.route) },
                onBindApiKey = viewModel::bindApiKey,
                onUnbindApiKey = viewModel::unbindApiKey
            )
        }

        composable(Screen.TaskHistory.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            TaskHistoryScreen(
                historyManager = viewModel.historyManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderPage(title: String, navController: NavHostController) {
    Scaffold(
        bottomBar = { DiscoveryBottomNav(navController) }
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
}
