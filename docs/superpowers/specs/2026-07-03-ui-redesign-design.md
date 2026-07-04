# RunningHub UI 重设计方案(橄榄暗调 · 锁定暗色 · 样板间先行)

日期:2026-07-03
状态:已获用户批准,待实施计划
范围:Presentation/composeApp UI 层,全部 11 个页面,分批推进

## 1. 背景与现状

前期开发以功能实现为主,UI 未系统打磨。现状盘点结论:

- 代码中存在一次**做了一半的 design system 迁移**(roadmap RM-01/RM-02):
  - 新体系 `com.runninghub.app.ui.designsystem`:`RhTheme`(colors/shapes/typography 入口)、`RhColors`(26 个语义槽,暗色 `RhDarkColors` 为验收基准)、`RhTypography`(11 层级,letterSpacing 固定 0)、`RhSpacing`(8 档)、`RhShapes`,以及 Rh* 组件族(按钮/徽章/卡片/底部弹层/状态视图/底栏/参数选择器/计费组件)。
  - 旧体系 `com.runninghub.app.ui.theme`:Material3 `MaterialTheme` + `ExtendedColors`,与新体系在 `RunningHubTheme` 中双轨并存。
- UI 债务:
  - 巨型页面文件:`AppDetailScreen.kt` 1362 行、`TaskHistoryScreen.kt` 1160 行、`PlazaScreen.kt` 1009 行、`DiscoveryScreen.kt` 707 行。
  - 页面文件中约 56 处硬编码 `Color(0xFF…)`(不含色板定义文件),集中在 QuickCreate 相关文件(`QuickCreateDesign.kt` 23 处)。
  - 同名组件新旧两份(如两个 `AppCard`),页面引用来源不一。
- 导航:底部 5 Tab(快速创作[默认]/发现/Studio 广场/历史/我的),宽屏切侧边导航;Tab 外还有搜索、应用详情、创作者主页、社区、登录。

## 2. 已确认的设计决策

| 决策项 | 结论 |
|---|---|
| 视觉方向 | A · 橄榄暗调:完成现有 Rh 设计系统(近黑底 `#050608`、橄榄绿 `#A3B565`、紫辅助 `#8D63FF`),气质为"专业创作工具感"(对标 Midjourney/Runway/剪映) |
| 深浅色策略 | **锁定暗色**:App 始终暗色,不跟随系统深浅色切换 |
| 范围节奏 | 全部 11 页,分批推进,每批可验收 |
| 施工路线 | **样板间先行**:第一批用快速创作页的高保真 mockup 定案全部 token 与核心组件,验收后规范封板,后续页面照规范批量迁移 |
| 每批流程 | mockup 确认 → 实现 → Android 模拟器/真机截图验收;iOS 仅保证编译 |

## 3. 设计语言与暗色锁定

以 `RhDarkColors` 现值为视觉基准。技术动作:

1. `RunningHubTheme`(`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt`)移除 `isSystemInDarkTheme()` 跟随,固定暗色;Material 组件继续拿到暗色 `colorScheme`(Material 双轨在收尾批拆除)。
2. Android 状态栏/导航栏、启动屏背景、iOS 侧同步锁定暗色外观。
3. `RhLightColors` 保留类型但退出运行路径;样板间批验收后若无回退诉求,收尾批删除。
4. token 值如需微调(样板间 mockup 阶段发现对比度/层次问题),只改 `RhColors.kt` 一处。

## 4. Token 收敛(唯一来源 = RhTheme)

- `RhTheme` 补 `spacing` 入口(`RhSpacing` 已存在但未挂载)。
- 页面统一从 `RhTheme.colors/typography/shapes/spacing` 取值。
- 页面文件中约 56 处硬编码 `Color(0xFF…)` 全部替换为语义 token;`QuickCreateDesign.kt` 在样板间批消化。
- 旧 `ExtendedColors` 引用逐批替换,收尾批删除 `ui/theme` 旧色板。

## 5. 组件体系

- **已有可复用**(以打磨为准):Rh 按钮/徽章/内容卡片(ModelCard、PlazaWorkCard、HistoryTaskCard、AppCard)/底部弹层/状态视图/底栏/参数选择器/计费组件。
- **样板间批新增**:`RhTopBar`、`RhPromptField`、`RhChip`、`RhSegmentedControl`、`RhListItem`、`RhDialog`、`RhSnackbar`。
- **旧组件退役映射**:
  - 视觉类(旧 `AppCard`、`AppSearchBar`、`ErrorState`、`LoadingIndicator`)由 Rh 版本替代;
  - 功能类(`MediaPicker`、`SmartAsyncImage`、`VideoThumbnail`、图片/视频预览浮层)保留逻辑,颜色/间距接入 token。

## 6. 批次计划

每批流程:高保真 mockup 确认 → 实现 → Android 截图验收 → 进入下一批。

| 批次 | 内容 | 备注 |
|---|---|---|
| 批 0 样板间 | 快速创作页 + 主壳(底栏/窗口背景) | mockup 定案全部 token 和核心组件,验收后**规范封板** |
| 批 1 | 任务历史 + 应用详情 | 最大两个文件,顺带按治理阈值拆文件 |
| 批 2 | 发现 + 搜索 | 列表/卡片流,复用批 0-1 组件 |
| 批 3 | 广场 + 创作者主页 + 社区 | 内容流页面 |
| 批 4 收尾 | 我的 + 登录 + 设置 | 之后删除旧 theme/旧组件双轨、全局硬编码清零 |

## 7. 边界、验证与风险

- 只动 Presentation/composeApp UI 层,不碰 Domain/Data/网络/存储。
- 每批验证:`checkArchitectureBoundaries` + 相关模块 commonTest + Android 截图(用户看图验收)。
- iOS 仅保证编译通过;真机视觉不在本次验收范围(无 macOS 证据,按项目规则如实记录)。
- 文案继续走 Compose Resources,业务层不保存展示文案。
- QuickCreate 页面状态所有权在 `feature:quickcreate:presentation`,样板间批只改 UI 渲染层,不动 StateHolder/Coordinator。
- 大文件拆分与视觉改造同批完成,避免同一文件动两次。
- 工作区现存 `feature/kmp-refactoring` 分支上的构建脚本改动为用户既有工作,本方案实施不回滚、不覆盖。

## 8. 批 0 封板记录(2026-07-03)

- **范围**:commits `05afd50..49f8a88`(12 个),每任务均通过规范审查 + 质量审查双 gate,批次整体通过交叉终审。
- **验证证据**:`:composeApp:testDebugUnitTest --rerun`、`checkArchitectureBoundaries`、`:composeApp:assembleDebug` 全绿;真机(5d692d82)安装启动无崩溃,`logcat -b crash` 为空;空态截图 `batch0-empty-v2.png` 经用户验收通过(空态引导、面板内模式切换、橄榄 CTA、底栏、锁暗状态栏均符合定案 mockup)。**未验证**:iOS 编译/真机(Windows 环境无 macOS 证据);生成对话流真机截图(需登录跑真实任务,顺延至批 1 验收一并看)。
- **封板结论**:`RhColors`(暗色现值)/`RhTypography`/`RhSpacing`/`RhShapes` + `RhChip`/`RhSegmentedControl`/`RhTopBar`/`RhSnackbar`/`AppBottomBar` 为全局规范;后续批次照此迁移,token 值调整须经变更评审。
- **Token 取值标准写法(裁定)**:`colors`/`shapes` 必须经 `RhTheme.colors`/`RhTheme.shapes` 读取(CompositionLocal 承载);`spacing`/`typography` 为静态刻度,直接引用 `RhSpacing`/`RhTypography` 与经 `RhTheme.spacing`/`RhTheme.typography` 等价,两者均合规,组件内部惯例为直接引用。
- **强制规约**:composeApp 禁止使用实验性 Compose 布局 API(`FlowRow`/`FlowColumn` 等 `@ExperimentalLayoutApi`)——compose-multiplatform 1.7.3 与 androidx compose-bom 2024.12.01 混用导致实验签名编译期/运行期漂移,批 0 已发生启动 `NoSuchMethodError`(修复:`b4bec61`)。需要换行布局参照 `QuickCreateEmptyGuide.kt` 的稳定 `Layout` 实现。

### 批 0 遗留债务(收尾批/对应批次处理)

1. 删除 `RhLightColors` + 死代码 `ProvideRhTheme`(designsystem/RhTheme.kt,当前 0 调用方,收尾批删 RhLightColors 时会连锁编译失败,须一并删)。
2. 拆除旧 `ui/theme` 双轨(`ExtendedColors`、`Dimens`、`Primary300/Neutral*/Dark*/ErrorDark/SuccessDark` 等);quickcreate 内 7 个文件仍引用旧符号(ClassicComposer、HistoryContent、ResultContent、UploadFieldContent、ProjectContent、MediaChipCard、AdaptivePromptTextField),随批 1-4 迁移消化。
3. `LocalExtendedColors` 默认值当前为浅色,锁暗后未包 theme 的预览/测试会拿到新暗+旧浅混合视觉;随双轨拆除消失。
4. `RhSnackbar` 语义为顶部横幅,批 1 复用前评估改名(如 RhBanner)。
5. 评估把 `QuickCreateSampleWrapRow` 提升为 designsystem 公共换行布局组件。
6. `CompactControlPill` 选中态"橄榄底 + 紫边"与 RhChip 选中语义(橄榄底 + borderActive 边)不一致,统一之。
7. `RhSegmentedControl` 选中字重切换存在宽度抖动(CJK 标签不可感知,拉丁标签需固定测量宽或统一字重)。
8. a11y:RhChip/RhSegmentedControl 缺 `Role`/selected 语义;RhTopBar 无动作时的空 `IconButton` 假按钮。
9. `MainScreen.kt` 残留一处 `MaterialTheme.colorScheme.primaryContainer`。
10. 批 0 延后组件:`RhPromptField`/`RhListItem`/`RhDialog`(首个使用方批次新建)。

## 9. 批 1 封板记录(2026-07-03)

- **范围**:批 1a(任务历史)commits `943c7e4..a4fe79c`(5 个);批 1b(应用详情)commits `9231544..aa321a2`(11 个,含 4 个验收期修复)。每任务双阶段审查,批内验收期缺陷(FlowRow 之外的新问题)均在封板前闭环。
- **批 1a 交付**:TaskHistoryScreen 1234 行拆为 6 文件(主文件 152 行);RhTopBar/RhChip 筛选行/RhTaskStatusBadge/RhStates 三态接入;旧 `RhApp*` 色板别名与 5 处硬编码色清零。成功徽章经用户裁决保留 `statusSuccess` 绿色(语义状态色与品牌色分离)。
- **批 1b 交付**:AppDetailScreen 1449 行拆为 7 文件(主文件 ~330 行);参数区方案 A 落地——presentation 纯函数 `appDetailParamsLayout`(平铺阈值 6、媒体/多行文本核心、连续同节点名分组、**单字段命名组合并进「更多参数」**)+ 折叠 UI(已调计数徽章/组内重置/必填星标);长下拉弹层 `AppDetailOptionPickerSheet`(>6 弹层、>12 搜索、兄弟节点遮罩/imePadding/返回键关闭);64 处旧色清零;RhPrimaryButton 运行栏/RhChip 标签墙/RhSegmentedControl 接入;`RhWrapRow` 升为公共组件(带水平对齐参数);创作入口卡去除与配置参数区重复的只读参数预览。
- **验证证据**:`:composeApp:testDebugUnitTest`、`:feature:detail:presentation:testAndroidHostTest`(22 tests)、`checkArchitectureBoundaries`、`assembleDebug` 全绿;真机(历史页列表/抽屉)与模拟器(详情页轻/重参数/展开态/去重后)截图经用户验收通过。**未验证**:iOS 编译(Windows 无 macOS 证据);长下拉弹层实机行为(现有应用无 >6 选项下拉,有单测与代码审查证据);生成对话流截图(顺延)。

### 批 1 遗留债务

1. **参数分组标题为服务端技术节点名**(ImpactSwitch/easy float 等):启发式已尽力,彻底解决需服务端补语义分组元数据。
2. `AppDetailTaskResult` 的 TaskOutputCard 未替换为 ResultPreview(仅 token 化)。
3. 历史页搜索入口仍为 no-op 占位。
4. 必填仅媒体上传启发式星标,无提交校验联动。
5. presentation 的 `creationEntry().inputNodes`/`AppDetailInputNodeValuePreview` 已无 UI 消费方,待清理。
6. 历史详情抽屉 loading 为手写 spinner,未用 RhLoadingState。
7. `feature/detail/presentation/build.gradle.kts` 的 `withHostTestBuilder {}` 留在工作区随用户 AGP 迁移提交;建议迁移为全部库模块启用 host test(verifyL1 目前收集不到库模块单测)。
8. 历史页 CANCELED/UNKNOWN 状态的文案-颜色轻微不一致(迁移前既有语义,原样保留)。
9. Hero 图片叠加色保留 4 处(压暗渐变/返回钮 scrim/其上白色前景/轮播指示点),语义为内容叠加,非 token 违规。

## 10. 批 2 封板记录(2026-07-04)

- **范围与既有基线**:批 2 = 发现 + 搜索两页(§6)。发现页/搜索页的 Rh token 全量迁移、748 行拆分、RhSearchBar 新建、搜索路由到独立应用搜索页、内联搜索退役、旧 AppSearchBar/旧 AppCard 删除等,**已在本批之前的会话完成**(commits `065e57dc..0e0198c8`,含 `578ab81b`/`c23ad2b4`/`0e0198c8` 等);发现页现 309 行、搜索页 442 行,旧色残留 0 处。核实现状后,本批剩余工作收敛为用户本轮新提的两点:**应用卡片改整图叠加瓦片 + 按真实接口字段优化卡片数据**。
- **本会话提交**:`5aa39c37`(叠加卡重设计 + 卡片模型:presentation TDD 加 `featured`/`metrics`、移除恒 UNKNOWN 的假 `estimatedCost` 与 `primaryAction`;契约测试同步)→ `e1d32819`(打磨:删未用 `AppCardMetricIcon.Collect`、抽叠加卡图标尺寸常量)→ `78007083`(修搜索页在 360dp 设备退单列的密度 bug,恢复与发现页一致的双列)。叠加卡任务经规范审查 + 质量审查双 gate(质量审查 Approve,仅 Minor,已闭环 2 条)。
- **定案设计(mockup v2)**:卡片 = 封面铺满 3:4 瓦片 + 底部压暗 scrim,能力药丸(左上)、精选星标(右上,仅 `carefullyChosen`)、标题 + 真实指标(🔥`useCount`/👁`pv`/♥`likeCount`)叠在图层上;砍掉假费用与内联"生成/详情"文字操作(整卡可点)。媒体叠加的 `Color.Black/White`(scrim/药丸/白字)沿批 1 Hero 先例保留并注释,非 token 违规。发现页与搜索结果共用同一叠加卡。
- **搜索分离裁定(修正决策①)**:用户本轮反转"统一搜索"的初始设想。核实后确认现状已满足诉求——应用搜索为独立页(只搜应用),灵感搜索(广场/社区)独立且属批 3;之前退役的"内联搜索"仅是发现页内重复的应用搜索,删它不构成应用/灵感搜索合并。故批 2 不再改搜索结构,`RhSearchBar` 仅作共享 UI 组件。
- **验证证据**:`checkArchitectureBoundaries`、`:composeApp:testDebugUnitTest`、`:feature:discovery:presentation:testAndroidHostTest`(24 tests)、`:composeApp:assembleDebug` 全绿;模拟器 Pixel_10_Pro(冷启动)实测发现页/独立搜索页热词态/搜索结果双列态,叠加卡真实数据渲染、白字在暗/浅/图表类封面上均可读,`logcat -b crash` 为空,用户看图验收通过。**未验证**:iOS 编译(Windows 无 macOS 证据);登录态跑真实生成流。

### 批 2 遗留债务

1. **能力标签启发式**:`capability` 由标题/标签关键词推断,少数应用归入"通用创作";彻底解决需服务端补能力元数据(与批 1 债务 1 同源)。
2. `discovery_cost_unknown`、`discovery_action_generate`、`discovery_action_view_detail` 三个文案键在砍假费用/内联操作后已无 `.kt` 引用,为控制范围本批未删,留待收尾批统一清理。
3. `feature/discovery/presentation/build.gradle.kts` 的 `withHostTestBuilder {}` 已启用但随用户 AGP 迁移走,不进本批提交(与批 1 债务 7 同款)。实现期一次 `git reset HEAD .` 曾取消暂存用户的构建脚本改动,编辑内容完好,用户如需可重新 `git add`。
4. 叠加卡依赖封面质量:缺失/纯浅色封面靠 scrim 兜底(已验收可读),无封面走 `surfaceSunken` 占位;后续如遇极端浅色封面标题对比度不足,可评估加固定顶部压暗或首字母占位。
5. 精选星标仅在 `carefullyChosen=true` 时展示,测试数据中出现较少,真机大范围表现待登录态更多目录数据观察。

## 11. 批 3 封板记录(2026-07-04)

- **范围**:批 3 = 广场页(Plaza,底栏「灵感」tab)+ 创作者主页(CreatorProfile)。社区页(CommunityScreen,导航不可达死页)经用户裁定**本批不动**,登记债务。这三页最后一次改动是 `11652fa6`(2026-07-01),批 1/2 的 redesign 均未触及,即本批为真实待改基线(核实后开工,吸取批 2 探查报告过时的教训)。
- **本会话提交链**:`cd791f31`(Domain+Data:`PlazaCreationCard` 加 `ownerId`,mapper 映射 `owner.id`,契约测试)→ `cb7a2128`(presentation:`PlazaWorkCardUiModel` 加 `ownerId`/`ownerAvatar`/`likeCount`/`aspectRatio`,`plazaCardAspectRatio` clamp 0.6~1.4,host test 3 通过)→ `f585df34`(作品卡重做叠加式 + 广场重皮拆 1069→374 行,分 `PlazaHeaderSection`/`PlazaShortTile`/`PlazaScreenLogic`)+ `f79f27c1`(审查修复:"用同款"/作者行 a11y `Role.Button`、去临时 domain 耦合、删死排序锚点及假覆盖断言)→ `2c81efa4`(创作者主页重皮 + 复用叠加卡)+ `ee72a53c`(打磨:删未用 import、头像 contentDescription 兜底)→ `2d1d2e36`(**真机崩溃修复**)。作品卡任务经规范 + 质量双 gate(质量 Approve,2 条 Important 已闭环);创作者任务规范+质量合并审 Approve。
- **定案设计(mockup 定案)**:①作品卡沿批 2 叠加卡视觉语言重做——封面按**原始宽高比**(clamp 0.6~1.4)铺满 + 底部 scrim,叠标题 + 作者头像/昵称(**可点跳创作者主页**)+ 点赞,右上「用同款」橄榄胶囊;去掉旧"图下文"块与内联文字操作。②灵感/短片 → RhSegmentedControl,标签 → RhChip,排序下拉 → Rh 色板,三态 → RhStates(补重试),死搜索图标隐藏。③短片 16:9 瓦片仅 token 重皮(播放键/时长/scrim 为媒体叠加,保留+注释)。④创作者应用网格**零新组件复用批 2 叠加 AppCard**(`WebApp → toDiscoveryAppCardUiModel → DiscoveryAppCard`),关注按钮 RhPrimaryButton / 已关注 RhButton Secondary。⑤作者跳转唯一超纯 UI 层改动 = `ownerId`(domain+data),已 TDD。
- **验证证据**:`checkArchitectureBoundaries`、`:composeApp:testDebugUnitTest`、`:feature:community:presentation:testAndroidHostTest`(3 tests)全绿;`:feature:community:data` 无 host test 任务(AGP KMP-library DSL,仅 macOS `iosSimulatorArm64Test`),`ownerId` mapper 断言为**编译级验证**(未执行,macOS CI 补跑);`:composeApp:assembleDebug` 绿;模拟器 Pixel_10_Pro 实测广场瀑布流(叠加作品卡:作者+点赞+用同款)、短片双列瓦片、创作者主页(作者跳转 + 复用叠加卡双列),修复后 `logcat -b crash` FATAL 计数 0,用户看图验收通过。**未验证**:iOS 编译(Windows 无 macOS 证据);登录态关注/取消关注写操作;community:data 断言执行。
- **真机验证抓到并修复的崩溃**:点作者跳创作者主页时 `IllegalArgumentException: Key "" was already used`——该创作者的 `getUserAppList` 返回多个 `id` 为空的 WebApp,`LazyVerticalGrid` 以 `app.id` 为 key 时空串 "" 重复触发 LazyGrid 重复-key 崩溃。修复:`itemsIndexed` + `app.id.ifBlank { "creator-app-$index" }`(`2d1d2e36`)。

### 批 3 遗留债务

1. **社区页(CommunityScreen)为导航不可达死页**(6 个本地静态工具卡,点击不导航),本批未动;去留待收尾批或产品决策。
2. **灵感搜索未做**:广场死搜索图标已隐藏;做灵感搜索需先扩 `PlazaRepository`(加 keyword)+ Data 实现 + 服务端确认,超纯 UI 边界,单独排期。
3. **网格空-id key 崩溃为共性风险**:发现页/搜索页/广场瀑布流的 Lazy grid 同样按作品/应用 id keying,空或重复 id 会同类崩溃;批 2 数据恰未触发。已开后台任务 `task_113a291e` 统一加固(空-id 兜底),不阻塞本批。
4. **`community:presentation` 的 `withHostTestBuilder {}` 留在工作区未提交**(随用户 AGP 迁移),否则 `testAndroidHostTest` 收集不到;`community:data` 无 host test 任务,mapper 断言仅编译级验证。
5. 短片 `PlazaShortTile` 仅 token 重皮,未升入 designsystem(与作品卡不同,保留页面内私有);`PlazaSortDropdownMetrics/Anchor` 假覆盖已随审查修复删除。
6. 作品卡作者头像 `authorAvatarContent` 槽:null 头像渲染空圈(designsystem 默认首字母兜底仅在无槽调用时生效);creator 头像已补 contentDescription 兜底。
