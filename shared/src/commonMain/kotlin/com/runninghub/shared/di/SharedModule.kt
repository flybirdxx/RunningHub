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
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val sharedModule = module {
    single {
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

    single {
        val settingsRepo: SettingsRepository = get()
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }

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
            }
        }.also { client ->
            client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
                val url = context.url.buildString()
                if (url.contains("runninghub.cn") && !context.headers.contains("Authorization")) {
                    val token = settingsRepo.getAuthToken()
                    if (!token.isNullOrEmpty()) {
                        context.headers.append("Authorization", "Bearer $token")
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
    single<QuickCreateRepository> { QuickCreateRepositoryImpl(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
}
