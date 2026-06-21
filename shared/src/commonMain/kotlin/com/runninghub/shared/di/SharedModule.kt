package com.runninghub.shared.di

import com.runninghub.core.network.installRunningHubMainClientDefaults
import com.runninghub.core.network.installRunningHubRefreshClientDefaults
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PreferencesSettingsStore
import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.core.storage.createDataStore
import com.runninghub.core.storage.createPermissionDataStore
import com.runninghub.core.network.auth.installRunningHubAuthInterceptors
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.feature.auth.domain.SessionManager
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * shared 模块的 Koin 依赖图。
 *
 * 该模块是迁移期遗留组合入口，只保留跨平台存储、网络客户端和会话状态对象。
 * 生产 Android/iOS 启动图已改为直接装配 core 与各 Feature Data 模块；这里继续保留是为了
 * 兼容尚未删除的 shared 编译目标。认证凭据、余额缓存和会话状态分别由
 * [CredentialStore]、[BalanceCache] 与 [SessionManager] 承担。
 * [QuickCreateDraftStore] 只作为原始持久化端口暴露给 QuickCreate data 模块，避免 shared
 * 继续持有具体业务 Repository 实现。
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

    single { PreferencesSettingsStore(get()) }
    // 凭据读取单独通过 CredentialStore 暴露，避免网络层继续依赖草稿、余额等设置职责。
    single<CredentialStore> { get<PreferencesSettingsStore>() }
    single<BalanceCache> { get<PreferencesSettingsStore>() }
    single<QuickCreateDraftStore> { get<PreferencesSettingsStore>() }
    // 权限申请轨迹属于 shared 的数据能力，通过领域接口暴露给 composeApp，避免 UI 组合根触达 DataStore 实现。
    single<PermissionStateStore> { createPermissionDataStore() }
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
}
