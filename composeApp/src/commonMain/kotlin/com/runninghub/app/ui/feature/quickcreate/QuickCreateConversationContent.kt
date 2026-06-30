package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.result.ResultPreview
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaType as PreviewMediaType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState
import com.runninghub.feature.quickcreate.presentation.generation.quickCreateGenerationParameterSnapshot
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateConversationItemUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultAction
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultActionUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.result.quickCreateResultActions
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_result_action_copy_prompt
import runninghub.composeapp.generated.resources.quick_create_result_action_download
import runninghub.composeapp.generated.resources.quick_create_result_action_refund_status
import runninghub.composeapp.generated.resources.quick_create_result_action_retry
import runninghub.composeapp.generated.resources.quick_create_result_action_reuse_parameters
import runninghub.composeapp.generated.resources.quick_create_result_action_save
import runninghub.composeapp.generated.resources.quick_create_result_action_try_again
import runninghub.composeapp.generated.resources.quick_create_result_action_view_detail
import runninghub.composeapp.generated.resources.quick_create_result_action_view_result
import runninghub.composeapp.generated.resources.quick_create_result_action_view_task
import runninghub.composeapp.generated.resources.quick_create_result_expiry_24h
import runninghub.composeapp.generated.resources.quick_create_result_section_title
import runninghub.composeapp.generated.resources.quick_create_result_task_id_format
import runninghub.composeapp.generated.resources.quick_create_task_status_canceled
import runninghub.composeapp.generated.resources.quick_create_task_status_failed
import runninghub.composeapp.generated.resources.quick_create_task_status_processing
import runninghub.composeapp.generated.resources.quick_create_task_status_queuing
import runninghub.composeapp.generated.resources.quick_create_task_status_running_format
import runninghub.composeapp.generated.resources.quick_create_task_status_submitting_task
import runninghub.composeapp.generated.resources.quick_create_task_status_success

/**
 * 渲染设计稿中的对话式快捷创作主区。
 *
 * 该组件只消费 UiState 中已有的会话条目，不触发轮询或生成请求；任务生命周期仍由
 * ScreenModel/Coordinator 管理。空状态不渲染设计稿占位记录，真实任务状态即使暂未返回输出也保留状态卡。
 */
@Composable
internal fun QuickCreateConversationArea(
    uiState: QuickCreateUiState,
    onResultAction: (QuickCreateResultAction, QuickCreateConversationItemUi) -> Unit = { _, _ -> },
) {
    val conversationItems = uiState.conversationItems.ifEmpty {
        uiState.asLegacyConversationItems()
    }

    if (conversationItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(
        conversationItems.size,
        conversationItems.lastOrNull()?.taskStatus,
        conversationItems.lastOrNull()?.results?.size,
    ) {
        // 新任务追加在列表底部；内容高度变化后主动滚到底部，保持最新任务可见。
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        conversationItems.forEach { item ->
            if (item.prompt.isNotBlank()) {
                UserPromptBubble(prompt = item.prompt)
            }
            if (item.results.isNotEmpty() || item.taskStatus != QuickCreateTaskUiStatus.IDLE) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    QuickCreateRhAvatar(modifier = Modifier.padding(top = 4.dp))
                    GeneratedPosterCard(
                        item = item,
                        onResultAction = { action -> onResultAction(action, item) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun QuickCreateUiState.asLegacyConversationItems(): List<QuickCreateConversationItemUi> {
    val hasPrompt = submittedPrompt.isNotBlank()
    val showGeneratedCard = results.isNotEmpty() || taskStatus != QuickCreateTaskUiStatus.IDLE
    if (!hasPrompt && !showGeneratedCard) {
        return emptyList()
    }
    val parameterSnapshot = quickCreateGenerationParameterSnapshot()
    return listOf(
        QuickCreateConversationItemUi(
            prompt = submittedPrompt,
            taskStatus = taskStatus,
            taskId = taskId,
            aspectRatio = parameterSnapshot.aspectRatio,
            resolution = parameterSnapshot.resolution,
            statusText = statusText,
            results = results,
        )
    )
}

@Composable
private fun UserPromptBubble(prompt: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 250.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xEE0F1320),
            border = BorderStroke(1.dp, Color(0xFF263044)),
        ) {
            Text(
                text = prompt,
                color = QuickCreateDesignTokens.Text,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun GeneratedPosterCard(
    item: QuickCreateConversationItemUi,
    onResultAction: (QuickCreateResultAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = item.results.firstOrNull()
    val progress = (item.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent
    var resolvedImageAspectRatio by remember(result?.url) { mutableStateOf<Float?>(null) }
    val cardAspectRatio = if (result == null) {
        quickCreateConversationGeneratingAspectRatio(item.aspectRatio)
    } else {
        quickCreateConversationResultAspectRatio(
            resultWidth = result.width,
            resultHeight = result.height,
            resolvedImageAspectRatio = resolvedImageAspectRatio,
        )
    }
    val previewState = quickCreateResultPreviewState(
        item = item,
        result = result,
        cardAspectRatio = cardAspectRatio,
        progress = progress,
    )
    ResultPreview(
        state = previewState,
        onAction = { actionType -> onResultAction(actionType.toQuickCreateResultAction()) },
        modifier = modifier,
        mediaContent = {
            when {
                result?.mediaType == QuickCreateResultMediaType.IMAGE -> {
                    SmartAsyncImage(
                        imageUrl = result.url,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(if (cardAspectRatio != null) 1f else 0f),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(16.dp),
                        onImageAspectRatioResolved = { ratio -> resolvedImageAspectRatio = ratio },
                    )
                }
                result != null -> {
                    val previewUrl = result.thumbnailUrl?.takeIf { it.isNotBlank() } ?: result.url
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(if (cardAspectRatio != null) 1f else 0f),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(16.dp),
                        onImageAspectRatioResolved = { ratio -> resolvedImageAspectRatio = ratio },
                    )
                }
                else -> {
                    GeneratingFluidMask(modifier = Modifier.matchParentSize())
                }
            }
        },
    )
}

@Composable
private fun quickCreateResultPreviewState(
    item: QuickCreateConversationItemUi,
    result: QuickCreateResultUi?,
    cardAspectRatio: Float?,
    progress: Int?,
): ResultPreviewState {
    val media = when {
        result != null -> result.toResultPreviewMediaState(cardAspectRatio)
        item.taskStatus == QuickCreateTaskUiStatus.SUBMITTING ||
            item.taskStatus == QuickCreateTaskUiStatus.QUEUING ||
            item.taskStatus == QuickCreateTaskUiStatus.RUNNING -> ResultPreviewMediaState(
                url = "",
                previewUrl = null,
                mediaType = PreviewMediaType.Image,
                aspectRatio = cardAspectRatio,
            )
        else -> null
    }
    return ResultPreviewState(
        title = stringResource(Res.string.quick_create_result_section_title),
        taskIdLabel = item.taskId?.takeIf { it.isNotBlank() }?.let { taskId ->
            stringResource(Res.string.quick_create_result_task_id_format, taskId)
        },
        statusLabel = item.taskStatus.quickCreateTaskStatusLabel(progress),
        status = item.taskStatus.toRhTaskStatus(),
        media = media,
        expiryLabel = result?.let { stringResource(Res.string.quick_create_result_expiry_24h) },
        actions = quickCreateResultActions(
            taskStatus = item.taskStatus,
            taskId = item.taskId,
            prompt = item.prompt,
            results = item.results,
        ).map { action -> action.toResultPreviewActionState() },
    )
}

private fun QuickCreateResultUi.toResultPreviewMediaState(cardAspectRatio: Float?): ResultPreviewMediaState =
    ResultPreviewMediaState(
        url = url,
        previewUrl = thumbnailUrl,
        mediaType = when (mediaType) {
            QuickCreateResultMediaType.IMAGE -> PreviewMediaType.Image
            QuickCreateResultMediaType.VIDEO -> PreviewMediaType.Video
        },
        aspectRatio = cardAspectRatio,
    )

@Composable
private fun QuickCreateTaskUiStatus.quickCreateTaskStatusLabel(progress: Int?): String =
    when (this) {
        QuickCreateTaskUiStatus.IDLE -> stringResource(Res.string.quick_create_task_status_processing)
        QuickCreateTaskUiStatus.SUBMITTING -> stringResource(Res.string.quick_create_task_status_submitting_task)
        QuickCreateTaskUiStatus.QUEUING -> stringResource(Res.string.quick_create_task_status_queuing)
        QuickCreateTaskUiStatus.RUNNING -> {
            val visibleProgress = progress?.takeIf { it > 0 }?.coerceIn(0, 100)
            if (visibleProgress != null) {
                stringResource(Res.string.quick_create_task_status_running_format, visibleProgress)
            } else {
                stringResource(Res.string.quick_create_task_status_processing)
            }
        }
        QuickCreateTaskUiStatus.SUCCESS -> stringResource(Res.string.quick_create_task_status_success)
        QuickCreateTaskUiStatus.FAILED -> stringResource(Res.string.quick_create_task_status_failed)
        QuickCreateTaskUiStatus.CANCELED -> stringResource(Res.string.quick_create_task_status_canceled)
    }

private fun QuickCreateTaskUiStatus.toRhTaskStatus(): RhTaskStatus? =
    when (this) {
        QuickCreateTaskUiStatus.IDLE -> null
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.RUNNING -> RhTaskStatus.Running
        QuickCreateTaskUiStatus.QUEUING -> RhTaskStatus.Queued
        QuickCreateTaskUiStatus.SUCCESS -> RhTaskStatus.Success
        QuickCreateTaskUiStatus.FAILED -> RhTaskStatus.Failed
        QuickCreateTaskUiStatus.CANCELED -> RhTaskStatus.Canceled
    }

@Composable
private fun QuickCreateResultActionUi.toResultPreviewActionState(): ResultPreviewActionState =
    ResultPreviewActionState(
        type = action.toResultPreviewActionType(),
        label = action.quickCreateResultActionLabel(),
        enabled = enabled,
    )

private fun QuickCreateResultAction.toResultPreviewActionType(): ResultPreviewActionType =
    when (this) {
        QuickCreateResultAction.ViewTask -> ResultPreviewActionType.ViewTask
        QuickCreateResultAction.ViewResult -> ResultPreviewActionType.ViewResult
        QuickCreateResultAction.Save -> ResultPreviewActionType.Save
        QuickCreateResultAction.Download -> ResultPreviewActionType.Download
        QuickCreateResultAction.ReuseParameters -> ResultPreviewActionType.ReuseParameters
        QuickCreateResultAction.CopyPrompt -> ResultPreviewActionType.CopyPrompt
        QuickCreateResultAction.TryAgain -> ResultPreviewActionType.TryAgain
        QuickCreateResultAction.Retry -> ResultPreviewActionType.Retry
        QuickCreateResultAction.ViewDetail -> ResultPreviewActionType.ViewDetail
        QuickCreateResultAction.RefundStatus -> ResultPreviewActionType.RefundStatus
    }

private fun ResultPreviewActionType.toQuickCreateResultAction(): QuickCreateResultAction =
    when (this) {
        ResultPreviewActionType.ViewTask -> QuickCreateResultAction.ViewTask
        ResultPreviewActionType.ViewResult -> QuickCreateResultAction.ViewResult
        ResultPreviewActionType.Save -> QuickCreateResultAction.Save
        ResultPreviewActionType.Download -> QuickCreateResultAction.Download
        ResultPreviewActionType.ReuseParameters -> QuickCreateResultAction.ReuseParameters
        ResultPreviewActionType.CopyPrompt -> QuickCreateResultAction.CopyPrompt
        ResultPreviewActionType.TryAgain -> QuickCreateResultAction.TryAgain
        ResultPreviewActionType.Retry -> QuickCreateResultAction.Retry
        ResultPreviewActionType.ViewDetail -> QuickCreateResultAction.ViewDetail
        ResultPreviewActionType.RefundStatus -> QuickCreateResultAction.RefundStatus
    }

@Composable
private fun QuickCreateResultAction.quickCreateResultActionLabel(): String =
    when (this) {
        QuickCreateResultAction.ViewTask -> stringResource(Res.string.quick_create_result_action_view_task)
        QuickCreateResultAction.ViewResult -> stringResource(Res.string.quick_create_result_action_view_result)
        QuickCreateResultAction.Save -> stringResource(Res.string.quick_create_result_action_save)
        QuickCreateResultAction.Download -> stringResource(Res.string.quick_create_result_action_download)
        QuickCreateResultAction.ReuseParameters -> stringResource(Res.string.quick_create_result_action_reuse_parameters)
        QuickCreateResultAction.CopyPrompt -> stringResource(Res.string.quick_create_result_action_copy_prompt)
        QuickCreateResultAction.TryAgain -> stringResource(Res.string.quick_create_result_action_try_again)
        QuickCreateResultAction.Retry -> stringResource(Res.string.quick_create_result_action_retry)
        QuickCreateResultAction.ViewDetail -> stringResource(Res.string.quick_create_result_action_view_detail)
        QuickCreateResultAction.RefundStatus -> stringResource(Res.string.quick_create_result_action_refund_status)
    }

@Composable
private fun GeneratingFluidMask(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "quick-create-fluid-mask")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quick-create-fluid-drift",
    )
    val pulse = transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quick-create-fluid-pulse",
    )

    Box(
        modifier = modifier
            .background(Color(0xFF91AFC2))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = pulse.value),
                        Color(0xFF86A7BC).copy(alpha = 0.42f),
                        Color.Transparent,
                    ),
                    center = Offset(
                        x = 80f + 260f * drift.value,
                        y = 70f + 120f * (1f - drift.value),
                    ),
                    radius = 420f,
                ),
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x336D89B8),
                        Color(0x2279D9C8),
                        Color(0x334A476D),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(360f + 220f * drift.value, 396f),
                ),
            ),
    )
}

/**
 * 返回对话结果卡片可使用的最终图片比例。
 *
 * 服务端结果如果已经明确给出宽高，则优先使用该宽高比；如果没有宽高，再用图片加载成功后的
 * intrinsic size。这里不使用提交参数，因为最终图片可能没有按用户选择比例生成。
 */
internal fun quickCreateConversationResultAspectRatio(
    resultWidth: Int?,
    resultHeight: Int?,
    resolvedImageAspectRatio: Float?,
): Float? {
    val resultRatio = if (
        resultWidth != null &&
        resultHeight != null &&
        resultWidth > 0 &&
        resultHeight > 0
    ) {
        resultWidth.toFloat() / resultHeight.toFloat()
    } else {
        null
    }
    return resultRatio ?: resolvedImageAspectRatio?.takeIf { it.isFinite() && it > 0f }
}
/**
 * 返回生成中状态卡片可使用的占位比例。
 *
 * 该值只在还没有结果图时使用，用于让排队、生成中的视觉占位贴近本次提交参数；结果图出现后会改用实际图片比例。
 */
internal fun quickCreateConversationGeneratingAspectRatio(aspectRatio: String?): Float? {
    val value = aspectRatio?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val parts = value.split(':', '：', '/')
    if (parts.size != 2) return null
    val width = parts[0].trim().toFloatOrNull()
    val height = parts[1].trim().toFloatOrNull()
    if (width == null || height == null || width <= 0f || height <= 0f) return null
    return (width / height).coerceIn(0.35f, 2.4f)
}
