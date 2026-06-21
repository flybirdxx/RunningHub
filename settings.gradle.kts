pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")
include(":feature:community:domain")
include(":feature:community:data")
include(":feature:community:presentation")
include(":feature:discovery:domain")
include(":feature:discovery:data")
include(":feature:task:domain")
include(":feature:task:data")
include(":feature:task:presentation")
include(":feature:audio:domain")
include(":feature:audio:data")
include(":feature:model:domain")
include(":feature:model:data")
include(":feature:quickcreate:domain")
include(":feature:quickcreate:presentation")
include(":feature:quickcreate:data")
include(":shared")
include(":composeApp")
