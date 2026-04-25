package com.runninghub.app.ui.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.runninghub.app.ui.feature.discovery.DiscoveryBottomNav
import com.runninghub.app.ui.component.FabMenuOverlay
import androidx.compose.runtime.*
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.theme.RunningHubTeal

data class WorkshopTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String? = null,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavHostController) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    
    val tools = listOf(
        WorkshopTool(
            "UI 检视器",
            "查看设备屏幕参数与系统信息",
            Icons.Default.Info,
            Screen.UiInspector.route,
            Color(0xFF3B82F6) // Blue
        ),
        WorkshopTool(
            "隐写解码",
            "从图片中提取隐藏的秘密数据",
            Icons.Default.Visibility, // Use Visibility or LockOpen
            Screen.SecretDecode.route,
            Color(0xFF8B5CF6) // Purple
        ),
        WorkshopTool(
            "色彩提取",
            "从图片中提取调色板 (开发中)",
            Icons.Default.Colorize,
            null,
            Color(0xFFEC4899) // Pink
        ),
        WorkshopTool(
            "智能裁切",
            "自动识别主体裁切图片 (开发中)",
            Icons.Default.Crop,
            null,
            Color(0xFF10B981) // Emerald
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { 
                DiscoveryBottomNav(
                    navController = navController,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = !isMenuExpanded }
                ) 
            },
            containerColor = Color.Black
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Header
                Text(
                    text = "创意工坊",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(24.dp)
                )

                // Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tools) { tool ->
                        ToolCard(tool = tool) {
                            if (tool.route != null) {
                                navController.navigate(tool.route)
                            }
                        }
                    }
                }
            }
        }
        
        FabMenuOverlay(
            isVisible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            onMenuItemClick = { title ->
                if (title == "音频处理 API") {
                    navController.navigate(Screen.AudioGeneration.route)
                }
            }
        )
    }
}

@Composable
fun ToolCard(tool: WorkshopTool, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = tool.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = tool.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
