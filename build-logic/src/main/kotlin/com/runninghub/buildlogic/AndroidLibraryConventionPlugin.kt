package com.runninghub.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP 库模块约定插件。
 *
 * 自 AGP 9.0 起，`com.android.library` 不再兼容 Kotlin Multiplatform，官方要求库模块改用
 * `com.android.kotlin.multiplatform.library`。该插件由 `kotlin { android { ... } }` DSL 提供
 * android target，因此不再需要 `androidTarget()`（见 KotlinMultiplatformConventionPlugin）。
 *
 * namespace 按模块 Gradle 路径自动推导：`:feature:auth:data` -> `com.runninghub.feature.auth.data`，
 * 各模块 build 脚本不再需要单独声明 `android { namespace = ... }`。
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // 保证 KMP 与 AGP KMP 库插件的应用顺序：先有 KotlinMultiplatformExtension，
        // AGP 插件才能在其上注册 android target 扩展。重复 apply 同一插件是幂等的。
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            // AGP 同时注册了 "android"（当前）与 "androidLibrary"（已废弃）两个同类型扩展，
            // 按类型取会二义，必须按名字取当前的 "android"。
            val android = (this as ExtensionAware).extensions
                .getByName("android") as KotlinMultiplatformAndroidLibraryTarget

            android.namespace = androidNamespace()
            android.compileSdk = AndroidSdk.compile
            android.minSdk = AndroidSdk.libraryMin
            android.compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    /** `:feature:auth:data` -> `com.runninghub.feature.auth.data`。 */
    private fun Project.androidNamespace(): String = "com.runninghub" + path.replace(":", ".")
}
