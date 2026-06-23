package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_creation_completed_label
import runninghub.composeapp.generated.resources.quick_create_creation_generating_format
import runninghub.composeapp.generated.resources.quick_create_creation_read_receipt
import runninghub.composeapp.generated.resources.quick_create_creation_result_meta_format
import runninghub.composeapp.generated.resources.quick_create_creation_result_title
import runninghub.composeapp.generated.resources.quick_create_creation_status_time
import runninghub.composeapp.generated.resources.quick_create_creation_user_prompt_fallback

/**
 * 渲染设计稿中的对话式快捷创作主区。
 *
 * 该组件只消费 UiState 中已有的提示词、任务状态和结果列表，不触发轮询或生成请求；任务生命周期仍由
 * ScreenModel/Coordinator 管理。结果为空时使用海报占位，保证生成中状态也有稳定的视觉反馈。
 */
@Composable
internal fun QuickCreateConversationArea(
    uiState: QuickCreateUiState,
    onClearResults: () -> Unit,
) {
    val isImage = uiState.currentTab == QuickCreateTab.IMAGE
    val prompt = if (isImage) uiState.imageConfig.prompt else uiState.videoConfig.prompt
    val progress = (uiState.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent ?: 42
    val firstResult = uiState.results.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        UserPromptBubble(prompt = prompt.takeIf { it.isNotBlank() })
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            QuickCreateRhAvatar(modifier = Modifier.padding(top = 4.dp))
            GeneratedPosterCard(
                result = firstResult,
                progress = progress,
                hasCompletedResult = firstResult != null,
                onClearResults = onClearResults,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UserPromptBubble(prompt: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 250.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xEE0F1320),
            border = BorderStroke(1.dp, Color(0xFF263044)),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickCreateCatThumbnail(modifier = Modifier.size(62.dp))
                Text(
                    text = prompt ?: stringResource(Res.string.quick_create_creation_user_prompt_fallback),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.quick_create_creation_status_time),
                color = QuickCreateDesignTokens.Muted,
                fontSize = 12.sp,
            )
            Text(
                text = stringResource(Res.string.quick_create_creation_read_receipt),
                color = QuickCreateDesignTokens.Purple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GeneratedPosterCard(
    result: QuickCreateResultUi?,
    progress: Int,
    hasCompletedResult: Boolean,
    onClearResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {
            if (hasCompletedResult) {
                onClearResults()
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Green),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(396.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07192C)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0B68B9),
                                Color(0xFF1264CE),
                                Color.Transparent,
                            ),
                            endX = 360f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.64f)
                    .height(396.dp),
            ) {
                if (result?.mediaType == QuickCreateResultMediaType.IMAGE) {
                    SmartAsyncImage(
                        imageUrl = result.url,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    )
                }
                if (result?.mediaType != QuickCreateResultMediaType.IMAGE) {
                    QuickCreateCyberCatPoster(modifier = Modifier.matchParentSize())
                }
            }
            StatusBadge(
                text = stringResource(Res.string.quick_create_creation_generating_format, progress.coerceIn(0, 100)),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.quick_create_creation_completed_label),
                    color = Color(0xFF56FF8F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(
                        Res.string.quick_create_creation_result_meta_format,
                        stringResource(Res.string.quick_create_creation_result_title),
                        stringResource(Res.string.quick_create_creation_status_time),
                    ),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
