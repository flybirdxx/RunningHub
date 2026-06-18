package com.runninghub.shared.di

import com.runninghub.shared.data.local.SettingsRepositoryImpl
import com.runninghub.shared.data.local.createDataStore
import com.runninghub.shared.data.remote.api.AudioApi
import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.repository.AudioRepositoryImpl
import com.runninghub.shared.data.repository.AuthRepositoryImpl
import com.runninghub.shared.data.repository.QuickCreateRepositoryImpl
import com.runninghub.shared.data.repository.UserRepositoryImpl
import com.runninghub.shared.data.repository.WebAppRepositoryImpl
import com.runninghub.shared.domain.repository.AudioRepository
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

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

    single<SettingsRepository> { SettingsRepositoryImpl(get()) }

    // Dedicated non-Auth HttpClient for token refresh (prevents infinite 401 loop)
    single<HttpClient>(named("refreshClient")) {
        HttpClient {
            install(ContentNegotiation) { json(get<Json>()) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }

    single {
        val settingsRepo: SettingsRepository = get()
        val json = get<Json>()
        val refreshMutex = Mutex()

        HttpClient {
            install(ContentNegotiation) { json(json) }

            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }

            defaultRequest {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/144.0.0.0 Mobile Safari/537.36")
                header("Accept", "application/json, text/plain, */*")
                header("Origin", "https://www.runninghub.cn")
                header("Referer", "https://www.runninghub.cn/")
                header("user-language", "zh_CN")
            }
        }.also { client ->
            // REQUEST interceptor: attach Authorization header
            client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
                val url = context.url.buildString()
                if (url.contains("runninghub.cn") && !context.headers.contains("Authorization")) {
                    val token = settingsRepo.getAuthToken()
                    if (!token.isNullOrEmpty()) {
                        context.headers.append("Authorization", "Bearer $token")
                    }
                    val cookie = settingsRepo.getCookie()
                    if (!cookie.isNullOrEmpty() && !context.headers.contains(HttpHeaders.Cookie)) {
                        context.headers.append(HttpHeaders.Cookie, cookie)
                    }
                }
            }

            // RESPONSE interceptor: 401 → refresh → retry (deduped)
            val refreshClient = get<HttpClient>(named("refreshClient"))
            client.responsePipeline.intercept(HttpResponsePipeline.State) {
                val response = context.response
                if (response.status == HttpStatusCode.Unauthorized) {
                    // Read token OUTSIDE the mutex so we can detect concurrent refreshes
                    val tokenBeforeLock = settingsRepo.getAuthToken()
                    val refreshed = refreshMutex.withLock {
                        // Re-read inside lock — if another coroutine refreshed, token will differ
                        val currentToken = settingsRepo.getAuthToken()
                        if (tokenBeforeLock != currentToken && !currentToken.isNullOrEmpty()) {
                            return@withLock true
                        }

                        val rt = settingsRepo.getRefreshToken() ?: return@withLock false
                        try {
                            val refreshResponse = refreshClient.post("https://www.runninghub.cn/uc/token/refresh") {
                                contentType(ContentType.Application.Json)
                                header("Authorization", "Bearer $rt")
                                setBody(emptyMap<String, String>())
                            }
                            if (refreshResponse.status == HttpStatusCode.OK) {
                                val body = refreshResponse.bodyAsText()
                                // Parse access_token manually to avoid DTO dependency in DI module
                                val tokenMatch = Regex(""""access_token"\s*:\s*"([^"]+)"""").find(body)
                                val refreshMatch = Regex(""""refresh_token"\s*:\s*"([^"]+)"""").find(body)
                                if (tokenMatch != null) {
                                    settingsRepo.setAuthToken(tokenMatch.groupValues[1])
                                    refreshMatch?.groupValues?.get(1)?.let { settingsRepo.setRefreshToken(it) }
                                    return@withLock true
                                }
                            }
                        } catch (e: Exception) {
                            // Log refresh failure for debugging; do NOT silently discard root cause
                            println("[TokenRefresh] Failed: ${e.message}")
                        }
                        false
                    }

                    if (!refreshed) {
                        SessionExpiredHandler.expire()
                    }
                }
            }
        }
    }

    single { RunningHubApi(get()) }
    single { AudioApi(get()) }
    single { QuickCreateApi(get(), get()) }

    single<WebAppRepository> { WebAppRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<AudioRepository> { AudioRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<QuickCreateRepository> { QuickCreateRepositoryImpl(get(), get(), get()) }
}
