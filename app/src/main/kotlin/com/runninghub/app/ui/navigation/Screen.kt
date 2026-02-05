/**
 * [INPUT]: 依赖 Screen, DiscoveryScreen, NavHost, NavController
 * [OUTPUT]: 对外提供 AppNavigation 主导航图
 * [POS]: 业务流转的核心中枢，定义了全局路由跳转规则
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.navigation

sealed class Screen(val route: String) {
    data object Discovery : Screen("discovery")
    data object Search : Screen("search")
    data object AppDetail : Screen("app_detail/{appId}") {
        fun createRoute(appId: String) = "app_detail/$appId"
    }
    data object Community : Screen("community")
    data object Profile : Screen("profile")
    data object TaskHistory : Screen("task_history")
}
