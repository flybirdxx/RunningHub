package com.runninghub.app.ui.feature.quickcreate.presentation.inspiration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.Secondary500
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationBadgeTone
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationBadgeUi
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationPlaceholderMediaType
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationPreviewUi
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationTemplateUi

/**
 * 展示快捷创作的灵感模板区域。
 *
 * 该组件位于 inspiration 子区域，只消费 [QuickCreateUiState] 中已经准备好的标签、
 * 模板和分页状态。模板应用与加载更多通过回调返回给 ScreenModel 门面，避免 UI
 * 直接触碰模板 Repository 或分页实现。
 *
 * @param uiState 快捷创作页面状态，当前只读取灵感标签、模板列表和加载状态。
 * @param onApplyTemplate 用户选择模板时触发，参数为模板 ID。
 * @param onLoadMoreTemplates 用户请求加载下一页模板时触发。
 */
@Composable
internal fun QuickCreateInspirationArea(
    uiState: QuickCreateUiState,
    onApplyTemplate: (String) -> Unit,
    onLoadMoreTemplates: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                uiState.inspirationTags.forEach { tag ->
                    Surface(
                        color = if (tag.selected) Primary300.copy(alpha = 0.16f) else DarkSurfaceVariant,
                        shape = RoundedCornerShape(Dimens.RadiusFull),
                        border = BorderStroke(
                            1.dp,
                            if (tag.selected) Primary300.copy(alpha = 0.42f) else DarkOutlineVariant,
                        ),
                    ) {
                        Text(
                            tag.label,
                            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                            color = if (tag.selected) Primary300 else Neutral400,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (uiState.inspirationLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary300)
                }
            }
        }

        // 保留 StateHolder 提供的模板顺序，因为该顺序可能包含服务端或运营侧推荐权重。
        items(uiState.inspirationTemplates, key = { it.id }) { template ->
            QuickCreateInspirationTemplateCard(
                template = template,
                onApplyTemplate = onApplyTemplate,
            )
        }

        if (!uiState.inspirationLoading && uiState.inspirationTemplates.isEmpty()) {
            item { QuickCreateInspirationEmptyArea() }
        }

        if (uiState.inspirationTemplatesHasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMoreTemplates,
                    enabled = !uiState.inspirationTemplatesLoadingMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    border = BorderStroke(1.dp, DarkOutlineVariant),
                ) {
                    if (uiState.inspirationTemplatesLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                        Spacer(Modifier.width(Dimens.SpaceSM))
                    }
                    Text("加载更多模板")
                }
            }
        }
    }
}

/**
 * 展示灵感模板为空时的占位内容。
 *
 * 空状态只表达当前列表无可展示模板，不承担自动重试或加载逻辑，避免列表渲染层改变分页状态。
 */
@Composable
private fun QuickCreateInspirationEmptyArea() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Primary300.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(Dimens.SpaceMD))
            Text(
                "输入提示词开始创作",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 14.sp,
            )
        }
    }
}

/**
 * 展示单个灵感模板入口。
 *
 * 卡片只消费映射后的 UI 模型：预览类型、类别文案和徽标均由 StateHolder 写入状态前派生，
 * 避免 Composable 直接理解服务端 URL 优先级和布尔标记含义。
 *
 * @param template 灵感模板卡片 UI 模型。
 * @param onApplyTemplate 用户点击模板卡片时触发。
 */
@Composable
private fun QuickCreateInspirationTemplateCard(
    template: QuickCreateInspirationTemplateUi,
    onApplyTemplate: (String) -> Unit,
) {
    Surface(
        onClick = { onApplyTemplate(template.id) },
        shape = RoundedCornerShape(Dimens.RadiusLG),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkOutlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Primary300.copy(alpha = 0.26f), Secondary500.copy(alpha = 0.18f)),
                        ),
                        RoundedCornerShape(Dimens.RadiusMD),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (val preview = template.preview) {
                    is QuickCreateInspirationPreviewUi.Video -> VideoThumbnail(
                        url = preview.url,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is QuickCreateInspirationPreviewUi.Image -> SmartAsyncImage(
                        imageUrl = preview.url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    is QuickCreateInspirationPreviewUi.Placeholder -> Icon(
                        if (preview.mediaType == QuickCreateInspirationPlaceholderMediaType.VIDEO) {
                            Icons.Default.Videocam
                        } else {
                            Icons.Default.Image
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        template.categoryLabel,
                        color = Neutral500,
                        fontSize = 12.sp,
                    )
                    template.badges.forEach { badge ->
                        InspirationBadge(badge)
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Neutral500,
            )
        }
    }
}

@Composable
private fun InspirationBadge(badge: QuickCreateInspirationBadgeUi) {
    Text(
        badge.label,
        color = when (badge.tone) {
            QuickCreateInspirationBadgeTone.HOT -> ErrorDark
            QuickCreateInspirationBadgeTone.NEW -> Primary300
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}
