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
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationStateHolderFactory
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import kotlinx.coroutines.Dispatchers
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
    single<QuickCreateMediaResolver> {
        MediaResolverQuickCreateMediaResolver(get<MediaResolver>())
    }
    // 通用历史页已依赖 Task Domain 契约，迁移期仍用唯一兼容桥连接 QuickCreate 历史源。
    // 删除条件是 feature:task:data 正式提供完整 GenerationHistoryRepository，而不是只接入 WebApp 历史列表。
    single<GenerationHistoryRepository> { QuickCreateGenerationHistoryRepositoryAdapter(get()) }
    single {
        QuickCreatePresentationStateHolderFactory(
            historyRepository = get(),
            modelCatalogRepository = get(),
            generationRepository = get(),
            feePreviewRepository = get(),
            inspirationRepository = get(),
            mediaUploadRepository = get(),
            projectRepository = get(),
            mediaResolver = get<QuickCreateMediaResolver>(),
            draftRepository = get(),
            ioDispatcher = Dispatchers.Default,
        )
    }

    factoryOf(::DiscoveryScreenModel)
    factoryOf(::CommunityScreenModel)
    // AC-03：历史创作状态机已从生产源码退役，组合根只保留当前 QuickCreate 创作入口。
    // 这样可以避免两个创作流程同时请求模型、计费、上传或轮询任务。
    factoryOf(::PlazaScreenModel)
    factoryOf(::ProfileScreenModel)
    factoryOf(::SearchScreenModel)
    factory {
        AppDetailScreenModel(
            webAppCatalogRepository = get(),
            webAppTaskRepository = get(),
            mediaResolver = get(),
            ioDispatcher = Dispatchers.Default,
        )
    }
    factoryOf(::CreatorProfileScreenModel)
    factoryOf(::LoginScreenModel)
    factoryOf(::QuickCreateScreenModel)
}

/**
 * 把应用壳的平台媒体读取能力适配为 QuickCreate Presentation 模块的上传端口。
 *
 * Android/iOS 的 URI 权限、文件选择器和安全作用域读取仍由 [MediaResolver] 处理；
 * ScreenModel 和 feature presentation 只接收平台无关的字节、展示名和文件大小，避免页面门面继续
 * 直接依赖 app.platform 类型。
 *
 * @param mediaResolver 应用平台层提供的媒体解析器，负责处理本地 URI 权限和实际文件读取。
 */
private class MediaResolverQuickCreateMediaResolver(
    private val mediaResolver: MediaResolver,
) : QuickCreateMediaResolver {
    /**
     * 读取本地媒体 URI 对应的原始字节。
     *
     * 组合根只负责端口适配，不在这里压缩、转码或修改内容，确保上传校验仍由
     * QuickCreate Presentation 的上传协调器统一处理。
     */
    override fun readBytes(uri: String): ByteArray = mediaResolver.readBytes(uri)

    /**
     * 返回平台文件选择器可提供的展示名。
     *
     * `null` 表示平台层无法解析名称，调用方需要继续使用上传端口的兜底命名规则；
     * 这里不生成 UI 文案，避免 composeApp 组合根承担展示语义。
     */
    override fun getDisplayName(uri: String): String? = mediaResolver.getDisplayName(uri)

    /**
     * 返回本地媒体文件大小，单位为字节。
     *
     * 负数或不可识别大小的兼容语义沿用 [MediaResolver] 平台实现，后续上传限制仍由
     * QuickCreate Presentation 统一判定，避免 Android/iOS 行为在组合根处分叉。
     */
    override fun getFileSizeBytes(uri: String): Long = mediaResolver.getFileSizeBytes(uri)
}
