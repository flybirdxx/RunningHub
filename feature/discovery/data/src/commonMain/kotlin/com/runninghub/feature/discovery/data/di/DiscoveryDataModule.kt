package com.runninghub.feature.discovery.data.di

import com.runninghub.feature.discovery.data.remote.api.WebAppCatalogApi
import com.runninghub.feature.discovery.data.repository.WebAppCatalogRepositoryImpl
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import org.koin.dsl.module

/**
 * Discovery Data 模块的 Koin 依赖图。
 *
 * 本模块只注册 WebApp 公开目录数据实现。HttpClient 由应用组合根提供，Repository 通过
 * Discovery Domain 的 [WebAppCatalogRepository] 暴露，避免 Presentation 直接触达 Data 类。
 */
val discoveryDataModule = module {
    single { WebAppCatalogApi(get()) }
    single<WebAppCatalogRepository> { WebAppCatalogRepositoryImpl(get()) }
}
