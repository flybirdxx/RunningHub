package com.runninghub.app.platform

import coil3.ImageLoader
import coil3.PlatformContext

internal actual fun ImageLoader.Builder.addPlatformImageDecoders(): ImageLoader.Builder = this

internal actual fun ImageLoader.Builder.configurePlatformImageCache(
    context: PlatformContext,
): ImageLoader.Builder = this
