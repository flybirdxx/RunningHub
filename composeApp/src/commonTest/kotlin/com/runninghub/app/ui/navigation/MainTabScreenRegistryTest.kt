package com.runninghub.app.ui.navigation

import com.runninghub.app.ui.feature.quickcreate.QuickCreateVoyagerScreen
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class MainTabScreenRegistryTest {
    @Test
    fun `screenFor returns the same screen instance after switching away and back`() {
        val registry = MainTabScreenRegistry()
        val firstHistoryScreen = registry.screenFor(BottomNavTab.History)

        registry.screenFor(BottomNavTab.Discovery)
        registry.screenFor(BottomNavTab.QuickCreate)
        val secondHistoryScreen = registry.screenFor(BottomNavTab.History)

        // Gate G：同一 Tab 的 Screen 实例必须稳定，普通 Tab 切换不应创建第二个同类 ScreenModel 所有者。
        assertSame(firstHistoryScreen, secondHistoryScreen)
    }

    @Test
    fun `quick create tab maps to migrated quick create screen`() {
        val registry = MainTabScreenRegistry()

        // AC-03：生产创作入口必须固定到 QuickCreate，旧 Create 页面不得重新进入主导航状态机。
        assertIs<QuickCreateVoyagerScreen>(registry.screenFor(BottomNavTab.QuickCreate))
    }

    @Test
    fun `each tab owns a distinct screen instance`() {
        val registry = MainTabScreenRegistry()

        BottomNavTab.entries.forEachIndexed { index, leftTab ->
            BottomNavTab.entries.drop(index + 1).forEach { rightTab ->
                // Gate G：不同一级 Tab 的状态所有权需要明确隔离，避免多个 Tab 共享同一个 ScreenModel 生命周期。
                assertNotSame(registry.screenFor(leftTab), registry.screenFor(rightTab))
            }
        }
    }
}
