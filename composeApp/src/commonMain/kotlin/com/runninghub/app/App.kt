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
import com.runninghub.feature.auth.domain.SessionState
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
        RunningHubTheme(darkTheme = true) {
            val sessionManager = koinInject<SessionManager>()
            val sessionState by sessionManager.state.collectAsState()

            LaunchedEffect(Unit) {
                sessionManager.restore()
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                ProvideRhWindowInfo {
                    val startScreen = when (sessionState) {
                        SessionState.Restoring -> null
                        SessionState.Authenticated -> MainVoyagerScreen()
                        SessionState.Unauthenticated,
                        SessionState.Expired -> LoginVoyagerScreen()
                    }
                    startScreen?.let { screen ->
                        Navigator(screen) { navigator ->
                            LaunchedEffect(sessionState) {
                                when (sessionState) {
                                    SessionState.Authenticated -> {
                                        navigator.replaceAll(MainVoyagerScreen())
                                    }
                                    SessionState.Unauthenticated -> {
                                        navigator.replaceAll(LoginVoyagerScreen())
                                    }
                                    SessionState.Expired -> {
                                        // 会话失效属于全局导航事件，必须清空业务页面栈，避免返回键回到已失效页面。
                                        navigator.replaceAll(LoginVoyagerScreen())
                                        sessionManager.resetExpiration()
                                    }
                                    SessionState.Restoring -> Unit
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
