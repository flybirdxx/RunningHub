package com.runninghub.feature.model.data.di

import com.runninghub.feature.model.data.remote.api.ModelCatalogApi
import com.runninghub.feature.model.data.repository.ModelCatalogRepositoryImpl
import com.runninghub.feature.model.data.repository.ModelEndpointRegistry
import com.runninghub.feature.model.data.repository.ModelInvocationRepositoryImpl
import com.runninghub.feature.model.domain.ModelCatalogRepository
import com.runninghub.feature.model.domain.ModelInvocationRepository
import org.koin.dsl.module

/**
 * 标准模型 Data 模块的 Koin 依赖图。
 *
 * 本模块只注册标准模型目录、字段映射和 OpenAPI 调用实现。HttpClient、JSON 配置、
 * 认证拦截器、API 环境和凭据存储由平台运行期组合根提供，避免模型 Data 反向依赖
 * `shared` 或应用入口。
 */
val modelDataModule = module {
    single { ModelCatalogApi(get()) }
    single { ModelEndpointRegistry() }
    single<ModelCatalogRepository> { ModelCatalogRepositoryImpl(get(), get(), get()) }
    single<ModelInvocationRepository> { ModelInvocationRepositoryImpl(get(), get(), get(), get()) }
}
