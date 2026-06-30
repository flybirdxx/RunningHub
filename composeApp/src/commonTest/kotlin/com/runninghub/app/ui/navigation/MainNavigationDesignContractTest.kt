package com.runninghub.app.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavigationDesignContractTest {
    @Test
    fun `main navigation starts from creation and follows roadmap order`() {
        assertEquals(BottomNavTab.QuickCreate, MainNavigationDefaults.defaultTab)
        assertEquals(
            listOf(
                BottomNavTab.QuickCreate,
                BottomNavTab.Discovery,
                BottomNavTab.Studio,
                BottomNavTab.History,
                BottomNavTab.Profile,
            ),
            MainNavigationDefaults.orderedTabs,
        )
    }

    @Test
    fun `main navigation exposes Chinese product roles`() {
        assertEquals(
            listOf("创作", "发现", "灵感", "任务", "账户"),
            MainNavigationDefaults.orderedTabs.map { it.role.label },
        )
        assertEquals("默认主路径入口", BottomNavTab.QuickCreate.role.description)
        assertEquals("社区作品和使用同款", BottomNavTab.Studio.role.description)
    }
}
