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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.feature.login.LoginVoyagerScreen
import com.runninghub.shared.domain.model.User

class ProfileVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        ProfileScreenContent(
            uiState = uiState,
            onRefresh = screenModel::refreshUserData,
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
    onLogout: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF0B0F1A),
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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeader(user = uiState.user)
                    Spacer(Modifier.height(16.dp))
                    AssetsSection(user = uiState.user)
                    Spacer(Modifier.height(16.dp))
                    QuickActionsGrid()
                    Spacer(Modifier.height(16.dp))
                    StatsRow(user = uiState.user)
                    Spacer(Modifier.height(16.dp))
                    SettingsSection(onLogout = onLogout)
                    Spacer(Modifier.height(32.dp))
                }
            }
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
                        Color(0xFF1A1F35),
                        Color(0xFF0B0F1A),
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
                    .background(Color(0xFF2A3050)),
                contentAlignment = Alignment.Center
            ) {
                if (!user?.headIcon.isNullOrEmpty()) {
                    AsyncImage(
                        model = user?.headIcon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.nickName ?: "RunningHub 用户",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
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
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
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
                tint = Color(0xFF94A3B8),
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
        color = Color(0xFF6C5CE7).copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = memberName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA78BFA),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141929)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "我的资产",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                TextButton(
                    onClick = {},
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9500)),
                ) {
                    Text("充值", fontSize = 13.sp)
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
                    iconTint = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(Color(0xFF2A3050))
                )
                AssetItem(
                    label = "钱包余额",
                    value = "${user?.walletInfo?.currencySymbol ?: "¥"}${user?.walletInfo?.balance ?: "0.00"}",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = Color(0xFF00D2FF),
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
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = user.memberInfo?.memberName ?: "会员",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFD700),
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
                                            fontSize = 11.sp,
                                            color = Color(0xFFBFA76A),
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9500),
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text("续费", fontSize = 13.sp)
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
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
        )
    }
}

@Composable
private fun QuickActionsGrid() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141929)),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                QuickActionItem(Icons.Default.Campaign, "网站公告")
                QuickActionItem(Icons.Default.Folder, "作品管理")
                QuickActionItem(Icons.Default.Api, "API 管理")
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
private fun QuickActionItem(icon: ImageVector, label: String) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141929)),
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
        )
    }
}

@Composable
private fun SettingsSection(onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141929)),
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
                titleColor = Color(0xFFF87171),
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
        color = Color(0xFF2A3050),
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color(0xFFCBD5E1),
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
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF475569),
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
                        listOf(Color(0xFF6C5CE7), Color(0xFF00D2FF))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "未登录",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "登录后查看个人信息和资产",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
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
