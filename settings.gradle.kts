pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
include(":shared")
include(":composeApp")
