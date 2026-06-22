package com.runninghub.app.ui.feature.quickcreate.presentation.upload

import com.runninghub.feature.quickcreate.presentation.editor.quickCreationFieldMediaReferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.feature.quickcreate.MediaChipCard
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral300
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_upload_audio_reference
import runninghub.composeapp.generated.resources.quick_create_upload_audio_reference_count_format
import runninghub.composeapp.generated.resources.quick_create_upload_done_count_format
import runninghub.composeapp.generated.resources.quick_create_upload_field_default_hint
import runninghub.composeapp.generated.resources.quick_create_upload_field_select_audio
import runninghub.composeapp.generated.resources.quick_create_upload_field_select_image
import runninghub.composeapp.generated.resources.quick_create_upload_field_select_media
import runninghub.composeapp.generated.resources.quick_create_upload_field_select_video
import runninghub.composeapp.generated.resources.quick_create_upload_image_reference
import runninghub.composeapp.generated.resources.quick_create_upload_image_reference_count_format
import runninghub.composeapp.generated.resources.quick_create_upload_progress_count_format
import runninghub.composeapp.generated.resources.quick_create_upload_video_reference
import runninghub.composeapp.generated.resources.quick_create_upload_video_reference_count_format

/**
 * 渲染快捷创作编辑器里的全局素材选择工具条。
 *
 * 全局素材不绑定服务端字段，主要作为当前图片/视频创作配置的参考素材。组件只统计并展示
 * 已选择的图片、视频和音频数量，点击后通过回调交给外层处理权限和平台媒体选择器。
 *
 * @param mediaReferences 当前 tab 的全局媒体引用列表，字段级素材应在进入本组件前过滤掉。
 * @param onLaunchImagePicker 用户请求选择图片参考素材时触发。
 * @param onLaunchVideoPicker 用户请求选择视频参考素材时触发。
 * @param onLaunchAudioPicker 用户请求选择音频参考素材时触发。
 */
@Composable
internal fun QuickCreateMediaToolbarRow(
    mediaReferences: List<MediaReference>,
    onLaunchImagePicker: () -> Unit,
    onLaunchVideoPicker: () -> Unit,
    onLaunchAudioPicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        val imageCount = mediaReferences.count { it.type == QuickCreateMediaType.IMAGE }
        val videoCount = mediaReferences.count { it.type == QuickCreateMediaType.VIDEO }
        val audioCount = mediaReferences.count { it.type == QuickCreateMediaType.AUDIO }

        MediaToolbarChip(
            label = if (imageCount > 0) {
                stringResource(Res.string.quick_create_upload_image_reference_count_format, imageCount)
            } else {
                stringResource(Res.string.quick_create_upload_image_reference)
            },
            icon = Icons.Default.Image,
            hasItems = imageCount > 0,
            onClick = onLaunchImagePicker,
        )
        MediaToolbarChip(
            label = if (videoCount > 0) {
                stringResource(Res.string.quick_create_upload_video_reference_count_format, videoCount)
            } else {
                stringResource(Res.string.quick_create_upload_video_reference)
            },
            icon = Icons.Default.Videocam,
            hasItems = videoCount > 0,
            onClick = onLaunchVideoPicker,
        )
        MediaToolbarChip(
            label = if (audioCount > 0) {
                stringResource(Res.string.quick_create_upload_audio_reference_count_format, audioCount)
            } else {
                stringResource(Res.string.quick_create_upload_audio_reference)
            },
            icon = Icons.Default.MusicNote,
            hasItems = audioCount > 0,
            onClick = onLaunchAudioPicker,
        )
    }
}

/**
 * 渲染服务端动态上传字段的素材选择入口。
 *
 * 该组件属于 QuickCreate 的 upload presentation 子区域，只负责展示某个服务端字段已经绑定的
 * [MediaReference] 和触发选择素材回调；真实文件选择、权限请求、上传任务和失败恢复由
 * Screen/Coordinator 层处理，避免 Composable 直接持有平台 API 或上传状态机。
 *
 * @param paramKey 服务端字段的参数键，用于从 [mediaReferences] 中筛选该字段专属素材。
 * @param mediaType 字段期望的素材类型；为 `null` 时按钮禁用，表示服务端字段无法可靠映射到本地选择器。
 * @param hint 服务端字段的格式、数量或大小提示，允许为空，空字符串时使用默认说明。
 * @param mediaReferences 当前创作配置中的全部媒体引用，组件只展示 [paramKey] 对应的字段素材。
 * @param onUploadFieldClick 用户点击选择素材时触发，调用方负责打开对应平台选择器。
 * @param onRemoveMedia 用户移除字段素材时触发，调用方负责同步状态并取消或忽略相关上传结果。
 */
@Composable
internal fun QuickCreateServiceUploadFieldPicker(
    paramKey: String,
    mediaType: QuickCreateMediaType?,
    hint: String,
    mediaReferences: List<MediaReference>,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    val fieldRefs = mediaReferences.quickCreationFieldMediaReferences(paramKey)
    val doneCount = fieldRefs.count { it.uploadStatus == UploadStatus.DONE }
    val uploadingCount = fieldRefs.count {
        it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING
    }
    val label = when (mediaType) {
        QuickCreateMediaType.IMAGE -> stringResource(Res.string.quick_create_upload_field_select_image)
        QuickCreateMediaType.VIDEO -> stringResource(Res.string.quick_create_upload_field_select_video)
        QuickCreateMediaType.AUDIO -> stringResource(Res.string.quick_create_upload_field_select_audio)
        null -> stringResource(Res.string.quick_create_upload_field_select_media)
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(
                        hint.takeIf { it.isNotBlank() },
                        doneCount.takeIf { it > 0 }?.let {
                            stringResource(Res.string.quick_create_upload_done_count_format, it)
                        },
                        uploadingCount.takeIf { it > 0 }?.let {
                            stringResource(Res.string.quick_create_upload_progress_count_format, it)
                        },
                    ).joinToString(" · ").ifBlank {
                        stringResource(Res.string.quick_create_upload_field_default_hint)
                    },
                    fontSize = 11.sp,
                    color = Neutral500,
                )
            }
            Surface(
                enabled = mediaType != null,
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (doneCount > 0) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (doneCount > 0) Primary300.copy(alpha = 0.4f) else DarkOutlineVariant,
                ),
                onClick = {
                    val resolvedType = mediaType ?: return@Surface
                    onUploadFieldClick(resolvedType, paramKey)
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (mediaType) {
                            QuickCreateMediaType.IMAGE -> Icons.Default.Image
                            QuickCreateMediaType.VIDEO -> Icons.Default.Videocam
                            QuickCreateMediaType.AUDIO -> Icons.Default.MusicNote
                            null -> Icons.Default.Upload
                        },
                        contentDescription = null,
                        tint = if (doneCount > 0) Primary300 else Neutral400,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (doneCount > 0) Primary300 else Neutral300,
                        fontWeight = if (doneCount > 0) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
        // 字段级素材必须跟随服务端 paramKey 展示，避免和全局参考素材混在一起影响提交映射。
        fieldRefs.forEach { ref ->
            MediaChipCard(
                reference = ref,
                onRemove = { onRemoveMedia(ref.id) },
            )
        }
    }
}

@Composable
private fun MediaToolbarChip(
    label: String,
    icon: ImageVector,
    hasItems: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (hasItems) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        border = BorderStroke(
            1.dp,
            if (hasItems) Primary300.copy(alpha = 0.4f) else DarkOutlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(14.dp),
                tint = if (hasItems) Primary300 else Neutral500,
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasItems) Primary300 else Neutral400,
            )
        }
    }
}
