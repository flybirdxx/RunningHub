package com.runninghub.app.ui.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt
import com.runninghub.feature.community.presentation.CommunityTool
import com.runninghub.feature.community.presentation.CommunityUiState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.community_screen_subtitle
import runninghub.composeapp.generated.resources.community_screen_title

/**
 * 社区工具页的 Voyager 入口。
 *
 * 本类只负责从 Koin 获取 [CommunityScreenModel] 并把状态交给 Compose 内容层；
 * 工具目录和后续导航 route 由 Presentation 状态提供，避免应用壳重新持有静态目录。
 */
class CommunityVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<CommunityScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        CommunityScreenContent(
            uiState = uiState,
            onToolClick = { tool -> /* navigate based on tool.route */ }
        )
    }
}

/**
 * 渲染社区工具页的可复用内容。
 *
 * 页面标题文案通过 Compose Resources 读取，确保后续多语言和硬编码文案治理能在应用壳统一收口；
 * 工具卡片标题仍来自 [CommunityUiState]，后续应随工具目录迁移节奏再接入资源化文案。
 *
 * @param modifier 外层布局修饰符，由调用方决定尺寸、测试标签或额外边距；默认不附加约束。
 * @param uiState 社区工具页当前可渲染状态，包含工具入口列表和本地目录准备状态。
 * @param onToolClick 用户点击工具卡片时触发的回调，参数为被点击的 [CommunityTool]；
 * 应用壳负责根据其 route 决定真实导航目标。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreenContent(
    modifier: Modifier = Modifier,
    uiState: CommunityUiState,
    onToolClick: (CommunityTool) -> Unit = {}
) {
    val extColors = RunningHubThemeExt.colors
    val windowInfo = LocalRhWindowInfo.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(extColors.gradientStart, extColors.gradientEnd)
                        )
                    )
                    .statusBarsPadding()
                    .padding(
                        start = Dimens.SpaceXXL,
                        end = Dimens.SpaceXXL,
                        top = Dimens.SpaceLG,
                        bottom = Dimens.SpaceXXL
                    )
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.community_screen_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(Dimens.SpaceXS))
                    Text(
                        text = stringResource(Res.string.community_screen_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = windowInfo.feedGridMinCardWidth),
                        contentPadding = PaddingValues(Dimens.SpaceLG),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = windowInfo.feedContentMaxWidth)
                    ) {
                        items(uiState.tools, key = { it.id }) { tool ->
                            ToolCard(
                                tool = tool,
                                onClick = { onToolClick(tool) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier = Modifier,
    tool: CommunityTool,
    onClick: () -> Unit
) {
    val iconColor = iconColorForTool(tool.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceLG),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconSizeXL)
                    .clip(RoundedCornerShape(Dimens.RadiusMD))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveIcon(tool.iconName),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(Dimens.IconSizeMD)
                )
            }
            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun iconColorForTool(id: String): Color = when (id) {
    "audio_gen" -> MaterialTheme.colorScheme.primary
    "steganography" -> Color(0xFF8B5CF6)
    "ui_inspector" -> Color(0xFF3B82F6)
    "color_extract" -> Color(0xFFEC4899)
    "smart_crop" -> Color(0xFF10B981)
    "workflow" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

private fun resolveIcon(name: String): ImageVector = when (name) {
    "audiotrack" -> Icons.Default.Audiotrack
    "visibility" -> Icons.Default.Visibility
    "info" -> Icons.Default.Info
    "palette" -> Icons.Default.Palette
    "crop" -> Icons.Default.Crop
    "hub" -> Icons.Default.Hub
    else -> Icons.Default.Build
}

private fun communityPreviewState(): CommunityUiState = CommunityUiState(
    tools = listOf(
        CommunityTool(
            id = "audio_gen",
            title = "Audio Generation",
            description = "Generate voice, music, and short sound effects from text prompts.",
            iconName = "audiotrack",
            route = "audio_generation",
        ),
        CommunityTool(
            id = "steganography",
            title = "Secret Decode",
            description = "Extract hidden data from images without breaking the compact card layout.",
            iconName = "visibility",
            route = "secret_decode",
        ),
        CommunityTool(
            id = "ui_inspector",
            title = "UI Inspector",
            description = "Inspect screen metrics, insets, and display details across devices.",
            iconName = "info",
            route = "ui_inspector",
        ),
        CommunityTool(
            id = "color_extract",
            title = "Palette Extractor",
            description = "Build reusable palettes from reference imagery.",
            iconName = "palette",
            route = "color_extract",
        ),
        CommunityTool(
            id = "smart_crop",
            title = "Smart Crop",
            description = "Keep the subject centered while adapting aspect ratios.",
            iconName = "crop",
            route = "smart_crop",
        ),
        CommunityTool(
            id = "workflow",
            title = "Workflow Plaza",
            description = "Browse reusable community workflows and starter presets.",
            iconName = "hub",
            route = "workflow_plaza",
        ),
    )
)

@Preview
@Composable
private fun CommunityMediumPreview() {
    RunningHubPreviewSurface(windowWidth = 600.dp, windowHeight = 840.dp) {
        CommunityScreenContent(uiState = communityPreviewState())
    }
}

@Preview
@Composable
private fun CommunityExpandedPreview() {
    RunningHubPreviewSurface(windowWidth = 840.dp, windowHeight = 1180.dp) {
        CommunityScreenContent(uiState = communityPreviewState())
    }
}
