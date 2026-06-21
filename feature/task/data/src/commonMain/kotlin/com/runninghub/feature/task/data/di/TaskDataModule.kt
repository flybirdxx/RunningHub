package com.runninghub.feature.task.data.di

import com.runninghub.feature.task.data.remote.api.WebAppTaskApi
import com.runninghub.feature.task.data.repository.WebAppTaskRepositoryImpl
import com.runninghub.feature.task.domain.WebAppTaskHistoryRepository
import com.runninghub.feature.task.domain.WebAppTaskRepository
import org.koin.dsl.module

/**
 * Task Data 模块的 Koin 依赖图。
 *
 * 本模块注册需要 API Key 的 WebApp 运行链路实现。HttpClient 和 CredentialStore 由应用组合根提供，
 * Repository 只通过 Task Domain 的窄接口暴露，避免 composeApp 直接依赖 Data 类或 shared 兼容实现。
 */
val taskDataModule = module {
    single { WebAppTaskApi(get()) }
    single { WebAppTaskRepositoryImpl(get(), get()) }
    single<WebAppTaskRepository> { get<WebAppTaskRepositoryImpl>() }
    single<WebAppTaskHistoryRepository> { get<WebAppTaskRepositoryImpl>() }
}
