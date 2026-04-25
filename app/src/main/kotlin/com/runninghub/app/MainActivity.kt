/**
 * [INPUT]: 依赖 RunningHubTheme, AppNavigation, ComponentActivity, AndroidEntryPoint
 * [OUTPUT]: 对外提供 MainActivity 入口，App 导航宿主
 * [POS]: 项目的主入口 Activity，生命周期管理核心
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app

import android.os.Bundle
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
}
