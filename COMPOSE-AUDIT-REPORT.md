# Jetpack Compose Audit Report

Target: `F:\Program Files\RunningHub`
Date: 2026-04-25
Scope: `app/src/main/kotlin/com/runninghub/app/` (single `app` module — ui, data, di layers)
Excluded from scoring: test sources, androidTest sources
Confidence: High
Overall Score: **53/100**

## Scorecard

| Category | Score | Weight | Status | Notes |
|----------|-------|--------|--------|-------|
| Performance | 4/10 | 35% | needs work | No R8, no stability hygiene, uncached transforms in composition, lazy lists missing keys |
| State management | 6/10 | 25% | needs work | Good ViewModel/StateFlow pattern, but `collectAsState()` vs lifecycle-aware, zero `rememberSaveable` |
| Side effects | 7/10 | 20% | solid | Mostly correct effect usage; one backwards write, minor stale-capture risk |
| Composable API quality | 5/10 | 20% | needs work | Majority of reusable components missing `modifier`, zero `@Preview`, pervasive hardcoded values |

## Critical Findings

1. **Performance: `isMinifyEnabled = false` in release build — no R8 optimization**
   - Why it matters: Compose performance assumes release-mode R8. Without it, runtime is unoptimized, dead code is not removed, and benchmarks are meaningless. This is the single largest performance gap in the project.
   - Evidence: `app/build.gradle.kts:27` — `isMinifyEnabled = false`
   - Fix direction: Set `isMinifyEnabled = true` and `isShrinkResources = true` in the `release` block; verify ProGuard rules.
   - References: <https://developer.android.com/develop/ui/compose/performance>

2. **Performance: No stability annotations, no immutable collections, no compiler config — all data-class params are unstable under SSM-off**
   - Why it matters: Kotlin 1.9.23 means Strong Skipping is OFF. Every `data class` without `@Stable`/`@Immutable` or every raw `List` parameter blocks skipping for any composable that takes it. This affects `AiApp`, `Banner`, `Category`, `AiTool`, `Creator`, `WebAppDetailDto`, `InputNodeDto`, and every UI state model.
   - Evidence: `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryUiState.kt:9-62` — six plain `data class` models, all carrying `List<>` fields. Zero `@Stable`/`@Immutable` annotations in the entire codebase. No `kotlinx.collections.immutable` dependency.
   - Fix direction: (a) Upgrade to Kotlin 2.0+ to get Strong Skipping by default, or (b) annotate key UI models with `@Immutable`, switch collection params to `ImmutableList`, and add a `compose_compiler_config.conf` for third-party types.
   - References: <https://developer.android.com/develop/ui/compose/performance/stability>, <https://developer.android.com/develop/ui/compose/performance/stability/fix>

3. **State: `collectAsState()` used instead of `collectAsStateWithLifecycle()` on three screens**
   - Why it matters: `collectAsState()` keeps collecting when the UI is in the background, wasting resources and potentially processing stale events. `collectAsStateWithLifecycle()` pauses collection when the lifecycle is below `STARTED`.
   - Evidence: `AppDetailScreen.kt:55`, `CreatorProfileScreen.kt:48`, `ProfileScreen.kt:45`
   - Fix direction: Replace all three with `collectAsStateWithLifecycle()` (already imported and used elsewhere).
   - References: <https://developer.android.com/develop/ui/compose/state>

4. **API Quality: Zero `@Preview` annotations in the entire codebase**
   - Why it matters: Previews prove components are self-contained and render correctly without a running app. Their complete absence means every UI change requires a full app build to verify.
   - Evidence: Global search for `@Preview` returned zero results across 53 source files.
   - Fix direction: Add `@Preview` to key reusable components (`SmartAsyncImage`, `AiAppMasonryItem`, `FabMenuOverlay`, `RunActionButton`) and representative screen states.
   - References: <https://developer.android.com/develop/ui/compose/tooling/previews>

## Category Details

### Performance — 4/10

**Ceiling check**

- Strong Skipping: OFF (Kotlin 1.9.23 / Compose Compiler Extension 1.5.11; no explicit opt-in found)
- Ceiling table applied: SSM-off
- Module-wide `skippable%`: n/a — compiler reports not generated (no Gradle wrapper or build attempted on Windows host)
- Named-only `skippable%`: n/a
- Unstable shared types from compiler: n/a — inferred from source: at least 8+ plain data classes used as composable params
- Qualitative score: 4/10
- Ceiling: cap at 7 (no compiler reports available)
- Applied score: 4/10 (qualitative below ceiling, no adjustment needed)

**What is working**

- `derivedStateOf` used correctly for scroll-triggered load-more detection in `ProfileScreen.kt:54-61` and `CreatorProfileScreen.kt:55-62`
- `snapshotFlow` used appropriately inside `LaunchedEffect(listState)` for debounced scroll detection in `DiscoveryScreen.kt:224-238`
- `animateFloatAsState` with meaningful `label = "fab_rotation"` in `DiscoveryScreen.kt:648-651`
- Lambda modifier `graphicsLayer { rotationZ = rotation }` defers per-frame read to draw phase in `DiscoveryScreen.kt:657`
- `enableEdgeToEdge()` in `MainActivity.kt:30` (first-party, not accompanist)
- `remember(imageUrl)` and `remember(cleanUrl)` for caching URL-derived values in `SmartAsyncImage.kt:48-49`
- Stable `key =` on main discovery feed items: `DiscoveryScreen.kt:206`

**What is hurting the score**

- `isMinifyEnabled = false` in release — no R8 optimization at all
- No baseline profiles, no `ProfileInstaller` dependency
- Zero `@Stable`/`@Immutable` annotations, zero `kotlinx.collections.immutable` usage, no `compose_compiler_config.conf` — under SSM-off, every data-class parameter is unstable and blocks skipping
- `uiState.discoveryApps.chunked(2)` called inside the composable body (not cached in `remember`) — allocates a new nested list on every recomposition
- Multiple lazy lists missing `key` parameter where item identity matters

**Evidence**

- `app/build.gradle.kts:27` — `isMinifyEnabled = false` in release block · References: <https://developer.android.com/develop/ui/compose/performance>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:203` — `val chunkedApps = uiState.discoveryApps.chunked(2)` inside composable body, no `remember` wrapper · References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:441` — `items(categories)` in `CategoryPills` LazyRow without `key` · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/profile/TaskHistoryScreen.kt:60` — `items(history)` in LazyColumn without `key` · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/creator/CreatorProfileScreen.kt:177` — `items(uiState.webAppList)` in staggered grid without `key` · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/profile/ProfileScreen.kt:169` — `items(creatorUiState.webAppList)` in staggered grid without `key` · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/community/CommunityScreen.kt:107` — `items(tools)` in grid without `key` · References: <https://developer.android.com/develop/ui/compose/lists>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryUiState.kt:9-62` — all model classes (`Banner`, `AiApp`, `Category`, etc.) are plain `data class` with `List<>` fields, no stability annotations · References: <https://developer.android.com/develop/ui/compose/performance/stability>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:586` — `listOf(Triple(...), ...)` allocated inside composable body on every recomposition · References: <https://developer.android.com/develop/ui/compose/performance/stability>
- `app/src/main/kotlin/com/runninghub/app/ui/component/FabMenuOverlay.kt:79` — `listOf(Triple(...), ...)` allocated inside composable body on every recomposition · References: <https://developer.android.com/develop/ui/compose/performance/stability>

### State Management — 6/10

**What is working**

- ViewModel-based architecture with `MutableStateFlow` + `.asStateFlow()` for proper encapsulation across all ViewModels (`DiscoveryViewModel`, `AppDetailViewModel`, `ProfileViewModel`, `CreatorProfileViewModel`, `HistoryViewModel`, `AudioGenerationViewModel`, `SecretDecodeViewModel`)
- `MutableStateFlow.update { }` used consistently for atomic state mutations
- Clear `data class` UiState models as single source of truth for each screen
- `collectAsStateWithLifecycle()` correctly used in `AppNavigation.kt:50,106`, `DiscoveryScreen.kt:102`, `AudioGenerationScreen.kt:37`, `SecretDecodeScreen.kt:60`
- State hoisted properly in `DiscoveryScreen` (receives UiState + callbacks from caller in nav graph)
- Navigation 2.7.7 uses string-based routes (appropriate — type-safe routes require 2.8+)

**What is hurting the score**

- `collectAsState()` instead of `collectAsStateWithLifecycle()` on three screens — keeps collecting in background
- Zero `rememberSaveable` usage across the entire codebase — all transient UI state (`appIdInput`, `isMenuExpanded`, dialog visibility, `pendingMediaNode`, etc.) is lost on configuration change or process death
- `hiltViewModel()` invoked inside `DiscoveryScreen` for `ProfileViewModel` at line 97 — ViewModel obtained deep in the composable tree rather than at the screen entry point in the nav graph

**Evidence**

- `app/src/main/kotlin/com/runninghub/app/ui/feature/detail/AppDetailScreen.kt:55` — `viewModel.uiState.collectAsState()` instead of lifecycle-aware · References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/creator/CreatorProfileScreen.kt:48` — `viewModel.uiState.collectAsState()` · References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/profile/ProfileScreen.kt:45` — `creatorViewModel.uiState.collectAsState()` · References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/search/SearchScreen.kt:25` — `var appIdInput by remember { mutableStateOf("") }` — lost on rotation, should be `rememberSaveable` · References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:97` — `profileViewModel: ProfileViewModel = hiltViewModel()` invoked deep in `DiscoveryScreen` composable · References: <https://developer.android.com/develop/ui/compose/architecture>

### Side Effects — 7/10

**What is working**

- `LaunchedEffect(appId)` correctly keyed to app ID for detail fetch in `AppDetailScreen.kt:57`
- `LaunchedEffect(userId)` correctly keyed for creator profile load in `CreatorProfileScreen.kt:50`
- `DisposableEffect(Unit) { onDispose { exoPlayer.release() } }` — proper ExoPlayer cleanup in `VideoPlayer.kt:44-48`
- `LaunchedEffect(videoUrl)` for media setup in `VideoPlayer.kt:38` — re-prepares on URL change
- `LaunchedEffect(listState)` with `snapshotFlow` for scroll detection in `DiscoveryScreen.kt:224` — collected inside effect, correct pattern
- `LaunchedEffect(shouldLoadMore)` for pagination trigger in `CreatorProfileScreen.kt:64` and `ProfileScreen.kt:63`
- `LaunchedEffect(uiState.user?.id)` properly keyed to user identity in `ProfileScreen.kt:47`
- `DisposableEffect(file)` with `onDispose { mediaPlayer?.release() }` in `SecretDecodeScreen.kt:423` — proper cleanup

**What is hurting the score**

- Backwards write in composable body: `isLoadSuccess = true` set directly in `MediaInputNode` composition at `AppDetailScreen.kt:656` — writes to a state variable during composition rather than from an effect or callback
- `LaunchedEffect(Unit)` in `BannerSection` at `DiscoveryScreen.kt:347` captures `banners.size` and `pagerState.currentPage`. If `banners` changes, the effect does not restart since the key is `Unit`. The risk is low (pager state handles index clamping), but `rememberUpdatedState` or keying on `banners.size` would be more correct.

**Evidence**

- `app/src/main/kotlin/com/runninghub/app/ui/feature/detail/AppDetailScreen.kt:656` — `isLoadSuccess = true` written directly in composition body (inside `else` branch of `if (node.fieldType == "IMAGE")`) · References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:347-352` — `LaunchedEffect(Unit)` auto-play banner captures `banners.size` without `rememberUpdatedState` · References: <https://developer.android.com/develop/ui/compose/side-effects>

### Composable API Quality — 5/10

**What is working**

- `SmartAsyncImage` exposes `modifier: Modifier = Modifier` as the first optional parameter with a no-op default — correct pattern (`SmartAsyncImage.kt:41`)
- `VideoPlayer` exposes `modifier: Modifier = Modifier` correctly (`VideoPlayer.kt:22`)
- `BaseInputCard` exposes `modifier: Modifier = Modifier` correctly (`AppDetailScreen.kt:527`)
- Theme system in place with `MaterialTheme` color scheme tokens routed through `DarkColorScheme` (`Theme.kt`)
- Slot API used for `BaseInputCard` — `content: @Composable ColumnScope.() -> Unit` (`AppDetailScreen.kt:528`)

**What is hurting the score**

- Majority of shared/reusable composables are missing the `modifier` parameter entirely
- Zero `@Preview` annotations across the entire codebase
- Pervasive hardcoded colors: explicit `Color(0xFFXXXXXX)` constructs instead of `MaterialTheme.colorScheme` tokens
- Pervasive hardcoded `dp`/`sp` values instead of dimension resources or theme typography
- Hardcoded Chinese strings everywhere instead of `stringResource(R.string.…)` — blocks i18n
- Several screens take `NavHostController` directly as a parameter instead of navigation event callbacks — tight coupling to navigation implementation

**Evidence**

- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:462` — `AiAppMasonryItem(app: AiApp, modifier: Modifier, onClick: () -> Unit)` — `modifier` is required (no default), violating the `Modifier = Modifier` convention · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:294,343,434,567` — `DiscoveryHeader`, `BannerSection`, `CategoryPills`, `DiscoveryBottomNav` all missing `modifier` parameter · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/detail/AppDetailScreen.kt:464,514,574` — `RunActionButton`, `StatItem`, `MediaInputNode` missing `modifier` parameter · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/creator/CreatorProfileScreen.kt:205,323` — `CreatorHeader`, `StatLabelValue` missing `modifier` parameter · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/community/CommunityScreen.kt:131` — `ToolCard` missing `modifier` parameter · References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:553` — `Surface(color = Color(0xFFFFCC00), ...)` hardcoded color · References: <https://developer.android.com/develop/ui/compose/designsystems/material3>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:402` — `Text(banner.title, ... fontSize = 24.sp, fontWeight = FontWeight.Bold)` hardcoded typography instead of `MaterialTheme.typography` · References: <https://developer.android.com/develop/ui/compose/designsystems/material3>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryScreen.kt:191,198,256,260` — Hardcoded Chinese strings (`"正在寻找优质作品..."`, `"暂无作品"`, `"没有更多作品了"`) instead of `stringResource` · References: <https://developer.android.com/develop/ui/compose/resources>
- `app/src/main/kotlin/com/runninghub/app/ui/feature/search/SearchScreen.kt:24` — `fun SearchScreen(navController: NavHostController)` takes `NavHostController` directly instead of navigation callbacks · References: <https://developer.android.com/develop/ui/compose/navigation>

## Prioritized Fixes

1. **Enable R8 and shrink resources in release build** — Set `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts:27`. This is the single highest-leverage performance change. Add ProGuard rules for Retrofit, Room, and Hilt. References: <https://developer.android.com/develop/ui/compose/performance>

2. **Replace `collectAsState()` with `collectAsStateWithLifecycle()` on three screens** — `AppDetailScreen.kt:55`, `CreatorProfileScreen.kt:48`, `ProfileScreen.kt:45`. This prevents background collection of flows when the UI is not visible. The dependency (`lifecycle-runtime-compose:2.7.0`) is already present. References: <https://developer.android.com/develop/ui/compose/state>

3. **Add stable `key =` to all lazy list `items()` calls** — `CategoryPills` at `DiscoveryScreen.kt:441` (`key = { it.tagIds.hashCode() }`), `TaskHistoryScreen.kt:60` (`key = { it.taskId }`), `CreatorProfileScreen.kt:177` and `ProfileScreen.kt:169` (`key = { it.id }`), `CommunityScreen.kt:107` (`key = { it.title }`). Enables correct item reuse on reorder/update and is prerequisite for `animateItem()`. References: <https://developer.android.com/develop/ui/compose/lists>

4. **Cache `chunked(2)` with `remember`** — In `DiscoveryScreen.kt:203`, wrap `uiState.discoveryApps.chunked(2)` in `remember(uiState.discoveryApps) { uiState.discoveryApps.chunked(2) }` to avoid reallocating the nested list on every recomposition. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>

5. **Fix backwards write in `MediaInputNode`** — In `AppDetailScreen.kt:656`, move `isLoadSuccess = true` into a `SideEffect` or an `onSuccess` callback instead of setting it directly in the composition body. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>

6. **Add `modifier: Modifier = Modifier` to all shared composables** — At least 12 reusable composables are missing the `modifier` parameter. Priority targets: `AiAppMasonryItem`, `DiscoveryHeader`, `BannerSection`, `CategoryPills`, `DiscoveryBottomNav`, `RunActionButton`, `CreatorHeader`, `ToolCard`, `StatItem`. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>

7. **Add `@Preview` to key components** — Start with `SmartAsyncImage`, `AiAppMasonryItem`, `FabMenuOverlay`, `RunActionButton`, and one representative screen state per feature. References: <https://developer.android.com/develop/ui/compose/tooling/previews>

8. **Introduce `rememberSaveable` for UI state that should survive configuration changes** — `SearchScreen.kt:25` (`appIdInput`), dialog visibility states in `ProfileSettingsDialog.kt:37-38`, `pendingMediaNode` in `AppDetailScreen.kt:136`. References: <https://developer.android.com/develop/ui/compose/state>

## Notes And Limits

- Single `app` module audited; no multi-module structure.
- Confidence: High — all Compose source files were read and representative patterns verified.
- Strong Skipping mode: OFF (Kotlin 1.9.23 / Compose Compiler Extension 1.5.11; no explicit opt-in found). This means stability annotations and immutable collections matter significantly.
- Weight choice: default 35/25/20/20.
- Renormalization: none — all four categories scored.
- Compiler diagnostics used: no — the audit ran on a Windows host without Gradle wrapper execution. All stability claims are inferred from source, not measured against compiler reports. Performance score capped at 7 per policy, but qualitative score (4) was below the cap.
- Navigation Compose version 2.7.7 — string-based routes are appropriate; type-safe `@Serializable` routes require 2.8+.

## Suggested Follow-Up

- Run `material-3` audit — the codebase shows pervasive hardcoded colors and typography bypassing `MaterialTheme` tokens, suggesting potential design-system consistency issues worth auditing.
- Upgrade Kotlin to 2.0+ to get Strong Skipping by default and the Compose Compiler Gradle plugin, which simplifies compiler-report generation.
- Set up a baseline-profile module for startup and scroll-heavy paths once R8 is enabled.
