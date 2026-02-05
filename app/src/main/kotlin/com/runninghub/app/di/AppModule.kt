/**
 * [INPUT]: 依赖 CoroutineDispatcher, SingletonComponent
 * [OUTPUT]: 对外提供 IO 调度器等全局单例依赖
 * [POS]: 依赖注入的顶层供应模块，管理系统级单例的生命周期
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
