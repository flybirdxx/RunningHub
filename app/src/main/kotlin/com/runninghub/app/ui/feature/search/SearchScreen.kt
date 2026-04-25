package com.runninghub.app.ui.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.runninghub.app.ui.feature.discovery.DiscoveryBottomNav
import com.runninghub.app.ui.component.FabMenuOverlay
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.theme.RunningHubTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    var appIdInput by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { 
                DiscoveryBottomNav(
                    navController = navController,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = !isMenuExpanded }
                ) 
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = RunningHubTeal
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "通过 ID 访问应用",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "输入应用 ID 直接跳转到详情页",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = appIdInput,
                onValueChange = { appIdInput = it },
                label = { Text("App ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RunningHubTeal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = RunningHubTeal,
                    cursorColor = RunningHubTeal
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (appIdInput.isNotBlank()) {
                        navController.navigate(Screen.AppDetail.createRoute(appIdInput.trim()))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RunningHubTeal,
                    contentColor = Color.Black
                ),
                enabled = appIdInput.isNotBlank()
            ) {
                Text("立即跳转", fontWeight = FontWeight.Bold)
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
