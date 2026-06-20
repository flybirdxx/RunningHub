package com.runninghub.app

import android.app.Application
import com.runninghub.app.di.appModule
import com.runninghub.app.platform.initMediaResolver
import com.runninghub.feature.quickcreate.data.di.quickCreateDataModule
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
            // 应用组合根负责装配 feature data 模块，避免 shared 继续反向持有 QuickCreate 远程实现。
            modules(sharedModule, quickCreateDataModule, appModule)
        }
    }
}
