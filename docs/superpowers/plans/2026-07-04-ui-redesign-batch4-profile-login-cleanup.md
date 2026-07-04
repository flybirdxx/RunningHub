# 批 4 收尾:我的 + 登录 + 拆旧 theme 双轨 实施计划(mockup 定案)

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development。逐任务实现,每任务规范 + 质量双 gate。步骤用 `- [ ]`。

**Goal:** 完成 UI 重设计最后一批——①我的页(Profile)Rh 重皮 + 最小接线;②登录页(Login)橄榄化;③删除社区死页;④**全拆旧 `ui/theme` 双轨**(页面直接取色 91 处 `colorScheme` + 56 处 `typography` 迁 RhTheme;QuickCreate 旧组件簇 7 文件 + 2 component + 2 媒体 overlay 的 `Primary300/Neutral*/ErrorDark/BrandLime` 等旧色迁 Rh;shimmer/premiumGold/gradient 的 ExtendedColors 消费迁 Rh;删 `Color.kt` 死值 / `ExtendedColors.kt` / `Dimens.kt` / `ProvideRhTheme` / `RhLightColors`;`MaterialTheme` 装配保留但 `DarkColorScheme` 值对齐 Rh 暗色);⑤全局硬编码色清零。

**已确认决策(本轮):**
1. 社区死页 `CommunityScreen`(导航不可达)**删除**(4 kt + 1 测试 + DI 两行;不动 Plaza 所在的 `feature:community` domain/data/presentation 里的 Plaza* 代码)。
2. 我的页**接最小线**:未登录态加「去登录」按钮跳登录页;设置齿轮接通已有 `ApiKey/Cookie` 弹窗(状态已在 presentation);编辑资料/清缓存/关于三菜单**无后端能力,视觉恢复 + 登记债务**(保持可点但暂 no-op 或隐藏,以现状 presentation 能力为准)。
3. 旧 theme **全拆含 QuickCreate 旧簇**。

**Architecture:** UI 在 composeApp;Profile/Login 状态在 `feature:auth:presentation`。**关键顺序:先把所有旧符号的消费方迁走(Tasks 1-6),旧符号变 0 引用后再删除(Task 7)**,保证每 commit 编译绿。

**Tech Stack:** Compose Multiplatform 1.7.3 + Voyager + Koin;`.\gradlew.bat --console=plain <task>`。

---

## 全局强制约束(每个任务)

1. **禁用实验性 Compose 布局 API**(FlowRow/FlowColumn)。换行用 RhWrapRow。
2. **颜色只从 `RhTheme.colors`**;`spacing/typography/shapes` 用 `RhSpacing/RhTypography/RhTheme.shapes`。**本批新增 token 需谨慎**——RhColors 批 0 已封板;优先复用现有语义 token,不新增槽位(shimmer 用 `surfaceSunken`/`surfaceElevated` 组合,premiumGold 用 `priceMoney`,gradient 用现有色组合)。媒体叠加 `Color.Black/White` 沿例保留 + 注释。
3. **`MaterialTheme` 装配保留**:`RunningHubTheme` 里的 `MaterialTheme(colorScheme = DarkColorScheme, ...)` 必须保留(Material3 组件 Scaffold/AlertDialog/DropdownMenu/CircularProgressIndicator/PullToRefreshBox 隐式消费);要清的是**页面代码里直接 `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*` 取值**,改为 RhTheme。`DarkColorScheme` 的值保持与 Rh 暗色一致。
4. 文案走 Compose Resources(单一 `values/strings.xml`,无 values-zh)。
5. KDoc 全角标点。
6. **commit path-limited**;工作区有用户既有构建脚本改动 + `feature/community/presentation/build.gradle.kts`(host-test,不提交);`git add <具体文件>` only,禁止 `-A/./-u`,禁止 `git reset`。
7. presentation 模块不得导入 Compose/Ktor/Koin/Data。
8. **QuickCreate 是批 0 封板页**:Task 5 改其旧色后,Task 8 必须重新截图 QuickCreate 确认无视觉回归。
9. Windows:`.\gradlew.bat --console=plain <task>`。

## 旧符号 → Rh 语义映射(Task 1-6 通用参照)

**须按每处的视觉意图选语义 token,不是盲替换。** 一般对应:

| 旧符号(`ui/theme/Color.kt`) | Rh 语义 token(按用途) |
|---|---|
| `Primary300` / `BrandLime` | 主强调/品牌 → `brandPrimary`;品牌底 → `brandMuted` |
| `Secondary500` | `brandSecondary` |
| `Neutral100/200`(暗底上的浅面) | 面 → `surfaceElevated`/`surfaceDefault`/`surfaceSunken`(按层级);分隔 → `borderDefault`/`borderSubtle` |
| `Neutral300/400/500`(文字) | 文字 → `textPrimary`/`textSecondary`/`textTertiary`(按层级) |
| `ErrorDark` | `statusFailed` |
| `SuccessDark` | `statusSuccess` |
| `WarningDark` | `statusWarning` |
| `DarkSurfaceVariant`/`DarkOutlineVariant` | `surfaceElevated`/`borderDefault` |
| `ExtendedColors.premiumGold` | `priceMoney`(金色语义) |
| `ExtendedColors.shimmerBase/shimmerHighlight` | `surfaceSunken`/`surfaceElevated` |
| `ExtendedColors.gradientStart/gradientEnd` | 随社区死页删除消失(唯一消费方) |
| `Dimens.*`(间距) | `RhSpacing.*`(xs4/sm8/md12/lg16/xl20/xxl24/xxxl32/huge40);圆角 `Dimens.RadiusLG` → `RhTheme.shapes.lg` |
| `MaterialTheme.colorScheme.*`(页面直接取) | 对应 `RhTheme.colors.*` |
| `MaterialTheme.typography.*`(页面直接取) | 对应 `RhTypography.*` |

不确定某处视觉意图时,读上下文 + 对比 `DarkColorScheme` 该槽的现值(它已映射 Rh),选最贴近的语义 token。

---

### Task 1:我的页(Profile)重皮 + 最小接线

**Files:** `composeApp/.../ui/feature/profile/ProfileScreen.kt`(593 行);strings(如需)

- [ ] **Step 1:重皮**——17 处 `MaterialTheme.colorScheme.*` → `RhTheme.colors.*`;6 处 `MaterialTheme.typography.*` → `RhTypography.*`;`Dimens.RadiusLG`(L422)→ `RhTheme.shapes.lg`;`RunningHubThemeExt.colors.premiumGold`(L310/319/332,MemberBadge)→ `RhTheme.colors.priceMoney`。头部渐变改 Rh 组合(如 `brandMuted → surfaceElevated`)。三态若有旧组件换 RhStates。
- [ ] **Step 2:最小接线**
  - 未登录态 `NotLoggedInContent`(L502-541)加「去登录」按钮(`RhPrimaryButton`),点击 `navigator.push(LoginVoyagerScreen())` 或走根 App 的登录 target(读 App.kt 现有登录导航方式,与之一致)。
  - 设置齿轮(L265 `clickable {}`)接通:调用 `screenModel` 已有的 show 设置弹窗方法(ProfileStateHolder 有对应状态),渲染 `SettingsDialog` 的 ApiKey/Cookie 弹窗(见 Task 1b,或本任务一并接);若接线复杂则本步只接「打开弹窗」,弹窗重皮在 Task 1b。
  - 编辑资料/清缓存/关于三菜单:保持可点视觉,onClick 暂留 no-op 但**登记债务**(无后端能力);或按 presentation 现有能力接线(读 ProfileStateHolder 确认有无 clearCache 等)。
- [ ] **Step 3:旧色自检**——grep 该文件 `MaterialTheme.colorScheme|MaterialTheme.typography|RunningHubThemeExt|Dimens\.` → 0。
- [ ] **Step 4:编译 + 单测** `:composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` → 绿。
- [ ] **Step 5:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/profile/ProfileScreen.kt composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(profile): reskin with Rh tokens, wire login CTA and settings entry, gold badge to priceMoney"
```

---

### Task 2:SettingsDialog 重皮 + 接线

**Files:** `composeApp/.../ui/feature/profile/SettingsDialog.kt`(171 行)

- [ ] **Step 1:重皮**——`ApiKeyDialog`/`CookieDialog` 两个 `AlertDialog`:4 处 `colorScheme` + 6 处 `typography` + 4 处 `Dimens` → Rh;输入框用 Rh 风格(参照 RhSearchBar 的 OutlinedTextField 配色或现有 Rh 输入约定);确认/取消按钮 RhPrimaryButton / RhButton Secondary。AlertDialog 容器色走 `RhTheme.colors.surfaceElevated`。
- [ ] **Step 2:确认接线闭环**——ProfileScreen 设置齿轮 → show 状态 → 渲染这两个弹窗 → 确认写 ApiKey/Cookie(presentation 已有逻辑)。若 Task 1 已接「打开」,本任务补齐渲染与提交回调。
- [ ] **Step 3:编译 + 单测** → 绿。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/profile/SettingsDialog.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/profile/ProfileScreen.kt
git commit -m "feat(profile): reskin ApiKey/Cookie settings dialogs to Rh and wire them to the gear entry"
```
(若 ProfileScreen 未再改则不 add。)

---

### Task 3:登录页(Login)橄榄化

**Files:** `composeApp/.../ui/feature/login/LoginScreen.kt`(457 行)、`SmsCaptchaDialog.kt`(65 行);strings(如需)

- [ ] **Step 1:重皮**——27 处 `colorScheme` + 6 处 `typography` → Rh;手机号/验证码/密码输入框改 Rh 下沉输入框风格(下沉底 `surfaceSunken` + 聚焦 `borderActive`,参照 RhSearchBar 配色);登录按钮 `RhPrimaryButton`;验证码「获取/倒计时」按钮用 `brandPrimary` 文字态;协议文案 `textTertiary`;品牌标 Rh 化。`SmsCaptchaDialog`(WebView 图形验证码弹窗)容器/按钮 token 化,WebView 本体逻辑不动。
- [ ] **Step 2:交互不变**——手机号 +86 前缀、验证码 60s 倒计时、密码模式切换、图形验证码前置弹窗、登录成功由 SessionManager 驱动根 App 切换,全部保持;只换视觉。
- [ ] **Step 3:旧色自检 + 编译 + 单测** → 绿。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/login/ composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(login): redesign login + sms captcha with Rh tokens and RhPrimaryButton"
```

---

### Task 4:删除社区死页

**Files:** 删 `composeApp/.../ui/feature/community/CommunityScreen.kt`、`CommunityScreenModel.kt`;删 `feature/community/presentation/.../CommunityStateHolder.kt` + `commonTest/.../CommunityStateHolderTest.kt`;改 `composeApp/.../di/AppModule.kt`

- [ ] **Step 1:确认零引用**——grep `CommunityVoyagerScreen`、`CommunityScreenModel`、`CommunityStateHolder` 全仓:除定义 + DI + 自身测试外应 0 导航引用。**特别确认**:`feature/community/presentation` 里 Plaza* 相关(PlazaStateHolder/PlazaReusePresentation 等)**不依赖** CommunityStateHolder(它们独立);若 CommunityStateHolder 被 Plaza 复用则停下报告,不删。
- [ ] **Step 2:删除**——删上述 4 个 kt + 1 测试;`AppModule.kt` 删 `factoryOf(::CommunityScreenModel)`(L64)+ 其 import(L5)。
- [ ] **Step 3:编译 + 单测** `:composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest :feature:community:presentation:testAndroidHostTest`(后者若启用)→ 绿。附带收益:`gradientStart/gradientEnd` 消费方消失、4 处工具图标硬编码色 + 15 处 Dimens 随文件删除。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/community/ composeApp/src/commonMain/kotlin/com/runninghub/app/di/AppModule.kt feature/community/presentation/src/commonMain/kotlin/com/runninghub/feature/community/presentation/CommunityStateHolder.kt feature/community/presentation/src/commonTest/kotlin/com/runninghub/feature/community/presentation/CommunityStateHolderTest.kt
git commit -m "chore(community): remove unreachable community tools dead page and its DI/state"
```
(用 `git add -u <dir>` 记录删除时仍须限定到具体目录,不用全仓 `-u`;或对每个删除文件显式 `git add <path>`。)

---

### Task 5:迁移 shimmer + QuickCreate 旧簇 + component/overlay 旧色(拆双轨前置)

**这是最大任务。** 目标:让 `ui/theme/Color.kt` 的存活符号(Primary300/Neutral*/ErrorDark/BrandLime/Secondary500/DarkSurfaceVariant 等)+ `ExtendedColors`(shimmerBase/highlight)+ `Dimens` 的**全部剩余消费方**迁到 Rh,迁完后这些旧符号 0 引用(供 Task 7 删除)。

**Files(按映射表逐文件迁移,读每处上下文选语义 token):**
- shimmer:`ui/component/ShimmerEffect.kt`、`ui/component/SmartAsyncImage.kt`(shimmerBase/highlight → surfaceSunken/surfaceElevated)
- QuickCreate 旧簇(Primary300/Neutral*/ErrorDark 等):`QuickCreateClassicComposerContent.kt`、`QuickCreateUploadFieldContent.kt`、`QuickCreateResultContent.kt`、`QuickCreateHistoryContent.kt`、`QuickCreateProjectContent.kt`、`MediaChipCard.kt`、`AdaptivePromptTextField.kt`
- component:`ui/component/CollapsibleSection.kt`、`ui/component/ImageUploadButton.kt`
- 媒体 overlay(BrandLime):`ImagePreviewOverlay.kt`、`VideoPreviewOverlay.kt`
- 各文件的 `Dimens.*` → `RhSpacing`/`RhTheme.shapes`
- 各文件残余 `MaterialTheme.colorScheme/typography`(TaskProgressIndicator 20 处 colorScheme、TaskDetailDrawer/TaskHistoryListSections 的 typography、PermissionBottomSheet、MainScreen、AppBarLogo、ErrorState、LoadingIndicator 等)→ Rh

- [ ] **Step 1:分文件迁移**——建议每 3-5 个文件一组编译一次确认绿,避免一次动太多难定位。每处按语义映射表选 token;媒体叠加色保留+注释。
- [ ] **Step 2:全量旧符号自检**——grep 全 composeApp `Primary300|Neutral[0-9]|ErrorDark|SuccessDark|WarningDark|BrandLime|Secondary500|DarkSurfaceVariant|DarkOutlineVariant|shimmerBase|shimmerHighlight|Dimens\.|RunningHubThemeExt|ExtendedColors` → 除 `ui/theme/*.kt` 定义文件与 token 测试外 **0 匹配**。页面直接 `MaterialTheme.colorScheme/typography` 也应清零(Material 组件内部消费不算)。
- [ ] **Step 3:编译 + 单测** → 绿。
- [ ] **Step 4:Commit**(可按文件组拆多个 commit,均 path-limited)
```
git add <本组迁移的具体文件...>
git commit -m "refactor(ui): migrate quickcreate cluster + shimmer + overlays off legacy theme colors to Rh tokens"
```

---

### Task 6:全局硬编码色清零

**Files:** `designsystem/components/badges/RhTaskStatusBadge.kt`(L78 `0xFF8B929D` → `textTertiary`)、`ImagePreviewOverlay.kt`(L384/386 若非媒体叠加则 token 化)、`VideoThumbnail.ios.kt`(L33 占位深蓝 → `surfaceSunken`,iosMain)

- [ ] **Step 1:清零**——RhTaskStatusBadge 的 Canceled 灰换 `RhTheme.colors.textTertiary`(designsystem 内漏网 token);ImagePreviewOverlay 两处按用途 token 化或加媒体叠加注释;VideoThumbnail.ios 占位换 token。**QuickCreateDesign.kt 的 9 处 Canvas 插画色保留**(空态星球/宇航员插画配色,非语义 UI 色,加注释说明为插画资产色)。
- [ ] **Step 2:自检**——grep `composeApp/src` `Color(0x`(排除 `ui/theme/*` 色板定义、`designsystem/theme/RhColors.kt`、`ExtendedColors.kt`、token 测试、已注释的媒体叠加/插画)→ 仅剩 QuickCreateDesign 插画色(带注释)。
- [ ] **Step 3:编译** → 绿。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/badges/RhTaskStatusBadge.kt <其余具体文件>
git commit -m "chore(ui): zero remaining hardcoded colors to Rh tokens (illustration colors kept + annotated)"
```

---

### Task 7:删除旧 theme 双轨符号

**前置:Task 1-6 已让旧符号 0 引用。** **Files:** `ui/theme/Color.kt`、`ExtendedColors.kt`、`Dimens.kt`、`Theme.kt`、`designsystem/theme/RhTheme.kt`、`designsystem/theme/RhColors.kt`、`WindowSizeClass.kt`;可能改 token 测试。

- [ ] **Step 1:删死代码**
  - `ExtendedColors.kt` 整文件删除;`Theme.kt` 移除 `LocalExtendedColors`/`DarkExtendedColors` 提供(保留 `LocalRhColors`/`LocalRhShapes` + `MaterialTheme(DarkColorScheme, AppTypography)`)。
  - `Dimens.kt` 整文件删除。
  - `designsystem/RhTheme.kt` 删死代码 `ProvideRhTheme`;`RhColors.kt` 删 `RhLightColors`(批 0 债务 1,连锁,一并删)。
  - `Color.kt`:删所有 0 引用的死值(Light* 全组、RhApp*/Surface*/Text*Dark/StatusError/ControlTeal*/BaseBlack、Primary/Secondary/Neutral 其余无引用色阶等)。**保留** `DarkColorScheme` 仍需要的槽位(background/surface/primary 等映射值)——把这些值就近内联进 `Theme.kt` 的 `DarkColorScheme` 定义或收敛为一个极小的 Color.kt;`DarkColorScheme` 各槽值保持与 Rh 暗色一致。
  - `Type.kt`/`AppTypography`:`MaterialTheme` 装配仍需 Typography;保留 `AppTypography`(或与 RhTypography 对齐),不删。
  - `WindowSizeClass.kt`:若 `adaptiveGridColumns/adaptiveAppBarHeight/isWide` 已无消费方则删这些函数;`WindowSizeClass`/`rememberWindowSizeClass`/`adaptiveGridSpacing` 若仍被 Discovery/Plaza 等消费则保留(**注意:后台任务可能正在改 Discovery/Plaza,保守起见 WindowSizeClass 若仍有消费方就保留,只删确证 0 引用的函数**)。
- [ ] **Step 2:token 测试**——`commonTest/.../theme/RunningHubThemeTokenTest.kt` 若断言了被删符号,同步更新(只删对已删符号的断言,保留其余)。
- [ ] **Step 3:全量自检 + 编译 + 单测**——grep 全仓确认被删符号 0 引用;`:composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` + `checkArchitectureBoundaries` → 绿。
- [ ] **Step 4:Commit**
```
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/ composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/theme/RhTheme.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/theme/RhColors.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/theme/RunningHubThemeTokenTest.kt
git commit -m "chore(theme): remove legacy dual-track (ExtendedColors/Dimens/dead Color values/ProvideRhTheme/RhLightColors)"
```

---

### Task 8:批次聚合验证 + 截图验收 + 封板

- [ ] **Step 1:聚合 Gradle**
```
.\gradlew.bat --console=plain checkArchitectureBoundaries
.\gradlew.bat --console=plain :composeApp:testDebugUnitTest
.\gradlew.bat --console=plain :composeApp:assembleDebug
```
- [ ] **Step 2:模拟器截图**(adb `D:\Program Files\Android\SDK\platform-tools\adb.exe`;emulator `-avd Pixel_10_Pro -no-snapshot-load` 冷启;`MSYS_NO_PATHCONV=1` + screencap→pull→rm;debug 包 `com.runninghub.app.debug`):
  需截:①我的页(登录态:头部/会员金徽/资产卡/菜单)②未登录态(去登录按钮)③设置弹窗(ApiKey/Cookie)④登录页 ⑤**QuickCreate 页(批 0 封板页,验证旧色迁移后无视觉回归)**。未登录态与登录态截图需相应会话状态(未登录可先退出登录)。
- [ ] **Step 3:crash 检查** `adb ... logcat -d -b crash` → FATAL 0。**重点点测**:切各 tab、进登录页、开设置弹窗、退出登录/去登录往返,确认拆双轨未引入渲染崩溃。
- [ ] **Step 4:用户看图验收** → 通过后 spec `docs/superpowers/specs/2026-07-03-ui-redesign-design.md` 加 §12 批 4 封板 + 债务 + **全 UI 重设计收官总结**,commit spec。

---

## 自审记录

- **Spec 覆盖**:批 4 = 我的 + 登录 + 设置(§6 批 4)+ 收尾治理(拆双轨/硬编码清零/删死页)。三决策(删社区死页/最小接线/全拆含 QuickCreate)均有任务。
- **编译绿顺序**:消费方先迁(T1 Profile、T2 Settings、T3 Login、T4 删社区页、T5 shimmer+QuickCreate 簇+overlay、T6 硬编码)→ 旧符号 0 引用后再删(T7)。每步编译+单测把关。
- **MaterialTheme 保留**:只清页面直接取色,装配层 `MaterialTheme(DarkColorScheme, AppTypography)` 保留供 M3 组件隐式消费,值对齐 Rh 暗色。
- **QuickCreate 回归防护**:T5 改批 0 封板页旧色,T8 强制重截 QuickCreate 确认无回归。
- **与后台任务隔离**:`task_113a291e` 在独立 worktree 改 Discovery/Search/Plaza 网格 key,批 4 不碰这三页;唯一交叉点是 `WindowSizeClass.kt`(Discovery/Plaza 消费)——T7 对其保守处理(有消费方就保留,只删确证 0 引用函数)。
- **已知需现场核对(非占位符)**:App.kt 的登录导航方式(push 还是 root target);ProfileStateHolder 是否有 clearCache/编辑资料能力;`DarkColorScheme` 各槽被 Color.kt 哪些值喂;`RunningHubThemeTokenTest` 断言了哪些符号;`AppTypography` 与 RhTypography 关系。