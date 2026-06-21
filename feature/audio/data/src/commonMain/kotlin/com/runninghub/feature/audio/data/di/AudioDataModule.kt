package com.runninghub.feature.audio.data.di

import com.runninghub.feature.audio.data.remote.api.AudioApi
import com.runninghub.feature.audio.data.repository.AudioRepositoryImpl
import com.runninghub.feature.audio.domain.AudioRepository
import org.koin.dsl.module

/**
 * Audio Data 模块的 Koin 依赖图。
 *
 * 本模块只注册文本转音频和音频任务查询相关实现。HttpClient、JSON 配置、
 * 认证拦截器和环境切换均由平台运行期组合根提供，避免 Audio Data 反向依赖 `shared`
 * 或应用入口。
 */
val audioDataModule = module {
    single { AudioApi(get()) }
    single<AudioRepository> { AudioRepositoryImpl(get()) }
}
