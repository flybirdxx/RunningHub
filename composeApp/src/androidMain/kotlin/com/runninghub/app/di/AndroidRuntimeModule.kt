package com.runninghub.app.di

import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.network.auth.installRunningHubAuthInterceptors
import com.runninghub.core.network.CountingNetworkActivityTracker
import com.runninghub.core.network.NetworkActivityTracker
import com.runninghub.core.network.installRunningHubMainClientDefaults
import com.runninghub.core.network.installRunningHubRefreshClientDefaults
import com.runninghub.core.network.installRunningHubNetworkActivityTracking
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PreferencesSettingsStore
import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.core.storage.createDataStore
import com.runninghub.core.storage.createPermissionDataStore
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
 * 本地存储、SessionManager、TokenRefresher 和主 HttpClient。业务 Data 实现继续由各
 * Feature Data 模块注册，避免 Android Application 直接装配 `shared`。
 */
val androidRuntimeModule = module {
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
    // 凭据、余额和草稿先复用同一个迁移期实现；L2 安全存储治理时只替换 CredentialStore 绑定。
    single<CredentialStore> { get<PreferencesSettingsStore>() }
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
