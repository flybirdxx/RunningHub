plugins {
    id("runninghub.kotlin.multiplatform")
    id("runninghub.android.application")
    id("runninghub.compose.multiplatform")
}

val composeMultiplatformVersion = libs.versions.compose.multiplatform.get()

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.storage)
            implementation(projects.feature.auth.domain)
            implementation(projects.feature.discovery.domain)
            implementation(projects.feature.quickcreate.domain)
            implementation(projects.feature.quickcreate.presentation)
            implementation(project(":shared"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation("org.jetbrains.compose.components:components-ui-tooling-preview:$composeMultiplatformVersion")

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.coil.gif)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            // Android 应用入口负责装配 QuickCreate data 模块；commonMain 只依赖领域接口，
            // 避免 ScreenModel 或 Composable 直接引用 Data 层实现。
            implementation(projects.feature.quickcreate.data)
            implementation(libs.koin.android)
            implementation(libs.coil.video)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.ui)
            implementation(libs.lottie.compose)
            implementation(libs.activity.compose)
            implementation(libs.filepicker)
            implementation(libs.kotlinx.serialization.json)
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:$composeMultiplatformVersion")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:$composeMultiplatformVersion")
}

android {
    namespace = "com.runninghub.app"

    defaultConfig {
        applicationId = "com.runninghub.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
