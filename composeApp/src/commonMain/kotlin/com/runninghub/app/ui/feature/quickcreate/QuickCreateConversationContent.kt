package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.designsystem.components.result.ConversationResultCard
import com.runninghub.app.ui.designsystem.components.result.ConversationResultCardActionState
import com.runninghub.app.ui.designsystem.components.result.ConversationResultCardActionType
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_result_action_copy_prompt
import runninghub.composeapp.generated.resources.quick_create_result_action_copy_to_composer
import runninghub.composeapp.generated.resources.quick_create_result_action_download
import runninghub.composeapp.generated.resources.quick_create_result_action_retry
import runninghub.composeapp.generated.resources.quick_create_result_action_reuse_parameters
import runninghub.composeapp.generated.resources.quick_create_result_action_save
import runninghub.composeapp.generated.resources.quick_create_result_action_try_again
import runninghub.composeapp.generated.resources.quick_create_result_action_view_detail
import runninghub.composeapp.generated.resources.quick_create_result_action_view_result
import runninghub.composeapp.generated.resources.quick_create_result_action_view_task
import runninghub.composeapp.generated.resources.quick_create_result_badge_expiry
import runninghub.composeapp.generated.resources.quick_create_result_badge_progress_format
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
    bottomInset: Dp = 0.dp,
    onResultAction: (QuickCreateResultAction, QuickCreateConversationItemUi) -> Unit = { _, _ -> },
    downloadingResultUrls: Set<String> = emptySet(),
) {
    val conversationItems = uiState.conversationItems.ifEmpty {
        uiState.asLegacyConversationItems()
    }

    if (conversationItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
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
            // 底部悬浮输入面板覆盖在会话区之上;bottomInset 按面板实际高度收缩视口,
            // 保证滚到底后最后一张卡的操作按钮完整露出、不被面板遮挡。
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp + bottomInset)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        conversationItems.forEachIndexed { index, item ->
            // key 以 taskId 优先（缺失时回退索引）标识条目，避免列表增删导致位置漂移时，
            // 把上一张卡的局部状态（图片 intrinsic 比例、长按工具条可见性）串到别的条目上。
            key(item.taskId ?: index) {
                if (item.prompt.isNotBlank()) {
                    UserPromptBubble(prompt = item.prompt)
                }
                if (item.results.isNotEmpty() || item.taskStatus != QuickCreateTaskUiStatus.IDLE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        QuickCreateRhAvatar(modifier = Modifier.padding(top = 4.dp))
                        GeneratedPosterCard(
                            item = item,
                            onResultAction = { action -> onResultAction(action, item) },
                            modifier = Modifier.weight(1f),
                            // 下载中的判定与页面下载协程一致：以卡片渲染的第一个结果 URL 为键。
                            downloadInProgress = item.results.firstOrNull()
                                ?.url
                                ?.let { url -> url in downloadingResultUrls } == true,
                            onToolbarVisibilityChanged = { visible ->
                                // 最底部条目通常已滚到底，长按浮出的工具条渲染在卡片下方、
                                // 会落在视口外；工具条变为可见时主动滚到底部把它带进视口。
                                // 不用 BringIntoViewRequester（实验性 API），
                                // 复用页面既有的 scrollState 动画方案。
                                if (visible && index == conversationItems.lastIndex) {
                                    scope.launch {
                                        // 回调发生在工具条组合之前，此刻 maxValue 还是旧值、
                                        // 已滚到底时再滚就是 no-op；等两帧让工具条完成组合与
                                        // 测量、maxValue 更新后再滚，工具条才能进入视口。
                                        withFrameNanos { }
                                        withFrameNanos { }
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }
                            },
                        )
                    }
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
            color = RhTheme.colors.brandMuted,
        ) {
            Text(
                text = prompt,
                color = RhTheme.colors.textPrimary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * 会话条目的结果卡分发入口。
 *
 * 叠加式重设计（result-card-v2）后按任务阶段分三条渲染路径：
 * - 成功且有结果：聊天气泡式叠加卡 [ConversationSuccessResultCard]，支持长按工具条；
 * - 生成中（提交/排队/运行）：叠加卡 + 流体占位 [ConversationGeneratingCard]，比例来自提交参数快照；
 * - 其余（失败/取消及成功但暂无输出等边缘态）：保持原有 [ResultPreview] 紧凑卡路径不变。
 *
 * @param onToolbarVisibilityChanged 长按工具条可见性变化回调；由会话区用来在最底部条目
 * 浮出工具条时滚动视口，保证工具条完整可见。
 * @param downloadInProgress 该卡结果图是否正在保存到相册；下载中禁用叠加下载圆钮防重复触发。
 */
@Composable
private fun GeneratedPosterCard(
    item: QuickCreateConversationItemUi,
    onResultAction: (QuickCreateResultAction) -> Unit,
    modifier: Modifier = Modifier,
    onToolbarVisibilityChanged: (Boolean) -> Unit = {},
    downloadInProgress: Boolean = false,
) {
    val result = item.results.firstOrNull()
    when {
        item.taskStatus == QuickCreateTaskUiStatus.SUCCESS && result != null ->
            ConversationSuccessResultCard(
                item = item,
                result = result,
                onResultAction = onResultAction,
                modifier = modifier,
                onToolbarVisibilityChanged = onToolbarVisibilityChanged,
                downloadInProgress = downloadInProgress,
            )
        item.taskStatus == QuickCreateTaskUiStatus.SUBMITTING ||
            item.taskStatus == QuickCreateTaskUiStatus.QUEUING ||
            item.taskStatus == QuickCreateTaskUiStatus.RUNNING ->
            ConversationGeneratingCard(item = item, modifier = modifier)
        else ->
            ConversationLegacyStatusCard(
                item = item,
                result = result,
                onResultAction = onResultAction,
                modifier = modifier,
            )
    }
}

/**
 * 生成中（提交/排队/运行）的叠加式占位卡。
 *
 * 卡片比例来自点击生成时的提交参数快照（[QuickCreateConversationItemUi.aspectRatio]），
 * 让占位形状贴近最终结果；媒体槽铺满尺寸感知的流体蒙版。
 * 状态徽：有可见进度时展示短百分数（如「42%」），否则复用现有短状态文案；
 * 生成中不展示叠加操作与过期徽。
 */
@Composable
private fun ConversationGeneratingCard(
    item: QuickCreateConversationItemUi,
    modifier: Modifier = Modifier,
) {
    val progress = (item.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent
    val visibleProgress = progress?.takeIf { it > 0 }?.coerceIn(0, 100)
    val statusLabel = if (visibleProgress != null) {
        stringResource(Res.string.quick_create_result_badge_progress_format, visibleProgress)
    } else {
        item.taskStatus.quickCreateTaskStatusLabel(progress = null)
    }
    ConversationResultCard(
        aspectRatio = quickCreateConversationGeneratingAspectRatio(item.aspectRatio),
        modifier = modifier,
        statusLabel = statusLabel,
        status = item.taskStatus.toRhTaskStatus(),
        mediaContent = {
            QuickCreateGeneratingFluidMask(modifier = Modifier.matchParentSize())
        },
    )
}

/**
 * 成功态的叠加式结果卡：媒体即卡片，底部 scrim 上叠下载/复制到素材区圆钮。
 *
 * 长按卡片浮出下方胶囊工具条（再来一张/复用参数/复制 Prompt）；工具条可见期间卡片
 * 走品牌色描边高亮，再次点击卡片或点任一工具条项收起。工具条可见性是纯 UI 瞬态，
 * 按任务维度 remember 局部持有，不进 UiState；每次变化通过
 * [onToolbarVisibilityChanged] 通知会话区处理视口滚动。
 */
@Composable
private fun ConversationSuccessResultCard(
    item: QuickCreateConversationItemUi,
    result: QuickCreateResultUi,
    onResultAction: (QuickCreateResultAction) -> Unit,
    modifier: Modifier = Modifier,
    onToolbarVisibilityChanged: (Boolean) -> Unit = {},
    downloadInProgress: Boolean = false,
) {
    var resolvedImageAspectRatio by remember(result.url) { mutableStateOf<Float?>(null) }
    var toolbarVisible by remember(item.taskId) { mutableStateOf(false) }
    // 收敛工具条可见性写入口：状态实际变化时同步回调，避免各手势分支漏通知。
    fun setToolbarVisible(visible: Boolean) {
        if (toolbarVisible != visible) {
            toolbarVisible = visible
            onToolbarVisibilityChanged(visible)
        }
    }
    val cardAspectRatio = quickCreateConversationResultAspectRatio(
        resultWidth = result.width,
        resultHeight = result.height,
        resolvedImageAspectRatio = resolvedImageAspectRatio,
    )
    val overlayActions = quickCreateResultActions(
        taskStatus = item.taskStatus,
        taskId = item.taskId,
        results = item.results,
    ).mapNotNull { action ->
        action.toConversationResultCardActionState(downloadInProgress = downloadInProgress)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ConversationResultCard(
            aspectRatio = cardAspectRatio,
            statusLabel = stringResource(Res.string.quick_create_task_status_success),
            status = RhTaskStatus.Success,
            expiryLabel = stringResource(Res.string.quick_create_result_badge_expiry),
            overlayActions = overlayActions,
            onAction = { actionType -> onResultAction(actionType.toQuickCreateResultAction()) },
            highlighted = toolbarVisible,
            // 只有工具条可见时才挂整卡点击（用于收起）；平时不消费点击，留给未来的预览手势。
            onClick = if (toolbarVisible) ({ setToolbarVisible(false) }) else null,
            onLongPress = { setToolbarVisible(true) },
            mediaContent = {
                ConversationResultMedia(
                    result = result,
                    cardAspectRatio = cardAspectRatio,
                    onRatioResolved = { ratio -> resolvedImageAspectRatio = ratio },
                )
            },
        )
        if (toolbarVisible) {
            ConversationResultLongPressToolbar(
                onAction = { action ->
                    setToolbarVisible(false)
                    onResultAction(action)
                },
            )
        }
    }
}

/**
 * 失败/取消等非叠加态沿用的原 [ResultPreview] 紧凑卡路径。
 *
 * 保留其动作按钮（重试/查看详情/再来一张）与媒体兜底逻辑；生成中与成功态已迁移到
 * 叠加卡，不再进入该函数。
 */
@Composable
private fun ConversationLegacyStatusCard(
    item: QuickCreateConversationItemUi,
    result: QuickCreateResultUi?,
    onResultAction: (QuickCreateResultAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = (item.statusText as? QuickCreateTaskStatusText.Running)?.progressPercent
    var resolvedImageAspectRatio by remember(result?.url) { mutableStateOf<Float?>(null) }
    val cardAspectRatio = result?.let {
        quickCreateConversationResultAspectRatio(
            resultWidth = it.width,
            resultHeight = it.height,
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
            // media 为 null 时组件不会调用媒体槽，因此这里 result 一定非空。
            if (result != null) {
                ConversationResultMedia(
                    result = result,
                    cardAspectRatio = cardAspectRatio,
                    onRatioResolved = { ratio -> resolvedImageAspectRatio = ratio },
                )
            }
        },
    )
}

/**
 * 成功卡与紧凑卡共用的结果媒体渲染槽。
 *
 * 图片结果直接使用原图，视频结果优先使用缩略图；比例未知时先隐藏媒体，
 * 等服务端宽高或 intrinsic 比例就绪再显示，避免在 1:1 回退卡上闪现被裁切的图。
 */
@Composable
private fun BoxScope.ConversationResultMedia(
    result: QuickCreateResultUi,
    cardAspectRatio: Float?,
    onRatioResolved: (Float) -> Unit,
) {
    when (result.mediaType) {
        QuickCreateResultMediaType.IMAGE -> SmartAsyncImage(
            imageUrl = result.url,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(if (cardAspectRatio != null) 1f else 0f),
            contentScale = ContentScale.Crop,
            onImageAspectRatioResolved = onRatioResolved,
        )
        QuickCreateResultMediaType.VIDEO -> VideoThumbnail(
            url = result.url,
            posterUrl = result.thumbnailUrl?.takeIf { it.isNotBlank() },
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * 成功态结果卡长按浮出的胶囊工具条。
 *
 * 占位首版：收纳现有已接线动作（再来一张/复用参数/复制 Prompt），后续可扩。
 * 纯展示组件：只回传动作语义，收起时机由调用方控制。
 */
@Composable
private fun ConversationResultLongPressToolbar(
    onAction: (QuickCreateResultAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RhTheme.shapes.full),
        color = RhTheme.colors.surfaceElevated,
        border = BorderStroke(1.dp, RhTheme.colors.borderDefault),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RhSpacing.xs, vertical = RhSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationToolbarItem(
                icon = Icons.Default.Refresh,
                label = stringResource(Res.string.quick_create_result_action_try_again),
                onClick = { onAction(QuickCreateResultAction.TryAgain) },
            )
            ConversationToolbarItem(
                icon = Icons.Default.Tune,
                label = stringResource(Res.string.quick_create_result_action_reuse_parameters),
                onClick = { onAction(QuickCreateResultAction.ReuseParameters) },
            )
            ConversationToolbarItem(
                icon = Icons.Default.ContentCopy,
                label = stringResource(Res.string.quick_create_result_action_copy_prompt),
                onClick = { onAction(QuickCreateResultAction.CopyPrompt) },
            )
        }
    }
}

/**
 * 长按工具条中的单个「图标 + 文字」项。
 */
@Composable
private fun ConversationToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(RhTheme.shapes.full))
            .clickable(onClick = onClick)
            .padding(horizontal = RhSpacing.sm, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RhTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.caption,
        )
    }
}

/**
 * 把 Presentation 动作真源映射为叠加卡圆钮状态。
 *
 * 只有下载与复制到素材区两个动作进入叠加圆钮；复制到素材区作为主操作走品牌色强调，
 * 其余动作（重试/再来一张等）由失败态紧凑卡或长按工具条承接，这里返回 null 过滤。
 * 图标取自 material-icons-extended（项目既有依赖，见 composeApp/build.gradle.kts）。
 *
 * @param downloadInProgress 该卡结果图是否正在保存到相册；只影响下载圆钮的可点击性。
 */
@Composable
private fun QuickCreateResultActionUi.toConversationResultCardActionState(
    downloadInProgress: Boolean = false,
): ConversationResultCardActionState? =
    when (action) {
        QuickCreateResultAction.Download -> ConversationResultCardActionState(
            type = ConversationResultCardActionType.Download,
            icon = Icons.Default.Download,
            contentDescription = stringResource(Res.string.quick_create_result_action_download),
            emphasized = false,
            enabled = enabled && !downloadInProgress,
        )
        QuickCreateResultAction.CopyToComposer -> ConversationResultCardActionState(
            type = ConversationResultCardActionType.CopyToComposer,
            icon = Icons.Default.AddPhotoAlternate,
            contentDescription = stringResource(Res.string.quick_create_result_action_copy_to_composer),
            emphasized = true,
            enabled = enabled,
        )
        else -> null
    }

/**
 * 叠加卡圆钮动作转回 Presentation 动作语义，复用页面现有 onResultAction 分发。
 */
private fun ConversationResultCardActionType.toQuickCreateResultAction(): QuickCreateResultAction =
    when (this) {
        ConversationResultCardActionType.Download -> QuickCreateResultAction.Download
        ConversationResultCardActionType.CopyToComposer -> QuickCreateResultAction.CopyToComposer
    }

@Composable
private fun quickCreateResultPreviewState(
    item: QuickCreateConversationItemUi,
    result: QuickCreateResultUi?,
    cardAspectRatio: Float?,
    progress: Int?,
): ResultPreviewState {
    val media = result?.toResultPreviewMediaState(cardAspectRatio)
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
        QuickCreateResultAction.CopyToComposer -> ResultPreviewActionType.CopyToComposer
        QuickCreateResultAction.ReuseParameters -> ResultPreviewActionType.ReuseParameters
        QuickCreateResultAction.CopyPrompt -> ResultPreviewActionType.CopyPrompt
        QuickCreateResultAction.TryAgain -> ResultPreviewActionType.TryAgain
        QuickCreateResultAction.Retry -> ResultPreviewActionType.Retry
        QuickCreateResultAction.ViewDetail -> ResultPreviewActionType.ViewDetail
    }

private fun ResultPreviewActionType.toQuickCreateResultAction(): QuickCreateResultAction =
    when (this) {
        ResultPreviewActionType.ViewTask -> QuickCreateResultAction.ViewTask
        ResultPreviewActionType.ViewResult -> QuickCreateResultAction.ViewResult
        ResultPreviewActionType.Save -> QuickCreateResultAction.Save
        ResultPreviewActionType.Download -> QuickCreateResultAction.Download
        ResultPreviewActionType.CopyToComposer -> QuickCreateResultAction.CopyToComposer
        ResultPreviewActionType.ReuseParameters -> QuickCreateResultAction.ReuseParameters
        ResultPreviewActionType.CopyPrompt -> QuickCreateResultAction.CopyPrompt
        ResultPreviewActionType.TryAgain -> QuickCreateResultAction.TryAgain
        ResultPreviewActionType.Retry -> QuickCreateResultAction.Retry
        ResultPreviewActionType.ViewDetail -> QuickCreateResultAction.ViewDetail
    }

@Composable
private fun QuickCreateResultAction.quickCreateResultActionLabel(): String =
    when (this) {
        QuickCreateResultAction.ViewTask -> stringResource(Res.string.quick_create_result_action_view_task)
        QuickCreateResultAction.ViewResult -> stringResource(Res.string.quick_create_result_action_view_result)
        QuickCreateResultAction.Save -> stringResource(Res.string.quick_create_result_action_save)
        QuickCreateResultAction.Download -> stringResource(Res.string.quick_create_result_action_download)
        QuickCreateResultAction.CopyToComposer -> stringResource(Res.string.quick_create_result_action_copy_to_composer)
        QuickCreateResultAction.ReuseParameters -> stringResource(Res.string.quick_create_result_action_reuse_parameters)
        QuickCreateResultAction.CopyPrompt -> stringResource(Res.string.quick_create_result_action_copy_prompt)
        QuickCreateResultAction.TryAgain -> stringResource(Res.string.quick_create_result_action_try_again)
        QuickCreateResultAction.Retry -> stringResource(Res.string.quick_create_result_action_retry)
        QuickCreateResultAction.ViewDetail -> stringResource(Res.string.quick_create_result_action_view_detail)
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
 * 解析提交参数快照中的宽高比协议值，作为生成中占位卡的比例。
 *
 * 支持半角冒号（"9:16"）、全角冒号（"3：4"）与斜杠（"16/9"）三种分隔；
 * 非法输入（缺分隔、非数字、零或负值、NaN/Infinity、空白）返回 null，由叠加卡回退 1:1。
 * 该值只在还没有结果图时使用，结果出现后改用 [quickCreateConversationResultAspectRatio]。
 *
 * 这里不做区间截断（coerce）：极端比例的 clamp 已由 ConversationResultCard 的
 * conversationResultCardSize 统一负责，避免两处 clamp 语义漂移。
 */
internal fun quickCreateConversationGeneratingAspectRatio(aspectRatio: String?): Float? {
    val value = aspectRatio?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val parts = value.split(':', '：', '/')
    if (parts.size != 2) return null
    val width = parts[0].trim().toFloatOrNull()
    val height = parts[1].trim().toFloatOrNull()
    if (width == null || height == null || width <= 0f || height <= 0f) return null
    // toFloatOrNull 能解析 "NaN"/"Infinity"，且 NaN 不小于 0 会穿过上面的检查，
    // 这里统一按「非法返回 null」契约收口。
    return (width / height).takeIf { it.isFinite() }
}
