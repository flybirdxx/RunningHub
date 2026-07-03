package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_description_title
import runninghub.composeapp.generated.resources.collapsible_section_collapse_content_description
import runninghub.composeapp.generated.resources.collapsible_section_expand_content_description

/**
 * App 详情页简介区。
 *
 * 从 AppDetailScreen.kt 拆分而来，承载简介 Composable 与折叠展示策略
 * （数据类、枚举、纯函数和阈值常量）；颜色统一读取 RhTheme 语义 token。
 */

@Composable
internal fun DescriptionSection(
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = RhTheme.colors
    val presentation = remember(description) { appDetailDescriptionPresentation(description) }
    var expanded by remember(presentation.cleanedDescription) { mutableStateOf(false) }
    val collapsed = presentation.isCollapsible && !expanded
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "description_chevron_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceDefault)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = if (collapsed) presentation.collapsedBottomPadding else 16.dp
                )
        ) {
            Text(
                text = stringResource(Res.string.app_detail_description_title),
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = presentation.cleanedDescription,
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = if (collapsed) presentation.collapsedMaxLines else Int.MAX_VALUE,
                overflow = if (collapsed) TextOverflow.Ellipsis else TextOverflow.Clip
            )
        }

        if (
            collapsed &&
            presentation.collapsedDepthEffect == AppDetailDescriptionDepthEffect.BottomGradientFade
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(presentation.collapsedDepthFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.surfaceDefault.copy(alpha = 0.62f),
                                colors.surfaceDefault
                            )
                        )
                    )
            )
        }

        if (
            presentation.isCollapsible &&
            presentation.toggleAffordance == AppDetailDescriptionToggleAffordance.BottomBorderTriangle
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(presentation.collapsedDepthFadeHeight)
                    .clickable { expanded = !expanded }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) {
                        stringResource(Res.string.collapsible_section_collapse_content_description)
                    } else {
                        stringResource(Res.string.collapsible_section_expand_content_description)
                    },
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(presentation.toggleIconSize)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

/**
 * App 详情简介的展示策略。
 *
 * @property cleanedDescription 去掉 HTML 标签后的简介正文；空字符串表示没有可展示内容。
 * @property isCollapsible true 表示正文超过默认展示容量，需要提供展开/收起入口；false 表示直接完整展示。
 * @property collapsedMaxLines 折叠态最多展示的文本行数，单位为行；短简介不会使用该限制。
 * @property toggleAffordance 折叠入口的视觉位置；长简介固定使用底部边框三角，避免挤占标题行。
 * @property collapsedDepthEffect 折叠态底部的视觉过渡；用于弱化长文本被截断时的硬切边界。
 * @property toggleIconSize 折叠入口箭头图标的视觉尺寸；保持和全局折叠控件一致。
 * @property collapsedBottomPadding 折叠态正文容器的底部内边距；0 表示箭头直接覆盖在渐隐文字上，不额外制造独立底栏。
 * @property collapsedDepthFadeHeight 折叠态底部渐隐层高度；箭头覆盖在该渐隐层上方。
 */
internal data class AppDetailDescriptionPresentation(
    val cleanedDescription: String,
    val isCollapsible: Boolean,
    val collapsedMaxLines: Int,
    val toggleAffordance: AppDetailDescriptionToggleAffordance,
    val collapsedDepthEffect: AppDetailDescriptionDepthEffect,
    val toggleIconSize: Dp,
    val collapsedBottomPadding: Dp,
    val collapsedDepthFadeHeight: Dp
)

/**
 * App 详情简介折叠入口的视觉形式。
 */
internal enum class AppDetailDescriptionToggleAffordance {
    /**
     * 在简介卡片底部边框中央显示三角箭头，折叠态向下，展开态向上。
     */
    BottomBorderTriangle
}

/**
 * App 详情简介折叠态的底部视觉过渡。
 */
internal enum class AppDetailDescriptionDepthEffect {
    /**
     * 在折叠内容底部叠加从透明到卡片背景色的渐隐层，让底部三角区域呈现景深模糊感。
     */
    BottomGradientFade
}

/**
 * 计算 App 详情简介是否需要折叠。
 *
 * 服务端简介可能包含 HTML 标签或很长的规则说明，详情页默认只展示有限行数，避免说明文案挤占参数区和运行按钮。
 *
 * @param description 服务端返回的原始简介文本，可能包含 HTML 标签、换行和较长正文。
 * @return 供简介 Composable 使用的清洗文本和折叠配置。
 */
internal fun appDetailDescriptionPresentation(description: String): AppDetailDescriptionPresentation {
    val cleanedDescription = description.replace(Regex("<[^>]*>"), "").trim()
    val lineCount = cleanedDescription.lineSequence().count()
    val isCollapsible = cleanedDescription.length > APP_DETAIL_DESCRIPTION_COLLAPSE_THRESHOLD ||
        lineCount > APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES

    return AppDetailDescriptionPresentation(
        cleanedDescription = cleanedDescription,
        isCollapsible = isCollapsible,
        collapsedMaxLines = APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES,
        toggleAffordance = AppDetailDescriptionToggleAffordance.BottomBorderTriangle,
        collapsedDepthEffect = AppDetailDescriptionDepthEffect.BottomGradientFade,
        toggleIconSize = APP_DETAIL_DESCRIPTION_TOGGLE_ICON_SIZE,
        collapsedBottomPadding = APP_DETAIL_DESCRIPTION_COLLAPSED_BOTTOM_PADDING,
        collapsedDepthFadeHeight = APP_DETAIL_DESCRIPTION_DEPTH_FADE_HEIGHT
    )
}

private const val APP_DETAIL_DESCRIPTION_COLLAPSE_THRESHOLD = 220
private const val APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES = 6
private val APP_DETAIL_DESCRIPTION_TOGGLE_ICON_SIZE = 18.dp
private val APP_DETAIL_DESCRIPTION_COLLAPSED_BOTTOM_PADDING = 0.dp
private val APP_DETAIL_DESCRIPTION_DEPTH_FADE_HEIGHT = 24.dp
