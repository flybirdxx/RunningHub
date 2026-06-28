package com.runninghub.app.ui.component

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
actual fun VideoThumbnail(
    url: String,
    modifier: Modifier,
    posterUrl: String?,
    autoPlay: Boolean,
    cropToFill: Boolean,
    playAudio: Boolean,
    onPlaybackProgressChange: ((VideoPlaybackProgress) -> Unit)?,
    seekRequest: VideoPlaybackSeekRequest?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cleanUrl = remember(url) { url.trim().takeIf { it.isNotEmpty() } }
    val posterSource = remember(posterUrl, cleanUrl) {
        posterUrl?.trim()?.takeIf { it.isNotEmpty() } ?: cleanUrl
    }
    var hasRenderedFirstFrame by remember(cleanUrl) { mutableStateOf(false) }
    val latestOnPlaybackProgressChange = rememberUpdatedState(onPlaybackProgressChange)

    Box(modifier = modifier) {
        SmartAsyncImage(
            imageUrl = posterSource,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (cropToFill) ContentScale.Crop else ContentScale.Fit,
        )

        if (autoPlay && cleanUrl != null) {
            val exoPlayer = remember(cleanUrl, cropToFill) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(cleanUrl))
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = videoThumbnailPlaybackVolume(playAudio)
                    videoScalingMode = if (cropToFill) {
                        C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    } else {
                        C.VIDEO_SCALING_MODE_DEFAULT
                    }
                    playWhenReady = true
                    prepare()
                }
            }

            DisposableEffect(cleanUrl, exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        hasRenderedFirstFrame = true
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }

            DisposableEffect(lifecycleOwner, exoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START,
                        Lifecycle.Event.ON_RESUME -> {
                            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                                exoPlayer.prepare()
                            }
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        }
                        Lifecycle.Event.ON_PAUSE,
                        Lifecycle.Event.ON_STOP -> {
                            hasRenderedFirstFrame = false
                            exoPlayer.pause()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(exoPlayer, onPlaybackProgressChange != null) {
                if (onPlaybackProgressChange == null) return@LaunchedEffect

                while (true) {
                    latestOnPlaybackProgressChange.value?.invoke(
                        exoPlayer.videoPlaybackProgress(hasRenderedFirstFrame = hasRenderedFirstFrame),
                    )
                    delay(VideoProgressUpdateIntervalMs)
                }
            }

            DisposableEffect(cleanUrl) {
                onDispose {
                    latestOnPlaybackProgressChange.value?.invoke(VideoPlaybackProgress())
                }
            }

            LaunchedEffect(exoPlayer, seekRequest) {
                val request = seekRequest ?: return@LaunchedEffect
                val durationMs = exoPlayer.videoDurationMsOrNull() ?: return@LaunchedEffect
                exoPlayer.seekTo((durationMs * request.fraction.coerceIn(0f, 1f)).toLong())
                latestOnPlaybackProgressChange.value?.invoke(
                    exoPlayer.videoPlaybackProgress(hasRenderedFirstFrame = hasRenderedFirstFrame),
                )
            }

            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = if (cropToFill) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        player = exoPlayer
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { playerView ->
                    if (playerView.player !== exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    exoPlayer.volume = videoThumbnailPlaybackVolume(playAudio)
                    exoPlayer.videoScalingMode = if (cropToFill) {
                        C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    } else {
                        C.VIDEO_SCALING_MODE_DEFAULT
                    }
                    playerView.resizeMode = if (cropToFill) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    if (exoPlayer.playbackState == Player.STATE_IDLE) {
                        exoPlayer.prepare()
                    }
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (hasRenderedFirstFrame) 1f else 0f),
            )
        }
    }
}

private fun Player.videoPlaybackProgress(hasRenderedFirstFrame: Boolean): VideoPlaybackProgress =
    VideoPlaybackProgress(
        positionMs = currentPosition.coerceAtLeast(0L),
        durationMs = videoDurationMsOrNull(),
        bufferedPositionMs = bufferedPosition.takeIf { it >= 0L && it != C.TIME_UNSET },
        isLoading = playbackState == Player.STATE_IDLE ||
            playbackState == Player.STATE_BUFFERING ||
            !hasRenderedFirstFrame,
    )

private fun Player.videoDurationMsOrNull(): Long? =
    duration.takeIf { it > 0L && it != C.TIME_UNSET }

private const val VideoProgressUpdateIntervalMs = 250L
