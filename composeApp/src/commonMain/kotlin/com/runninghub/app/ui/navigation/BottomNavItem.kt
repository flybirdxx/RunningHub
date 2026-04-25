package com.runninghub.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
) {
    Discovery("发现", Icons.Default.Explore, Screen.Discovery),
    Community("社区", Icons.Default.People, Screen.Community),
    Profile("我的", Icons.Default.Person, Screen.Profile),
}
