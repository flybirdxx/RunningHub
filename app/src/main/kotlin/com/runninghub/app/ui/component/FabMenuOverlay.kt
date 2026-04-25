package com.runninghub.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip

@Composable
fun FabMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMenuItemClick: (String) -> Unit = {}
) {
    // --- Scrim (Dim Background) ---
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }

    // --- Menu Proper ---
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 110.dp), // Height above Bottom Nav
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color(0xFF1E1F22).copy(alpha = 0.92f), // Slightly more opaque for better readability
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 20.dp,
                modifier = Modifier
                    .width(260.dp) // Slightly wider for better text fit
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val menuItems = listOf(
                        Triple("图像生成 API", Icons.Default.AutoFixHigh, Color(0xFF6366F1)),
                        Triple("视频专业 API", Icons.Default.VideoLibrary, Color(0xFF10B981)),
                        Triple("音频处理 API", Icons.Default.Audiotrack, Color(0xFFF59E0B)),
                        Triple("3D 渲染 API", Icons.Default.ViewInAr, Color(0xFFEC4899))
                    )
                    
                    menuItems.forEach { (title, icon, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    onDismiss()
                                    onMenuItemClick(title) 
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = color.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                    tint = color
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
