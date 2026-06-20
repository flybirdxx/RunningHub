package com.runninghub.app.di

import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.platform.createMediaResolver
import com.runninghub.app.ui.feature.community.CommunityScreenModel
import com.runninghub.app.ui.feature.create.CreateScreenModel
import com.runninghub.app.ui.feature.creator.CreatorProfileScreenModel
import com.runninghub.app.ui.feature.detail.AppDetailScreenModel
import com.runninghub.app.ui.feature.discovery.DiscoveryScreenModel
import com.runninghub.app.ui.feature.login.LoginScreenModel
import com.runninghub.app.ui.feature.plaza.PlazaScreenModel
import com.runninghub.app.ui.feature.profile.ProfileScreenModel
import com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModel
import com.runninghub.app.ui.feature.search.SearchScreenModel
import com.runninghub.shared.data.local.createPermissionDataStore
import com.runninghub.shared.domain.permission.PermissionStateStore
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * composeApp 模块的 Koin 依赖图。
 *
 * 该组合根负责把平台实现绑定到 Presentation 可依赖的领域边界。
 * 权限状态存储通过 [PermissionStateStore] 暴露，避免页面直接依赖 data/local 的
 * DataStore 实现命名空间。
 */
val appModule = module {
    single<MediaResolver> { createMediaResolver() }
    single<PermissionStateStore> { createPermissionDataStore() }

    factoryOf(::DiscoveryScreenModel)
    factoryOf(::CommunityScreenModel)
    factory { CreateScreenModel(get(), get()) }
    factoryOf(::PlazaScreenModel)
    factoryOf(::ProfileScreenModel)
    factoryOf(::SearchScreenModel)
    factoryOf(::AppDetailScreenModel)
    factoryOf(::CreatorProfileScreenModel)
    factoryOf(::LoginScreenModel)
    factoryOf(::QuickCreateScreenModel)
}
