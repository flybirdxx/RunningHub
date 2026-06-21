package com.runninghub.app.platform

import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder

internal actual fun ImageLoader.Builder.addPlatformImageDecoders(): ImageLoader.Builder =
    components {
        // AnimatedImageDecoder 依赖 Android P 的 ImageDecoder；minSdk 仍为 26，
        // 因此旧系统必须降级到 GifDecoder，避免 lint 通过后在 Android 8.x 运行时崩溃。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
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
