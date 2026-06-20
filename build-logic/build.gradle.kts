plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "runninghub.android.application"
            implementationClass = "com.runninghub.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "runninghub.android.library"
            implementationClass = "com.runninghub.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "runninghub.compose.multiplatform"
            implementationClass = "com.runninghub.buildlogic.ComposeMultiplatformConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id = "runninghub.kotlin.multiplatform"
            implementationClass = "com.runninghub.buildlogic.KotlinMultiplatformConventionPlugin"
        }
    }
}
