package com.runninghub.shared.platform

actual class Platform actual constructor() {
    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    actual val version: String = android.os.Build.VERSION.RELEASE
}
