package com.runninghub.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.feature.community.CommunityVoyagerScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryVoyagerScreen
import com.runninghub.app.ui.feature.profile.ProfileVoyagerScreen
import com.runninghub.app.ui.feature.quickcreate.QuickCreateVoyagerScreen
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.shared.domain.repository.SettingsRepository
import org.koin.compose.koinInject

enum class BottomNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Discovery("发现", Icons.Filled.Explore, Icons.Outlined.Explore),
    QuickCreate("创作", Icons.Filled.Star, Icons.Outlined.Star),
    Studio("工坊", Icons.Filled.Build, Icons.Outlined.Build),
    Profile("我的", Icons.Filled.Person, Icons.Outlined.Person),
}

class MainVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.Discovery) }
        val settingsRepository = koinInject<SettingsRepository>()
        var creditCoins by rememberSaveable { mutableStateOf("--") }

        var isLoggedIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            creditCoins = settingsRepository.getLastKnownCoins() ?: "--"
            isLoggedIn = settingsRepository.isLoggedIn()
        }

        val sizeClass = rememberWindowSizeClass()

        // Guest mode banner (dismissible)
        var showGuestBanner by rememberSaveable { mutableStateOf(!isLoggedIn) }
        if (showGuestBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "您正在以游客模式浏览，部分功能受限",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clickable { showGuestBanner = false }
                        .padding(4.dp)
                )
            }
        }

        if (sizeClass.isWide) {
            // Expanded / Large: sidebar rail (iOS sidebar style)
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    BottomNavTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabContent(selectedTab)
                    CreditIndicator(
                        coins = creditCoins,
                        onClick = { selectedTab = BottomNavTab.Profile },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                }
            }
        } else {
            // Compact / Medium: bottom tab bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        tonalElevation = 0.dp,
                    ) {
                        BottomNavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    TabContent(selectedTab)
                    CreditIndicator(
                        coins = creditCoins,
                        onClick = { selectedTab = BottomNavTab.Profile },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun TabContent(tab: BottomNavTab) {
        // Use AnimatedVisibility to keep all tabs in composition — preserves scroll position and input state
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(tab == BottomNavTab.Discovery, enter = fadeIn(), exit = fadeOut()) {
                DiscoveryVoyagerScreen().Content()
            }
            AnimatedVisibility(tab == BottomNavTab.QuickCreate, enter = fadeIn(), exit = fadeOut()) {
                QuickCreateVoyagerScreen().Content()
            }
            AnimatedVisibility(tab == BottomNavTab.Studio, enter = fadeIn(), exit = fadeOut()) {
                CommunityVoyagerScreen().Content()
            }
            AnimatedVisibility(tab == BottomNavTab.Profile, enter = fadeIn(), exit = fadeOut()) {
                ProfileVoyagerScreen().Content()
            }
        }
    }
}

@Composable
private fun CreditIndicator(
    coins: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = modifier.clickable(onClick = onClick),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🪙",
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = coins,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
