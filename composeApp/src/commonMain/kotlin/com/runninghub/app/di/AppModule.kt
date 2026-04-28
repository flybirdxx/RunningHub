package com.runninghub.app.di

import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.platform.createMediaResolver
import com.runninghub.app.ui.feature.community.CommunityScreenModel
import com.runninghub.app.ui.feature.creator.CreatorProfileScreenModel
import com.runninghub.app.ui.feature.detail.AppDetailScreenModel
import com.runninghub.app.ui.feature.discovery.DiscoveryScreenModel
import com.runninghub.app.ui.feature.login.LoginScreenModel
import com.runninghub.app.ui.feature.profile.ProfileScreenModel
import com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModel
import com.runninghub.app.ui.feature.search.SearchScreenModel
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.data.local.createPermissionDataStore
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    single<MediaResolver> { createMediaResolver() }
    single<PermissionDataStore> { createPermissionDataStore() }

    factoryOf(::DiscoveryScreenModel)
    factoryOf(::CommunityScreenModel)
    factoryOf(::ProfileScreenModel)
    factoryOf(::SearchScreenModel)
    factoryOf(::AppDetailScreenModel)
    factoryOf(::CreatorProfileScreenModel)
    factoryOf(::LoginScreenModel)
    factoryOf(::QuickCreateScreenModel)
}
