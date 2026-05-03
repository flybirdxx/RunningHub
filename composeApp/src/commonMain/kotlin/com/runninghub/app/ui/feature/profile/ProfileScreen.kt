package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.feature.login.LoginVoyagerScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.shared.domain.model.User

class ProfileVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Voyager rule: no suspend in ScreenModel.init{} — load here
        LaunchedEffect(Unit) {
            screenModel.loadUserData()
        }

        ProfileScreenContent(
            uiState = uiState,
            onRefresh = screenModel::refreshUserData,
            onBindApiKey = screenModel::bindApiKey,
            onBindCookie = screenModel::bindCookie,
            onShowApiKeyDialog = screenModel::showApiKeyDialog,
            onDismissApiKeyDialog = screenModel::dismissApiKeyDialog,
            onShowCookieDialog = screenModel::showCookieDialog,
            onDismissCookieDialog = screenModel::dismissCookieDialog,
            onLogout = {
                screenModel.logout {
                    navigator.replaceAll(LoginVoyagerScreen())
                }
            },
        )
    }
}

@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    onRefresh: () -> Unit = {},
    onBindApiKey: (String) -> Unit = {},
    onBindCookie: (String) -> Unit = {},
    onShowApiKeyDialog: () -> Unit = {},
    onDismissApiKeyDialog: () -> Unit = {},
    onShowCookieDialog: () -> Unit = {},
    onDismissCookieDialog: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.user != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when {
            uiState.isLoading && uiState.user == null -> {
                LoadingIndicator(Modifier.padding(padding))
            }
            !uiState.isLoggedIn && uiState.user == null -> {
                NotLoggedInContent(modifier = Modifier.padding(padding))
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                val sizeClass = rememberWindowSizeClass()
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = if (sizeClass >= WindowSizeClass.Medium) 600.dp else Dp.Unspecified)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ProfileHeader(user = uiState.user)
                        Spacer(Modifier.height(16.dp))
                        AssetsSection(user = uiState.user)
                        Spacer(Modifier.height(16.dp))
                        QuickActionsGrid(
                            onApiKey = onShowApiKeyDialog,
                            onCookie = onShowCookieDialog,
                        )
                        Spacer(Modifier.height(16.dp))
                        StatsRow(user = uiState.user)
                        Spacer(Modifier.height(16.dp))
                        SettingsSection(onLogout = onLogout)
                        Spacer(Modifier.height(32.dp))
                    }
                } // PullToRefreshBox
            }
        }

        // API Key binding dialog
        if (uiState.showApiKeyDialog) {
            val apiKeyBuffer = remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismissApiKeyDialog,
                title = { Text("绑定 API Key") },
                text = {
                    Column {
                        Text(
                            "输入来自 RunningHub 网站的 API Key，用于访问 AI 应用。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyBuffer.value,
                            onValueChange = { apiKeyBuffer.value = it },
                            label = { Text("API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onBindApiKey(apiKeyBuffer.value) }) {
                        Text("绑定", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissApiKeyDialog) {
                        Text("取消")
                    }
                },
            )
        }

        // Cookie binding dialog
        if (uiState.showCookieDialog) {
            val cookieBuffer = remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismissCookieDialog,
                title = { Text("绑定 Cookie") },
                text = {
                    Column {
                        Text(
                            "输入来自 RunningHub 网站的 Cookie，用于高级功能访问。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = cookieBuffer.value,
                            onValueChange = { cookieBuffer.value = it },
                            label = { Text("Cookie") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onBindCookie(cookieBuffer.value) }) {
                        Text("绑定", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissCookieDialog) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileHeader(user: User?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!user?.headIcon.isNullOrEmpty()) {
                    AsyncImage(
                        model = user?.headIcon,
                        contentDescription = user?.nickName ?: "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.nickName ?: "RunningHub 用户",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!user?.mobile.isNullOrEmpty()) {
                        val maskedPhone = user?.mobile?.let {
                            if (it.length >= 7) "${it.substring(0, 3)}****${it.substring(it.length - 4)}"
                            else it
                        } ?: ""
                        Text(
                            text = "Tel: $maskedPhone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (user?.memberInfo != null) {
                    Spacer(Modifier.height(6.dp))
                    MemberBadge(memberName = user.memberInfo?.memberName)
                }
            }

            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { }
            )
        }
    }
}

@Composable
private fun MemberBadge(memberName: String?) {
    if (memberName.isNullOrEmpty()) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "会员等级",
                tint = RunningHubThemeExt.colors.premiumGold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = memberName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}

@Composable
private fun AssetsSection(user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "我的资产",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = {},
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = RunningHubThemeExt.colors.premiumOrange),
                ) {
                    Text("充值", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AssetItem(
                    label = "RH币余额",
                    value = formatNumber(user?.totalCoin ?: "0"),
                    icon = Icons.Default.MonetizationOn,
                    iconTint = RunningHubThemeExt.colors.premiumGold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                AssetItem(
                    label = "钱包余额",
                    value = "${user?.walletInfo?.currencySymbol ?: "¥"}${user?.walletInfo?.balance ?: "0.00"}",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }

            if (user?.memberInfo != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF2D2411),
                                        Color(0xFF3D3015),
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = "会员权益",
                                    tint = RunningHubThemeExt.colors.premiumGold,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = user.memberInfo?.memberName ?: "会员",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = RunningHubThemeExt.colors.premiumGold,
                                    )
                                    val expiry = user.memberInfo?.memberExpiredTime?.split(" ")?.firstOrNull() ?: ""
                                    val remaining = user.memberInfo?.memberRemainingDays
                                    val expiryText = if (!remaining.isNullOrEmpty() && remaining != "0") {
                                        "还有${remaining}天到期"
                                    } else if (expiry.isNotEmpty()) {
                                        "${expiry} 到期"
                                    } else ""
                                    if (expiryText.isNotEmpty()) {
                                        Text(
                                            text = expiryText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFBFA76A),
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RunningHubThemeExt.colors.premiumOrange,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text("续费", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onApiKey: () -> Unit = {},
    onCookie: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickActionItem(Icons.Default.Campaign, "网站公告")
                QuickActionItem(Icons.Default.Folder, "作品管理")
                QuickActionItem(Icons.Default.Api, "API 管理", onClick = onApiKey)
                QuickActionItem(Icons.Default.Groups, "开发者社区")
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickActionItem(Icons.Default.CardMembership, "会员权益")
                QuickActionItem(Icons.Default.Science, "创新实验")
                QuickActionItem(Icons.Default.PrivacyTip, "隐私政策")
                QuickActionItem(Icons.Default.History, "历史记录")
            }
        }
    }
}

@Composable
private fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatsRow(user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(label = "获赞", value = formatNumber(user?.likeCount ?: "0"))
            StatItem(label = "收藏", value = formatNumber(user?.collectCount ?: "0"))
            StatItem(label = "关注", value = formatNumber(user?.followCount ?: "0"))
            StatItem(label = "粉丝", value = formatNumber(user?.fanCount ?: "0"))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SettingsSection(onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            SettingsMenuItem(
                icon = Icons.Default.Info,
                title = "关于 RunningHub",
                onClick = {}
            )
            MenuDivider()
            SettingsMenuItem(
                icon = Icons.Default.DeleteOutline,
                title = "清除缓存",
                onClick = {}
            )
            MenuDivider()
            SettingsMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "退出登录",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onLogout
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = titleColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "更多",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NotLoggedInContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "已锁定",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "未登录",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "登录后查看个人信息和资产",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

private fun formatNumber(value: String): String {
    val num = value.replace(",", "").toDoubleOrNull() ?: return value
    return when {
        num >= 10000 -> String.format("%.1fw", num / 10000)
        num >= 1000 -> String.format("%.1fk", num / 1000)
        else -> num.toInt().toString()
    }
}
