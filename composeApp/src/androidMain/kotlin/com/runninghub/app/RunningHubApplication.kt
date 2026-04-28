package com.runninghub.app

import android.app.Application
import com.runninghub.app.di.appModule
import com.runninghub.app.platform.initMediaResolver
import com.runninghub.shared.data.local.initDataStore
import com.runninghub.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RunningHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initDataStore(this)
        initMediaResolver(this)
        startKoin {
            androidLogger()
            androidContext(this@RunningHubApplication)
            modules(sharedModule, appModule)
        }
    }
}
