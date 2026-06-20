package com.runninghub.shared.di

import com.runninghub.core.network.installRunningHubMainClientDefaults
import com.runninghub.core.network.installRunningHubRefreshClientDefaults
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.core.network.auth.installRunningHubAuthInterceptors
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.shared.data.local.SettingsRepositoryImpl
import com.runninghub.shared.data.local.createDataStore
import com.runninghub.shared.data.remote.api.AudioApi
import com.runninghub.shared.data.remote.api.ModelCatalogApi
import com.runninghub.shared.data.remote.api.PlazaApi
import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.repository.AudioRepositoryImpl
import com.runninghub.shared.data.repository.AuthRepositoryImpl
import com.runninghub.shared.data.repository.BalanceSnapshotRepositoryImpl
import com.runninghub.shared.data.repository.GenerationHistoryRepositoryImpl
import com.runninghub.shared.data.repository.ModelCatalogRepositoryImpl
import com.runninghub.shared.data.repository.ModelInvocationRepositoryImpl
import com.runninghub.shared.data.repository.PlazaRepositoryImpl
import com.runninghub.shared.data.repository.ProfileCredentialRepositoryImpl
import com.runninghub.shared.data.repository.QuickCreateDraftRepositoryImpl
import com.runninghub.shared.data.repository.QuickCreateRepositoryImpl
import com.runninghub.shared.data.repository.SessionRestoreRepositoryImpl
import com.runninghub.shared.data.repository.UserRepositoryImpl
import com.runninghub.shared.data.repository.WebAppRepositoryImpl
import com.runninghub.shared.domain.repository.AudioRepository
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.BalanceSnapshotRepository
import com.runninghub.shared.domain.repository.GenerationHistoryRepository
import com.runninghub.shared.domain.repository.ModelCatalogRepository
import com.runninghub.shared.domain.repository.ModelInvocationRepository
import com.runninghub.shared.domain.repository.PlazaRepository
import com.runninghub.shared.domain.repository.ProfileCredentialRepository
import com.runninghub.shared.domain.repository.QuickCreateDraftRepository
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import com.runninghub.shared.domain.session.SessionManager
import com.runninghub.shared.domain.session.SessionRestoreRepository
import com.runninghub.shared.domain.usecase.GetLastKnownBalanceUseCase
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * shared 模块的 Koin 依赖图。
 *
 * 该模块集中注册跨平台数据存储、网络客户端、Repository 实现和会话状态对象。
 * Data 层依赖通过接口暴露给上层，认证凭据、余额缓存、草稿和会话状态分别由
 * [CredentialStore]、[BalanceCache]、[QuickCreateDraftRepository] 与 [SessionManager] 承担。
 * [QuickCreateDraftStore] 只作为 Data 实现的原始持久化端口，避免 UI 继续依赖宽泛的设置仓库。
 */
val sharedModule = module {
    single {
        // NOTE: coerceInputValues=true prevents crashes on type mismatches but can SILENTLY
        // accept malformed JSON (e.g., String->Int coercion, renamed fields→defaults).
        // Consider disabling in DEBUG builds or adding API contract tests.
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            prettyPrint = false
            coerceInputValues = true
        }
    }

    single { createDataStore() }

    single { SettingsRepositoryImpl(get()) }
    // 凭据读取单独通过 CredentialStore 暴露，避免网络层继续依赖草稿、余额等设置职责。
    single<CredentialStore> { get<SettingsRepositoryImpl>() }
    single<BalanceCache> { get<SettingsRepositoryImpl>() }
    single<QuickCreateDraftStore> { get<SettingsRepositoryImpl>() }
    single<QuickCreateDraftRepository> { QuickCreateDraftRepositoryImpl(get(), get()) }
    single<SessionRestoreRepository> { SessionRestoreRepositoryImpl(get()) }
    // 会话状态使用可注入单例，避免全局 object 在测试和多入口场景中残留旧状态。
    single { SessionManager(get()) }

    // Dedicated non-Auth HttpClient for token refresh (prevents infinite 401 loop)
    single<HttpClient>(named("refreshClient")) {
        HttpClient {
            installRunningHubRefreshClientDefaults(get<Json>())
        }
    }

    single {
        TokenRefresher(
            refreshClient = get(named("refreshClient")),
            credentialStore = get(),
        )
    }

    single {
        val credentialStore: CredentialStore = get()
        val sessionManager: SessionManager = get()
        val tokenRefresher: TokenRefresher = get()
        val json = get<Json>()

        HttpClient {
            installRunningHubMainClientDefaults(json)
        }.also { client ->
            // 认证头注入和 401 后刷新属于 core/network 横切能力，组合根只负责连接会话失效回调。
            client.installRunningHubAuthInterceptors(
                credentialStore = credentialStore,
                tokenRefresher = tokenRefresher,
                onSessionExpired = { sessionManager.expire() },
            )
        }
    }

    single { RunningHubApi(get()) }
    single { AudioApi(get()) }
    single { QuickCreateApi(get(), get()) }
    single { ModelCatalogApi(get()) }
    single { PlazaApi(get()) }

    single<WebAppRepository> { WebAppRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<AudioRepository> { AudioRepositoryImpl(get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            credentialStore = get(),
            sessionManager = get(),
            tokenRefresher = get(),
            balanceCache = get(),
        )
    }
    single<QuickCreateRepository> { QuickCreateRepositoryImpl(get(), get(), get()) }
    single<ModelCatalogRepository> { ModelCatalogRepositoryImpl(get(), get()) }
    single<ModelInvocationRepository> { ModelInvocationRepositoryImpl(get(), get(), get()) }
    single<GenerationHistoryRepository> { GenerationHistoryRepositoryImpl(get()) }
    single<PlazaRepository> { PlazaRepositoryImpl(get()) }
    single<ProfileCredentialRepository> { ProfileCredentialRepositoryImpl(get()) }
    single<BalanceSnapshotRepository> { BalanceSnapshotRepositoryImpl(get()) }
    single { GetLastKnownBalanceUseCase(get()) }
}
