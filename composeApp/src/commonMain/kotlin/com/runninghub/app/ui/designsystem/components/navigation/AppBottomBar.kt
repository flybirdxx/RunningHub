package com.runninghub.app.ui.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme

/**
 * 应用底部导航项的纯 UI 状态。
 *
 * 该状态只描述底栏展示所需的标签、图标和选中态，不绑定 Voyager、业务状态或页面仓库，
 * 让 Design System 组件可以被不同壳层复用。
 *
 * @property label 用户可见的短标签。
 * @property contentDescription 图标无障碍描述，默认应与标签一致。
 * @property selectedIcon 选中态图标。
 * @property unselectedIcon 未选中态图标。
 * @property selected 当前项是否选中。
 */
@Immutable
data class AppBottomBarItemState(
    val label: String,
    val contentDescription: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val selected: Boolean,
)

/**
 * RunningHub 手机端应用底栏。
 *
 * 组件固定使用 Design System token：选中态使用品牌主色，未选态使用三级文字色，底部分隔线使用弱边框色。
 * 它只负责渲染和回传点击下标，不持有或推断导航业务状态。
 *
 * @param items 按展示顺序排列的底栏项。
 * @param onItemSelected 点击某项时回传其下标。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItemState>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceElevated,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        // edge-to-edge 下系统导航条(三键/手势)叠在窗口底部:背景由 Surface 延伸垫底,
        // 内容区先按导航条 inset 抬高再固定 72dp,避免三键导航设备上系统键与底栏图标重叠。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = RhSpacing.md,
                        top = RhSpacing.xs,
                        end = RhSpacing.md,
                        bottom = RhSpacing.lg,
                    ),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    AppBottomBarItem(
                        item = item,
                        onClick = { onItemSelected(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBottomBarItem(
    item: AppBottomBarItemState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    val labelColor = if (item.selected) colors.brandPrimary else colors.textTertiary
    val iconColor = if (item.selected) colors.textInverse else colors.textTertiary

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = RhSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .background(
                    color = if (item.selected) colors.brandPrimary else Color.Transparent,
                    shape = RoundedCornerShape(RhTheme.shapes.full),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (item.selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.contentDescription,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = item.label,
            color = labelColor,
            style = RhTheme.typography.meta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
