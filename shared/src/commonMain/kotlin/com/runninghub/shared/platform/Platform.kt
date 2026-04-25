package com.runninghub.shared.platform

interface Platform {
    val name: String
    val isAndroid: Boolean get() = false
    val isIos: Boolean get() = false
}

expect fun getPlatform(): Platform
