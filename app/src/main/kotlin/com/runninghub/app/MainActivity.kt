/**
 * [INPUT]: 依赖 RunningHubTheme, AppNavigation, ComponentActivity, AndroidEntryPoint
 * [OUTPUT]: 对外提供 MainActivity 入口，App 导航宿主
 * [POS]: 项目的主入口 Activity，生命周期管理核心
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.runninghub.app.ui.navigation.AppNavigation
import com.runninghub.app.ui.theme.RunningHubTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var userRepository: com.runninghub.app.data.repository.UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            RunningHubTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }

    /**
     * 防御 MIUI ContentCatcher 系统注入 null Bundle 导致的
     * ActivityThread.deliverResultsIfNeeded NPE。
     *
     * MIUI 在 Activity 切换时通过 ContentCatcher 注入 ActivityResult，
     * 但 Intent data 或其 extras Bundle 可能为 null，
     * 导致框架内部 Bundle.getString() 调用触发 NPE。
     */
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: NullPointerException) {
            Log.w("MainActivity", "Caught MIUI system NPE in onActivityResult (requestCode=$requestCode)", e)
        }
    }
}
