plugins {
    id("runninghub.kotlin.multiplatform")
    id("runninghub.android.library")
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
}

android {
    namespace = "com.runninghub.shared"
}
