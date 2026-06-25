package com.runninghub.app.di

import com.runninghub.app.BuildConfig
import com.runninghub.app.ModelCatalogInitialPreloader
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.network.auth.installRunningHubAuthInterceptors
import com.runninghub.core.network.ApiEnvironment
import com.runninghub.core.network.CountingNetworkActivityTracker
import com.runninghub.core.network.NetworkActivityTracker
import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.network.installRunningHubMainClientDefaults
import com.runninghub.core.network.installRunningHubRefreshClientDefaults
import com.runninghub.core.network.installRunningHubNetworkActivityTracking
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.AppStartupStore
import com.runninghub.core.storage.MigratingCredentialStore
import com.runninghub.core.storage.ModelCatalogCacheStore
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PreferencesSettingsStore
import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.core.storage.QuickCreateModelSelectionStore
import com.runninghub.core.storage.createDataStore
import com.runninghub.core.storage.createPermissionDataStore
import com.runninghub.core.storage.createSecureCredentialStore
import com.runninghub.feature.auth.domain.SessionManager
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Android 应用运行期核心依赖图。
 *
 * 本模块替代迁移期旧组合根中仍被启动层需要的核心绑定：JSON、DataStore-backed
 * 本地存储、安全凭据存储、SessionManager、TokenRefresher 和主 HttpClient。业务 Data 实现继续由各
 * Feature Data 模块注册，避免 Android Application 直接装配 `shared`。
 */
val androidRuntimeModule = module {
    single<ApiEnvironment>(createdAtStart = true) {
        // Android 环境只从 BuildConfig 读取公开 base URL 和可信主机；debug/release 的差异由
        // Gradle build type 注入，避免 Data 层或 commonMain 直接判断构建类型。
        androidApiEnvironment().also(RunningHubApiEnvironment::configure)
    }

    single {
        // 与历史组合根保持相同 JSON 容错策略，避免迁移组合根时改变远端响应解析行为。
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
    single(createdAtStart = true) { AndroidNetworkActivityLogObserver(androidContext(), get()) }
    single { PreferencesSettingsStore(get()) }
    single<CredentialStore> {
        // 新凭据写入 Android Keystore backed 存储；旧 DataStore 凭据按字段懒迁移，避免老用户会话丢失。
        MigratingCredentialStore(
            primary = createSecureCredentialStore(),
            legacy = get<PreferencesSettingsStore>(),
        )
    }
    single<BalanceCache> { get<PreferencesSettingsStore>() }
    single<QuickCreateDraftStore> { get<PreferencesSettingsStore>() }
    single<QuickCreateModelSelectionStore> { get<PreferencesSettingsStore>() }
    single<ModelCatalogCacheStore> { get<PreferencesSettingsStore>() }
    single<AppStartupStore> { get<PreferencesSettingsStore>() }
    single<PermissionStateStore> { createPermissionDataStore() }
    single {
        ModelCatalogInitialPreloader(
            startupStore = get(),
            modelCatalogRepository = get(),
        )
    }
    // SessionManager 保持可注入单例，由根 App 观察状态并决定 Login/Main 入口。
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
            // AC-11 运行验收需要观察 Tab 切换后的请求数量趋势；计数器只记录聚合数量，不记录 URL 或认证信息。
            client.installRunningHubNetworkActivityTracking(get())
            // 401 失效只通过 SessionManager 传播，避免业务 Repository 或 UI 自行处理根导航。
            client.installRunningHubAuthInterceptors(
                credentialStore = credentialStore,
                tokenRefresher = tokenRefresher,
                onSessionExpired = { sessionManager.expire() },
            )
        }
    }
}

/**
 * 根据 Android build type 注入的 BuildConfig 构造 API 环境。
 *
 * 这些值不是凭据，只包含公开服务地址和允许携带认证头的精确主机。没有显式传入 Gradle
 * property 时，debug 与 release 都会回退到生产地址，保证本地构建不依赖外部配置；
 * 需要 staging/dev 时只改构建参数，不改 Data 层 endpoint。
 */
private fun androidApiEnvironment(): ApiEnvironment =
    ApiEnvironment(
        webBaseUrl = BuildConfig.RUNNINGHUB_WEB_BASE_URL,
        apiBaseUrl = BuildConfig.RUNNINGHUB_API_BASE_URL,
        userCenterBaseUrl = BuildConfig.RUNNINGHUB_USER_CENTER_BASE_URL,
        taskBaseUrl = BuildConfig.RUNNINGHUB_TASK_BASE_URL,
        openApiV2BaseUrl = BuildConfig.RUNNINGHUB_OPEN_API_V2_BASE_URL,
        trustedAuthHosts = BuildConfig.RUNNINGHUB_TRUSTED_AUTH_HOSTS.toTrustedHostSet(),
    )

private fun String.toTrustedHostSet(): Set<String> =
    split(',')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
