package com.runninghub.app.di

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
import com.runninghub.core.storage.MigratingCredentialStore
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PreferencesSettingsStore
import com.runninghub.core.storage.QuickCreateDraftStore
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
        // 当前仓库尚未登记 staging/dev 公开地址，Android 启动层先显式注入生产环境；
        // 后续只需在这里按构建类型替换 ApiEnvironment，不再改 Data 层 endpoint 拼接代码。
        RunningHubApiEnvironment.production().also(RunningHubApiEnvironment::configure)
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
    single<PermissionStateStore> { createPermissionDataStore() }
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
