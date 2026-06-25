package com.runninghub.app.di

import com.runninghub.core.network.CountingNetworkActivityTracker
import com.runninghub.core.network.ApiEnvironment
import com.runninghub.core.network.NetworkActivityTracker
import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.network.auth.installRunningHubAuthInterceptors
import com.runninghub.core.network.installRunningHubMainClientDefaults
import com.runninghub.core.network.installRunningHubRefreshClientDefaults
import com.runninghub.core.network.installRunningHubNetworkActivityTracking
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.MigratingCredentialStore
import com.runninghub.core.storage.ModelCatalogCacheStore
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PreferencesSettingsStore
import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.core.storage.QuickCreateModelSelectionStore
import com.runninghub.core.storage.createDataStore
import com.runninghub.core.storage.createPermissionDataStore
import com.runninghub.core.storage.createSecureCredentialStore
import com.runninghub.feature.audio.data.di.audioDataModule
import com.runninghub.feature.auth.data.di.authDataModule
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.feature.community.data.di.communityDataModule
import com.runninghub.feature.discovery.data.di.discoveryDataModule
import com.runninghub.feature.model.data.di.modelDataModule
import com.runninghub.feature.quickcreate.data.di.quickCreateDataModule
import com.runninghub.feature.task.data.di.taskDataModule
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSProcessInfo

/**
 * iOS 应用运行期核心依赖图。
 *
 * iOS 包装工程没有 Android Application，因此必须在 SwiftUI 入口显式调用
 * [startRunningHubKoin]。本模块与 Android 运行期模块保持相同的领域绑定：DataStore-backed
 * 非敏感本地存储、Keychain 凭据存储、SessionManager、TokenRefresher、主 HttpClient 和网络活动计数器均在平台层装配，
 * commonMain 与 Presentation 层仍只依赖领域接口，避免把 Data 实现泄漏到共享 UI。
 */
val iosRuntimeModule = module {
    single<ApiEnvironment>(createdAtStart = true) {
        // iOS 环境只从进程环境变量读取公开 base URL 和可信主机；没有显式传入时回退生产环境。
        // 这样 macOS runner、Xcode scheme 或 TestFlight 包装层都可以切换环境，而不改 Data 层 endpoint。
        iosApiEnvironment().also(RunningHubApiEnvironment::configure)
    }

    single {
        // 与 Android 启动图保持同一 JSON 容错策略，避免 iOS 运行验收与 Android 解析行为分叉。
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
    single<NetworkActivityTracker> { CountingNetworkActivityTracker() }
    single { PreferencesSettingsStore(get()) }
    single<CredentialStore> {
        // 新凭据写入 iOS Keychain；旧 DataStore 凭据按字段懒迁移，避免迁移升级后强制用户重新登录。
        MigratingCredentialStore(
            primary = createSecureCredentialStore(),
            legacy = get<PreferencesSettingsStore>(),
        )
    }
    single<BalanceCache> { get<PreferencesSettingsStore>() }
    single<QuickCreateDraftStore> { get<PreferencesSettingsStore>() }
    single<QuickCreateModelSelectionStore> { get<PreferencesSettingsStore>() }
    single<ModelCatalogCacheStore> { get<PreferencesSettingsStore>() }
    single<PermissionStateStore> { createPermissionDataStore() }
    single { SessionManager(get()) }

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
            // iOS Simulator 冒烟同样需要确认不可见页面不会持续产生请求；计数器只保留聚合数量，不记录 URL 或凭据。
            client.installRunningHubNetworkActivityTracking(get())
            // 401 失效只通过 SessionManager 传播，保持双端根导航和会话失效语义一致。
            client.installRunningHubAuthInterceptors(
                credentialStore = credentialStore,
                tokenRefresher = tokenRefresher,
                onSessionExpired = { sessionManager.expire() },
            )
        }
    }
}

/**
 * 启动 iOS 包装应用的 Koin 依赖图。
 *
 * SwiftUI App 的初始化和预览可能多次进入该函数，因此这里先检查全局 Koin 上下文；
 * 已启动时直接返回，避免重复 startKoin 造成 iOS 启动失败。该函数只做依赖图装配，
 * 不触发登录、网络请求或会话恢复；会话恢复仍由 commonMain 的 App 根组合函数处理。
 */
fun startRunningHubKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) {
        return
    }

    startKoin {
        modules(
            iosRuntimeModule,
            audioDataModule,
            authDataModule,
            communityDataModule,
            discoveryDataModule,
            modelDataModule,
            taskDataModule,
            quickCreateDataModule,
            appModule,
        )
    }
}

/**
 * 根据 iOS 进程环境变量构造 API 环境。
 *
 * Xcode scheme、macOS CI 或包装层可以通过 `RUNNINGHUB_*` 环境变量注入 staging/dev 地址；
 * 未设置时使用生产默认值。这里不读取 Token、Cookie 或 API Key，只读取公开服务地址和认证主机白名单。
 */
private fun iosApiEnvironment(): ApiEnvironment {
    val defaults = RunningHubApiEnvironment.production()
    val environment = NSProcessInfo.processInfo.environment

    fun value(name: String, fallback: String): String =
        (environment[name] as? String)?.takeIf { it.isNotBlank() } ?: fallback

    fun hosts(fallback: Set<String>): Set<String> =
        value("RUNNINGHUB_TRUSTED_AUTH_HOSTS", fallback.joinToString(","))
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    return ApiEnvironment(
        webBaseUrl = value("RUNNINGHUB_WEB_BASE_URL", defaults.webBaseUrl),
        apiBaseUrl = value("RUNNINGHUB_API_BASE_URL", defaults.apiBaseUrl),
        userCenterBaseUrl = value("RUNNINGHUB_USER_CENTER_BASE_URL", defaults.userCenterBaseUrl),
        taskBaseUrl = value("RUNNINGHUB_TASK_BASE_URL", defaults.taskBaseUrl),
        openApiV2BaseUrl = value("RUNNINGHUB_OPEN_API_V2_BASE_URL", defaults.openApiV2BaseUrl),
        trustedAuthHosts = hosts(defaults.trustedAuthHosts),
    )
}
