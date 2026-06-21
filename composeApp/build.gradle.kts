plugins {
    id("runninghub.kotlin.multiplatform")
    id("runninghub.android.application")
    id("runninghub.compose.multiplatform")
}

val composeMultiplatformVersion = libs.versions.compose.multiplatform.get()

fun runningHubStringBuildConfigField(
    propertyName: String,
    defaultValue: String,
): String = "\"${providers.gradleProperty(propertyName).orElse(defaultValue).get()}\""

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
            implementation(projects.feature.community.domain)
            implementation(projects.feature.community.presentation)
            implementation(projects.feature.discovery.domain)
            implementation(projects.feature.task.domain)
            implementation(projects.feature.task.presentation)
            implementation(projects.feature.quickcreate.domain)
            implementation(projects.feature.quickcreate.presentation)

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
            // Android 应用入口负责装配迁移期 feature data 模块；commonMain 只依赖领域接口，
            // 避免 ScreenModel 或 Composable 直接引用 Data 层实现。
            implementation(projects.core.network)
            implementation(projects.feature.audio.data)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.community.data)
            implementation(projects.feature.discovery.data)
            implementation(projects.feature.model.data)
            implementation(projects.feature.task.data)
            implementation(projects.feature.quickcreate.data)
            implementation(libs.koin.android)
            implementation(libs.coil.video)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.ui)
            implementation(libs.lottie.compose)
            implementation(libs.activity.compose)
            implementation(libs.filepicker)
            implementation(libs.datastore.preferences.core)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:$composeMultiplatformVersion")
        }

        iosMain.dependencies {
            // iOS 包装应用同样在平台启动层装配运行期 Data 模块；commonMain 保持只依赖领域接口。
            implementation(projects.core.network)
            implementation(projects.feature.audio.data)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.community.data)
            implementation(projects.feature.discovery.data)
            implementation(projects.feature.model.data)
            implementation(projects.feature.task.data)
            implementation(projects.feature.quickcreate.data)
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

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.runninghub.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // AC-11 运行验收需要在真机上与用户已安装的正式包并存，
            // 使用独立 applicationId 可以避免签名不同导致的无损安装失败。
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Debug 环境默认回退生产地址，但允许本地或 CI 通过 Gradle property 注入 staging/dev。
            // 这样切换环境只发生在平台启动层，不需要改 commonMain 或 Data 层 endpoint。
            buildConfigField(
                "String",
                "RUNNINGHUB_WEB_BASE_URL",
                runningHubStringBuildConfigField("runninghub.debug.webBaseUrl", "https://www.runninghub.cn/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_API_BASE_URL",
                runningHubStringBuildConfigField("runninghub.debug.apiBaseUrl", "https://www.runninghub.cn/api/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_USER_CENTER_BASE_URL",
                runningHubStringBuildConfigField("runninghub.debug.userCenterBaseUrl", "https://www.runninghub.cn/uc/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_TASK_BASE_URL",
                runningHubStringBuildConfigField("runninghub.debug.taskBaseUrl", "https://www.runninghub.cn/task/openapi/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_OPEN_API_V2_BASE_URL",
                runningHubStringBuildConfigField("runninghub.debug.openApiV2BaseUrl", "https://www.runninghub.cn/openapi/v2/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_TRUSTED_AUTH_HOSTS",
                runningHubStringBuildConfigField("runninghub.debug.trustedAuthHosts", "www.runninghub.cn"),
            )
        }

        release {
            // Release 包必须启用 R8 和资源压缩，避免生产构建携带未使用代码、资源或调试可见符号。
            // 反射和序列化依赖由 proguard-rules.pro 显式保留；若新增运行期反射框架，需同步补规则。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release 默认使用生产环境；若发布流水线需要灰度环境，必须显式传入 release 专用 property。
            buildConfigField(
                "String",
                "RUNNINGHUB_WEB_BASE_URL",
                runningHubStringBuildConfigField("runninghub.release.webBaseUrl", "https://www.runninghub.cn/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_API_BASE_URL",
                runningHubStringBuildConfigField("runninghub.release.apiBaseUrl", "https://www.runninghub.cn/api/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_USER_CENTER_BASE_URL",
                runningHubStringBuildConfigField("runninghub.release.userCenterBaseUrl", "https://www.runninghub.cn/uc/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_TASK_BASE_URL",
                runningHubStringBuildConfigField("runninghub.release.taskBaseUrl", "https://www.runninghub.cn/task/openapi/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_OPEN_API_V2_BASE_URL",
                runningHubStringBuildConfigField("runninghub.release.openApiV2BaseUrl", "https://www.runninghub.cn/openapi/v2/"),
            )
            buildConfigField(
                "String",
                "RUNNINGHUB_TRUSTED_AUTH_HOSTS",
                runningHubStringBuildConfigField("runninghub.release.trustedAuthHosts", "www.runninghub.cn"),
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
