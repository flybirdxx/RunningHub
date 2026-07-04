# 批 3:广场 + 创作者主页 UI 重设计实施计划(mockup 定案)

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development。逐任务实现,每任务规范审查 + 质量审查双 gate。步骤用 `- [ ]`。

**Goal:** 广场页(Plaza,底栏「灵感」tab)与创作者主页(CreatorProfile)从旧 Material/旧扩展主题迁到封板 Rh 设计系统;**作品卡重做为整图叠加式**(封面按原始宽高比铺满 + 底部 scrim,叠标题/作者头像昵称/点赞 + 右上「用同款」橄榄胶囊),**点作者跳创作者主页**;创作者主页头部 Rh 化、关注按钮 Rh 化、「发布的应用」网格**复用批 2 叠加 AppCard**;PlazaScreen 1069 行按治理阈值拆分。

**已确认决策(本轮):**
1. 社区页(CommunityScreen,导航不可达死页)**本批不动**,登记债务。
2. 广场死搜索图标(`onClick = {}`)**隐藏**,灵感搜索登记债务(Domain 无作品关键词搜索能力,需服务端+Domain+Data 支持,超纯 UI 边界)。
3. 作品卡作者(头像/昵称)**可点跳创作者主页**——需 Domain `PlazaCreationCard` 补 `ownerId` + Data mapper 映射 `owner.id`(唯一超纯 UI 层改动,量极小)。

**Architecture:** UI 在 composeApp;Plaza 状态在 `feature:community:presentation`(PlazaStateHolder),Creator 状态在 `feature:auth:presentation`(CreatorProfileStateHolder);均无 Compose 依赖。改动顺序保每 commit 绿:Domain/Data 先行 → presentation 暴露字段 → designsystem 组件 → UI 消费方。

**Tech Stack:** Compose Multiplatform 1.7.3 + Voyager + Koin;kotlin.test + kotlinx-coroutines-test;`.\gradlew.bat --console=plain <task>`。

---

## 全局强制约束(每个任务)

1. **禁用实验性 Compose 布局 API**(FlowRow/FlowColumn/@ExperimentalLayoutApi)——运行期崩溃。换行用 RhWrapRow。`LazyVerticalStaggeredGrid` 用的 `@OptIn(ExperimentalFoundationApi)` 是 Foundation 实验注解(非布局签名漂移问题),保留可接受。
2. **颜色只从 `RhTheme.colors`**;`spacing/typography/shapes` 用 `RhSpacing/RhTypography/RhTheme.shapes`。禁止新增 `MaterialTheme.colorScheme.*`、`ExtendedColors`/`RunningHubThemeExt`、`ui/theme` 旧色别名(`RhAppCard/RhAppMuted/BrandLime/StatusError` 等)、`Color(0xFF...)`。**唯一例外**:媒体封面上的文字保护叠加(scrim 压暗渐变 `Color.Black.copy`、其上 `Color.White` 前景、半透黑徽标底),沿批 1/2 先例保留 + 中文注释,不迁 RhColors。
3. 文案走 Compose Resources;designsystem 组件文案由调用方传入。
4. KDoc 全角标点。
5. **commit path-limited**(`git add <具体文件>`);工作区有用户既有构建脚本改动,禁止 `git add -A/./-u`,禁止回滚用户改动。
6. presentation 模块不得导入 Compose/Ktor/Koin/Data。
7. Windows:`.\gradlew.bat --console=plain <task>`。

## 涉及文件总览

**Domain/Data:**
- `feature/community/domain/.../Plaza.kt`(PlazaCreationCard 加 `ownerId`)
- `feature/community/data/.../remote/dto/PlazaMappers.kt`(映射 `owner.id`)+ 对应契约测试

**Presentation:**
- `feature/community/presentation/.../PlazaReusePresentation.kt`(PlazaWorkCardUiModel 加 `ownerId`/`ownerAvatar`/`likeCount`/`aspectRatio`)+ 测试
- (Creator apps 无需改 presentation——`CreatorProfileUiState.apps: List<WebApp>` 直接复用 discovery 的 `WebApp.toDiscoveryAppCardUiModel()`)

**designsystem:**
- `composeApp/.../designsystem/components/cards/PlazaWorkCard.kt`(重做叠加式)+ 契约测试

**composeApp UI:**
- `composeApp/.../ui/feature/plaza/PlazaScreen.kt`(重皮 + 拆分 + 隐藏死搜索 + 作者点击 + 短片 token 重皮)
- `composeApp/.../ui/feature/plaza/PlazaReuseUiAdapters.kt`(适配新卡 state)
- 新建拆分文件:`PlazaHeaderSection.kt`、`PlazaShortTile.kt`(短片卡)
- `composeApp/.../ui/feature/creator/CreatorProfileScreen.kt`(重皮 + 复用 AppCard + 关注按钮 Rh)
- strings(如需新键,单一 `values/strings.xml`,本项目无 `values-zh`)

**不碰:** 社区页(CommunityScreen/Model/StateHolder,死页,债务)、短片播放/图片预览 overlay 逻辑(仅 token 重皮)、用户工作区改动。

---

### Task 1:Domain + Data 补 `ownerId`(TDD)

**Files:** `feature/community/domain/.../Plaza.kt`;`feature/community/data/.../remote/dto/PlazaMappers.kt`;Data 契约测试(`feature/community/data/src/commonTest/.../PlazaMappersTest.kt` 或同目录既有测试)

- [ ] **Step 1:写失败测试**——在 Plaza mapper 测试里断言:给定 DTO `owner = PlazaOwnerDto(id = "u-1", name = "墨白", avatar = "…")`,映射出的 `PlazaCreationCard.ownerId == "u-1"`(沿用测试文件既有 DTO 构造与 MockEngine/映射调用方式)。
- [ ] **Step 2:确认失败** `.\gradlew.bat --console=plain :feature:community:data:testDebugUnitTest --tests "*PlazaMappers*"`(或该模块实际测试任务名;先 `tasks` 查)→ `unresolved reference: ownerId` 或断言失败。
- [ ] **Step 3:实现**——`PlazaCreationCard` 加 `val ownerId: String? = null`(放在 `ownerName` 前,补 KDoc `@property ownerId 作者稳定 ID，用于跳转创作者主页；匿名或缺失时为空。`);mapper 里 `ownerId = dto.owner?.id`。
- [ ] **Step 4:全测** `:feature:community:data:testDebugUnitTest` + `:feature:community:domain:` 相关 → 绿。
- [ ] **Step 5:Commit**
```
git add feature/community/domain/src/commonMain/kotlin/com/runninghub/feature/community/domain/Plaza.kt feature/community/data/src/commonMain/kotlin/com/runninghub/feature/community/data/remote/dto/PlazaMappers.kt feature/community/data/src/commonTest/kotlin/com/runninghub/feature/community/data/remote/dto/PlazaMappersTest.kt
git commit -m "feat(community): map plaza creation owner id for creator profile navigation"
```
(测试文件路径以实际为准,保持 path-limited。)

---

### Task 2:Presentation 暴露作品卡新字段(TDD)

**Files:** `feature/community/presentation/.../PlazaReusePresentation.kt`;测试 `feature/community/presentation/src/commonTest/.../*.kt`

`PlazaWorkCardUiModel` 现有 `source: PlazaCreationCard`(已内嵌全部 domain 字段)、id/title/authorName/preview/useCount/reuseSummary/…。为叠加卡新增显式字段(便于 UI 直取 + 测试):

- [ ] **Step 1:写失败测试**——断言由 `PlazaCreationCard(ownerId="u-1", ownerAvatar="a", likeCount="1.2k", imageWidth=800, imageHeight=1200, …)` 派生的 `PlazaWorkCardUiModel`:`ownerId=="u-1"`、`ownerAvatar=="a"`、`likeCount=="1.2k"`、`aspectRatio` ≈ `800/1200`(0.666…)且被 clamp 到合理区间;缺 width/height 时 `aspectRatio == null`。
- [ ] **Step 2:确认失败**(该模块测试任务:`:feature:community:presentation:testDebugUnitTest` 或需 host test builder;先查 `tasks --all | grep community:presentation`,若无 JVM 测试任务,按批 1/2 债务 7 方式临时加 `withHostTestBuilder {}` 到 `feature/community/presentation/build.gradle.kts`,**该文件不提交**)。
- [ ] **Step 3:实现**——`PlazaWorkCardUiModel` 加 `ownerId: String?`、`ownerAvatar: String?`、`likeCount: String?`、`aspectRatio: Float?`;派生处(找到构造 PlazaWorkCardUiModel 的映射函数)填充:`ownerId = source.ownerId`、`ownerAvatar = source.ownerAvatar`、`likeCount = source.likeCount`、`aspectRatio = plazaCardAspectRatio(source.imageWidth, source.imageHeight)`。新增纯函数 `plazaCardAspectRatio(w: Int?, h: Int?): Float?`:两者非空且 >0 时返回 `(w/h).coerceIn(0.6f, 1.4f)`,否则 null(瀑布流防极端宽高比)。
- [ ] **Step 4:全测**该模块 → 绿。
- [ ] **Step 5:Commit**(不含 build.gradle.kts)
```
git add feature/community/presentation/src/commonMain/kotlin/com/runninghub/feature/community/presentation/PlazaReusePresentation.kt feature/community/presentation/src/commonTest/kotlin/com/runninghub/feature/community/presentation/<test>.kt
git commit -m "feat(community-presentation): expose owner id/avatar, like count, clamped aspect ratio for plaza work card"
```

---

### Task 3:PlazaWorkCard 重做为整图叠加式(designsystem)

**Files:** `composeApp/.../designsystem/components/cards/PlazaWorkCard.kt`(重写);契约测试(全仓 grep `PlazaWorkCardState(` 找测试引用点同步)

叠加卡结构(mockup 定案,参照批 2 AppCard 风格):`Surface(onClick, surfaceElevated, shape md)` → `Box(fillMaxWidth().aspectRatio(state.aspectRatio ?: 0.75f).clip(md))` → previewContent 铺满 → 底部 scrim 渐变(内容层,注释)→ 右上「用同款」橄榄胶囊(`brandPrimary` 底 + `textInverse` 字,点击 `onAction(UseSame)`)→ 底部 `Column(align BottomStart, padding)`:标题(白 2 行)+ 作者行 `Row`{ 圆形头像(`onAuthorClick` 可点,`ownerAvatar` 空时首字母/占位)+ 昵称(白,可点)+ `Spacer(weight 1f)` + 点赞(♥ icon + likeCount,白 82%) }。去掉旧「图下文」Column 与内联文字操作。

新 `PlazaWorkCardState`:
```kotlin
data class PlazaWorkCardState(
    val id: String,
    val title: String,
    val authorId: String?,          // 空则作者不可点
    val authorName: String?,
    val authorAvatar: String?,
    val likeCountLabel: String?,    // 已格式化,空则不展示点赞
    val aspectRatio: Float?,        // 空则用默认 0.75f
    val preview: PlazaWorkCardPreviewState,
    val useSameLabel: String,       // 「用同款」文案,调用方传入
    val enabled: Boolean = true,
)
```
组件签名:`PlazaWorkCard(state, onClick, onUseSame, onAuthorClick: (String) -> Unit, modifier, previewContent)`。`onAuthorClick` 仅在 `authorId != null` 时于头像/昵称触发。移除 `PlazaWorkCardAction` 枚举与 `PlazaWorkCardMetricState`(若无其他引用,先 grep)。

- [ ] **Step 1:重写组件 + state**。头像圆形 24dp,scrim/白字加注释。
- [ ] **Step 2:同步契约测试**——构造新 state 断言叠加槽位(title/author/like/aspect/useSameLabel)。
- [ ] **Step 3:编译**(此步 adapters/PlazaScreen 会暂时红,合并到 Task 4 一起编译;本任务与 Task 4 合并为一次 commit 保绿)。见 Task 4。

---

### Task 4:广场页重皮 + 拆分 + 作者跳转(合并 Task 3 提交)

**Files:** `PlazaScreen.kt`(重写,1069 → 主文件目标 ≤300 行)、`PlazaReuseUiAdapters.kt`(适配新卡)、新建 `PlazaHeaderSection.kt`、`PlazaShortTile.kt`;strings(如需)

- [ ] **Step 1:适配 `PlazaReuseUiAdapters.kt`**——`toPlazaWorkCardState()` 映射新字段:`authorId = ownerId`、`authorName`、`authorAvatar = ownerAvatar`、`likeCountLabel = likeCount?.takeIf{...}`、`aspectRatio`、`useSameLabel = stringResource(plaza_use_same_action)`;去掉旧 metric/actionLabel 逻辑。占位 `PlazaWorkPreviewPlaceholder` 的 `RhCard`/`RhMuted` 旧别名换 `RhTheme.colors.surfaceSunken`/`textTertiary`。
- [ ] **Step 2:拆 `PlazaHeaderSection.kt`**——头部(标题 + 刷新;**移除死搜索 IconButton**)、灵感/短片切换换 **RhSegmentedControl**、排序下拉 token 重皮(容器 `surfaceElevated`、选中 `brandPrimary`,参照 discovery 排序写法)、标签行换 **RhChip**(选中态)。旧 `MaterialTheme.typography` → `RhTypography`,旧色别名 → RhTheme token。
- [ ] **Step 3:拆 `PlazaShortTile.kt`**——短片 16:9 卡从 PlazaScreen 迁出,仅 token 重皮(scrim/播放钮/时长为媒体叠加,保留 + 注释);硬编码 `Color(0xFF1E293B/111827/1F2937)` 占位换 `surfaceSunken`。
- [ ] **Step 4:重写 `PlazaScreen.kt`**——保留 Voyager 入口 + 内容骨架 + 三个 overlay(图片/视频/复用面板逻辑不动,仅去 blur 外硬编码色);瀑布流 `LazyVerticalStaggeredGrid` 渲染新 `PlazaWorkCard`,接 `onAuthorClick = { ownerId -> navigator.push(CreatorProfileScreen(ownerId)) }`(import `com.runninghub.app.ui.feature.creator.CreatorProfileScreen`);三态换 **RhStates** 并补错误重试(`RhErrorState(onAction = 重新加载)`);把内容逻辑纯函数(排序选项/分页判定/宽高比/预览项)迁到文件尾部或保留。旧色/旧排版全清零。
- [ ] **Step 5:旧色自检**——grep `composeApp/.../ui/feature/plaza/` 内 `MaterialTheme.colorScheme`、`MaterialTheme.typography`、`ExtendedColors`、`RunningHubThemeExt`、`RhApp`、`BrandLime`、`StatusError`、`Color(0xFF` → 0 匹配(媒体叠加 `Color.Black/White` 除外,须注释)。
- [ ] **Step 6:编译 + 单测** `.\gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` → 绿。
- [ ] **Step 7:Commit(合并 Task 3 组件 + 契约测试)**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/cards/PlazaWorkCard.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/<PlazaWorkCard 契约测试>.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/ composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(plaza): redesign work card as overlay tile with author link + like, reskin/split plaza screen, hide dead search"
```

---

### Task 5:创作者主页重皮 + 复用叠加 AppCard

**Files:** `composeApp/.../ui/feature/creator/CreatorProfileScreen.kt`(重写);strings(如需)

- [ ] **Step 1:重写页面**
  - 顶栏 Material3 `TopAppBar` → **RhTopBar**(返回 + 昵称标题)。
  - 头部渐变:`RunningHubThemeExt` gradient → `RhTheme.colors` 组合(如 `brandMuted`→`surfaceElevated` 竖向渐变,或用 `backgroundGradientBrush`);头像圆形 + `bg` 边;右侧关注按钮:关注 = **RhPrimaryButton**,已关注 = 次级/描边态(以 designsystem 现有按钮变体为准;无次级按钮则用 `RhTheme.colors.borderDefault` 描边 + `surfaceElevated` 底 + `textSecondary` 字的 token 化 OutlinedButton)。
  - 昵称/简介/统计(粉丝/关注/获赞):`MaterialTheme.typography` → `RhTypography`,`colorScheme` → RhTheme token,`Dimens` → RhSpacing。
  - **「发布的应用」网格复用批 2 叠加卡**:`apps: List<WebApp>` → `apps.map { it.toDiscoveryAppCardUiModel() }`(import `com.runninghub.feature.discovery.presentation.toDiscoveryAppCardUiModel`,composeApp 已依赖 discovery presentation)→ 渲染 `DiscoveryAppCard(card = it, onClick = { navigator.push(AppDetailScreen(it.id)) })`(import `com.runninghub.app.ui.feature.discovery.DiscoveryAppCard`,同模块 internal 可用)。删自绘 `AppGridItem`。
  - 三态:旧 `LoadingIndicator`/`ErrorState` → **RhLoadingState/RhErrorState**(retry = 重新加载);空态 → **RhEmptyState**。
- [ ] **Step 2:旧色自检**——grep 该文件 `MaterialTheme.colorScheme`、`MaterialTheme.typography`、`RunningHubThemeExt`、`Dimens`、`LoadingIndicator`、`ErrorState`、`Color(0xFF` → 0 匹配。
- [ ] **Step 3:编译 + 单测** → 绿。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/creator/CreatorProfileScreen.kt composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(creator): reskin profile with Rh tokens, RhPrimaryButton follow, reuse overlay AppCard grid"
```

---

### Task 6:批次聚合验证 + 截图验收 + 封板

- [ ] **Step 1:聚合 Gradle**
```
.\gradlew.bat --console=plain checkArchitectureBoundaries
.\gradlew.bat --console=plain :composeApp:testDebugUnitTest :feature:community:data:testDebugUnitTest
# community/presentation host test(若已加 withHostTestBuilder)::feature:community:presentation:testAndroidHostTest
.\gradlew.bat --console=plain :composeApp:assembleDebug
```
- [ ] **Step 2:模拟器截图**(adb 于 `D:\Program Files\Android\SDK\platform-tools\adb.exe`;emulator `-avd Pixel_10_Pro -no-snapshot-load` 冷启;截图用 `MSYS_NO_PATHCONV=1` + `screencap -p /sdcard/x.png`→`pull`→`rm`;debug 包 `com.runninghub.app.debug/com.runninghub.app.MainActivity`;底栏第 3 tab「灵感」= 广场,tap 约 (540,2210)):
  需截:①广场灵感创作瀑布流(叠加作品卡:作者+点赞+用同款)②广场短片 tab ③创作者主页(从应用详情点作者进,或广场点作者进)。**重点看**:瀑布流不同宽高比卡对齐、作者头像/昵称可点区域、叠加白字可读、创作者应用网格双列叠加卡。
- [ ] **Step 3:crash 检查** `adb ... logcat -d -b crash` → 空。
- [ ] **Step 4:用户看图验收** → 通过后 spec `docs/superpowers/specs/2026-07-03-ui-redesign-design.md` 加 §11 批 3 封板 + 债务(社区死页、灵感搜索、短片卡是否入 designsystem、community/presentation host test 未提交等),commit spec。

---

## 自审记录

- **Spec 覆盖**:批 3 = 广场 + 创作者主页(§6 批 3 的两页;社区页经用户裁定本批不动,登记债务)。作品卡叠加化 + 作者跳转 + 用同款胶囊 + 创作者复用 AppCard + 关注按钮 Rh 五项定案均有任务。
- **编译绿顺序**:Domain/Data(T1)→ presentation 只增字段(T2)→ designsystem 卡 + UI 消费合并提交(T3+T4 一次 commit)→ 创作者页(T5)。T1/T2 独立可先绿。
- **超纯 UI 层改动仅 1 处**:T1 的 `ownerId`(domain+data+mapper 测试),为作者跳转所必需,已单独成任务并 TDD。
- **复用最大化**:创作者应用网格零新组件(WebApp → 批 2 `toDiscoveryAppCardUiModel` → DiscoveryAppCard);作品卡因语义不同(原始宽高比 + 作者主体 + 点赞 + 用同款)重做,不复用 AppCard。
- **债务预登记**:社区死页不动;广场灵感搜索(需 Domain/Data/服务端);短片卡仅 token 重皮未入 designsystem;community/presentation 若加 host test builder 随用户 AGP 迁移不提交。
- **已知需现场核对(非占位符)**:community data/presentation 实际测试任务名与测试文件路径;`PlazaWorkCardMetricState`/`PlazaWorkCardAction` 是否真无其他引用(grep 再删);designsystem 是否有次级/描边按钮组件供关注「已关注」态;`CreatorProfileScreen` 构造参数签名(userId)与 AppDetailScreen 跳转点一致性。