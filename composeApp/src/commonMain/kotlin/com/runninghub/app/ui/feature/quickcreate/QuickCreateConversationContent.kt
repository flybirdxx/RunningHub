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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.runninghub.feature.quickcreate.presentation.generation.quickCreateGenerationParameterSnapshot
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateConversationItemUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_creation_generating
import runninghub.composeapp.generated.resources.quick_create_creation_generating_format
import runninghub.composeapp.generated.resources.quick_create_empty_stage_generate
import runninghub.composeapp.generated.resources.quick_create_empty_stage_queue
import runninghub.composeapp.generated.resources.quick_create_empty_stage_save
import runninghub.composeapp.generated.resources.quick_create_empty_stage_upload
import runninghub.composeapp.generated.resources.quick_create_generation_change_model_action
import runninghub.composeapp.generated.resources.quick_create_generation_failed_recovery_hint
import runninghub.composeapp.generated.resources.quick_create_generation_retry_action
import runninghub.composeapp.generated.resources.quick_create_task_status_canceled
import runninghub.composeapp.generated.resources.quick_create_task_status_failed
import runninghub.composeapp.generated.resources.quick_create_task_status_processing
import runninghub.composeapp.generated.resources.quick_create_task_status_queuing
import runninghub.composeapp.generated.resources.quick_create_task_status_running_format
import runninghub.composeapp.generated.resources.quick_create_task_status_submitting_task
import runninghub.composeapp.generated.resources.quick_create_task_status_success
import runninghub.composeapp.generated.resources.quick_create_task_status_uploading_media_format

/**
 * 渲染设计稿中的对话式快捷创作主区。
 *
 * 该组件只消费 UiState 中已有的会话条目，不触发轮询或生成请求；任务生命周期仍由
 * ScreenModel/Coordinator 管理。空状态不渲染设计稿占位记录，真实任务状态即使暂未返回输出也保留状态卡。
 */
@Composable
internal fun QuickCreateConversationArea(
    uiState: QuickCreateUiState,
    onRetryPrompt: (QuickCreateTab, String) -> Unit,
    onChangeModel: () -> Unit,
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
                        result = item.results.firstOrNull(),
                        generatingAspectRatio = item.aspectRatio,
                        progress = (item.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent,
                        taskStatus = item.taskStatus,
                        statusText = item.statusText,
                        onRetry = { onRetryPrompt(quickCreateRetryTargetTab(item, uiState.currentTab), item.prompt) },
                        onChangeModel = onChangeModel,
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
            sourceTab = currentTab,
            taskStatus = taskStatus,
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
    result: QuickCreateResultUi?,
    generatingAspectRatio: String?,
    progress: Int?,
    taskStatus: QuickCreateTaskUiStatus,
    statusText: QuickCreateTaskStatusText?,
    onRetry: () -> Unit,
    onChangeModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resolvedImageAspectRatio by remember(result?.url) { mutableStateOf<Float?>(null) }
    val cardAspectRatio = if (result == null) {
        quickCreateConversationGeneratingAspectRatio(generatingAspectRatio)
    } else {
        quickCreateConversationResultAspectRatio(
            resultWidth = result.width,
            resultHeight = result.height,
            resolvedImageAspectRatio = resolvedImageAspectRatio,
        )
    }
    val cardSizeModifier = cardAspectRatio?.let { ratio ->
        Modifier.aspectRatio(ratio)
    } ?: Modifier.heightIn(min = 180.dp)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Green),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(cardSizeModifier)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent),
        ) {
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
            taskStatus.quickCreateTaskBadgeText(progress)?.let { badgeText ->
                StatusBadge(
                    text = badgeText,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
            if (result == null || taskStatus != QuickCreateTaskUiStatus.SUCCESS) {
                GenerationStagePanel(
                    taskStatus = taskStatus,
                    statusText = statusText,
                    progress = progress,
                    onRetry = onRetry,
                    onChangeModel = onChangeModel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GenerationStagePanel(
    taskStatus: QuickCreateTaskUiStatus,
    statusText: QuickCreateTaskStatusText?,
    progress: Int?,
    onRetry: () -> Unit,
    onChangeModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = quickCreateConversationStageStates(taskStatus, statusText)
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val statusLabel = statusText?.let { quickCreateConversationStatusText(it) }
            ?: taskStatus.quickCreateTaskBadgeText(progress)
        statusLabel?.let {
            Text(
                text = it,
                color = QuickCreateDesignTokens.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            stages.forEachIndexed { index, stage ->
                GenerationStageStep(
                    stage = stage,
                    number = index + 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (taskStatus == QuickCreateTaskUiStatus.FAILED || taskStatus == QuickCreateTaskUiStatus.CANCELED) {
            Text(
                text = stringResource(Res.string.quick_create_generation_failed_recovery_hint),
                color = QuickCreateDesignTokens.Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = QuickCreateDesignTokens.Text,
                    ),
                    border = BorderStroke(1.dp, QuickCreateDesignTokens.Purple.copy(alpha = 0.74f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(Res.string.quick_create_generation_retry_action),
                        fontSize = 12.sp,
                    )
                }
                OutlinedButton(
                    onClick = onChangeModel,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = QuickCreateDesignTokens.Text,
                    ),
                    border = BorderStroke(1.dp, QuickCreateDesignTokens.Cyan.copy(alpha = 0.68f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(Res.string.quick_create_generation_change_model_action),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerationStageStep(
    stage: QuickCreateGenerationStageUi,
    number: Int,
    modifier: Modifier = Modifier,
) {
    val active = stage.state == QuickCreateGenerationStageState.Current ||
        stage.state == QuickCreateGenerationStageState.Done
    val failed = stage.state == QuickCreateGenerationStageState.Failed
    val dotColor = when {
        failed -> Color(0xFFFF5A67)
        active -> QuickCreateDesignTokens.Green
        else -> Color(0xFF343945)
    }
    val labelColor = when {
        failed -> Color(0xFFFFB4BB)
        active -> QuickCreateDesignTokens.Text
        else -> QuickCreateDesignTokens.Muted
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            color = dotColor.copy(alpha = if (active || failed) 0.9f else 0.72f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    color = if (active || failed) Color.Black else QuickCreateDesignTokens.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = quickCreateGenerationStageText(stage.stage),
            color = labelColor,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun quickCreateGenerationStageText(stage: QuickCreateGenerationStage): String =
    when (stage) {
        QuickCreateGenerationStage.Upload -> stringResource(Res.string.quick_create_empty_stage_upload)
        QuickCreateGenerationStage.Queue -> stringResource(Res.string.quick_create_empty_stage_queue)
        QuickCreateGenerationStage.Generate -> stringResource(Res.string.quick_create_empty_stage_generate)
        QuickCreateGenerationStage.Save -> stringResource(Res.string.quick_create_empty_stage_save)
    }

@Composable
private fun quickCreateConversationStatusText(text: QuickCreateTaskStatusText): String =
    when (text) {
        QuickCreateTaskStatusText.Canceled -> stringResource(Res.string.quick_create_task_status_canceled)
        is QuickCreateTaskStatusText.Error -> text.error.asQuickCreateErrorText()
        QuickCreateTaskStatusText.Failed -> stringResource(Res.string.quick_create_task_status_failed)
        QuickCreateTaskStatusText.Processing -> stringResource(Res.string.quick_create_task_status_processing)
        QuickCreateTaskStatusText.Queuing -> stringResource(Res.string.quick_create_task_status_queuing)
        is QuickCreateTaskStatusText.Running -> stringResource(
            Res.string.quick_create_task_status_running_format,
            text.progressPercent,
        )
        QuickCreateTaskStatusText.SubmittingTask -> stringResource(
            Res.string.quick_create_task_status_submitting_task,
        )
        QuickCreateTaskStatusText.Success -> stringResource(Res.string.quick_create_task_status_success)
        is QuickCreateTaskStatusText.UploadingMedia -> stringResource(
            Res.string.quick_create_task_status_uploading_media_format,
            text.pendingCount,
        )
    }

internal enum class QuickCreateGenerationStage {
    Upload,
    Queue,
    Generate,
    Save,
}

internal enum class QuickCreateGenerationStageState {
    Done,
    Current,
    Pending,
    Failed,
}

internal data class QuickCreateGenerationStageUi(
    val stage: QuickCreateGenerationStage,
    val state: QuickCreateGenerationStageState,
)

internal fun quickCreateConversationStageStates(
    status: QuickCreateTaskUiStatus,
    statusText: QuickCreateTaskStatusText?,
): List<QuickCreateGenerationStageUi> {
    val failed = status == QuickCreateTaskUiStatus.FAILED || status == QuickCreateTaskUiStatus.CANCELED
    val currentStage = when {
        statusText is QuickCreateTaskStatusText.UploadingMedia -> QuickCreateGenerationStage.Upload
        status == QuickCreateTaskUiStatus.SUBMITTING -> QuickCreateGenerationStage.Upload
        status == QuickCreateTaskUiStatus.QUEUING -> QuickCreateGenerationStage.Queue
        status == QuickCreateTaskUiStatus.RUNNING -> QuickCreateGenerationStage.Generate
        status == QuickCreateTaskUiStatus.SUCCESS -> QuickCreateGenerationStage.Save
        failed -> QuickCreateGenerationStage.Generate
        else -> null
    }
    return QuickCreateGenerationStage.entries.map { stage ->
        val state = when {
            failed && stage == currentStage -> QuickCreateGenerationStageState.Failed
            status == QuickCreateTaskUiStatus.SUCCESS -> QuickCreateGenerationStageState.Done
            currentStage == null -> QuickCreateGenerationStageState.Pending
            stage.ordinal < currentStage.ordinal -> QuickCreateGenerationStageState.Done
            stage == currentStage -> QuickCreateGenerationStageState.Current
            else -> QuickCreateGenerationStageState.Pending
        }
        QuickCreateGenerationStageUi(stage = stage, state = state)
    }
}

internal fun quickCreateRetryTargetTab(
    item: QuickCreateConversationItemUi,
    fallbackTab: QuickCreateTab,
): QuickCreateTab = item.sourceTab ?: fallbackTab

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

@Composable
private fun QuickCreateTaskUiStatus.quickCreateTaskBadgeText(progress: Int?): String? =
    when (this) {
        QuickCreateTaskUiStatus.IDLE -> null
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING -> {
            val visibleProgress = progress?.takeIf { it > 0 }?.coerceIn(0, 100)
            if (visibleProgress != null) {
                stringResource(
                    Res.string.quick_create_creation_generating_format,
                    visibleProgress,
                )
            } else {
                stringResource(Res.string.quick_create_creation_generating)
            }
        }
        QuickCreateTaskUiStatus.SUCCESS -> null
        QuickCreateTaskUiStatus.FAILED -> stringResource(Res.string.quick_create_task_status_failed)
        QuickCreateTaskUiStatus.CANCELED -> stringResource(Res.string.quick_create_task_status_canceled)
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

@Composable
private fun StatusBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0x88124A74),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1775AC)),
    ) {
        Text(
            text = text,
            color = QuickCreateDesignTokens.Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
