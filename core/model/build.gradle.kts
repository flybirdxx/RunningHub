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
}

android {
    namespace = "com.runninghub.core.model"
}
