pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.4.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "RunningHub"
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:storage")
include(":core:designsystem")
include(":feature:auth:domain")
include(":feature:discovery:domain")
include(":feature:quickcreate:domain")
include(":feature:quickcreate:presentation")
include(":feature:quickcreate:data")
include(":shared")
include(":composeApp")
