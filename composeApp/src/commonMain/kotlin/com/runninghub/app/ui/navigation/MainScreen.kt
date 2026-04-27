package com.runninghub.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.Secondary500

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
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
            containerColor = Color(0xFF0B0F1A),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0B0F1A)),
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

@Composable
private fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .navigationBarsPadding()
            .background(Color(0xFF0D1117)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavTab.entries.forEach { tab ->
            BottomNavItem(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    tab: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isQuickCreate = tab == BottomNavTab.QuickCreate

    val iconColor by animateColorAsState(
        targetValue = when {
            isSelected && isQuickCreate -> Color(0xFF0D1117)
            isSelected -> Primary300
            else -> Color(0xFF64748B)
        },
        animationSpec = tween(200),
        label = "iconColor",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected && isQuickCreate -> Secondary500
            isSelected -> Primary300
            else -> Color(0xFF64748B)
        },
        animationSpec = tween(200),
        label = "textColor",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isQuickCreate) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Secondary500 else Primary300.copy(alpha = 0.15f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tab.selectedIcon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor,
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                modifier = Modifier.size(20.dp),
                tint = iconColor,
            )
        }

        Text(
            text = tab.label,
            fontSize = 10.sp,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
