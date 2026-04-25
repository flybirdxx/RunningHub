package com.runninghub.app.ui.navigation

sealed class Screen(val route: String) {
    data object Discovery : Screen("discovery")
    data object Community : Screen("community")
    data object Profile : Screen("profile")
    data object Search : Screen("search")
    data class AppDetail(val appId: String) : Screen("detail/{appId}") {
        companion object {
            const val ROUTE = "detail/{appId}"
            fun createRoute(appId: String) = "detail/$appId"
        }
    }
    data class CreatorProfile(val userId: String) : Screen("creator/{userId}") {
        companion object {
            const val ROUTE = "creator/{userId}"
            fun createRoute(userId: String) = "creator/$userId"
        }
    }
}
