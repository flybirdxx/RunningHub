package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.image_upload_button_local_preview_badge
import runninghub.composeapp.generated.resources.image_upload_button_pick_audio_hint
import runninghub.composeapp.generated.resources.image_upload_button_pick_image_hint
import runninghub.composeapp.generated.resources.image_upload_button_pick_video_hint
import runninghub.composeapp.generated.resources.image_upload_button_remove_content_description
import runninghub.composeapp.generated.resources.image_upload_button_upload_audio
import runninghub.composeapp.generated.resources.image_upload_button_upload_failed_content_description
import runninghub.composeapp.generated.resources.image_upload_button_upload_failed_retry
import runninghub.composeapp.generated.resources.image_upload_button_upload_image
import runninghub.composeapp.generated.resources.image_upload_button_upload_video
import runninghub.composeapp.generated.resources.image_upload_button_uploaded_badge
import runninghub.composeapp.generated.resources.image_upload_button_uploading_progress_format

/**
 * 渲染 AppDetail 和 QuickCreate 复用的媒体上传入口。
 *
 * 组件只负责展示本地预览、远端上传状态、错误态和移除入口；
 * 实际文件选择、上传请求和状态持久化由调用方通过回调和参数完成。
 * 组件自带的按钮文案、状态徽标、上传进度和无障碍描述均来自 Compose Resources；
 * [fileName] 与 URL 派生名称属于用户文件数据，不在本组件内资源化。
 *
 * @param localUri 本地媒体 URI 字符串，通常来自平台文件选择器；`null` 或空字符串表示没有本地预览。
 * @param remoteUrl 上传成功后的远端媒体 URL；非空时优先作为展示地址，并把状态标记为已上传。
 * @param fileName 用户选择的文件名或调用方映射出的展示名；`null` 时非图片媒体使用 URL 末段作为降级展示。
 * @param isUploading 是否正在上传，`true` 时禁用点击选择并展示进度；`false` 表示当前没有上传请求在组件内展示。
 * @param uploadProgress 上传进度，取值通常为 `0f..1f`；组件按百分比格式化展示，不在此处校正越界值。
 * @param isError 当前媒体是否处于上传错误态，`true` 时展示错误提示并允许用户重新选择；`false` 表示按普通状态展示。
 * @param mediaType 媒体类型，决定默认图标、上传动作文案和非图片预览图标。
 * @param square 是否使用 1:1 正方形布局，`true` 用于紧凑网格；`false` 使用默认横向上传区域高度。
 * @param onPickFile 用户点击上传区域重新选择文件时触发；上传中不会触发。
 * @param onRemoveFile 用户点击移除按钮时触发，由调用方清理本地和远端媒体状态。
 * @param modifier 外层调用方用于控制布局位置和尺寸的修饰符。
 */
@Composable
fun ImageUploadButton(
    localUri: String?,
    remoteUrl: String?,
    fileName: String?,
    isUploading: Boolean,
    uploadProgress: Float,
    isError: Boolean = false,
    mediaType: MediaType = MediaType.IMAGE,
    square: Boolean = false,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayUrl = remoteUrl ?: localUri
    val hasFile = !displayUrl.isNullOrBlank()
    var imageAspectRatio by remember(displayUrl) { mutableStateOf<Float?>(null) }
    val uploadLabel = when (mediaType) {
        MediaType.IMAGE -> stringResource(Res.string.image_upload_button_upload_image)
        MediaType.VIDEO -> stringResource(Res.string.image_upload_button_upload_video)
        MediaType.AUDIO -> stringResource(Res.string.image_upload_button_upload_audio)
    }
    val pickHint = when (mediaType) {
        MediaType.IMAGE -> stringResource(Res.string.image_upload_button_pick_image_hint)
        MediaType.VIDEO -> stringResource(Res.string.image_upload_button_pick_video_hint)
        MediaType.AUDIO -> stringResource(Res.string.image_upload_button_pick_audio_hint)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val uploadSizeModifier = when {
            square -> Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
            hasFile && mediaType == MediaType.IMAGE -> {
                val safeAspectRatio = imageAspectRatio
                    ?.takeIf { it.isFinite() && it > 0f }
                    ?: IMAGE_UPLOAD_DEFAULT_PREVIEW_ASPECT_RATIO
                if (maxWidth.value.isFinite() && maxWidth > 0.dp) {
                    // 图片预览始终保持通栏卡片，只在合理区间内按原图比例调整高度。
                    val targetHeight = (maxWidth / safeAspectRatio)
                        .coerceIn(IMAGE_UPLOAD_MIN_PREVIEW_HEIGHT, IMAGE_UPLOAD_MAX_PREVIEW_HEIGHT)
                    Modifier
                        .fillMaxWidth()
                        .height(targetHeight)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(IMAGE_UPLOAD_DEFAULT_PREVIEW_HEIGHT)
                }
            }
            else -> Modifier
                .fillMaxWidth()
                .height(IMAGE_UPLOAD_DEFAULT_PREVIEW_HEIGHT)
        }

        Box(
            modifier = Modifier
                .then(uploadSizeModifier)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .background(RhTheme.colors.surfaceElevated.copy(alpha = 0.72f))
                .border(
                    width = 1.5.dp,
                    color = when {
                        isError -> RhTheme.colors.statusFailed.copy(alpha = 0.6f)
                        hasFile -> RhTheme.colors.brandPrimary.copy(alpha = 0.5f)
                        else -> RhTheme.colors.textSecondary.copy(alpha = 0.35f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = !isUploading) { onPickFile() },
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = RhTheme.colors.brandPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(
                                Res.string.image_upload_button_uploading_progress_format,
                                (uploadProgress * 100).toInt(),
                            ),
                            color = RhTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = RhTheme.colors.brandPrimary,
                            trackColor = RhTheme.colors.surfaceSunken
                        )
                    }
                }

                isError -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(
                                Res.string.image_upload_button_upload_failed_content_description,
                            ),
                            tint = RhTheme.colors.statusFailed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(Res.string.image_upload_button_upload_failed_retry),
                            color = RhTheme.colors.statusFailed,
                            fontSize = 13.sp
                        )
                    }
                }

                hasFile -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (mediaType == MediaType.IMAGE) {
                            SmartAsyncImage(
                                imageUrl = displayUrl,
                                contentDescription = fileName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                // 图片态直接裁剪填满预览卡片，避免出现黑边或过窄的竖向占位。
                                contentScale = ContentScale.Crop,
                                onImageAspectRatioResolved = { ratio -> imageAspectRatio = ratio }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (mediaType == MediaType.VIDEO) {
                                        Icons.Default.Videocam
                                    } else {
                                        Icons.Default.MusicNote
                                    },
                                    contentDescription = uploadLabel,
                                    tint = RhTheme.colors.brandPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = fileName ?: displayUrl.substringAfterLast("/").substringAfterLast("%2F"),
                                    color = RhTheme.colors.textPrimary,
                                    fontSize = 12.sp,
                                    maxLines = if (square) 2 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    // 徽标叠在媒体预览上，沿用黑色半透明底保证在任意画面上的可读性。
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (remoteUrl != null) {
                                        stringResource(Res.string.image_upload_button_uploaded_badge)
                                    } else {
                                        stringResource(Res.string.image_upload_button_local_preview_badge)
                                    },
                                    color = RhTheme.colors.brandPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // IconButton 保留默认触摸目标，不额外压缩尺寸，确保移除入口仍可稳定点击。
                            IconButton(onClick = onRemoveFile) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(
                                        Res.string.image_upload_button_remove_content_description,
                                    ),
                                    // 移除按钮叠在媒体预览上，固定白色保证在任意画面上的可见度。
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (fileName != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    // 文件名条叠在媒体预览底部，沿用黑色半透明底保证可读性。
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = fileName,
                                    // 文件名文字叠在媒体预览上，固定白色保证在任意画面上的可读性。
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        Icon(
                            imageVector = when (mediaType) {
                                MediaType.IMAGE -> Icons.Default.Add
                                MediaType.VIDEO -> Icons.Default.Videocam
                                MediaType.AUDIO -> Icons.Default.MusicNote
                            },
                            contentDescription = uploadLabel,
                            tint = RhTheme.colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = pickHint,
                            color = RhTheme.colors.textSecondary,
                            fontSize = if (square) 12.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private val IMAGE_UPLOAD_MIN_PREVIEW_HEIGHT = 180.dp
private val IMAGE_UPLOAD_MAX_PREVIEW_HEIGHT = 320.dp
private val IMAGE_UPLOAD_DEFAULT_PREVIEW_HEIGHT = 200.dp
private const val IMAGE_UPLOAD_DEFAULT_PREVIEW_ASPECT_RATIO = 16f / 9f
