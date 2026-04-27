package com.runninghub.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.AnimatedImageDecoder
import coil3.request.CachePolicy
import com.runninghub.app.ui.feature.login.LoginVoyagerScreen
import com.runninghub.app.ui.navigation.MainVoyagerScreen
import com.runninghub.app.ui.theme.RunningHubTheme
import com.runninghub.shared.domain.repository.AuthRepository
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    KoinContext {
        RunningHubTheme {
            val authRepository = koinInject<AuthRepository>()
            var startScreen by remember { mutableStateOf<cafe.adriel.voyager.core.screen.Screen?>(null) }

            LaunchedEffect(Unit) {
                startScreen = try {
                    if (authRepository.isLoggedIn()) {
                        MainVoyagerScreen()
                    } else {
                        LoginVoyagerScreen()
                    }
                } catch (_: Exception) {
                    LoginVoyagerScreen()
                }
            }

            startScreen?.let { screen ->
                Navigator(screen)
            }
        }
    }
}
