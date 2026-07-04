package com.runninghub.app.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.collapsible_section_collapse_content_description
import runninghub.composeapp.generated.resources.collapsible_section_expand_content_description
import runninghub.composeapp.generated.resources.collapsible_section_expand_hint

/**
 * 渲染可展开/收起的通用内容分组。
 *
 * 展开提示和无障碍描述通过 Compose Resources 获取，确保通用组件不会继续扩大硬编码文案基线；
 * 业务标题和附加头部内容由调用方传入，组件只负责本地展开状态与视觉容器。
 *
 * @param title 分组标题，来源于调用方所属页面或 Presentation 状态。
 * @param modifier 外层布局修饰符，用于控制宽度、边距或测试标记。
 * @param initiallyExpanded 初始是否展开；`true` 表示首次渲染展示内容，`false` 表示仅展示标题行。
 * @param headerContent 标题行右侧的附加内容；为 `null` 时不渲染额外区域。
 * @param content 展开状态下展示的具体内容，由调用方提供。
 */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    headerContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(250),
        label = "chevron_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RhTheme.shapes.md))
            .background(RhTheme.colors.surfaceSunken)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = RhSpacing.lg, vertical = RhSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) {
                    stringResource(Res.string.collapsible_section_collapse_content_description)
                } else {
                    stringResource(Res.string.collapsible_section_expand_content_description)
                },
                tint = RhTheme.colors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationAngle)
            )
            Spacer(Modifier.width(RhSpacing.sm))
            Text(
                text = title,
                color = RhTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            headerContent?.invoke()
            if (!expanded) {
                Text(
                    text = stringResource(Res.string.collapsible_section_expand_hint),
                    color = RhTheme.colors.brandPrimary,
                    fontSize = 12.sp
                )
            }
        }

        if (expanded) {
            Column(modifier = Modifier.padding(bottom = RhSpacing.sm)) {
                content()
            }
        }
    }
}
