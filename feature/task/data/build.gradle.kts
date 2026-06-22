plugins {
    id("runninghub.android.library")
    id("runninghub.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.network)
            implementation(projects.core.storage)
            implementation(projects.feature.task.domain)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.serialization.json)
        }
    }
}

android {
    namespace = "com.runninghub.feature.task.data"
}
