package com.runninghub.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.designsystem.components.navigation.AppBottomBar
import com.runninghub.app.ui.designsystem.components.navigation.AppBottomBarItemState
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.feature.discovery.DiscoveryVoyagerScreen
import com.runninghub.app.ui.feature.history.TaskHistoryVoyagerScreen
import com.runninghub.app.ui.feature.plaza.PlazaVoyagerScreen
import com.runninghub.app.ui.feature.profile.ProfileVoyagerScreen
import com.runninghub.app.ui.feature.quickcreate.QuickCreateVoyagerScreen
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.feature.community.domain.PlazaReuseMediaKind
import com.runninghub.feature.community.presentation.PlazaWorkDetailUiModel
import com.runninghub.feature.community.presentation.PlazaWorkPreviewType
import com.runninghub.feature.auth.domain.GetLastKnownBalanceUseCase
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreatePlazaReuseIntent
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreatePlazaReuseMediaKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.main_navigation_credit_icon
import runninghub.composeapp.generated.resources.main_navigation_guest_mode_dismiss
import runninghub.composeapp.generated.resources.main_navigation_guest_mode_message
import runninghub.composeapp.generated.resources.main_navigation_tab_discovery
import runninghub.composeapp.generated.resources.main_navigation_tab_history
import runninghub.composeapp.generated.resources.main_navigation_tab_profile
import runninghub.composeapp.generated.resources.main_navigation_tab_quick_create
import runninghub.composeapp.generated.resources.main_navigation_tab_studio

/**
 * 主导航可切换的一级 Tab。
 *
 * 每个枚举值对应一个长期存在的 Voyager Screen 实例，但只有当前选中的 Tab 会进入
 * Composition。这样可以保留明确的状态所有权，同时避免不可见页面继续执行轮询、上传或自动刷新。
 */
enum class BottomNavTab(
    internal val role: MainNavigationRole,
    internal val labelResource: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    QuickCreate(
        MainNavigationRole.Creation,
        Res.string.main_navigation_tab_quick_create,
        Icons.Filled.Star,
        Icons.Outlined.Star,
    ),
    Discovery(
        MainNavigationRole.Discovery,
        Res.string.main_navigation_tab_discovery,
        Icons.Filled.Explore,
        Icons.Outlined.Explore,
    ),
    Studio(
        MainNavigationRole.Inspiration,
        Res.string.main_navigation_tab_studio,
        Icons.Filled.Build,
        Icons.Outlined.Build,
    ),
    History(
        MainNavigationRole.Tasks,
        Res.string.main_navigation_tab_history,
        Icons.Filled.History,
        Icons.Outlined.History,
    ),
    Profile(
        MainNavigationRole.Account,
        Res.string.main_navigation_tab_profile,
        Icons.Filled.Person,
        Icons.Outlined.Person,
    ),
}

/**
 * 一级导航的产品职责定义。
 *
 * 该定义只描述用户理解导航所需的中文职责，不改变底层 Voyager Screen 映射。
 */
internal enum class MainNavigationRole(
    val label: String,
    val description: String,
) {
    Creation("创作", "默认主路径入口"),
    Discovery("发现", "模板、应用、模型入口"),
    Inspiration("灵感", "社区作品和使用同款"),
    Tasks("任务", "运行状态、历史、结果和账单"),
    Account("账户", "钱包、会员和设置"),
}

/**
 * 主导航默认入口和展示顺序。
 *
 * Roadmap RM-03 要求在不重命名 enum、不移动 Screen 注册关系的前提下，把用户可见职责调整为
 * “创作 / 发现 / 灵感 / 任务 / 账户”。页面渲染和测试都应通过该对象读取顺序，避免各处自行遍历 entries。
 */
internal object MainNavigationDefaults {
    val defaultTab: BottomNavTab = BottomNavTab.QuickCreate
    val orderedTabs: List<BottomNavTab> = listOf(
        BottomNavTab.QuickCreate,
        BottomNavTab.Discovery,
        BottomNavTab.Studio,
        BottomNavTab.History,
        BottomNavTab.Profile,
    )
}

/**
 * 返回主导航 Tab 的用户可见标签。
 *
 * 标签同时用于底部导航、宽屏侧边栏和图标无障碍描述，因此统一从 Compose Resources
 * 读取，避免不同导航形态出现文案漂移。
 */
@Composable
private fun BottomNavTab.displayLabel(): String = stringResource(labelResource)

/**
 * 持有主导航一级 Tab 对应的 Voyager Screen 实例。
 *
 * 该类型位于 Presentation 导航壳层，只负责把稳定的 Tab 选择映射到稳定的 Screen 实例：
 * - 普通重组不会重新创建 Screen，避免同一 Tab 反复初始化 ScreenModel。
 * - `screenFor` 每次只返回当前 Tab 的 Screen，由 `TabContent` 决定唯一进入 Composition 的页面。
 * - 创作入口固定为 [QuickCreateVoyagerScreen]，旧 Create 页面不会从主导航进入生产状态机。
 */
internal class MainTabScreenRegistry(
    private val discoveryScreen: Screen = DiscoveryVoyagerScreen(),
    private val quickCreateScreen: Screen = QuickCreateVoyagerScreen(),
    private val plazaScreen: Screen = PlazaVoyagerScreen(),
    private val historyScreen: Screen = TaskHistoryVoyagerScreen(),
    private val profileScreen: Screen = ProfileVoyagerScreen(),
) {
    /**
     * 返回指定一级 Tab 当前应组合的唯一 Screen。
     *
     * 调用方只应组合返回值，不应遍历全部 Screen；这样不可见 Tab 会离开 Composition，
     * 其页面协程、轮询和上传任务才能按 Voyager ScreenModel 生命周期释放。
     */
    internal fun screenFor(tab: BottomNavTab): Screen = when (tab) {
        BottomNavTab.Discovery -> discoveryScreen
        BottomNavTab.QuickCreate -> quickCreateScreen
        BottomNavTab.Studio -> plazaScreen
        BottomNavTab.History -> historyScreen
        BottomNavTab.Profile -> profileScreen
    }
}

/**
 * 应用主导航容器。
 *
 * 该 Screen 只负责 Tab 选择和首页壳层展示；余额角标通过 [GetLastKnownBalanceUseCase]
 * 读取弱缓存，不直接依赖 storage 或 DataStore 边界。
 */
private fun PlazaWorkDetailUiModel.toQuickCreatePlazaReuseIntent(): QuickCreatePlazaReuseIntent {
    val snapshot = source.reuseSnapshot
    return QuickCreatePlazaReuseIntent(
        sourceWorkId = id,
        sourceAuthorName = authorName,
        templateId = snapshot.templateId,
        skuId = snapshot.skuId,
        prompt = snapshot.prompt,
        aspectRatio = snapshot.aspectRatio,
        resolution = snapshot.resolution,
        quantity = snapshot.quantity,
        referenceMediaUrl = snapshot.referenceMediaUrl,
        referenceMediaKind = toQuickCreatePlazaReuseMediaKind(),
    )
}

private fun PlazaWorkDetailUiModel.toQuickCreatePlazaReuseMediaKind(): QuickCreatePlazaReuseMediaKind? =
    when (source.reuseSnapshot.referenceMediaType) {
        PlazaReuseMediaKind.IMAGE -> QuickCreatePlazaReuseMediaKind.IMAGE
        PlazaReuseMediaKind.VIDEO -> QuickCreatePlazaReuseMediaKind.VIDEO
        PlazaReuseMediaKind.AUDIO -> QuickCreatePlazaReuseMediaKind.AUDIO
        PlazaReuseMediaKind.UNKNOWN -> when (preview.type) {
            PlazaWorkPreviewType.Image -> QuickCreatePlazaReuseMediaKind.IMAGE
            PlazaWorkPreviewType.Video -> QuickCreatePlazaReuseMediaKind.VIDEO
            PlazaWorkPreviewType.Placeholder -> null
        }
    }

class MainVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        var selectedTab by rememberSaveable { mutableStateOf(MainNavigationDefaults.defaultTab) }
        var pendingPlazaReuseIntent by remember { mutableStateOf<QuickCreatePlazaReuseIntent?>(null) }
        val screenRegistry = remember {
            MainTabScreenRegistry(
                quickCreateScreen = QuickCreateVoyagerScreen(
                    consumePlazaReuseIntent = {
                        pendingPlazaReuseIntent.also {
                            pendingPlazaReuseIntent = null
                        }
                    },
                ),
                plazaScreen = PlazaVoyagerScreen(
                    onUseSameWork = { detail ->
                        pendingPlazaReuseIntent = detail.toQuickCreatePlazaReuseIntent()
                        selectedTab = BottomNavTab.QuickCreate
                    },
                ),
            )
        }
        val getLastKnownBalance = koinInject<GetLastKnownBalanceUseCase>()
        var creditCoins by rememberSaveable { mutableStateOf("--") }

        LaunchedEffect(Unit) {
            // 主导航只展示最近一次余额快照；实时余额仍由 Profile/账户接口负责刷新。
            creditCoins = getLastKnownBalance() ?: "--"
        }

        val sizeClass = rememberWindowSizeClass()
        val density = LocalDensity.current
        // 软键盘会覆盖窄屏底部 TabBar；隐藏后 Scaffold 不再为不可见底栏保留内容占位。
        val imeVisible = WindowInsets.ime.getBottom(density) > 0

        // 访客模式横幅目前默认不展示；保留可关闭结构，后续接入游客态时不需要再改导航壳布局。
        var showGuestBanner by rememberSaveable { mutableStateOf(false) }
        if (showGuestBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.main_navigation_guest_mode_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(Res.string.main_navigation_guest_mode_dismiss),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clickable { showGuestBanner = false }
                        .padding(4.dp)
                )
            }
        }

        if (sizeClass.isWide) {
            // 宽屏使用侧边导航，避免底部 Tab 在横向空间充足时占用内容高度。
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    MainNavigationDefaults.orderedTabs.forEach { tab ->
                        val tabLabel = tab.displayLabel()
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tabLabel,
                                )
                            },
                            label = { Text(tabLabel) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = RhTheme.colors.brandPrimary,
                                selectedTextColor = RhTheme.colors.brandPrimary,
                                unselectedIconColor = RhTheme.colors.textTertiary,
                                unselectedTextColor = RhTheme.colors.textTertiary,
                            ),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TabContent(selectedTab, screenRegistry)
                    CreditIndicator(
                        coins = creditCoins,
                        onClick = { selectedTab = BottomNavTab.Profile },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                }
            }
        } else {
            // 窄屏保持底部 Tab，匹配手机端单手切换的主要操作路径。
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (!imeVisible) {
                        val orderedTabs = MainNavigationDefaults.orderedTabs
                        AppBottomBar(
                            items = orderedTabs.map { tab ->
                                val tabLabel = tab.displayLabel()
                                AppBottomBarItemState(
                                    label = tabLabel,
                                    contentDescription = tabLabel,
                                    selectedIcon = tab.selectedIcon,
                                    unselectedIcon = tab.unselectedIcon,
                                    selected = selectedTab == tab,
                                )
                            },
                            onItemSelected = { index -> selectedTab = orderedTabs[index] },
                        )
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    TabContent(selectedTab, screenRegistry)
                }
            }
        }
    }

    @Composable
    private fun TabContent(tab: BottomNavTab, screenRegistry: MainTabScreenRegistry) {
        val saveableStateHolder = rememberSaveableStateHolder()

        Box(Modifier.fillMaxSize()) {
            saveableStateHolder.SaveableStateProvider(tab.name) {
                // AC-10：只组合当前 Tab。不可见页面离开 Composition 后，页面 LaunchedEffect、
                // Voyager ScreenModel scope 和轮询/上传 Job 会随生命周期释放；返回时只恢复
                // rememberSaveable 能表达的滚动、输入等轻量 UI 状态，长生命周期业务状态由草稿或仓库恢复。
                screenRegistry.screenFor(tab).Content()
            }
        }
    }
}

@Composable
private fun CreditIndicator(
    coins: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = modifier.clickable(onClick = onClick),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.main_navigation_credit_icon),
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = coins,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
