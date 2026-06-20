package com.runninghub.buildlogic

import org.gradle.api.JavaVersion

internal object AndroidSdk {
    const val compile = 37
    const val libraryMin = 24

    val javaVersion: JavaVersion = JavaVersion.VERSION_17
}
