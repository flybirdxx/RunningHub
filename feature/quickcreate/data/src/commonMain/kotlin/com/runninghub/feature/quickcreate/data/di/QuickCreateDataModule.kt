package com.runninghub.feature.quickcreate.data.di

import com.runninghub.feature.quickcreate.data.remote.api.QuickCreateApi
import com.runninghub.feature.quickcreate.data.repository.QuickCreateDraftRepositoryImpl
import com.runninghub.feature.quickcreate.data.repository.QuickCreateModelSelectionRepositoryImpl
import com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImpl
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import org.koin.dsl.module

/**
 * QuickCreate 功能的数据层 Koin 模块。
 *
 * 该模块注册快捷创作远程 API、远程 Repository 实现和草稿 Repository 实现。
 * 草稿底层字符串存储由 core/storage 的 `QuickCreateDraftStore` 暴露，本模块负责把它适配为
 * QuickCreate 领域仓库。通用历史页所需的遗留兼容桥放在 composeApp 组合层注册，
 * 因此本模块只绑定 QuickCreate Domain 的窄仓库接口，保持 `Data -> Domain` 的依赖方向。
 */
val quickCreateDataModule = module {
    single<QuickCreateDraftRepository> { QuickCreateDraftRepositoryImpl(get(), get()) }
    single<QuickCreateModelSelectionRepository> { QuickCreateModelSelectionRepositoryImpl(get()) }
    single { QuickCreateApi(get(), get()) }
    single { QuickCreateRepositoryImpl(get(), get(), get(), get()) }
    single<QuickCreationFeePreviewRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationGenerationRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationInspirationRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationMediaUploadRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationModelCatalogRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationProjectRepository> { get<QuickCreateRepositoryImpl>() }
    single<QuickCreationTaskHistoryRepository> { get<QuickCreateRepositoryImpl>() }
}
