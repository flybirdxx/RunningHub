package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.runninghub.app.data.remote.model.UserDto
import com.runninghub.app.ui.theme.RunningHubTeal

@Composable
fun ProfileSettingsDialog(
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onBindApiKey: (String) -> Unit,
    onBindEnterpriseApiKey: (String) -> Unit,
    onUnbindApiKey: () -> Unit
) {
    var showBindAppKeyDialog by remember { mutableStateOf(false) }
    var showBindEnterpriseKeyDialog by remember { mutableStateOf(false) }

    if (showBindAppKeyDialog) {
        BindAppKeyDialog(
            onDismiss = { showBindAppKeyDialog = false },
            onConfirm = { key ->
                onBindApiKey(key)
                showBindAppKeyDialog = false
            }
        )
    }

    if (showBindEnterpriseKeyDialog) {
        BindEnterpriseKeyDialog(
            onDismiss = { showBindEnterpriseKeyDialog = false },
            onConfirm = { key ->
                onBindEnterpriseApiKey(key)
                showBindEnterpriseKeyDialog = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!uiState.hasApiKey) {
                    BindApiKeyScreen(
                        isBinding = uiState.isBinding,
                        error = uiState.bindError,
                        onBind = onBindApiKey
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ProfileHeader(uiState.user)

                        if (!uiState.hasAppApiKey) {
                            AppApiKeyWarningCard(onClick = { showBindAppKeyDialog = true })
                        }

                        AccountStatsSection(uiState)

                        if (!uiState.hasEnterpriseApiKey) {
                            EnterpriseApiKeyWarningCard(onClick = { showBindEnterpriseKeyDialog = true })
                        }

                        ProfileMenuSection(
                            uiState = uiState,
                            onNavigateToHistory = onNavigateToHistory,
                            onUnbindApiKey = {
                                onUnbindApiKey()
                                onDismiss()
                            },
                            onBindAppKey = { showBindAppKeyDialog = true },
                            onBindEnterpriseKey = { showBindEnterpriseKeyDialog = true }
                        )

                        Spacer(Modifier.height(32.dp))
                    }
                }

                if (uiState.isLoading && uiState.hasApiKey) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RunningHubTeal
                    )
                }
                
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BindAppKeyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绑定 App API Key") },
        text = {
            Column {
                Text("运行 AI 应用需绑定 API Key。\n此操作不会影响您的登录状态。", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
            ) {
                Text("绑定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        },
        containerColor = Color(0xFF2A2C30),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun BindEnterpriseKeyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绑定企业级 API Key") },
        text = {
            Column {
                Text("音频生成功能需绑定企业级（共享级）API Key。\n此操作不会影响您的登录状态。", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enterprise API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
            ) {
                Text("绑定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        },
        containerColor = Color(0xFF2A2C30),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun BindApiKeyScreen(
    isBinding: Boolean,
    error: String?,
    onBind: (String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = RunningHubTeal,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "绑定 API Key",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请输入您的 RunningHub API Key 以访问个人中心",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("API Key / Login Cookie") },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            maxLines = 6,
            placeholder = { Text("请输入 API Key 或浏览器登录 Cookie") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RunningHubTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = RunningHubTeal,
                unfocusedLabelColor = Color.Gray,
                cursorColor = RunningHubTeal,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(Modifier.height(8.dp))
        Text(
            text = "如何获取? 登录 RunningHub -> 个人中心 -> API Key",
            style = MaterialTheme.typography.labelSmall,
            color = RunningHubTeal
        )

        Spacer(Modifier.height(24.dp))

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { onBind(apiKeyInput) },
            enabled = !isBinding && apiKeyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
        ) {
            if (isBinding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("立即绑定")
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: UserDto? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (user?.headIcon != null) {
                AsyncImage(
                    model = user.headIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.Gray
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user?.nickName ?: "RunningHub用户",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!user?.apiType.isNullOrEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = RunningHubTeal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, RunningHubTeal.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = user?.apiType ?: "API",
                            fontSize = 10.sp,
                            color = RunningHubTeal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ID: ${user?.id ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (!user?.mobile.isNullOrEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    val mobile = user!!.mobile!!
                    val masked = if(mobile.length >= 7) {
                        mobile.substring(0, 3) + "****" + mobile.substring(mobile.length - 4)
                    } else mobile
                    
                    Text(
                        text = masked,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppApiKeyWarningCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
        border = BorderStroke(1.dp, Color(0xFFFF5722))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5722))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("缺少 App API Key", color = Color(0xFFFFCCBC), fontWeight = FontWeight.Bold)
                Text("无法运行 AI 应用，点击绑定", color = Color(0xFFFFCCBC), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EnterpriseApiKeyWarningCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
        border = BorderStroke(1.dp, Color(0xFF3F51B5))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Business, null, tint = Color(0xFF9FA8DA))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("缺少企业级 API Key", color = Color.White, fontWeight = FontWeight.Bold)
                Text("无法使用 MiniMax 音频生成，点击绑定", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AccountStatsSection(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AssetCard(
            label = "RH币",
            value = uiState.user?.totalCoin ?: "0",
            desc = "用于工作流/AI应用运行时长消耗",
            buttonText = "充值",
            startColor = Color(0xFFFFF7E6),
            endColor = Color(0xFFFFEBD4),
            textColor = Color(0xFF7A4F00),
            buttonColor = Color(0xFFFF8800)
        )

        AssetCard(
            label = "钱包",
            value = "¥${uiState.user?.walletInfo?.balance ?: "0.00"}",
            desc = "用于共享或三方API调用消耗",
            buttonText = "充值",
            startColor = Color(0xFFFFF7E6),
            endColor = Color(0xFFFFEBD4),
            textColor = Color(0xFF7A4F00),
            buttonColor = Color(0xFFFF8800)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val memberName = uiState.user?.memberInfo?.memberName ?: "普通用户"
            val expireTime = uiState.user?.memberInfo?.memberExpiredTime?.split(" ")?.get(0) ?: ""
            
            Text(
                text = "$memberName ${if(expireTime.isNotEmpty()) "${expireTime}到期" else ""}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0C9A6)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("续费", color = Color(0xFF5E4215), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AssetCard(
    label: String, 
    value: String, 
    desc: String, 
    buttonText: String,
    startColor: Color,
    endColor: Color,
    textColor: Color,
    buttonColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
        ) {
            Icon(
                Icons.Default.MonetizationOn, 
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp, y = 20.dp)
                    .size(160.dp)
                    .alpha(0.05f),
                tint = Color.Black
            )

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(label, fontSize = 14.sp, color = textColor.copy(alpha = 0.8f))
                            Spacer(Modifier.width(8.dp))
                            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(desc, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                    }
                    
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(buttonText, color = Color.White, fontSize = 12.sp)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                   Text("余额预警未开启 (去设置)", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                   Text("- o -", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileMenuSection(
    uiState: ProfileUiState,
    onNavigateToHistory: () -> Unit,
    onUnbindApiKey: () -> Unit,
    onBindAppKey: () -> Unit,
    onBindEnterpriseKey: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 4,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val itemModifier = Modifier.weight(1f)
            
            GridMenuItem(Icons.Default.Receipt, "消费记录", modifier = itemModifier)
            GridMenuItem(Icons.Default.AccountBalanceWallet, "我的收益", modifier = itemModifier)
            GridMenuItem(Icons.Default.Description, "开票管理", modifier = itemModifier)
            
            if (uiState.hasAppApiKey) {
                GridMenuItem(Icons.Default.Terminal, "API 控制台", modifier = itemModifier)
            } else {
                 GridMenuItem(
                     Icons.Default.AddLink, 
                     "绑定 App Key", 
                     modifier = itemModifier,
                     onClick = onBindAppKey
                 )
            }

            if (uiState.hasEnterpriseApiKey) {
                GridMenuItem(Icons.Default.BusinessCenter, "企业控制台", modifier = itemModifier)
            } else {
                GridMenuItem(
                    Icons.Default.Business,
                    "绑定企业 Key",
                    modifier = itemModifier,
                    onClick = onBindEnterpriseKey
                )
            }
            
            Box(modifier = itemModifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Icon(Icons.Default.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Surface(
                            color = Color(0xFFFF5252),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 24.dp, y = (-8).dp)
                        ) {
                            Text(
                                "送RH币",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("邀请码", fontSize = 14.sp, color = Color.White)
                }
            }
            
            GridMenuItem(Icons.Default.BrandingWatermark, "水印设置", modifier = itemModifier)
            GridMenuItem(Icons.Default.Settings, "用户设置", modifier = itemModifier)
            GridMenuItem(
                Icons.Default.ExitToApp, 
                "退出登录", 
                modifier = itemModifier,
                onClick = onUnbindApiKey
            )
        }
    }
}

@Composable
private fun GridMenuItem(
    icon: ImageVector, 
    title: String, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 14.sp, color = Color.White)
    }
}
