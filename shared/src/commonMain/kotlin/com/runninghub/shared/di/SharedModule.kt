package com.runninghub.shared.di

import com.runninghub.shared.data.remote.AudioApiService
import com.runninghub.shared.data.remote.WebAppApiService
import com.runninghub.shared.data.remote.createApiClient
import com.runninghub.shared.data.repository.AudioRepositoryImpl
import com.runninghub.shared.data.repository.SettingsRepositoryImpl
import com.runninghub.shared.data.repository.TaskRepositoryImpl
import com.runninghub.shared.data.repository.UserRepositoryImpl
import com.runninghub.shared.data.repository.WebAppRepositoryImpl
import com.runninghub.shared.domain.repository.AudioRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.TaskRepository
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module

val networkModule = module {
    single {
        val settingsRepo: SettingsRepository = get()
        createApiClient(
            getApiKey = { runBlocking { settingsRepo.getApiKeySync() } },
            getEnterpriseApiKey = { runBlocking { settingsRepo.getEnterpriseApiKeySync() } },
            getCookie = { runBlocking { settingsRepo.getCookieSync() } }
        )
    }
    single { WebAppApiService(get()) }
    single { AudioApiService(get()) }
}

val repositoryModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<WebAppRepository> { WebAppRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<TaskRepository> { TaskRepositoryImpl(get()) }
    single<AudioRepository> { AudioRepositoryImpl(get()) }
}

expect fun platformModule(): Module

val sharedModules = listOf(networkModule, repositoryModule)
