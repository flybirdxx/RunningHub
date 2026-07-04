package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewType as DsPlazaWorkCardPreviewType
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterStatus
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateSheet
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateSheetState
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.feature.community.presentation.PlazaReuseParameterStatus
import com.runninghub.feature.community.presentation.PlazaReuseParameterUiModel
import com.runninghub.feature.community.presentation.PlazaWorkCardUiModel
import com.runninghub.feature.community.presentation.PlazaWorkDetailUiModel
import com.runninghub.feature.community.presentation.PlazaWorkPreviewType
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_default_creation_owner
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_aspect_ratio
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_model_template
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_prompt
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_reference
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_resolution
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_dismiss
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_source_protection
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_title
import runninghub.composeapp.generated.resources.plaza_like_count_format
import runninghub.composeapp.generated.resources.plaza_untitled_creation
import runninghub.composeapp.generated.resources.plaza_use_same_action
import runninghub.composeapp.generated.resources.plaza_use_same_generate_action

@Composable
internal fun PlazaWorkCardUiModel.toPlazaWorkCardState(): PlazaWorkCardState =
    PlazaWorkCardState(
        id = id,
        title = title.takeIf { it.isNotBlank() } ?: stringResource(Res.string.plaza_untitled_creation),
        authorId = ownerId?.takeIf { it.isNotBlank() },
        authorName = authorName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.plaza_default_creation_owner),
        authorAvatar = ownerAvatar?.takeIf { it.isNotBlank() },
        likeCountLabel = likeCount?.takeIf { it.isNotBlank() }?.let { count ->
            stringResource(Res.string.plaza_like_count_format, count)
        },
        aspectRatio = aspectRatio,
        preview = PlazaWorkCardPreviewState(
            url = preview.url,
            type = preview.type.toDsPlazaWorkCardPreviewType(),
        ),
        useSameLabel = stringResource(Res.string.plaza_use_same_action),
    )

/**
 * 渲染 Plaza 作品卡片作者头像插槽。
 *
 * designsystem 卡片无法直接加载远端图片，因此由 composeApp 注入 [SmartAsyncImage]；
 * 头像地址为空时回退到卡片自带的首字母占位圆。
 */
@Composable
internal fun PlazaWorkCardAuthorAvatar(avatarUrl: String?, contentDescription: String?) {
    val url = avatarUrl?.takeIf { it.isNotBlank() } ?: return
    SmartAsyncImage(
        imageUrl = url,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

private fun PlazaWorkPreviewType.toDsPlazaWorkCardPreviewType(): DsPlazaWorkCardPreviewType =
    when (this) {
        PlazaWorkPreviewType.Image -> DsPlazaWorkCardPreviewType.Image
        PlazaWorkPreviewType.Video -> DsPlazaWorkCardPreviewType.Video
        PlazaWorkPreviewType.Placeholder -> DsPlazaWorkCardPreviewType.Placeholder
    }

@Composable
internal fun PlazaWorkCardPreview(preview: PlazaWorkCardPreviewState) {
    val url = preview.url?.takeIf { it.isNotBlank() }
    when (preview.type) {
        DsPlazaWorkCardPreviewType.Image -> {
            if (url == null) {
                PlazaWorkPreviewPlaceholder()
            } else {
                SmartAsyncImage(
                    imageUrl = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        DsPlazaWorkCardPreviewType.Video -> {
            if (url == null) {
                PlazaWorkPreviewPlaceholder()
            } else {
                VideoThumbnail(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    cropToFill = true,
                    playAudio = false,
                )
            }
        }
        DsPlazaWorkCardPreviewType.Placeholder -> PlazaWorkPreviewPlaceholder()
    }
}

@Composable
private fun PlazaWorkPreviewPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = RhTheme.colors.textTertiary,
            modifier = Modifier.fillMaxSize(0.18f),
        )
    }
}

@Composable
internal fun PlazaReuseTemplateOverlay(
    detail: PlazaWorkDetailUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        // 模态遮罩：覆盖在页面之上的半透明背板，属于媒体/弹层内容层，保留 Color.Black 硬编码。
        modifier = modifier.background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RhTheme.colors.surfaceElevated,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            ReuseTemplateSheet(
                state = detail.toReuseTemplateSheetState(),
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun PlazaWorkDetailUiModel.toReuseTemplateSheetState(): ReuseTemplateSheetState {
    return ReuseTemplateSheetState(
        title = stringResource(Res.string.plaza_reuse_sheet_title),
        sourceTitle = title.takeIf { it.isNotBlank() } ?: stringResource(Res.string.plaza_untitled_creation),
        sourceAuthor = authorName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.plaza_default_creation_owner),
        sourceProtectionText = stringResource(Res.string.plaza_reuse_sheet_source_protection),
        parameters = listOfNotNull(
            reuseSummary.modelTemplate.toReuseTemplateParameterStateOrNull(
                label = stringResource(Res.string.plaza_reuse_parameter_model_template),
            ),
            reuseSummary.prompt.toReuseTemplateParameterStateOrNull(
                label = stringResource(Res.string.plaza_reuse_parameter_prompt),
            ),
            reuseSummary.aspectRatio.toReuseTemplateParameterStateOrNull(
                label = stringResource(Res.string.plaza_reuse_parameter_aspect_ratio),
            ),
            reuseSummary.resolution.toReuseTemplateParameterStateOrNull(
                label = stringResource(Res.string.plaza_reuse_parameter_resolution),
            ),
            reuseSummary.referenceMedia.toReuseTemplateParameterStateOrNull(
                label = stringResource(Res.string.plaza_reuse_parameter_reference),
            ),
        ),
        confirmActionLabel = stringResource(Res.string.plaza_use_same_generate_action),
        dismissActionLabel = stringResource(Res.string.plaza_reuse_sheet_dismiss),
        confirmBypassesPriceConfirmation = false,
    )
}

private fun PlazaReuseParameterUiModel.toReuseTemplateParameterStateOrNull(
    label: String,
): ReuseTemplateParameterState? {
    val displayValue = value?.takeIf { it.isNotBlank() } ?: return null
    if (status != PlazaReuseParameterStatus.Available) return null
    return ReuseTemplateParameterState(
        label = label,
        value = displayValue,
        status = ReuseTemplateParameterStatus.Available,
    )
}
