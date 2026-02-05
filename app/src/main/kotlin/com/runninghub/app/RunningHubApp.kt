/**
 * [INPUT]: 依赖 Application, HiltAndroidApp
 * [OUTPUT]: 对外提供 RunningHubApp 类，全局 Context 容器
 * [POS]: 应用程序生命周期起点，负责 Hilt 依赖树的初始化
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RunningHubApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // Support GIFs
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // Support Video Frames
                add(VideoFrameDecoder.Factory())
            }
            .allowHardware(false) // Disable hardware bitmaps to fix emulator rendering issues
            .logger(coil.util.DebugLogger())
            .build()
    }
}
