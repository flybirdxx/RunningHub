package com.runninghub.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.feature.community.CommunityVoyagerScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryVoyagerScreen
import com.runninghub.app.ui.feature.profile.ProfileVoyagerScreen
import com.runninghub.app.ui.feature.quickcreate.QuickCreateVoyagerScreen

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Discovery("发现", Icons.Filled.Explore, Icons.Outlined.Explore),
    Community("社区", Icons.Filled.Groups, Icons.Outlined.Groups),
    QuickCreate("创作", Icons.Filled.AutoAwesome, Icons.Filled.AutoAwesome),
    Profile("我的", Icons.Filled.Person, Icons.Outlined.Person),
}

class MainVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.Discovery) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    BottomNavTab.entries.forEach { tab ->
                        val isQuickCreate = tab == BottomNavTab.QuickCreate
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (isQuickCreate) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedTab == tab)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = tab.selectedIcon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (selectedTab == tab)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isQuickCreate)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary,
                                selectedTextColor = if (isQuickCreate)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (selectedTab) {
                    BottomNavTab.Discovery -> DiscoveryVoyagerScreen().Content()
                    BottomNavTab.Community -> CommunityVoyagerScreen().Content()
                    BottomNavTab.QuickCreate -> QuickCreateVoyagerScreen().Content()
                    BottomNavTab.Profile -> ProfileVoyagerScreen().Content()
                }
            }
        }
    }
}
