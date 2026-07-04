# 批 2:发现页 + 搜索页 UI 重设计实施计划(v2 · mockup v2 定案)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 发现页与搜索页从旧 Material 暗色 scheme 全量迁移到封板 Rh 设计系统;应用卡片重设计为**整图叠加瓦片**(封面铺满 + 底部 scrim,标题/能力/精选/真实指标叠在图层上,砍掉假费用与内联文字操作);搜索**保持功能分开**(应用搜索与灵感搜索不合并),仅把搜索框抽成公共 `RhSearchBar`;搜索结果与发现页统一用新叠加卡;DiscoveryScreen 748 行按治理阈值拆分。

**mockup v2 定案变更(相对 v1):**
1. 卡片从"复用现有 AppCard(图下黑色信息块)"→ **整图叠加瓦片**(3:4 竖版,无黑块)。
2. 砍掉卡片假费用 `≈X 积分`(`estimatedCost` 恒为 UNKNOWN,无费用协议),改展真实 `useCount`/`pv`/`likeCount`。
3. 砍掉卡片右下"生成/详情"内联文字操作,整卡可点。
4. 搜索**不统一、不删内联**:发现页保留现有内联应用搜索(仅重皮),独立 SearchScreen 也保留重皮;两者都是应用搜索,不合并、不删除;灵感搜索(广场/社区)属批 3,本批不碰。RhSearchBar 为共享 UI 组件,检索逻辑各自保留。

**Architecture:** UI 渲染在 composeApp(应用壳),页面状态在 `feature:discovery:presentation`(DiscoveryStateHolder / SearchStateHolder,无 Compose 依赖)。改动顺序保证每个 commit 编译绿:新组件先行 → presentation 卡片模型只调不删语义 → UI 消费方重写 → 最后退役旧组件。

**Tech Stack:** Compose Multiplatform 1.7.3 + Voyager + Koin;测试 kotlin.test + kotlinx-coroutines-test;验证 `.\gradlew.bat --console=plain <task>`。

---

## 全局强制约束(每个任务都必须遵守)

1. **禁用实验性 Compose 布局 API**(`FlowRow`/`FlowColumn`/`@ExperimentalLayoutApi`)——批 0 封板规约,违反会运行期 `NoSuchMethodError` 崩溃。换行布局用 `RhWrapRow`。
2. **颜色只从 `RhTheme.colors` 读取**;`spacing`/`typography`/`shapes` 引用 `RhSpacing`/`RhTypography`/`RhTheme.shapes` 合规。禁止新增 `MaterialTheme.colorScheme.*`、`Color(0xFF...)`、`ExtendedColors`。**唯一例外:媒体封面上的文字保护叠加**——叠加卡与 banner 的压暗渐变 `Color.Black.copy(alpha=…)`、其上 `Color.White` 前景、半透明黑徽标底,属内容层语义(保证任意封面上白字可读),沿批 1 Hero 封板先例保留并加中文注释说明,不迁 RhColors。
3. **文案全部走 Compose Resources**(`composeApp/src/commonMain/composeResources/values*/strings.xml`),designsystem 组件文案由调用方传入。
4. **KDoc 用全角标点**(项目注释规范 `docs/governance/chinese-commenting.md`)。
5. **commit 必须 path-limited**(`git add <具体文件>`),工作区有用户既有构建脚本改动(build-logic、libs.versions.toml、skills/ 等),禁止 `git add -A`/`git add .`,禁止回滚用户改动。
6. presentation 模块(`feature/discovery/presentation`)不得导入 Compose/Ktor/Koin/Data。
7. Windows 本地命令用 `.\gradlew.bat --console=plain <task>`。

## 涉及文件总览

**新建:**
- `composeApp/.../ui/designsystem/components/inputs/RhSearchBar.kt`
- `composeApp/.../ui/feature/discovery/DiscoveryBannerSection.kt`
- `composeApp/.../ui/feature/discovery/DiscoveryCategorySort.kt`
- (发现页顶栏若拆分)`composeApp/.../ui/feature/discovery/DiscoveryTopBar.kt`

**重写/修改:**
- `composeApp/.../ui/designsystem/components/cards/AppCard.kt`(重设计为叠加卡,改 `AppCardState` API)
- `composeApp/src/commonTest/.../RhAppCardComponentsContractTest.kt`(同步新 API)
- `feature/discovery/presentation/.../DiscoveryAppCardPresentation.kt`(卡片 UiModel:加 `featured`、`metrics` 列表,移除假 `estimatedCost`)
- `feature/discovery/presentation/src/commonTest/.../*`(卡片映射契约测试)
- `feature/discovery/presentation/.../SearchStateHolder.kt`(`SearchUiState` 加 `resultCards` 派生)
- `composeApp/.../ui/feature/discovery/DiscoveryAppCards.kt`(包装层适配新卡 + 第 70 行旧色)
- `composeApp/.../ui/feature/discovery/DiscoveryScreen.kt`(重皮 + 拆文件,**保留内联搜索**)
- `composeApp/.../ui/feature/search/SearchScreen.kt`(重皮:RhTopBar/RhSearchBar/RhChip/叠加卡/RhStates)
- `composeApp/src/commonMain/composeResources/values/strings.xml` + `values-zh/strings.xml`(键两文件同步)

**删除(退役,确认零引用后):**
- `composeApp/.../ui/component/AppSearchBar.kt`(旧搜索框,被 RhSearchBar 取代)
- `composeApp/.../ui/component/AppCard.kt`(旧作者卡,搜索结果改用叠加卡后无引用)

**不碰:** `feature/discovery/domain`、`feature/discovery/data`、core 模块、广场/社区(灵感搜索属批 3)、用户工作区既有改动。

---

### Task 1:RhSearchBar 公共搜索框组件

**Files:** Create `composeApp/.../ui/designsystem/components/inputs/RhSearchBar.kt`

- [ ] **Step 1:创建组件**——参照旧 `AppSearchBar`(`composeApp/.../ui/component/AppSearchBar.kt`)行为契约,颜色全走 RhTheme,文案由调用方传入:

```kotlin
package com.runninghub.app.ui.designsystem.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhTheme

/** RhSearchBar 的固定尺寸契约。 */
object RhSearchBarDefaults {
    val minHeight = 44.dp
}

/**
 * 设计系统搜索输入框。下沉表面色承载输入区，聚焦时边框切换为激活色。
 *
 * 该组件只提供统一外观与交互；应用搜索、灵感搜索等不同检索逻辑由各自调用方持有，
 * 组件本身不感知检索目标，保证多处复用而功能互不耦合。
 *
 * @param query 当前搜索关键词，空字符串表示尚未输入。
 * @param onQueryChange 用户编辑关键词时触发，调用方负责保存状态。
 * @param placeholder 占位文案，调用方负责本地化。
 * @param modifier 外层布局修饰符，默认填满父容器宽度。
 * @param onSearch 用户通过键盘搜索动作提交时触发，参数为当前关键词。
 * @param searchIconContentDescription 搜索图标无障碍描述，调用方负责本地化。
 * @param clearContentDescription 清空按钮无障碍描述，调用方负责本地化。
 */
@Composable
fun RhSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    searchIconContentDescription: String? = null,
    clearContentDescription: String? = null,
) {
    val colors = RhTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = RhSearchBarDefaults.minHeight),
        placeholder = { Text(text = placeholder, color = colors.textTertiary) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, searchIconContentDescription, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, clearContentDescription, tint = colors.textTertiary)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            focusedBorderColor = colors.borderActive,
            unfocusedBorderColor = colors.borderDefault,
            cursorColor = colors.brandPrimary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
```

- [ ] **Step 2:编译** `.\gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid` → BUILD SUCCESSFUL
- [ ] **Step 3:Commit**
```powershell
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/inputs/RhSearchBar.kt
git commit -m "feat(designsystem): add RhSearchBar shared search input"
```

---

### Task 2:AppCard 重设计为整图叠加瓦片(设计系统组件)

**Files:**
- Rewrite `composeApp/.../ui/designsystem/components/cards/AppCard.kt`
- Modify `composeApp/src/commonTest/.../RhAppCardComponentsContractTest.kt`

叠加卡结构(mockup v2):`Box(aspectRatio 3:4)` 内 —— 封面铺满(`previewContent`)→ 底部压暗渐变 scrim → 左上能力标签(半透黑底 pill)→ 右上精选星(仅 `featured=true`,橄榄底)→ 底部 `Column{ 标题(白,2 行省略) + 指标 Row(每项 图标+值,白 76%) }`。视频类型由 `previewContent` 叠中央播放键(沿用现有 VideoThumbnail 或调用方处理)。去掉旧的 `estimatedCostLabel`、`primaryAction` 与图下黑色 Column body。

- [ ] **Step 1:改 `AppCardState` API 与组件**

新数据契约(替换现有):
```kotlin
/** AppCard 结果预览媒体类型。 */
enum class AppCardPreviewType { Image, Video, Audio, Empty }

/** 叠加卡指标的语义图标类型，组件据此选择前导图标，保持 designsystem 不依赖业务枚举。 */
enum class AppCardMetricIcon { Use, Like, View, Collect }

/**
 * 叠加卡的单条辅助指标。
 * @property icon 指标语义图标。
 * @property value 已格式化的展示值，保持调用方格式。
 */
data class AppCardMetricState(val icon: AppCardMetricIcon, val value: String)

/**
 * AppCard 结果预览状态。
 * @property url 可展示预览地址；为空时展示占位。
 * @property type 预览资源类型。
 */
data class AppCardPreviewState(val url: String?, val type: AppCardPreviewType)

/**
 * 整图叠加式创作入口卡片状态。
 * @property id WebApp ID 或卡片稳定 ID。
 * @property title 模板名。
 * @property capabilityLabel 调用方已本地化的能力类型（图像/视频/音频等）。
 * @property preview 封面预览。
 * @property metrics 真实辅助指标（0..2 条，例如使用数、点赞数），按展示优先级排序。
 * @property featured 是否运营精选，为 true 时右上展示精选标记。
 */
data class AppCardState(
    val id: String,
    val title: String,
    val capabilityLabel: String,
    val preview: AppCardPreviewState,
    val metrics: List<AppCardMetricState>,
    val featured: Boolean = false,
)
```

组件签名保留 `previewContent` 插槽(封面由调用方渲染真实图/视频),去掉 `onAction`:
```kotlin
@Composable
fun AppCard(
    state: AppCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featuredLabel: String,                 // “精选”文案,调用方本地化传入
    previewContent: @Composable BoxScope.(AppCardPreviewState) -> Unit = { AppCardPreviewPlaceholder(it) },
)
```
渲染要点:
- 外层 `Surface(onClick, color = RhTheme.colors.surfaceElevated, shape = RoundedCornerShape(RhTheme.shapes.md))`,内 `Box.fillMaxWidth().aspectRatio(3f/4f).clip(md)`。
- 封面 `previewContent(state.preview)` 铺满。
- 压暗渐变(内容层语义,加注释):`Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)))`,起始约 42% 高度(用 `0f to Transparent, 0.42f to Transparent, 1f to black`)。
- 能力标签:`Modifier.align(TopStart).padding(RhSpacing.sm)`,半透黑底 pill(`Color.Black.copy(alpha=0.55f)`),`RhTypography.meta`,白字。
- 精选星:`if (state.featured)` `Modifier.align(TopEnd)`,橄榄底 `RhTheme.colors.brandPrimary` + `textInverse` 字 + `Icons.Rounded.Star`,文案 `featuredLabel`。
- 底部 `Column.align(BottomStart).padding(horizontal=md, vertical=sm)`:标题 `RhTypography.cardTitle` 白 2 行省略;指标 `Row(spacedBy sm)`,每项 `Icon(metricIcon)+Text(value)` 白 76%。metric 图标映射:Use→`Icons.Rounded.LocalFireDepartment`(或 `Whatshot`)、Like→`Icons.Rounded.FavoriteBorder`、View→`Icons.Rounded.Visibility`、Collect→`Icons.Rounded.BookmarkBorder`(以实际可用 Material 图标为准,不确定就近取)。
- 保留私有 `AppCardPreviewPlaceholder`:占位 `RhTheme.colors.surfaceSunken` 铺满。

- [ ] **Step 2:同步契约测试** `RhAppCardComponentsContractTest.kt` —— 按新 `AppCardState`(无 `estimatedCostLabel`/`primaryAction`,`metric`→`metrics` 列表,加 `featured`)重写断言:构造含 2 条 metrics + featured=true 的 state,断言 `title`/`capabilityLabel`/`preview.type`/`metrics.size`/`metrics[0].icon`/`featured`。

- [ ] **Step 3:编译**(此时 `DiscoveryAppCards.kt` 仍用旧 API 会失败,属预期,Task 3 修复;本步只单独编译测试模块确认组件与测试自洽,或直接连同 Task 3 一起编译)。稳妥起见把 Step 3 合并到 Task 3 之后统一编译。

- [ ] **Step 4:Commit**(与 Task 3 同批编译通过后再提,或本任务先提组件+测试,允许包装层暂时编译红——**为保证每 commit 绿,本任务与 Task 3 合并为一次 commit**):见 Task 3 Step 5。

---

### Task 3:发现页卡片 UiModel 优化 + 包装层适配(presentation TDD + composeApp)

**Files:**
- Modify `feature/discovery/presentation/.../DiscoveryAppCardPresentation.kt`
- Test `feature/discovery/presentation/src/commonTest/.../DiscoveryAppCardPresentationTest.kt`(若无则新建;沿用现有测试风格)
- Modify(不提交)`feature/discovery/presentation/build.gradle.kts`
- Modify `composeApp/.../ui/feature/discovery/DiscoveryAppCards.kt`

- [ ] **Step 0:启用本地 JVM 测试(不提交)**——`feature/discovery/presentation/build.gradle.kts` 的 `kotlin {}` 顶部加(与 `feature/detail/presentation/build.gradle.kts:7-10` 一致):
```kotlin
    // 启用 Android host test,让 commonTest 可在本地 JVM 执行(任务名 testAndroidHostTest)。
    androidLibrary { withHostTestBuilder {} }
```
**该文件不提交**——随用户 AGP 迁移一起提交(批 1 债务 7 同款)。

- [ ] **Step 1:写失败测试**——覆盖三点:①`featured` 来自 `WebApp.carefullyChosen`;②`metrics` 首条为 useCount(缺失时回退 pv),有 likeCount 时追加为第二条;③模型不再含 `estimatedCost`。参照 `core/model` 的 `WebApp` 实际构造(见 `WebApp.kt`)与现有测试写法。

- [ ] **Step 2:运行确认失败** `.\gradlew.bat --console=plain :feature:discovery:presentation:testAndroidHostTest --tests "*DiscoveryAppCardPresentation*"`

- [ ] **Step 3:改模型与映射**
  - `DiscoveryAppCardMetricKind` 加 `LIKE_COUNT`。
  - `DiscoveryAppCardUiModel`:删 `estimatedCost` 字段;`supportingMetric: DiscoveryAppCardMetricUi?` → `metrics: List<DiscoveryAppCardMetricUi>`;加 `featured: Boolean`;删 `primaryAction`(整卡可点,不再需要动作语义)——**先 grep 确认 `primaryAction`/`estimatedCost`/`DiscoveryAppEstimatedCost*` 无其他消费方**,有则一并处理。
  - `toDiscoveryAppCardUiModel()`:`featured = carefullyChosen`;`metrics = buildList { (useCount 或 pv) 作首条;likeCount 非空追加 }`。
  - 删除 `DiscoveryAppEstimatedCostKind`/`DiscoveryAppEstimatedCostUi`(若无其他引用)。

- [ ] **Step 4:改包装层 `DiscoveryAppCards.kt`** 适配新 `AppCard`:
  - `toAppCardState()`:`metrics = metrics.map { AppCardMetricState(icon = it.kind.toMetricIcon(), value = formatCount(it.value)) }`;`featured = featured`;删 `estimatedCostLabel`/`primaryAction`/`capabilityLabel` 保留。
  - 新增 `DiscoveryAppCardMetricKind.toMetricIcon()`:USE_COUNT→`AppCardMetricIcon.Use`、VIEW_COUNT→`View`、LIKE_COUNT→`Like`。
  - `DiscoveryAppCard()`:`AppCard(state, onClick, modifier, featuredLabel = stringResource(Res.string.discovery_featured_label), previewContent = { DiscoveryAppCardPreview(it) })`,删 `onAction`。
  - 删除已无用的 `toEstimatedCostLabel`/`toActionLabel`/`toDesignSystemActionType` 与对应 `discovery_cost_unknown`/`discovery_action_*` import(字符串键在 Task 6 统一清理)。
  - 第 70 行 `DiscoveryAppCardPreview` 占位已用 `RhTheme.colors.surfaceSunken`,无需改。

- [ ] **Step 5:新增文案键**——`values/strings.xml` + `values-zh/strings.xml` 同步加 `discovery_featured_label`(如“精选”)。

- [ ] **Step 6:编译 + 测试** `.\gradlew.bat --console=plain :feature:discovery:presentation:testAndroidHostTest :composeApp:compileDebugKotlinAndroid` → 全绿(此时叠加卡链路自洽)。

- [ ] **Step 7:Commit(合并 Task 2 组件 + 测试 + 本任务)**
```powershell
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/cards/AppCard.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhAppCardComponentsContractTest.kt feature/discovery/presentation/src/commonMain/kotlin/com/runninghub/feature/discovery/presentation/DiscoveryAppCardPresentation.kt feature/discovery/presentation/src/commonTest/kotlin/com/runninghub/feature/discovery/presentation/DiscoveryAppCardPresentationTest.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/discovery/DiscoveryAppCards.kt composeApp/src/commonMain/composeResources/values/strings.xml composeApp/src/commonMain/composeResources/values-zh/strings.xml
git commit -m "feat(discovery): redesign AppCard as full-bleed overlay tile with real metrics, drop fake cost"
```
(不含 `build.gradle.kts`。)

---

### Task 4:SearchUiState 增加统一卡片派生(presentation TDD)

**Files:** Modify `SearchStateHolder.kt` + Test `SearchStateHolderTest.kt`

- [ ] **Step 1:写失败测试**——`SearchUiState(results = listOf(app)).resultCards` 派生出 `DiscoveryAppCardUiModel`,断言 size/id/templateName。
- [ ] **Step 2:确认失败** `--tests "*SearchStateHolderTest*"`(`unresolved reference: resultCards`)。
- [ ] **Step 3:实现**——`SearchUiState` data class 体内加派生(复用同包 `toDiscoveryAppCardUiModel()`):
```kotlin
    /** 搜索结果的创作入口卡片语义，与发现页共用同一 UiModel，供应用壳直接渲染叠加卡。 */
    val resultCards: List<DiscoveryAppCardUiModel>
        get() = results.map { it.toDiscoveryAppCardUiModel() }
```
- [ ] **Step 4:全测** `.\gradlew.bat --console=plain :feature:discovery:presentation:testAndroidHostTest` → 全绿。
- [ ] **Step 5:Commit(不含 build.gradle.kts)**
```powershell
git add feature/discovery/presentation/src/commonMain/kotlin/com/runninghub/feature/discovery/presentation/SearchStateHolder.kt feature/discovery/presentation/src/commonTest/kotlin/com/runninghub/feature/discovery/presentation/SearchStateHolderTest.kt
git commit -m "feat(discovery-presentation): derive unified AppCard models from search results"
```

---

### Task 5:独立搜索页重皮(SearchScreen)

**Files:** Rewrite `composeApp/.../ui/feature/search/SearchScreen.kt`;Modify strings(如需)

结构(mockup v2,**功能不变只重皮**):RhTopBar(返回+标题“搜索应用”)→ RhSearchBar → 空关键词展示热门标签 RhChip 行 → 结果用统一叠加卡自适应网格(`GridCells.Adaptive(windowInfo.feedGridMinCardWidth)`,合并原 Compact/Medium 两套布局为一套)→ 三态走 RhStates(RhLoadingState/RhErrorState/RhEmptyState)。分页沿用现有基于可见项的 `derivedStateOf` 判断(比发现页正确写法,保留)。

要点:
- 结果卡片用 composeApp 内 `internal` 的 `DiscoveryAppCard(card = uiState.resultCards[i], onClick = { onAppClick(id) })`,跨包引用合法。
- 热词图标沿用火焰,颜色从 `extColors.hotBadge`(旧 ExtendedColors)→ `RhTheme.colors.statusWarning` 或 `brandSecondary`(以 mockup 紫色为准取 `brandSecondary`)。
- 删 `SearchScreen.kt:7,353` 多余 `@OptIn(ExperimentalLayoutApi::class)`(实际用 `horizontalScroll`)。
- 旧 `AppSearchBar`→`RhSearchBar`;旧 `AppCard`(作者卡)→ 叠加卡;`LoadingIndicator`/`ErrorState`→RhStates;`TopAppBar`→`RhTopBar`。
- `RhTypography`/`RhTheme.shapes` 字段名以定义文件为准(cardTitle/caption/meta;shapes.md/sm)。Preview 若无 RhTheme 提供,包 `RunningHubTheme` 或 `RhAdaptivePreview`。
- 若 `SearchScreenModel.clearSearch`/`onClearSearch` 无消费方,同步删除保持整洁(先 grep 确认)。

- [ ] **Step 1:重写 SearchScreen.kt**(参照旧 plan 的完整实现骨架 + 上述要点;结果卡改叠加卡)。
- [ ] **Step 2:新增/复用文案键**(`search_loading`/`search_error_retry` 等,两文件同步,已存在则复用)。
- [ ] **Step 3:编译 + 单测** `.\gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` → 绿。
- [ ] **Step 4:Commit**
```powershell
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/search/SearchScreen.kt composeApp/src/commonMain/composeResources/values/strings.xml composeApp/src/commonMain/composeResources/values-zh/strings.xml
git commit -m "feat(search): reskin app-search page with RhTopBar/RhSearchBar/RhChip and overlay result cards"
```

---

### Task 6:发现页重皮 + 拆文件(**保留内联搜索**)

**Files:**
- Create `DiscoveryBannerSection.kt`、`DiscoveryCategorySort.kt`(可选再拆 `DiscoveryTopBar.kt`)
- Rewrite `DiscoveryScreen.kt`(748 行 → 主文件目标 ≤300 行)
- Modify `DiscoveryScreenModel.kt`(仅在删了 SearchScreenModel 死方法时联动,否则不动)

**核心约束:发现页现有内联应用搜索行为保持不变,只重皮。** 不删 `DiscoveryStateHolder` 内联搜索状态、不把搜索改成跳独立页。内联搜索的输入框换 `RhSearchBar`,内联搜索结果换叠加卡(`DiscoveryAppCard`),其余交互原样。

- [ ] **Step 1:创建 `DiscoveryBannerSection.kt`**——从 DiscoveryScreen 迁出 banner 横滑区。表面色→`surfaceElevated`、圆角→`RhTheme.shapes.md`、字体→`RhTypography.cardTitle/caption`;封面压暗渐变+白字**原样保留**(内容层语义,加注释)。指标文案用真实 `useCount`(回退 `pv`),`formatCount` 同模块 internal 直接用。(可复用旧 plan 中该文件完整实现。)

- [ ] **Step 2:创建 `DiscoveryCategorySort.kt`**——分类行自绘胶囊→`RhChip`(选中态);排序行保留 `DropdownMenu` 换 Rh 色板(容器 `surfaceElevated`、选中项 `brandPrimary`);`catalogSortLabel` 映射迁入此文件。(可复用旧 plan 中该文件完整实现。)

- [ ] **Step 3:重写 `DiscoveryScreen.kt`**——保留:Voyager Screen + `DiscoveryContent`(Scaffold + `LazyVerticalGrid` 编排 banner/分类/排序/卡片流)+ **内联搜索分支(重皮)** + 错误映射。迁出 banner/分类/排序代码。顶栏 `DiscoveryTopBar`:logo + 搜索图标,点击**触发现有内联搜索展开**(不是 push 独立页)。列表:
  - 加载更多改基于可见项 `derivedStateOf`(替换 `LaunchedEffect(currentPage)` 反模式)。
  - 卡片 `DiscoveryAppCard(card = uiState.appCards[i], ...)`(叠加卡)。
  - 内联搜索态:搜索框 `RhSearchBar`,结果 `uiState.searchResultCards`(现有派生)渲染叠加卡。
  - `DiscoveryPreviews.kt` 若因签名变化编译失败,同步修正传参。
  - `MaterialTheme.colorScheme.*`(21 处)全部→`RhTheme.colors.*`;三态→RhStates;裸 `CircularProgressIndicator` 保留但 `color = brandPrimary`。

- [ ] **Step 4:旧色清零自检**——Grep `composeApp/.../ui/feature/discovery/` 与 `.../ui/feature/search/` 内 `MaterialTheme.colorScheme`、`ExtendedColors`、`RunningHubThemeExt` → 期望 0 匹配(banner/叠加卡的 `Color.Black/White` 媒体叠加除外,须带注释)。

- [ ] **Step 5:新增文案键**(`discovery_loading`/`discovery_error_retry` 等,已有则复用)。

- [ ] **Step 6:编译 + 单测** `.\gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` → 绿。

- [ ] **Step 7:Commit**
```powershell
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/discovery/ composeApp/src/commonMain/composeResources/values/strings.xml composeApp/src/commonMain/composeResources/values-zh/strings.xml
git commit -m "feat(discovery): reskin discovery page with Rh tokens + overlay cards, split 748-line screen, keep inline search"
```

---

### Task 7:旧组件与死文案退役

**Files:** Delete 旧 `AppSearchBar.kt`、旧 `AppCard.kt`;Modify strings

- [ ] **Step 1:确认零引用后删除**——Grep `AppSearchBar` 与 `ui.component.AppCard`(注意区分 designsystem 的 AppCard)于 `composeApp/src`,除定义外应 0 引用。`app_search_bar_*` 字符串键仍被 RhSearchBar 调用方使用则保留。有其他引用则停下报告,不强删。
- [ ] **Step 2:删死文案键**——Grep 确认每个键 0 引用后从两 strings 文件同步删:`discovery_cost_unknown`、`discovery_action_generate`、`discovery_action_view_detail`,以及旧 AppCard/内联搜索专属且已无引用的键(逐个 grep,仍被引用的保留)。
- [ ] **Step 3:编译 + 单测** → BUILD SUCCESSFUL。
- [ ] **Step 4:Commit**
```powershell
git add -u composeApp/src/commonMain/kotlin/com/runninghub/app/ui/component/ composeApp/src/commonMain/composeResources/
git commit -m "chore(ui): retire legacy AppSearchBar/AppCard and dead card strings"
```

---

### Task 8:批次聚合验证 + 截图验收 + 封板

- [ ] **Step 1:聚合 Gradle**
```powershell
.\gradlew.bat --console=plain checkArchitectureBoundaries
.\gradlew.bat --console=plain :composeApp:testDebugUnitTest :feature:discovery:presentation:testAndroidHostTest
.\gradlew.bat --console=plain :composeApp:assembleDebug
```
- [ ] **Step 2:模拟器/真机截图**(screencap→pull→rm,避免 PowerShell `>` 损坏二进制):
```powershell
adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app.debug/com.runninghub.app.MainActivity
adb -s emulator-5554 shell screencap -p /sdcard/b2.png; adb -s emulator-5554 pull /sdcard/b2.png <scratchpad>/; adb -s emulator-5554 shell rm /sdcard/b2.png
```
需截:①发现页(banner+分类+叠加卡流)②发现页内联搜索态 ③独立搜索页热词空态 ④搜索结果叠加卡网格。**重点看:封面缺失/浅色封面上叠加白字的兜底可读性**(叠加卡最大风险)。
- [ ] **Step 3:crash 检查** `adb -s emulator-5554 logcat -b crash -d` → 无本应用 crash。
- [ ] **Step 4:用户看图验收**——通过后在 spec `docs/superpowers/specs/2026-07-03-ui-redesign-design.md` 增批 2 封板记录(§10),登记债务,commit spec。

---

## 自审记录

- **Spec 覆盖**:批 2 = 发现 + 搜索两页迁移(§6);卡片叠加化 + 砍假费用 + 真实指标 + 搜索分离(不合并)四项 mockup v2 定案均有对应任务;31 处 `MaterialTheme.colorScheme` 由 Task 5/6 覆盖,`hotBadge` ExtendedColors 由 Task 5 覆盖,`ExperimentalLayoutApi` 死注解随 Task 5 重写消失。
- **编译绿顺序**:RhSearchBar(T1)→ 叠加卡组件+卡片模型+包装层合并提交(T2/T3 一次 commit,避免中间态编译红)→ SearchUiState 派生只增(T4)→ 搜索页重皮(T5)→ 发现页重皮保留内联搜索(T6)→ 退役旧组件(T7)。
- **搜索分离**:无任何搜索状态删除/合并;应用搜索(发现页内联 + 独立 SearchScreen)与灵感搜索(批 3)各自独立;RhSearchBar 仅统一外观。
- **叠加卡风险**:整图叠加要求封面质量;`previewContent` 缺失走 `surfaceSunken` 占位 + 顶部标题仍需可读——T8 截图重点验收浅色/缺失封面。
- **已知需现场核对(非占位符)**:Material 图标名(`LocalFireDepartment`/`Whatshot`/`Visibility`/`BookmarkBorder`/`Star` 具体可用性)、`WebApp` 测试构造参数、`primaryAction`/`estimatedCost` 是否真无其他消费方(grep)、`RhAdaptivePreview` 是否内置 RhTheme。
```
