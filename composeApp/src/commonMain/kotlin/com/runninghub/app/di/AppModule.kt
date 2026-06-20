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
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * composeApp 模块的 Koin 依赖图。
 *
 * 该组合根只负责绑定 composeApp 自身拥有的平台能力与 ScreenModel。
 * 数据层实现由 sharedModule 暴露为领域边界，避免 commonMain 直接导入 data/local
 * 或其他持久化实现命名空间。
 */
val appModule = module {
    single<MediaResolver> { createMediaResolver() }

    factoryOf(::DiscoveryScreenModel)
    factoryOf(::CommunityScreenModel)
    factory {
        CreateScreenModel(
            historyRepository = get(),
            mediaResolver = get(),
            modelCatalogRepository = get(),
            generationRepository = get(),
            feePreviewRepository = get(),
            mediaUploadRepository = get(),
        )
    }
    factoryOf(::PlazaScreenModel)
    factoryOf(::ProfileScreenModel)
    factoryOf(::SearchScreenModel)
    factoryOf(::AppDetailScreenModel)
    factoryOf(::CreatorProfileScreenModel)
    factoryOf(::LoginScreenModel)
    factory {
        QuickCreateScreenModel(
            historyRepository = get(),
            modelCatalogRepository = get(),
            generationRepository = get(),
            feePreviewRepository = get(),
            inspirationRepository = get(),
            mediaUploadRepository = get(),
            projectRepository = get(),
            mediaResolver = get(),
            draftRepository = get(),
        )
    }
}
