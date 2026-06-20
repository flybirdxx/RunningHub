plugins {
    id("runninghub.android.library")
    id("runninghub.kotlin.multiplatform")
    id("runninghub.compose.multiplatform")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
        }
    }
}

android {
    namespace = "com.runninghub.core.designsystem"
}
