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
            implementation(projects.core.network)
            implementation(projects.core.storage)
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.model.domain)
            implementation(projects.feature.quickcreate.domain)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(projects.core.model)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
    }
}

android {
    namespace = "com.runninghub.feature.quickcreate.data"
}
