package com.runninghub.feature.community.data.di

import com.runninghub.feature.community.data.remote.api.PlazaApi
import com.runninghub.feature.community.data.repository.PlazaRepositoryImpl
import com.runninghub.feature.community.domain.PlazaRepository
import org.koin.dsl.module

/**
 * Community Data 模块的 Koin 依赖图。
 *
 * 本模块只注册社区广场数据实现。HttpClient 由应用组合根提供，Repository 通过
 * Community Domain 的 [PlazaRepository] 暴露，避免 composeApp 或 Presentation 直接依赖 Data 类。
 */
val communityDataModule = module {
    single { PlazaApi(get()) }
    single<PlazaRepository> { PlazaRepositoryImpl(get()) }
}
