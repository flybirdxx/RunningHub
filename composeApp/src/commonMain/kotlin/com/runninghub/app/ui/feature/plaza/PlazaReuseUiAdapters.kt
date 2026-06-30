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
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardAction
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewType as DsPlazaWorkCardPreviewType
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterStatus
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateSheet
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateSheetState
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.theme.RhAppCard as RhCard
import com.runninghub.app.ui.theme.RhAppMuted as RhMuted
import com.runninghub.feature.community.presentation.PlazaReuseParameterStatus
import com.runninghub.feature.community.presentation.PlazaReuseParameterUiModel
import com.runninghub.feature.community.presentation.PlazaWorkCardUiModel
import com.runninghub.feature.community.presentation.PlazaWorkDetailUiModel
import com.runninghub.feature.community.presentation.PlazaWorkPreviewType
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_default_creation_owner
import runninghub.composeapp.generated.resources.plaza_reuse_missing_parameter
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_aspect_ratio
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_model_template
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_prompt
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_quantity
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_reference
import runninghub.composeapp.generated.resources.plaza_reuse_parameter_resolution
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_dismiss
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_source_protection
import runninghub.composeapp.generated.resources.plaza_reuse_sheet_title
import runninghub.composeapp.generated.resources.plaza_untitled_creation
import runninghub.composeapp.generated.resources.plaza_use_count_format
import runninghub.composeapp.generated.resources.plaza_use_same_action
import runninghub.composeapp.generated.resources.plaza_use_same_generate_action

@Composable
internal fun PlazaWorkCardUiModel.toPlazaWorkCardState(): PlazaWorkCardState =
    PlazaWorkCardState(
        id = id,
        title = title.takeIf { it.isNotBlank() } ?: stringResource(Res.string.plaza_untitled_creation),
        authorName = authorName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.plaza_default_creation_owner),
        preview = PlazaWorkCardPreviewState(
            url = preview.url,
            type = preview.type.toDsPlazaWorkCardPreviewType(),
        ),
        metric = useCount?.takeIf { it.isNotBlank() }?.let { count ->
            PlazaWorkCardMetricState(
                label = stringResource(Res.string.plaza_use_count_format, count),
                value = "",
            )
        },
        primaryAction = PlazaWorkCardAction.UseSame,
        actionLabel = stringResource(Res.string.plaza_use_same_action),
        sourceProtected = sourceProtected,
    )

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
            .background(RhCard),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = RhMuted,
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
    val missingLabel = stringResource(Res.string.plaza_reuse_missing_parameter)
    return ReuseTemplateSheetState(
        title = stringResource(Res.string.plaza_reuse_sheet_title),
        sourceTitle = title.takeIf { it.isNotBlank() } ?: stringResource(Res.string.plaza_untitled_creation),
        sourceAuthor = authorName?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.plaza_default_creation_owner),
        sourceProtectionText = stringResource(Res.string.plaza_reuse_sheet_source_protection),
        parameters = listOf(
            reuseSummary.modelTemplate.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_model_template),
                missingLabel = missingLabel,
            ),
            reuseSummary.prompt.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_prompt),
                missingLabel = missingLabel,
            ),
            reuseSummary.aspectRatio.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_aspect_ratio),
                missingLabel = missingLabel,
            ),
            reuseSummary.resolution.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_resolution),
                missingLabel = missingLabel,
            ),
            reuseSummary.quantity.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_quantity),
                missingLabel = missingLabel,
            ),
            reuseSummary.referenceMedia.toReuseTemplateParameterState(
                label = stringResource(Res.string.plaza_reuse_parameter_reference),
                missingLabel = missingLabel,
            ),
        ),
        confirmActionLabel = stringResource(Res.string.plaza_use_same_generate_action),
        dismissActionLabel = stringResource(Res.string.plaza_reuse_sheet_dismiss),
        confirmBypassesPriceConfirmation = false,
    )
}

private fun PlazaReuseParameterUiModel.toReuseTemplateParameterState(
    label: String,
    missingLabel: String,
): ReuseTemplateParameterState =
    ReuseTemplateParameterState(
        label = label,
        value = value ?: missingLabel,
        status = when (status) {
            PlazaReuseParameterStatus.Available -> ReuseTemplateParameterStatus.Available
            PlazaReuseParameterStatus.Missing -> ReuseTemplateParameterStatus.Missing
        },
    )
