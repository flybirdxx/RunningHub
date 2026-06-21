package com.runninghub.app

import android.app.Application
import com.runninghub.app.di.androidRuntimeModule
import com.runninghub.app.di.appModule
import com.runninghub.app.platform.initMediaResolver
import com.runninghub.core.storage.initDataStore
import com.runninghub.feature.auth.data.di.authDataModule
import com.runninghub.feature.community.data.di.communityDataModule
import com.runninghub.feature.discovery.data.di.discoveryDataModule
import com.runninghub.feature.quickcreate.data.di.quickCreateDataModule
import com.runninghub.feature.task.data.di.taskDataModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Android 应用启动入口。
 *
 * 本类只负责平台级初始化和 Koin 组合根装配。DataStore 由 core:storage 提供平台文件路径，
 * 运行期核心依赖由 androidRuntimeModule 注册；初始化顺序必须先准备 DataStore 和媒体解析器，
 * 再启动 Koin，避免 Repository 创建时访问未配置的本地存储或媒体解析器。
 */
class RunningHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initDataStore(this)
        initMediaResolver(this)
        startKoin {
            androidLogger()
            androidContext(this@RunningHubApplication)
            // 应用组合根负责装配运行期核心能力和已迁出的 feature data 模块，shared 不再参与生产启动图。
            modules(
                androidRuntimeModule,
                authDataModule,
                communityDataModule,
                discoveryDataModule,
                taskDataModule,
                quickCreateDataModule,
                appModule,
            )
        }
    }
}
