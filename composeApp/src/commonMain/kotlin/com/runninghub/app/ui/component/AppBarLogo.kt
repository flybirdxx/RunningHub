package com.runninghub.app.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_bar_logo_title

/**
 * 渲染应用顶部栏品牌标识。
 *
 * 当前迁移期仍以文字品牌作为稳定占位，文案通过 Compose Resources 获取，避免通用组件保留
 * 硬编码 UI 文案；后续如果恢复图片 Logo，可继续复用 [assetPath] 作为资源定位参数。
 *
 * @param assetPath 预留的品牌图片资源路径，当前文字 Logo 实现不读取该值。
 * @param modifier 外层布局修饰符，由调用方控制尺寸、边距或测试标记。
 */
@Composable
fun AppBarLogo(
    assetPath: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.app_bar_logo_title),
        modifier = modifier,
        style = RhTypography.cardTitle,
        fontWeight = FontWeight.Bold,
        color = RhTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
