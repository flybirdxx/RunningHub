package com.runninghub.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.feature.community.CommunityScreen
import com.runninghub.app.ui.feature.creator.CreatorProfileScreen
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryScreen
import com.runninghub.app.ui.feature.profile.ProfileScreen
import com.runninghub.app.ui.feature.search.SearchScreen

@Composable
fun AppNavHost() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Discovery) }
    val backStack = remember { mutableStateListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun goBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeLast()
            true
        } else false
    }

    val isMainScreen = currentScreen is Screen.Discovery ||
            currentScreen is Screen.Community ||
            currentScreen is Screen.Profile

    Scaffold(
        bottomBar = {
            if (isMainScreen) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentScreen == item.screen,
                            onClick = {
                                if (currentScreen != item.screen) {
                                    backStack.clear()
                                    currentScreen = item.screen
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val screen = currentScreen) {
                is Screen.Discovery -> DiscoveryScreen(
                    onAppClick = { navigateTo(Screen.AppDetail(it)) },
                    onSearchClick = { navigateTo(Screen.Search) }
                )
                is Screen.Community -> CommunityScreen()
                is Screen.Profile -> ProfileScreen()
                is Screen.Search -> SearchScreen(
                    onAppClick = { navigateTo(Screen.AppDetail(it)) },
                    onBack = { goBack() }
                )
                is Screen.AppDetail -> AppDetailScreen(
                    appId = screen.appId,
                    onBack = { goBack() },
                    onCreatorClick = { navigateTo(Screen.CreatorProfile(it)) }
                )
                is Screen.CreatorProfile -> CreatorProfileScreen(
                    userId = screen.userId,
                    onBack = { goBack() },
                    onAppClick = { navigateTo(Screen.AppDetail(it)) }
                )
            }
        }
    }
}
