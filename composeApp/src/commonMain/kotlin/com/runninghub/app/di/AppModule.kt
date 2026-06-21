package com.runninghub.app.di

import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.platform.createMediaResolver
import com.runninghub.app.ui.feature.community.CommunityScreenModel
import com.runninghub.app.ui.feature.creator.CreatorProfileScreenModel
import com.runninghub.app.ui.feature.detail.AppDetailScreenModel
import com.runninghub.app.ui.feature.discovery.DiscoveryScreenModel
import com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapter
import com.runninghub.app.ui.feature.login.LoginScreenModel
import com.runninghub.app.ui.feature.plaza.PlazaScreenModel
import com.runninghub.app.ui.feature.profile.ProfileScreenModel
import com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModel
import com.runninghub.app.ui.feature.search.SearchScreenModel
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * composeApp 模块的 Koin 依赖图。
 *
 * 该组合根只负责绑定 composeApp 自身拥有的平台能力与 ScreenModel。
 * 数据层实现由运行期核心模块或具体 Feature Data 模块暴露为领域边界，commonMain 只做接口级装配，
 * 避免 ScreenModel 直接导入 data/local 或其他持久化实现命名空间。
 */
val appModule = module {
    single<MediaResolver> { createMediaResolver() }
    // AC-11：通用历史页已依赖 Task Domain 契约，适配器放在组合根侧连接 QuickCreate Domain。
    // 这样 QuickCreate Data 不再依赖 shared，后续 shared 历史页退役时只需删除这层兼容桥。
    single<GenerationHistoryRepository> { QuickCreateGenerationHistoryRepositoryAdapter(get()) }

    factoryOf(::DiscoveryScreenModel)
    factoryOf(::CommunityScreenModel)
    // AC-03：历史创作状态机已从生产源码退役，组合根只保留当前 QuickCreate 创作入口。
    // 这样可以避免两个创作流程同时请求模型、计费、上传或轮询任务。
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
