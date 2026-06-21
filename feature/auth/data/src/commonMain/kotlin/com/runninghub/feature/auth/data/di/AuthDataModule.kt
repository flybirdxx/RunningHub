package com.runninghub.feature.auth.data.di

import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.data.repository.AuthRepositoryImpl
import com.runninghub.feature.auth.data.repository.BalanceSnapshotRepositoryImpl
import com.runninghub.feature.auth.data.repository.ProfileCredentialRepositoryImpl
import com.runninghub.feature.auth.data.repository.SessionRestoreRepositoryImpl
import com.runninghub.feature.auth.data.repository.UserRepositoryImpl
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.BalanceSnapshotRepository
import com.runninghub.feature.auth.domain.GetLastKnownBalanceUseCase
import com.runninghub.feature.auth.domain.ProfileCredentialRepository
import com.runninghub.feature.auth.domain.SessionRestoreRepository
import com.runninghub.feature.auth.domain.UserRepository
import org.koin.dsl.module

/**
 * Auth Data 模块的 Koin 依赖图。
 *
 * 本模块只注册认证、用户资料、会话恢复、个人中心凭据和余额快照相关实现。
 * 其依赖的 HttpClient、CredentialStore、BalanceCache、TokenRefresher 和 SessionManager
 * 由 Core/迁移期组合根提供，避免 Auth Data 反向依赖 shared。
 */
val authDataModule = module {
    single { AuthApi(get()) }
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            credentialStore = get(),
            sessionManager = get(),
            tokenRefresher = get(),
            balanceCache = get(),
        )
    }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SessionRestoreRepository> { SessionRestoreRepositoryImpl(get()) }
    single<ProfileCredentialRepository> { ProfileCredentialRepositoryImpl(get()) }
    single<BalanceSnapshotRepository> { BalanceSnapshotRepositoryImpl(get()) }
    single { GetLastKnownBalanceUseCase(get()) }
}
