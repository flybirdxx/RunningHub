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
import androidx.compose.ui.Alignment
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
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_creation_generating
import runninghub.composeapp.generated.resources.quick_create_creation_generating_format
import runninghub.composeapp.generated.resources.quick_create_task_status_canceled
import runninghub.composeapp.generated.resources.quick_create_task_status_failed

/**
 * 渲染设计稿中的对话式快捷创作主区。
 *
 * 该组件只消费 UiState 中已有的会话条目，不触发轮询或生成请求；任务生命周期仍由
 * ScreenModel/Coordinator 管理。空状态不渲染设计稿占位记录，真实任务状态即使暂未返回输出也保留状态卡。
 */
@Composable
internal fun QuickCreateConversationArea(
    uiState: QuickCreateUiState,
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
                        fallbackAspectRatio = item.aspectRatio,
                        progress = (item.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent,
                        taskStatus = item.taskStatus,
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
    fallbackAspectRatio: String?,
    progress: Int?,
    taskStatus: QuickCreateTaskUiStatus,
    modifier: Modifier = Modifier,
) {
    val cardAspectRatio = result.displayAspectRatio(fallbackAspectRatio)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Green),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardAspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent),
        ) {
            when {
                result?.mediaType == QuickCreateResultMediaType.IMAGE -> {
                    SmartAsyncImage(
                        imageUrl = result.url,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                result != null -> {
                    val previewUrl = result.thumbnailUrl?.takeIf { it.isNotBlank() } ?: result.url
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(16.dp),
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
        }
    }
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

private fun QuickCreateResultUi?.displayAspectRatio(fallbackAspectRatio: String?): Float {
    val resultRatio = this?.let { result ->
        val width = result.width
        val height = result.height
        if (width != null && height != null && width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            null
        }
    }
    return resultRatio ?: fallbackAspectRatio.parseAspectRatioOrNull() ?: 1f
}

private fun String?.parseAspectRatioOrNull(): Float? {
    val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val parts = value.split(':')
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
