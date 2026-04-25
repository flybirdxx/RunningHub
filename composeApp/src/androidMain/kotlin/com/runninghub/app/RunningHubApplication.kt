package com.runninghub.app

import android.app.Application
import com.runninghub.app.di.appModule
import com.runninghub.shared.di.platformModule
import com.runninghub.shared.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RunningHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@RunningHubApplication)
            modules(sharedModules + platformModule() + appModule)
        }
    }
}
