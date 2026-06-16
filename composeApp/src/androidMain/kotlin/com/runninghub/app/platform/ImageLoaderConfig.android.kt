package com.runninghub.app.platform

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder

internal actual fun ImageLoader.Builder.addPlatformImageDecoders(): ImageLoader.Builder =
    components {
        add(AnimatedImageDecoder.Factory())
        add(VideoFrameDecoder.Factory())
    }

internal actual fun ImageLoader.Builder.configurePlatformImageCache(
    context: PlatformContext,
): ImageLoader.Builder =
    memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, IMAGE_MEMORY_CACHE_PERCENT)
            .build()
    }.diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve(IMAGE_DISK_CACHE_DIR))
            .maxSizePercent(IMAGE_DISK_CACHE_PERCENT)
            .minimumMaxSizeBytes(IMAGE_DISK_CACHE_MIN_SIZE_BYTES)
            .maximumMaxSizeBytes(IMAGE_DISK_CACHE_MAX_SIZE_BYTES)
            .build()
    }

private const val IMAGE_MEMORY_CACHE_PERCENT = 0.25
private const val IMAGE_DISK_CACHE_PERCENT = 0.03
private const val IMAGE_DISK_CACHE_DIR = "runninghub_image_cache"
private const val IMAGE_DISK_CACHE_MIN_SIZE_BYTES = 64L * 1024L * 1024L
private const val IMAGE_DISK_CACHE_MAX_SIZE_BYTES = 512L * 1024L * 1024L
