package com.runninghub.app.ui.designsystem.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatusBadge
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 结果媒体预览的最大高度；避免竖版大图在手机上占满一屏、把状态与操作区挤出视口。 */
private val ResultPreviewMediaMaxHeight = 340.dp

/**
 * 结果预览中媒体内容的稳定类型。
 *
 * Design System 只关心图片或视频两类渲染分支，不读取服务端原始格式字符串。
 */
enum class ResultPreviewMediaType {
    Image,
    Video,
}

/**
 * 结果预览按钮的稳定动作语义。
 *
 * 组件只把动作回传给调用方，不在 Design System 内执行导航、保存、下载或剪贴板副作用。
 */
enum class ResultPreviewActionType {
    ViewTask,
    ViewResult,
    Save,
    Download,
    ReuseParameters,
    CopyPrompt,
    TryAgain,
    Retry,
    ViewDetail,
}

/**
 * 结果预览的媒体状态。
 *
 * @property url 结果原始地址，不能为空。
 * @property previewUrl 可选缩略图地址，视频优先使用该地址渲染封面。
 * @property mediaType 稳定媒体类型，决定占位和调用方媒体渲染分支。
 * @property aspectRatio 可选宽高比；为空时组件使用固定高度占位，避免布局跳动。
 */
data class ResultPreviewMediaState(
    val url: String,
    val previewUrl: String?,
    val mediaType: ResultPreviewMediaType,
    val aspectRatio: Float?,
) {
    val renderUrl: String
        get() = previewUrl?.takeIf { it.isNotBlank() } ?: url
}

/**
 * 结果预览的按钮状态。
 *
 * @property type 动作语义。
 * @property label 调用方已本地化的按钮文案。
 * @property enabled 当前动作是否可点击。
 */
data class ResultPreviewActionState(
    val type: ResultPreviewActionType,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * 结果预览组件的完整状态。
 *
 * @property title 卡片标题。
 * @property taskIdLabel 可选任务 ID 展示文案。
 * @property statusLabel 状态徽标或状态文本的本地化文案。
 * @property status 可映射到 Design System 的任务状态；为空时按普通辅助文本展示。
 * @property media 可选媒体预览；为空时只展示状态和操作。
 * @property expiryLabel 可选云端过期提醒。
 * @property actions 当前可展示的后续动作列表。
 */
data class ResultPreviewState(
    val title: String,
    val taskIdLabel: String?,
    val statusLabel: String,
    val status: RhTaskStatus? = null,
    val media: ResultPreviewMediaState?,
    val expiryLabel: String?,
    val actions: List<ResultPreviewActionState>,
)

/**
 * 展示生成任务的状态、结果媒体、过期提醒和后续动作。
 *
 * 组件不直接保存或下载媒体，也不声明任何平台副作用成功；调用方通过 [onAction] 接收稳定动作语义后再处理。
 */
@Composable
fun ResultPreview(
    state: ResultPreviewState,
    onAction: (ResultPreviewActionType) -> Unit,
    modifier: Modifier = Modifier,
    mediaContent: @Composable BoxScope.(ResultPreviewMediaState) -> Unit = { media ->
        ResultPreviewMediaPlaceholder(media)
    },
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.lg),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            state.media?.let { media ->
                // 媒体区限制最大高度：手机上竖版结果图若按整宽渲染会超过一屏,
                // 超限时按宽高比自动收窄并水平居中,两侧用 sunken 底色留边。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = RhTheme.shapes.lg, topEnd = RhTheme.shapes.lg))
                        .background(RhTheme.colors.surfaceSunken),
                    contentAlignment = Alignment.Center,
                ) {
                    // 注意不能加 fillMaxWidth：它会把 minWidth 锁成整宽,竖图时 aspectRatio
                    // 找不到可行尺寸而放弃比例;去掉后竖图按 340dp 上限自动收窄居中。
                    Box(
                        modifier = Modifier
                            .heightIn(max = ResultPreviewMediaMaxHeight)
                            .then(
                                media.aspectRatio?.let { Modifier.aspectRatio(it) }
                                    ?: Modifier.fillMaxWidth().height(220.dp),
                            ),
                    ) {
                        mediaContent(media)
                    }
                }
            }
            Column(
                modifier = Modifier.padding(RhSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
            ) {
                Text(
                    text = state.title,
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.cardTitle,
                )
                state.taskIdLabel?.let { label ->
                    Text(
                        text = label,
                        color = RhTheme.colors.textTertiary,
                        style = RhTypography.meta,
                    )
                }
                if (state.status != null) {
                    RhTaskStatusBadge(
                        status = state.status,
                        label = state.statusLabel,
                    )
                } else {
                    Text(
                        text = state.statusLabel,
                        color = RhTheme.colors.textSecondary,
                        style = RhTypography.caption,
                    )
                }
                state.expiryLabel?.let { label ->
                    Text(
                        text = label,
                        color = RhTheme.colors.statusWarning,
                        style = RhTypography.meta,
                    )
                }
                if (state.actions.isNotEmpty()) {
                    ResultPreviewActions(
                        actions = state.actions,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ResultPreviewMediaPlaceholder(media: ResultPreviewMediaState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (media.mediaType == ResultPreviewMediaType.Video) {
                    RhTheme.colors.surfaceDefault
                } else {
                    RhTheme.colors.surfaceSunken
                },
            ),
    )
}

@Composable
private fun ResultPreviewActions(
    actions: List<ResultPreviewActionState>,
    onAction: (ResultPreviewActionType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.xs)) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
            ) {
                rowActions.forEachIndexed { index, action ->
                    RhButton(
                        text = action.label,
                        onClick = { onAction(action.type) },
                        modifier = Modifier.weight(1f),
                        enabled = action.enabled,
                        style = if (index == 0) RhButtonStyle.Secondary else RhButtonStyle.Ghost,
                    )
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
