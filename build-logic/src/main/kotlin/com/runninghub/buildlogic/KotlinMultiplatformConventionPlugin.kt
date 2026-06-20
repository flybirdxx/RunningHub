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
        pluginManager.withPlugin("com.android.application") {
            configureAndroidTarget()
        }
        pluginManager.withPlugin("com.android.library") {
            configureAndroidTarget()
        }
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
