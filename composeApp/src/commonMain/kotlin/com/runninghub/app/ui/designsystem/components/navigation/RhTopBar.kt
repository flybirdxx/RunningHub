package com.runninghub.app.ui.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** RhTopBar 的固定尺寸契约。 */
object RhTopBarDefaults {
    val height = 56.dp
}

/**
 * 页面顶栏。三段布局互相覆盖定位，左右插槽显隐不影响标题居中。
 *
 * @param title 居中标题，调用方负责本地化。
 * @param navigationIcon 可选左侧导航插槽（返回或菜单）。
 * @param actions 可选右侧操作插槽。
 */
@Composable
fun RhTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RhTopBarDefaults.height)
            .background(RhTheme.colors.backgroundPrimary),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = RhSpacing.sm)) {
            navigationIcon?.invoke()
        }
        Text(
            text = title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = RhSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke(this)
        }
    }
}
