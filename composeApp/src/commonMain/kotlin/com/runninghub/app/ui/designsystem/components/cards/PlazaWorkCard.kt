package com.runninghub.app.ui.designsystem.components.cards

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** Plaza 作品卡片预览媒体类型。 */
enum class PlazaWorkCardPreviewType {
    Image,
    Video,
    Placeholder,
}

/** 叠加卡内前导图标的统一尺寸。 */
private val PlazaWorkCardIconSize = 14.dp

/** 叠加卡内作者头像的统一尺寸。 */
private val PlazaWorkCardAvatarSize = 24.dp

/**
 * Plaza 作品卡片预览状态。
 *
 * @property url 预览媒体地址；为空时组件展示占位背景。
 * @property type 预览媒体类型。
 */
data class PlazaWorkCardPreviewState(
    val url: String?,
    val type: PlazaWorkCardPreviewType,
)

/**
 * Plaza 作品卡片完整状态。
 *
 * @property id Plaza 作品 ID。
 * @property title 作品标题或 Prompt 摘要。
 * @property authorId 作者用户 ID；为空时作者行不可点击。
 * @property authorName 作者名称；为空时展示默认首字母占位。
 * @property authorAvatar 作者头像地址；designsystem 不直接加载，仅透传给调用方头像插槽。
 * @property likeCountLabel 已本地化的喜欢数文案；为空时隐藏喜欢指标。
 * @property aspectRatio 封面宽高比；为空时回退到默认 0.75f。
 * @property preview 作品媒体预览。
 * @property useSameLabel 调用方已本地化的“用同款”药丸文案。
 * @property authorProfileHint 调用方已本地化的作者行读屏补充语（如“ 创作者主页”），仅用于无障碍播报；为空时只播报作者名。
 * @property enabled 是否允许触发“用同款”动作。
 */
data class PlazaWorkCardState(
    val id: String,
    val title: String,
    val authorId: String?,
    val authorName: String?,
    val authorAvatar: String?,
    val likeCountLabel: String?,
    val aspectRatio: Float?,
    val preview: PlazaWorkCardPreviewState,
    val useSameLabel: String,
    val authorProfileHint: String = "",
    val enabled: Boolean = true,
)

/**
 * 渲染 Plaza 瀑布流的整图叠加式作品卡片。
 *
 * 封面按原始宽高比铺满卡片，底部渐变遮罩之上叠加标题、作者（头像 + 名称，可点击进入创作者主页）
 * 与喜欢数，右上角为橄榄绿“用同款”药丸。
 *
 * @param onClick 点击卡片主体的回调。
 * @param onUseSame 点击“用同款”药丸的回调。
 * @param onAuthorClick 点击作者行的回调，回传作者用户 ID；仅在 [PlazaWorkCardState.authorId] 非空时触发。
 * @param authorAvatarContent 作者头像插槽；默认渲染品牌底色的首字母圆形占位，调用方可注入真实头像加载。
 * @param previewContent 封面预览插槽，调用方负责按 [PlazaWorkCardPreviewState] 渲染图片、视频或占位。
 */
@Composable
fun PlazaWorkCard(
    state: PlazaWorkCardState,
    onClick: () -> Unit,
    onUseSame: () -> Unit,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    authorAvatarContent: @Composable () -> Unit = { PlazaWorkCardAvatarPlaceholder(state.authorName) },
    previewContent: @Composable BoxScope.(PlazaWorkCardPreviewState) -> Unit = {
        PlazaWorkCardPreviewPlaceholder()
    },
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RhTheme.shapes.md),
        color = RhTheme.colors.surfaceElevated,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.aspectRatio ?: 0.75f)
                .clip(RoundedCornerShape(RhTheme.shapes.md)),
        ) {
            previewContent(state.preview)

            // 媒体叠加渐变遮罩：沿用 batch-1/2 Hero 与 AppCard 的内容层语义，属于覆盖在图片之上的
            // 可读性保护层，不代表主题色板，因此保留 Color.Black 硬编码而不迁移到 RhColors。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.84f),
                        ),
                    ),
            )

            // 用同款药丸：叠加在封面之上的品牌入口，橄榄绿底 + 反色文字保持在媒体层可读。
            Text(
                text = state.useSameLabel,
                color = RhTheme.colors.textInverse,
                style = RhTypography.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(RhSpacing.sm)
                    .clip(RoundedCornerShape(RhTheme.shapes.full))
                    .background(RhTheme.colors.brandPrimary)
                    .clickable(enabled = state.enabled) { onUseSame() }
                    // 让读屏把药丸播报为按钮，而非普通文本。
                    .semantics { role = Role.Button }
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.xs),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                // 白色叠加文字：位于遮罩之上，属于媒体内容层，保留 Color.White 硬编码。
                Text(
                    text = state.title,
                    color = Color.White,
                    style = RhTypography.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val authorRowSemantics = if (state.authorId != null) {
                        Modifier.semantics(mergeDescendants = true) {
                            role = Role.Button
                            state.authorName?.takeIf { it.isNotBlank() }?.let { author ->
                                // 让读屏把作者行播报为可进入创作者主页的按钮，而不仅是名字。
                                contentDescription = author + state.authorProfileHint
                            }
                        }
                    } else {
                        Modifier
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(RhTheme.shapes.full))
                            .clickable(enabled = state.authorId != null) {
                                state.authorId?.let(onAuthorClick)
                            }
                            .then(authorRowSemantics),
                        horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(PlazaWorkCardAvatarSize)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            authorAvatarContent()
                        }
                        state.authorName?.takeIf { it.isNotBlank() }?.let { author ->
                            // 白色叠加文字：媒体内容层作者名，保留 Color.White 硬编码。
                            Text(
                                text = author,
                                color = Color.White.copy(alpha = 0.85f),
                                style = RhTypography.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                    state.likeCountLabel?.let { likeLabel ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 白色叠加图标/文字：媒体内容层喜欢指标，保留 Color.White 硬编码。
                            Icon(
                                imageVector = Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.82f),
                                modifier = Modifier.size(PlazaWorkCardIconSize),
                            )
                            Text(
                                text = likeLabel,
                                color = Color.White.copy(alpha = 0.82f),
                                style = RhTypography.meta,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 渲染作者头像的首字母占位圆，designsystem 无法加载远端图片时的默认呈现。 */
@Composable
private fun PlazaWorkCardAvatarPlaceholder(authorName: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.brandPrimary),
        contentAlignment = Alignment.Center,
    ) {
        val initial = authorName?.trim()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        if (initial.isNotEmpty()) {
            Text(
                text = initial,
                color = RhTheme.colors.textInverse,
                style = RhTypography.meta,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BoxScope.PlazaWorkCardPreviewPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
    )
}
