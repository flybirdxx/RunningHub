package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewProfileUiState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt
import com.runninghub.app.util.formatOneDecimal
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo
import org.jetbrains.compose.ui.tooling.preview.Preview

class ProfileVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // Voyager rule: no suspend in ScreenModel.init{} - load here.
        LaunchedEffect(Unit) {
            screenModel.loadUserData()
        }

        ProfileScreenContent(
            uiState = uiState,
            onRefresh = screenModel::refreshUserData,
            // 注销只更新 SessionManager 背后的会话事实来源；根 App 统一观察会话状态并清空业务页面栈。
            onLogout = { screenModel.logout() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    onRefresh: () -> Unit = {},
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
                    val windowInfo = LocalRhWindowInfo.current
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = windowInfo.formContentMaxWidth)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ProfileHeader(user = uiState.user)
                            Spacer(Modifier.height(16.dp))
                            AccountSummarySection(user = uiState.user)
                            Spacer(Modifier.height(16.dp))
                            ProfileMenuSection(onLogout = onLogout)
                            Spacer(Modifier.height(32.dp))
                        }
                    }
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
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(user = user)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.nickName?.takeIf { it.isNotBlank() } ?: "RunningHub 用户",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = maskPhone(user?.mobile),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemberBadge(memberInfo = user?.memberInfo, modifier = Modifier.padding(top = 8.dp))
            }

            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { },
            )
        }
    }
}

@Composable
private fun ProfileAvatar(user: User?) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!user?.headIcon.isNullOrEmpty()) {
            AsyncImage(
                model = user.headIcon,
                contentDescription = user.nickName ?: "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "默认头像",
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun MemberBadge(
    memberInfo: MemberInfo?,
    modifier: Modifier = Modifier,
) {
    val memberName = memberInfo?.memberName?.takeIf { it.isNotBlank() } ?: return
    Surface(
        modifier = modifier.widthIn(max = 240.dp),
        shape = RoundedCornerShape(999.dp),
        color = RunningHubThemeExt.colors.premiumGold.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "会员等级",
                tint = RunningHubThemeExt.colors.premiumGold,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = buildString {
                    append(memberName)
                    remainingDays(memberInfo)?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.labelMedium,
                color = RunningHubThemeExt.colors.premiumGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountSummarySection(user: User?) {
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
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountSummaryItem(
                label = "RH 币",
                value = formatNumber(user?.totalCoin ?: "0"),
                icon = Icons.Default.MonetizationOn,
                tint = RunningHubThemeExt.colors.premiumGold,
                modifier = Modifier.weight(1f),
            )
            AccountSummaryItem(
                label = "钱包",
                value = walletBalance(user?.walletInfo),
                icon = Icons.Default.AccountBalanceWallet,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            AccountSummaryItem(
                label = "会员剩余",
                value = remainingDays(user?.memberInfo) ?: "--",
                icon = Icons.Default.WorkspacePremium,
                tint = RunningHubThemeExt.colors.premiumOrange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountSummaryItem(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileMenuSection(onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(Dimens.RadiusLG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            ProfileMenuItem(icon = Icons.Default.Edit, title = "编辑资料", onClick = {})
            MenuDivider()
            ProfileMenuItem(icon = Icons.Default.WorkspacePremium, title = "会员续费", onClick = {})
            MenuDivider()
            ProfileMenuItem(icon = Icons.Default.MonetizationOn, title = "充值中心", onClick = {})
            MenuDivider()
            ProfileMenuItem(icon = Icons.Default.AccountBalanceWallet, title = "钱包明细", onClick = {})
            MenuDivider()
            ProfileMenuItem(icon = Icons.Default.DeleteOutline, title = "清除缓存", onClick = {})
            MenuDivider()
            ProfileMenuItem(icon = Icons.Default.Info, title = "关于 RunningHub", onClick = {})
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "退出登录",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onLogout,
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
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

private fun maskPhone(mobile: String?): String {
    val value = mobile?.takeIf { it.isNotBlank() } ?: return "未绑定手机号"
    return if (value.length >= 7) {
        "${value.take(3)}****${value.takeLast(4)}"
    } else {
        value
    }
}

private fun remainingDays(memberInfo: MemberInfo?): String? {
    val days = memberInfo?.memberRemainingDays?.takeIf { it.isNotBlank() && it != "0" }
    if (days != null) return "${days}天"

    val expiry = memberInfo?.memberExpiredTime
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore(" ")
    return expiry?.let { "$it 到期" }
}

private fun walletBalance(walletInfo: WalletInfo?): String {
    val balance = walletInfo?.balance ?: 0.0
    return "${currencySymbol(walletInfo)}${formatMoney(balance)}"
}

private fun formatMoney(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private fun formatNumber(value: String): String {
    val num = value.replace(",", "").toDoubleOrNull() ?: return value
    return when {
        // 个人页在 commonMain 渲染，不能依赖 JVM 的 String.format，否则 iOS 目标无法编译。
        num >= 10000 -> "${formatOneDecimal(num / 10000)}w"
        num >= 1000 -> "${formatOneDecimal(num / 1000)}k"
        else -> num.toInt().toString()
    }
}

private fun currencySymbol(walletInfo: WalletInfo?): String {
    val symbol = walletInfo?.currencySymbol
        ?.takeIf { it.isNotBlank() && !it.contains('\uFFFD') && !it.contains('\u951F') }
    if (symbol != null) return symbol

    return when (walletInfo?.currency?.uppercase()) {
        "CNY", "RMB" -> "\u00A5"
        "USD" -> "$"
        else -> "\u00A5"
    }
}

@Composable
private fun ProfileAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        ProfileScreenContent(
            uiState = previewProfileUiState(),
            onRefresh = {},
            onLogout = {},
        )
    }
}

@Preview
@Composable
private fun ProfilePhone320Preview() {
    ProfileAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun ProfilePhone360Preview() {
    ProfileAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun ProfileTabletPreview() {
    ProfileAdaptivePreview(RhPreviewSpec.Medium600)
}
