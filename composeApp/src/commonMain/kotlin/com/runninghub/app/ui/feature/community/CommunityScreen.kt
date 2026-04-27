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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreenContent(
    modifier: Modifier = Modifier,
    uiState: CommunityUiState,
    onToolClick: (CommunityTool) -> Unit = {}
) {
    val extColors = RunningHubThemeExt.colors

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
                    .padding(
                        start = Dimens.SpaceXXL,
                        end = Dimens.SpaceXXL,
                        top = Dimens.Space3XL,
                        bottom = Dimens.SpaceXXL
                    )
            ) {
                Column {
                    Text(
                        text = "创意工坊",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(Dimens.SpaceXS))
                    Text(
                        text = "探索 AI 创意工具，释放无限可能",
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(Dimens.SpaceLG),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    modifier = Modifier.fillMaxSize()
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
                    maxLines = 1
                )
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
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
