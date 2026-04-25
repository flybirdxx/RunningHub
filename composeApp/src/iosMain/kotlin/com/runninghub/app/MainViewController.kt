package com.runninghub.app

import androidx.compose.ui.window.ComposeUIViewController
import com.runninghub.app.di.appModule
import com.runninghub.shared.di.platformModule
import com.runninghub.shared.di.sharedModules
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { App() }

fun initKoin() {
    startKoin {
        modules(sharedModules + platformModule() + appModule)
    }
}
