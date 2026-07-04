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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.koin.compose.koinInject
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewProfileUiState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.designsystem.components.billing.MembershipCard
import com.runninghub.app.ui.designsystem.components.billing.MembershipCardState
import com.runninghub.app.ui.designsystem.components.billing.MembershipStatusVisualState
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceAction
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceCard
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceCardState
import com.runninghub.app.ui.designsystem.components.billing.WalletBalanceRiskState
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.feature.auth.presentation.profile.ProfileAssetCenterUiModel
import com.runninghub.feature.auth.presentation.profile.ProfileAssetLoadState
import com.runninghub.feature.auth.presentation.profile.ProfileMembershipCenterUiModel
import com.runninghub.feature.auth.presentation.profile.ProfileMembershipStatus
import com.runninghub.feature.auth.presentation.profile.ProfileUiState
import com.runninghub.feature.auth.presentation.profile.ProfileWalletCenterUiModel
import com.runninghub.feature.auth.presentation.profile.ProfileWalletRisk
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
import runninghub.composeapp.generated.resources.profile_membership_active
import runninghub.composeapp.generated.resources.profile_membership_expired
import runninghub.composeapp.generated.resources.profile_membership_level_label
import runninghub.composeapp.generated.resources.profile_membership_none
import runninghub.composeapp.generated.resources.profile_membership_remaining_unknown
import runninghub.composeapp.generated.resources.profile_menu_about
import runninghub.composeapp.generated.resources.profile_menu_clear_cache
import runninghub.composeapp.generated.resources.profile_menu_edit_profile
import runninghub.composeapp.generated.resources.profile_menu_logout
import runninghub.composeapp.generated.resources.profile_menu_more_content_description
import runninghub.composeapp.generated.resources.profile_not_logged_in_action
import runninghub.composeapp.generated.resources.profile_not_logged_in_subtitle
import runninghub.composeapp.generated.resources.profile_not_logged_in_title
import runninghub.composeapp.generated.resources.profile_settings_content_description
import runninghub.composeapp.generated.resources.settings_chooser_api_key_entry
import runninghub.composeapp.generated.resources.settings_chooser_cookie_entry
import runninghub.composeapp.generated.resources.settings_chooser_title
import runninghub.composeapp.generated.resources.settings_dialog_cancel_action
import runninghub.composeapp.generated.resources.profile_unbound_mobile
import runninghub.composeapp.generated.resources.profile_wallet_balance_failed
import runninghub.composeapp.generated.resources.profile_wallet_cny_label
import runninghub.composeapp.generated.resources.profile_wallet_recharge_action
import runninghub.composeapp.generated.resources.profile_wallet_rhb_label
import runninghub.composeapp.generated.resources.profile_wallet_unknown

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
        val sessionManager = koinInject<SessionManager>()
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
            // 未登录态“去登录”与根导航保持同一机制：把会话事实置为未认证，交给根 App 用 replaceAll 切到登录页。
            onGoToLogin = { sessionManager.logout() },
            onOpenApiKeyDialog = screenModel::showApiKeyDialog,
            onDismissApiKeyDialog = screenModel::dismissApiKeyDialog,
            onConfirmApiKey = screenModel::bindApiKey,
            onOpenCookieDialog = screenModel::showCookieDialog,
            onDismissCookieDialog = screenModel::dismissCookieDialog,
            onConfirmCookie = screenModel::bindCookie,
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
 * @param onGoToLogin 未登录态点击“去登录”时触发的登录导航回调。
 * @param onOpenApiKeyDialog 用户在设置入口选择 API Key 绑定时触发的打开回调。
 * @param onDismissApiKeyDialog API Key 弹窗取消或系统返回时触发的关闭回调。
 * @param onConfirmApiKey 用户确认绑定 API Key 时触发的回调，参数为敏感凭据原文，不做本地日志或回显。
 * @param onOpenCookieDialog 用户在设置入口选择 Cookie 登录时触发的打开回调。
 * @param onDismissCookieDialog Cookie 弹窗取消或系统返回时触发的关闭回调。
 * @param onConfirmCookie 用户确认 Cookie 登录时触发的回调，参数为敏感凭据原文，不做本地日志或回显。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    onRefresh: () -> Unit = {},
    onRecharge: () -> Unit = {},
    onLogout: () -> Unit = {},
    onGoToLogin: () -> Unit = {},
    onOpenApiKeyDialog: () -> Unit = {},
    onDismissApiKeyDialog: () -> Unit = {},
    onConfirmApiKey: (String) -> Unit = {},
    onOpenCookieDialog: () -> Unit = {},
    onDismissCookieDialog: () -> Unit = {},
    onConfirmCookie: (String) -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.user != null
    // 设置入口只是一个应用壳内的本地选择器，用于把齿轮点击分流到两个独立凭据弹窗；
    // 弹窗真实开关仍由 Presentation 层的 ProfileUiState 持有，避免把业务状态拉回 composeApp。
    var showSettingsChooser by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = RhTheme.colors.backgroundPrimary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when {
            uiState.isLoading && uiState.user == null -> {
                LoadingIndicator(Modifier.padding(padding))
            }
            !uiState.isLoggedIn && uiState.user == null -> {
                NotLoggedInContent(
                    modifier = Modifier.padding(padding),
                    onGoToLogin = onGoToLogin,
                )
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
                            ProfileHeader(
                                user = uiState.user,
                                onOpenSettings = { showSettingsChooser = true },
                            )
                            Spacer(Modifier.height(16.dp))
                            AssetCenterSection(
                                assetCenter = uiState.assetCenter,
                                onRecharge = onRecharge,
                            )
                            Spacer(Modifier.height(16.dp))
                            ProfileMenuSection(onLogout = onLogout)
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSettingsChooser) {
        SettingsChooserDialog(
            onDismiss = { showSettingsChooser = false },
            onSelectApiKey = {
                showSettingsChooser = false
                onOpenApiKeyDialog()
            },
            onSelectCookie = {
                showSettingsChooser = false
                onOpenCookieDialog()
            },
        )
    }

    if (uiState.showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = onDismissApiKeyDialog,
            onConfirm = onConfirmApiKey,
        )
    }

    if (uiState.showCookieDialog) {
        CookieDialog(
            onDismiss = onDismissCookieDialog,
            onConfirm = onConfirmCookie,
        )
    }
}

/**
 * 渲染账号设置选择器。
 *
 * 齿轮入口下有两个相互独立的凭据流程（API Key 绑定、Cookie 登录），此选择器只做分流，
 * 不持有任何凭据；真正的弹窗开关由 Presentation 层的 ProfileUiState 决定。
 *
 * @param onDismiss 用户取消或点击遮罩时触发的关闭回调。
 * @param onSelectApiKey 用户选择 API Key 绑定入口时触发的回调。
 * @param onSelectCookie 用户选择 Cookie 登录入口时触发的回调。
 */
@Composable
private fun SettingsChooserDialog(
    onDismiss: () -> Unit,
    onSelectApiKey: () -> Unit,
    onSelectCookie: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.lg),
        title = {
            Text(
                text = stringResource(Res.string.settings_chooser_title),
                style = RhTypography.sectionTitle,
                color = RhTheme.colors.textPrimary,
            )
        },
        text = {
            Column {
                SettingsChooserEntry(
                    icon = Icons.Default.Lock,
                    title = stringResource(Res.string.settings_chooser_api_key_entry),
                    onClick = onSelectApiKey,
                )
                SettingsChooserEntry(
                    icon = Icons.Default.Person,
                    title = stringResource(Res.string.settings_chooser_cookie_entry),
                    onClick = onSelectCookie,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = RhTheme.colors.textSecondary,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.settings_dialog_cancel_action),
                    style = RhTypography.button,
                )
            }
        },
    )
}

/**
 * 渲染账号设置选择器中的单个入口行。
 *
 * @param icon 入口图标。
 * @param title 已本地化的入口文案。
 * @param onClick 点击该入口时触发的回调。
 */
@Composable
private fun SettingsChooserEntry(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = RhTheme.colors.brandPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = RhTypography.body,
            color = RhTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = RhTheme.colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    onOpenSettings: () -> Unit = {},
) {
    val defaultUserName = stringResource(Res.string.profile_default_user_name)
    val unboundMobileText = stringResource(Res.string.profile_unbound_mobile)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RhTheme.colors.brandMuted,
                        RhTheme.colors.surfaceElevated,
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
                    style = RhTypography.sectionTitle,
                    color = RhTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = maskPhone(user?.mobile, unboundMobileText),
                    style = RhTypography.body,
                    color = RhTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemberBadge(memberInfo = user?.memberInfo, modifier = Modifier.padding(top = 8.dp))
            }

            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(Res.string.profile_settings_content_description),
                tint = RhTheme.colors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onOpenSettings),
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
            .background(RhTheme.colors.surfaceSunken),
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
                tint = RhTheme.colors.textTertiary,
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
        color = RhTheme.colors.priceMoney.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = stringResource(Res.string.profile_member_level_content_description),
                tint = RhTheme.colors.priceMoney,
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
                style = RhTypography.caption,
                color = RhTheme.colors.priceMoney,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssetCenterSection(
    assetCenter: ProfileAssetCenterUiModel,
    onRecharge: () -> Unit,
) {
    if (!assetCenter.visible) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WalletBalanceCard(
            state = assetCenter.wallet.toWalletBalanceCardState(),
            onAction = { action ->
                when (action) {
                    is WalletBalanceAction.Recharge -> onRecharge()
                }
            },
        )
        if (assetCenter.membership.status != ProfileMembershipStatus.None) {
            MembershipCard(state = assetCenter.membership.toMembershipCardState())
        }
    }
}

@Composable
private fun ProfileWalletCenterUiModel.toWalletBalanceCardState(): WalletBalanceCardState =
    WalletBalanceCardState(
        rhbPointsLabel = stringResource(Res.string.profile_wallet_rhb_label),
        rhbPointsValue = rhbPoints ?: stringResource(Res.string.profile_wallet_unknown),
        walletBalanceLabel = stringResource(Res.string.profile_wallet_cny_label),
        walletBalanceValue = walletBalance?.let { "$walletCurrency $it" }
            ?: stringResource(Res.string.profile_wallet_unknown),
        risk = risk.toWalletBalanceRiskState(),
        primaryAction = WalletBalanceAction.Recharge(stringResource(Res.string.profile_wallet_recharge_action)),
    )

@Composable
private fun ProfileWalletRisk.toWalletBalanceRiskState(): WalletBalanceRiskState =
    when (this) {
        ProfileWalletRisk.None -> WalletBalanceRiskState.None()
        ProfileWalletRisk.BalanceUnavailable -> {
            WalletBalanceRiskState.BalanceUnavailable(stringResource(Res.string.profile_wallet_balance_failed))
        }
    }

@Composable
private fun ProfileMembershipCenterUiModel.toMembershipCardState(): MembershipCardState =
    MembershipCardState(
        levelLabel = stringResource(Res.string.profile_membership_level_label),
        levelName = levelName ?: stringResource(Res.string.profile_membership_none),
        remainingLabel = membershipRemainingLabel(),
        status = status.toMembershipStatusVisualState(),
    )

@Composable
private fun ProfileMembershipCenterUiModel.membershipRemainingLabel(): String =
    when (status) {
        ProfileMembershipStatus.None -> stringResource(Res.string.profile_membership_none)
        ProfileMembershipStatus.Expired -> stringResource(Res.string.profile_membership_expired)
        ProfileMembershipStatus.Active -> remainingDays
            ?.let { stringResource(Res.string.profile_member_remaining_days_format, it) }
            ?: expiresAt?.let { stringResource(Res.string.profile_member_expiry_format, it) }
            ?: stringResource(Res.string.profile_membership_active)
        ProfileMembershipStatus.Unknown -> stringResource(Res.string.profile_membership_remaining_unknown)
    }

private fun ProfileMembershipStatus.toMembershipStatusVisualState(): MembershipStatusVisualState =
    when (this) {
        ProfileMembershipStatus.None -> MembershipStatusVisualState.None
        ProfileMembershipStatus.Active -> MembershipStatusVisualState.Active
        ProfileMembershipStatus.Expired -> MembershipStatusVisualState.Expired
        ProfileMembershipStatus.Unknown -> MembershipStatusVisualState.Unknown
    }

@Composable
private fun ProfileMenuSection(onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(RhTheme.shapes.lg),
        colors = CardDefaults.cardColors(containerColor = RhTheme.colors.surfaceDefault),
    ) {
        Column {
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                title = stringResource(Res.string.profile_menu_edit_profile),
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
                titleColor = RhTheme.colors.statusFailed,
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
        color = RhTheme.colors.borderSubtle,
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = RhTheme.colors.textSecondary,
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
            style = RhTypography.body,
            color = titleColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = stringResource(Res.string.profile_menu_more_content_description),
            tint = RhTheme.colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NotLoggedInContent(
    modifier: Modifier = Modifier,
    onGoToLogin: () -> Unit = {},
) {
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
                        listOf(RhTheme.colors.brandPrimary, RhTheme.colors.brandSecondary)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = stringResource(Res.string.profile_locked_content_description),
                tint = RhTheme.colors.textInverse,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.profile_not_logged_in_title),
            style = RhTypography.sectionTitle,
            color = RhTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.profile_not_logged_in_subtitle),
            style = RhTypography.body,
            color = RhTheme.colors.textTertiary,
        )
        Spacer(Modifier.height(24.dp))
        RhPrimaryButton(
            text = stringResource(Res.string.profile_not_logged_in_action),
            onClick = onGoToLogin,
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
