package com.runninghub.app.platform

import coil3.ImageLoader
import coil3.PlatformContext

internal expect fun ImageLoader.Builder.addPlatformImageDecoders(): ImageLoader.Builder

internal expect fun ImageLoader.Builder.configurePlatformImageCache(
    context: PlatformContext,
): ImageLoader.Builder
