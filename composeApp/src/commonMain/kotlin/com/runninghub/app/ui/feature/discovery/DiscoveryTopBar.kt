package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.component.AppBarLogo
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBarDefaults
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_search_content_description

/**
 * 发现页顶栏。左侧品牌 logo，右侧搜索入口；点击搜索跳转独立搜索页，
 * 不再承载内联搜索输入。高度沿用 RhTopBar 尺寸契约保持全局一致。
 */
@Composable
internal fun DiscoveryTopBar(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(colors.backgroundPrimary)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RhTopBarDefaults.height)
                .padding(horizontal = RhSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBarLogo(
                assetPath = "logo_appbar.svg",
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(max = 120.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.discovery_search_content_description),
                    tint = colors.textPrimary,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.borderSubtle)
    }
}
