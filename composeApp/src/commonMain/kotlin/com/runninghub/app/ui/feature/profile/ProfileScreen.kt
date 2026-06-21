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
import com.runninghub.feature.auth.presentation.profile.ProfileUiState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.profile_avatar_content_description
import runninghub.composeapp.generated.resources.profile_default_avatar_content_description
import runninghub.composeapp.generated.resources.profile_default_user_name
import runninghub.composeapp.generated.resources.profile_locked_content_description
import runninghub.composeapp.generated.resources.profile_member_badge_separator
import runninghub.composeapp.generated.resources.profile_member_expiry_format
import runninghub.composeapp.generated.resources.profile_member_level_content_description
import runninghub.composeapp.generated.resources.profile_member_remaining_days_format
import runninghub.composeapp.generated.resources.profile_menu_about
import runninghub.composeapp.generated.resources.profile_menu_clear_cache
import runninghub.composeapp.generated.resources.profile_menu_edit_profile
import runninghub.composeapp.generated.resources.profile_menu_logout
import runninghub.composeapp.generated.resources.profile_menu_more_content_description
import runninghub.composeapp.generated.resources.profile_menu_recharge_center
import runninghub.composeapp.generated.resources.profile_menu_renew_membership
import runninghub.composeapp.generated.resources.profile_menu_wallet_details
import runninghub.composeapp.generated.resources.profile_not_logged_in_subtitle
import runninghub.composeapp.generated.resources.profile_not_logged_in_title
import runninghub.composeapp.generated.resources.profile_settings_content_description
import runninghub.composeapp.generated.resources.profile_summary_member_remaining
import runninghub.composeapp.generated.resources.profile_summary_rh_coin
import runninghub.composeapp.generated.resources.profile_summary_wallet
import runninghub.composeapp.generated.resources.profile_unbound_mobile

/**
 * 个人中心页的 Voyager Screen。
 *
 * 该类型只负责把 Voyager 生命周期适配到 [ProfileScreenModel]，并把会话注销交回根会话状态机；
 * 用户资料加载、凭据绑定和注销状态清理由 `feature:auth:presentation` 维护。
 */
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

/**
 * 渲染个人中心页面内容。
 *
 * 页面仅消费 [ProfileUiState] 并通过回调发送刷新和注销意图；不直接访问 Token、持久化存储或 Data 实现。
 *
 * @param modifier 外部容器传入的布局修饰符。
 * @param uiState Profile Presentation 层输出的可渲染状态。
 * @param onRefresh 用户下拉刷新时触发的资料刷新回调。
 * @param onLogout 用户点击退出登录时触发的会话清理回调。
 */
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
    val defaultUserName = stringResource(Res.string.profile_default_user_name)
    val unboundMobileText = stringResource(Res.string.profile_unbound_mobile)
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
                    text = user?.nickName?.takeIf { it.isNotBlank() } ?: defaultUserName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = maskPhone(user?.mobile, unboundMobileText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemberBadge(memberInfo = user?.memberInfo, modifier = Modifier.padding(top = 8.dp))
            }

            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(Res.string.profile_settings_content_description),
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
    val avatarContentDescription = stringResource(Res.string.profile_avatar_content_description)
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
                contentDescription = user.nickName ?: avatarContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = stringResource(Res.string.profile_default_avatar_content_description),
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
    val remainingText = memberRemainingText(memberInfo)
    val memberBadgeSeparator = stringResource(Res.string.profile_member_badge_separator)
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
                contentDescription = stringResource(Res.string.profile_member_level_content_description),
                tint = RunningHubThemeExt.colors.premiumGold,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = buildString {
                    append(memberName)
                    remainingText?.let {
                        append(memberBadgeSeparator)
                        append(it)
                    }
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
    val remainingText = memberRemainingText(user?.memberInfo)
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
                label = stringResource(Res.string.profile_summary_rh_coin),
                value = formatNumber(user?.totalCoin ?: "0"),
                icon = Icons.Default.MonetizationOn,
                tint = RunningHubThemeExt.colors.premiumGold,
                modifier = Modifier.weight(1f),
            )
            AccountSummaryItem(
                label = stringResource(Res.string.profile_summary_wallet),
                value = walletBalance(user?.walletInfo),
                icon = Icons.Default.AccountBalanceWallet,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            AccountSummaryItem(
                label = stringResource(Res.string.profile_summary_member_remaining),
                value = remainingText ?: "--",
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
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                title = stringResource(Res.string.profile_menu_edit_profile),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.Default.WorkspacePremium,
                title = stringResource(Res.string.profile_menu_renew_membership),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.Default.MonetizationOn,
                title = stringResource(Res.string.profile_menu_recharge_center),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(Res.string.profile_menu_wallet_details),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.Default.DeleteOutline,
                title = stringResource(Res.string.profile_menu_clear_cache),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = stringResource(Res.string.profile_menu_about),
                onClick = {},
            )
            MenuDivider()
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = stringResource(Res.string.profile_menu_logout),
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
            contentDescription = stringResource(Res.string.profile_menu_more_content_description),
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
                contentDescription = stringResource(Res.string.profile_locked_content_description),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.profile_not_logged_in_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.profile_not_logged_in_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

private fun maskPhone(mobile: String?, unboundText: String): String {
    val value = mobile?.takeIf { it.isNotBlank() } ?: return unboundText
    return if (value.length >= 7) {
        "${value.take(3)}****${value.takeLast(4)}"
    } else {
        value
    }
}

@Composable
private fun memberRemainingText(memberInfo: MemberInfo?): String? {
    val days = memberInfo?.memberRemainingDays?.takeIf { it.isNotBlank() && it != "0" }
    if (days != null) {
        return stringResource(Res.string.profile_member_remaining_days_format, days)
    }

    val expiry = memberInfo?.memberExpiredTime
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore(" ")
    // 服务端返回日期字符串，UI 层只负责拼接本地化后缀，不解析时区或改变日期精度。
    return expiry?.let { stringResource(Res.string.profile_member_expiry_format, it) }
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
