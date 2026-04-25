package com.runninghub.app

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.navigation.AppNavHost
import com.runninghub.app.ui.theme.RunningHubTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        RunningHubTheme {
            AppNavHost()
        }
    }
}
