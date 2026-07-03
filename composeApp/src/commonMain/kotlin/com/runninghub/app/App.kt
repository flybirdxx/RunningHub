package com.runninghub.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.CachePolicy
import com.runninghub.app.platform.addPlatformImageDecoders
import com.runninghub.app.platform.configurePlatformImageCache
import com.runninghub.app.ui.adaptive.ProvideRhWindowInfo
import com.runninghub.app.ui.feature.login.LoginVoyagerScreen
import com.runninghub.app.ui.navigation.MainVoyagerScreen
import com.runninghub.app.ui.theme.RunningHubTheme
import com.runninghub.feature.auth.domain.SessionManager
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * Compose Multiplatform 应用根入口。
 *
 * 该入口负责初始化跨平台图片加载器、Koin 上下文、主题和 Voyager 根导航。
 * 启动时通过 [SessionManager.restore] 恢复会话，并观察 [SessionManager.state] 选择登录页或主页面；
 * 运行过程中遇到不可恢复 401 时，会清空页面栈并回到登录页。
 */
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .addPlatformImageDecoders()
            .configurePlatformImageCache(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    KoinContext {
        RunningHubTheme {
            val sessionManager = koinInject<SessionManager>()
            val sessionState by sessionManager.state.collectAsState()

            LaunchedEffect(Unit) {
                sessionManager.restore()
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                ProvideRhWindowInfo {
                    val rootDecision = rootNavigationDecisionFor(sessionState)
                    val startScreen = rootDecision.target?.toVoyagerScreen()
                    startScreen?.let { screen ->
                        Navigator(screen) { navigator ->
                            LaunchedEffect(sessionState) {
                                val navigationDecision = rootNavigationDecisionFor(sessionState)
                                navigationDecision.target?.let { target ->
                                    // 根导航必须使用 replaceAll 清空旧栈；会话失效或主动注销后不能通过返回键回到业务页面。
                                    navigator.replaceAll(target.toVoyagerScreen())
                                }
                                if (navigationDecision.resetExpiredSession) {
                                    // 失效事件只在登录页落栈后消费，避免重组期间旧 Expired 状态重复触发根导航。
                                    sessionManager.resetExpiration()
                                }
                            }
                            CurrentScreen()
                        }
                    }
                }
            }
        }
    }
}

private fun RootScreenTarget.toVoyagerScreen() =
    when (this) {
        RootScreenTarget.Main -> MainVoyagerScreen()
        RootScreenTarget.Login -> LoginVoyagerScreen()
    }
