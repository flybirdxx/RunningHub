plugins {
    id("runninghub.android.library")
    id("runninghub.kotlin.multiplatform")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.feature.discovery.domain)
            implementation(projects.feature.task.domain)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.runninghub.feature.detail.presentation"
}
