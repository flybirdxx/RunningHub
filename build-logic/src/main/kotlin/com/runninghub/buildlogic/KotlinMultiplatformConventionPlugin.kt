package com.runninghub.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")

        configureAndroidTargetWhenPresent()
    }

    private fun Project.configureAndroidTargetWhenPresent() {
        // Application 模块（composeApp）仍使用经典 com.android.application + androidTarget()。
        pluginManager.withPlugin("com.android.application") {
            configureAndroidTarget()
        }
        // KMP 库模块已迁移到 com.android.kotlin.multiplatform.library，该插件自行提供
        // android target，无需再调用 androidTarget()。相关配置见 AndroidLibraryConventionPlugin。
    }

    private fun Project.configureAndroidTarget() {
        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
