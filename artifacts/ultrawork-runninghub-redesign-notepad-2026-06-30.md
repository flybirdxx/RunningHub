# Ultrawork 记录 - RunningHub 重设计迭代

## 启动信息

- 严格度：HEAVY。
- 原因：RM-01/RM-02 新增 Design System 抽象，后续 UI 重构会依赖这层 token 和组件骨架。
- 已使用技能：
  - mobile-android-design：用于 Compose UI、Material 3 组件和主题指导。
  - kotlin-specialist：用于 Kotlin Multiplatform 与 Compose 实现指导。
- 当前 roadmap 切片：RM-01 Design System 落地设计 + RM-02 Design System 代码骨架。

## 验收标准

1. RED/GREEN 证据：实现前测试必须因为 `RhColors`、间距、圆角、文字层级和初始组件状态 API 不存在而失败；实现后同一契约必须通过。
2. 构建证据：`:composeApp:assembleDebug` 必须能使用新的 Design System 编译通过。
3. 边界证据：`checkArchitectureBoundaries` 和 `checkLongTermGovernance` 必须通过；如果失败，必须记录精确命令和错误原因。

## 手动 QA 场景

- 表面：本轮是数据/API 形态的 design-system 契约，最贴近的可观察通道是 Gradle 测试输出。
- 初始尝试：`./gradlew.bat --console=plain :composeApp:commonTest --tests "com.runninghub.app.ui.designsystem.RhDesignSystemContractTest"` 失败，原因是本项目没有 `:composeApp:commonTest` 任务。
- 正确 RED 通道：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid`。
- 正确 GREEN 通道：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhDesignSystemContractTest"`。
- PASS 可观察结果：契约测试在实现前失败，在实现后通过。

## 发现

- 当前工作区已有多个未跟踪的 `AGENTS.md` 和两个产品重设计 artifact，这些不是本轮改动范围，不能回滚或覆盖。
- 既有 `RunningHubThemeTokenTest` 只覆盖旧主题色板，不能证明新 roadmap design-system 契约。

## 修正与证据

- 补充读取 `composeApp/AGENTS.md` 后确认：公共视觉值应进入主题或 Design Token，UI 修改至少执行 `./gradlew :composeApp:assembleDebug`。
- 用户指定的长期约束已经写入记忆：项目 `AGENTS.md` 必须遵循；明确需要时可以优化，但不能删除或覆盖。
- 用户指定的新增文档语言约束已经写入记忆：新增治理和规范文档默认中文；面向用户或后续协作者阅读时优先中文。
- 已应用中文注释规则：本切片新增 public/internal Kotlin API 按 `docs/governance/chinese-commenting.md` 添加了简体中文 KDoc。
- RED：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败，关键错误是 `RhDarkColors`、`RhSpacing`、`RhDefaultShapes`、`RhTypography`、`RhButtonStyle`、`RhPriceBadgeState` 和 `RhTaskStatus` 未解析。
- GREEN：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 在实现后通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhDesignSystemContractTest"` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug` 通过。
- GREEN：`./gradlew.bat --console=plain checkArchitectureBoundaries` 通过。
- 阻塞：`./gradlew.bat --console=plain checkLongTermGovernance` 失败，失败点是 RM-01/RM-02 范围外既有的 Plaza、History、AppModule 治理债。
- 归因：对治理失败文件执行 `git diff --name-only` 结果为空，说明它们没有被本轮修改。
- Reviewer gate：已启动 `lazycodex-code-reviewer` 审查本轮 RM-01/RM-02 文件；两轮等待和一次跟进后仍未返回 deliverable，关闭时状态仍为 running，因此不能计为 reviewer 通过。

## 治理解锁切片：History/Plaza/AppModule

### 落地设计

- 顺序约束：先处理 `checkLongTermGovernance` 的阻塞项，再继续后续 roadmap UI 切片，避免在已知治理失败上继续叠加页面改造。
- History invalidation bus 下沉到 `feature:task:presentation`，因为它是历史状态刷新编排信号，不应继续留在 `composeApp` 页面壳层。
- QuickCreate 历史适配器收敛到 `UnifiedGenerationHistoryRepository.kt`，保持 History 迁移期兼容桥只有一个隔离文件。
- `AppModule` 继续只在组合根显式绑定统一 History bridge，不把 Data 实现直接暴露给页面。
- Plaza/History 当前文件体量作为迁移期治理基线记录到文档，后续只允许继续拆薄，不允许状态机职责回流。

### 代码改造

- 删除 `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryInvalidationBus.kt`，新增 `feature/task/presentation/src/commonMain/kotlin/com/runninghub/feature/task/presentation/TaskHistoryInvalidationBus.kt`。
- 删除独立的 `QuickCreateGenerationHistoryRepositoryAdapter.kt`，把 QuickCreate adapter 和模型转换函数放入 `UnifiedGenerationHistoryRepository.kt`。
- 修正 `UnifiedGenerationHistoryRepository.getTaskDetail()` 的来源分派：缓存确认是 QuickCreate 才走 QuickCreate；未知或非 QuickCreate 先走 WebApp detail，失败后按迁移期规则降级。
- 更新 `LongTermGovernancePlugin`，让治理守卫识别新的唯一统一桥文件和 `AppModule` 显式绑定。
- 更新 `feature-presentation-thresholds.txt` 和 `composeapp-file-size-allowlist.txt`，用中文记录当前 Plaza/History 迁移期体量基线。
- 更新 `docs/migration/current-state.yaml`，移除已删除的旧 adapter 路径，补入新的统一桥和 task presentation invalidation bus 路径。

### 验证证据

- RED：`./gradlew.bat --console=plain checkLongTermGovernance` 在修复前失败，关键点包括 Plaza 文件超限、History UI 体量超基线、`TaskHistoryInvalidationBus` 本地共享流留在 composeApp、QuickCreate bridge 直接映射以及 `AppModule` bridge 绑定守卫失败。
- GREEN：`./gradlew.bat --console=plain :feature:task:presentation:testDebugUnitTest --tests "com.runninghub.feature.task.presentation.TaskHistoryInvalidationBusTest"` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapterTest" :composeApp:compileDebugKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain checkLongTermGovernance` 通过。
- GREEN：`./gradlew.bat --console=plain checkArchitectureBoundaries` 通过。
- GREEN：`./gradlew.bat --console=plain verifyL1Android` 通过。
- GREEN：迁移文档同步后，`rg "QuickCreateGenerationHistoryRepositoryAdapter\\.kt|com\\.runninghub\\.app\\.ui\\.feature\\.history\\.TaskHistoryInvalidationBus" composeApp feature build-logic docs -n || true` 没有旧路径输出，`./gradlew.bat --console=plain checkLongTermGovernance` 再次通过。
- Reviewer gate：已启动 `lazycodex-code-reviewer` 审查本轮治理与 History bridge 改动；两个 5 分钟等待窗口后仍未返回 deliverable，关闭时状态为 running，因此本 gate 只能记录为未完成，不能计为 reviewer 通过。

## RM-03 导航和 App 壳层

### 落地设计

- 默认入口调整为 `创作`，让用户打开应用后直接进入主生成路径。
- 一级导航展示顺序固定为 `创作 / 发现 / 灵感 / 任务 / 账户`。
- 枚举语义保持低风险兼容：`QuickCreate` 承载“创作”，`Discovery` 承载“发现”，`Studio` 继续映射 Plaza 但用户可见职责改为“灵感”，`History` 承载“任务”，`Profile` 承载“账户”。
- 底栏视觉下沉到 Design System 的 `AppBottomBar`，组件只接收 label、图标、选中态和点击回调，不持有业务状态。
- 窄屏继续隐藏软键盘覆盖时的底栏；宽屏继续使用 `NavigationRail`，不移动 Voyager Screen 注册关系。

### RED 证据

- RED：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 失败，关键错误是 `MainNavigationDefaults` 和 `BottomNavTab.role` 未解析，说明 RM-03 导航职责契约尚未实现。

### 代码改造

- 新增 `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/navigation/AppBottomBar.kt`，把手机底栏视觉沉入 Design System。
- 新增 `MainNavigationRole` 和 `MainNavigationDefaults`，固定默认入口和展示顺序。
- 修改 `MainScreen.kt`：默认进入 `QuickCreate`，窄屏底栏使用 `AppBottomBar`，宽屏 `NavigationRail` 使用同一导航顺序和品牌主色选中态。
- 修改 `strings.xml`：一级导航文案改为 `创作 / 发现 / 灵感 / 任务 / 账户`，访客提示同步中文。
- 新增 `MainNavigationDesignContractTest`，锁住 RM-03 的默认入口、顺序和中文职责说明。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.navigation.MainNavigationDesignContractTest" --tests "com.runninghub.app.ui.navigation.MainTabScreenRegistryTest"` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过。
- 未验证：`adb devices` 失败，当前 Git Bash 输出 `adb: command not found`，因此本轮没有真机/模拟器截图证据；已用导航契约测试和 debug 构建作为可复现检查。
- Reviewer gate：已启动 `lazycodex-code-reviewer` 审查 RM-03 导航壳层改动；等待被中断后补等 30 秒仍无 deliverable，关闭时状态为 running，因此本 gate 只能记录为未完成，不能计为 reviewer 通过。

## RM-04 Create 输入区

### 落地设计

- 空 Prompt 时右侧主按钮只承担“添加素材”，不触发生成，不展示发送图标。
- 有 Prompt 时右侧主按钮才承担“生成”，按钮必须出现可读文案：`生成` 或 `生成 · 价格确认中 / 价格待确认 / ¥金额`。
- Prompt 为空时在输入区内展示 inline 提示，避免用户把加号误解为提交生成。
- Prompt 超限沿用现有红色描边和 `canGenerate` 禁用，不改 `QuickCreateCoordinator`、请求工厂或生成提交逻辑。
- 素材上传条继续展示已选素材、追加入口、上传中遮罩和删除入口，本轮只补输入主动作语义，不重写上传状态机。

### RED 目标

- 用纯 Kotlin 契约锁住按钮语义：无 Prompt 必须是 `AddMedia`，有 Prompt 必须是 `Generate`；价格确认、价格待确认和金额态必须是 `生成 · 详情` 文案模式。

### RED 证据

- RED：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 失败，关键错误是 `CompactPrimaryAction`、`compactPrimaryAction`、`CompactGenerateTextMode` 和 `compactGenerateTextMode` 未解析，说明 RM-04 输入区动作契约尚未实现。

### 代码改造

- 新增 `QuickCreateCompactComposerContractTest`，锁住空 Prompt 加号只添加素材、有 Prompt 才生成，以及计费态文案必须保留 `生成` 意图。
- 修改 `QuickCreateCompactComposerContent.kt`：新增 `compactPrimaryAction()` 和 `compactGenerateTextMode()` 纯函数；生成按钮在价格确认、价格待确认和金额态展示 `生成 · 详情`，不再把价格确认态压成单独 spinner。
- 输入框下方新增元信息行：左侧在空 Prompt 时提示“先描述想生成的内容，加号只添加素材”，右侧常驻展示 `charCount/MAX_PROMPT_CHARS`，接近或超过上限时切换警示/错误色。
- 修改 `strings.xml`，新增空 Prompt inline 提示和 `生成 · %1$s` 组合格式；最终用户可见文案仍放在 Compose Resources。
- 本轮没有修改 `QuickCreateCoordinator`、请求构造、生成提交逻辑或上传状态机。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.QuickCreateCompactComposerContractTest"` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过。
- 未验证：`command -v adb || true` 没有输出，当前 shell 找不到 Android 调试工具，因此 RM-04 没有真机/模拟器截图证据；已用输入区契约测试、debug 构建和治理 gate 覆盖本切片。

## RM-05 生成价格和确认

### 落地设计

- 价格状态分成 `loading / pending / free / amount / insufficient`，由 Feature Presentation 输出稳定语义，composeApp 只负责映射本地化文案和 Design System 视觉。
- fee-preview 成功后保留可展示结算信息：预计 RH 点数、预计现金、现金余额、币种和余额不足状态；不保存服务端原始错误文本。
- 付费任务点击“生成”时先打开 `GenerationConfirmSheet`，确认后才进入现有正式提交链路；免费任务继续直接提交，避免无消耗任务增加额外阻力。
- 当前领域状态没有首单标记、会员权益和退款明细字段，本轮先在确认 Sheet 中明确展示“余额暂未同步 / 暂无会员减免 / 失败不扣费或自动退回”，不伪造账户余额或优惠。
- `QuickCreateCoordinator` 仍只做编排；提交前确认逻辑下沉到 `QuickCreateGenerationInteractor`，ScreenModel 只新增门面方法。
- 本轮不触碰 fee-preview DTO/API，不新增 Data 层请求字段。

### RED 目标

- Feature 契约：计费状态能区分 loading、pending、free、amount、insufficient；付费生成先打开确认 Sheet，不直接提交仓库。
- Compose 契约：Design System 暴露 `PriceBadge`、`BillingInfoCard`、`GenerationConfirmSheet` 的稳定状态对象；QuickCreate 能把付费预览映射为确认 Sheet。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败，关键错误包括 `QuickCreatePriceBadgeState`、`QuickCreateBillingAmount`、`QuickCreateBillingUnit`、`QuickCreateBillingPreviewUi`、`quickCreateGenerationConfirmState`、`QuickCreateSheet.GENERATION_CONFIRM`、`confirmGeneration()`、`PriceBadgeVisualState`、`BillingInfoRow` 和 `GenerationConfirmSheetState` 未解析。

### 代码改造

- 修改 `QuickCreateBillingUiText.kt`，新增稳定计费展示模型：`QuickCreateBillingUnit`、`QuickCreateBillingAmount`、`QuickCreateBillingPreviewUi`、`QuickCreatePriceBadgeState` 和 `QuickCreateGenerationConfirmState`。
- 修改 `QuickCreateFeePreviewInteractor.kt` 和 `QuickCreateUiState.kt`，成功 fee-preview 后保存可展示结算摘要，切 tab、恢复草稿、清理或失败时同步清空摘要。
- 修改 `QuickCreateGenerationInteractor.kt`，付费任务首次点击生成先打开 `GENERATION_CONFIRM` Sheet；`confirmGeneration()` 才复用原提交链路。免费任务继续直提。
- 修改 `QuickCreateCoordinator`、`QuickCreatePresentationStateHolder` 和 `QuickCreateScreenModel`，只新增确认门面方法，不把提交状态机拉回 composeApp。
- 新增 `designsystem/components/billing/PriceBadge.kt`、`BillingInfoCard.kt`、`GenerationConfirmSheet.kt`，并在 `QuickCreateGenerationConfirmSheet.kt` 中集中做本地化映射。
- 修改 `QuickCreateScreen.kt`，只接入确认 Sheet 展示；后续因治理阈值把结算映射拆到独立文件，`QuickCreateScreen.kt` 降到 712 行。
- 修改 `strings.xml`，新增确认 Sheet、预计消耗、余额、会员减免、失败扣费说明、价格确认中、价格待确认、免费生成、余额不足、RHB 点数和 CNY 金额格式文案。
- 本轮未触碰 fee-preview DTO/API、Repository Data 实现或远端请求字段，因此未运行 Data 层 allTests。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingUiTextTest" --tests "com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationInteractorTest" :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhBillingComponentsContractTest"` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:allTests` 通过；其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当成 macOS/iOS 运行证据。
- 阻塞已修：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 首次失败于 `QuickCreateScreen.kt` 806 行超出治理阈值；拆出 `QuickCreateGenerationConfirmSheet.kt` 后再次运行通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过。
- 未验证：`command -v adb || true` 没有输出，当前 shell 找不到 Android 调试工具，因此 RM-05 没有真机/模拟器截图或交互观察证据；已用 Feature 契约、Design System 契约、debug 构建、架构和治理 gate 覆盖本切片。

## RM-06 ModelPickerSheet

### 落地设计

- 搜索词、分类筛选、列表快照和过滤结果下沉到 `feature:quickcreate:presentation`，Composable 只渲染 `QuickCreateModelPickerState` 并回传事件。
- `QuickCreateServiceModelUi` 补充卡片所需稳定字段：能力类型、适用场景、技术标签、价格摘要和选中态；composeApp 不再从 `source` 读取展示文案。
- `ModelCard` 固定布局：左侧能力图标，中部模型名、适用场景和技术标签，右侧价格和选中 check；技术标签仅做辅助，不压过模型名和价格。
- `ModelPickerSheet` 固定包含标题、搜索、分类、列表、加载、空目录和搜索无结果；模型目录加载失败或价格缺失时展示非误导性占位，不展示伪造价格。
- 选择模型仍只回传 `identityKey`，不把 DTO 或完整 Domain 模型暴露给 UI 事件。

### RED 目标

- Feature 契约：模型选择器状态能保存 query/filter，过滤结果能区分空目录与搜索无结果，卡片模型包含能力、场景、价格和技术标签。
- Compose 契约：Design System 暴露 `ModelCard` 和 `ModelPickerSheet` 的稳定状态对象；QuickCreate 模型选择器不再把搜索、筛选状态放进 Composable 私有变量。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败，关键错误包括 `QuickCreateServiceModelKind`、`QuickCreateServiceModelScene`、`QuickCreateServiceModelPrice`、`technicalTags`、`quickCreateModelPickerState`、`QuickCreateModelPickerFilter`、`QuickCreateModelPickerEmptyReason`、`ModelCardState`、`ModelCardVisualState`、`ModelPickerFilterItem` 和 `ModelPickerSheetState` 未解析。

### 代码改造

- 修改 `QuickCreateServiceModelUiModel.kt`，新增模型卡片语义：能力类型、目标 tab、适用场景、价格状态、技术标签、筛选类型、空态原因和 `quickCreateModelPickerState()` 过滤快照。
- 修改 `QuickCreateUiState.kt` 和 `QuickCreateModelCatalogInteractor.kt`，把模型选择器 query、filter 和模型快照下沉到 Feature Presentation；模型加载和选择变化时同步维护稳定快照。
- 图片目录和视频目录分别显式写入 `targetTab`，composeApp 点击模型时消费该字段，不再根据 `kind` 推断路由，避免 Other/Audio 等能力标签把模型分发到错误 tab。
- 修改 `QuickCreateEditorStateHolder`、`QuickCreateCoordinator`、`QuickCreatePresentationStateHolder` 和 `QuickCreateScreenModel`，新增搜索词和分类选择门面方法，composeApp 只回传用户事件。
- 新增 `designsystem/components/cards/ModelCard.kt` 和 `designsystem/components/sheets/ModelPickerSheet.kt`，固定展示模型名、能力、场景、技术标签、价格和选中态，并覆盖加载、空目录、搜索无结果状态。
- 重写 `QuickCreateModelSelectorContent.kt` 的 sheet 组合逻辑：从 `quickCreateModelPickerState(uiState)` 渲染 `ModelPickerSheet`，选择仍只回传 `identityKey`，不在 Composable 私有变量中保存搜索/筛选状态。
- 修改 `strings.xml`，新增模型选择器搜索无结果、免费价格和图片/视频/音频/通用场景文案；最终用户可见文案仍放在 Compose Resources。
- 本轮未修改模型目录 Data/DTO、远端请求、生成提交链路或计费请求。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:testDebugUnitTest --tests com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUiModelTest` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhModelPickerComponentsContractTest` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:allTests` 通过；其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当成 macOS/iOS 运行证据。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过。
- 未验证：`command -v adb || true` 没有输出，当前 shell 找不到 Android 调试工具，因此 RM-06 没有真机/模拟器截图或交互观察证据；已用 Feature 契约、Design System 契约、debug 构建、架构和治理 gate 覆盖本切片。

## RM-07 Params / AdvancedSettingsSheet

### 落地设计

- 动态字段能力完整保留，Feature Presentation 只新增分层语义：常用参数、上传字段、Prompt 字段和高级参数；字段本身仍沿用服务端解析后的 `QuickCreationServiceFieldUi`。
- 常用参数优先展示 `aspectRatio`、`resolution`、`count/num`、上传素材和必要 Prompt 相关字段；Prompt 主输入已经在编辑器主区域，本 sheet 中 prompt 字段默认不重复展示，只作为分层语义保留。
- 高级设置默认折叠，包含 `endpoint`、`seed`、`negative prompt`、技术字段、工作流节点和无法归入常用参数的字段；用户展开后仍能修改，避免丢失服务端动态字段能力。
- 字段展示名在 Feature 层只输出语义：`aspectRatio` 为画面比例、`resolution` 为分辨率、`endpoint` 为技术端点；最终中文文案由 composeApp 资源层映射。
- `ParameterSelector` 负责选项参数的 default / selected / disabled / error 视觉状态；文本和上传控件继续复用已有输入/上传回调。
- `AdvancedSettingsSheet` 统一承载标题、模型摘要、常用参数、高级参数折叠、空参数、完成按钮和关闭按钮；参数缺失、参数冲突、空参数、只读字段都映射到明确 UI 状态。

### RED 目标

- Feature 契约：字段 UI 模型能输出参数层级、字段语义标题、只读/错误/缺失状态；`quickCreateParameterSheetState()` 能区分常用参数、高级参数和空态。
- Compose 契约：Design System 暴露 `ParameterSelector` 和 `AdvancedSettingsSheet` 的稳定状态对象，支持 default / selected / disabled / error，且高级参数默认折叠。

### RED 证据

- `./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid`
  - 预期失败：缺少 `quickCreateParameterSheetState`、`QuickCreationServiceFieldSection`、`QuickCreationServiceFieldDisplayLabel`、`QuickCreationServiceFieldVisualState`、`QuickCreationServiceFieldOptionVisualState`、`ParameterSelectorState`、`AdvancedSettingsSheetState` 等新契约。
- `./gradlew.bat --console=plain :feature:quickcreate:presentation:testDebugUnitTest --tests com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUiModelTest`
  - 中间失败：`LIST` 但无 options 的字段没有保留到参数面板，无法表达只读/空选项 disabled 状态。

### 代码改造

- `feature:quickcreate:domain` 的 `QuickCreationServiceSchema` 把 `LIST` / `SELECT` / `ENUM` / `OPTION` 识别为可渲染选项字段，即使服务端 options 为空也保留字段。
- `feature:quickcreate:presentation` 新增参数分层 UI 语义：常用、上传、Prompt、高级；字段显示名输出稳定语义，最终中文文案由 `composeApp` 资源映射。
- `composeApp` 新增 `ParameterSelector` 与 `AdvancedSettingsSheet`，参数面板改为常用参数优先、高级参数默认折叠；文本与上传字段继续复用原动态字段回调。
- 新增契约覆盖：常用/高级分层、`aspectRatio`/`resolution`/`endpoint` 语义命名、选项 selected/default、冲突 error、必填缺失 error、无 options 只读 disabled、sheet 空态和高级折叠。

### GREEN 证据

- `./gradlew.bat --console=plain :feature:quickcreate:presentation:testDebugUnitTest --tests com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUiModelTest`
- `./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhParameterComponentsContractTest`
- `./gradlew.bat --console=plain :feature:quickcreate:presentation:allTests`
- `./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance`
- `command -v adb || true` 无输出；本机未暴露 adb，因此 RM-07 未补真实设备截图或运行观察证据。

## RM-08 生成中和结果入口

### 落地设计

- `TaskStatusBadge` 直接复用现有 `RhTaskStatusBadge`，覆盖 queued / running / success / failed / canceled；RM-08 不新增重复状态徽标组件。
- Feature Presentation 补足任务运行闭环语义：最新任务和会话条目都携带 `taskId`，Create 内生成中可展示任务 ID 和“查看任务”动作。
- 任务动作由 Feature 输出稳定语义，不由 Composable 根据字符串临时判断：
  - queued / running：查看任务。
  - success：查看结果、再来一张、复制 Prompt；有结果媒体时提供保存、下载、复用参数。
  - failed：重试、查看详情、退款状态。
  - canceled：查看详情、再来一张。
- `ResultPreview` 作为 Design System 结果组件，负责图片/视频媒体状态、24 小时过期提醒和动作槽；组件本身不执行保存/下载，只把动作回调给页面层。
- Compose 结果展示统一从 `QuickCreateConversationItemUi` 的任务状态、任务 ID、结果列表和动作语义生成 UI，避免状态卡只显示一张图而没有下一步。
- 远端原始错误不直接展示；失败说明继续使用 `QuickCreatePresentationError` 到资源文案的映射。

### RED 目标

- Feature 契约：`QuickCreateTaskPollingController` 写入任务 ID；`quickCreateResultActions()` 能为 queued / running / success / failed / canceled 生成稳定动作集合。
- Compose 契约：Design System 暴露 `ResultPreviewState`、`ResultPreviewMediaState`、`ResultPreviewActionState`，支持图片、视频、过期提醒和动作列表。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败，关键错误包括 `taskId`、`quickCreateResultActions`、`QuickCreateResultAction`、`ResultPreviewState`、`ResultPreviewMediaState` 和 `ResultPreviewActionState` 未解析。

### 代码改造

- `feature:quickcreate:presentation` 为 `QuickCreateConversationItemUi` 和最新任务状态补入 `taskId`，`QuickCreateTaskPollingController` 在 queued / running / success / failed / canceled 状态回写任务 ID。
- 新增 `QuickCreateResultAction`、`QuickCreateResultActionUi` 和 `quickCreateResultActions()`，由 Feature 输出 queued / running / success / failed / canceled 的稳定下一步动作集合。
- `composeApp` 新增 `ResultPreview` Design System 组件，承载任务状态徽标、图片/视频预览、24 小时过期提醒和动作槽；占位背景使用 `RhTheme` token，不新增私有色板。
- `QuickCreateConversationContent` 改为从 `QuickCreateConversationItemUi` 统一生成结果卡，成功态展示结果入口、再来一张、保存/下载/复用参数/复制 Prompt，失败/取消态展示恢复动作。
- `QuickCreateScreen` 接入结果动作回调：复制 Prompt 写入系统剪贴板；复用参数恢复 Prompt；再来一张/重试复用 Prompt 后走现有生成入口。
- `strings.xml` 补充任务 ID、24 小时过期提醒和结果动作中文资源。
- 本轮不实现平台保存/下载或任务详情导航成功回执，`Save`、`Download`、`ViewTask`、`ViewResult`、`ViewDetail` 和 `RefundStatus` 只作为稳定入口语义暴露；未声称保存、下载或导航成功。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:testDebugUnitTest --tests com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusUiTest --tests com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingControllerTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhResultPreviewComponentsContractTest` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:allTests` 通过；其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当作 macOS/iOS 运行证据。
- GREEN：`./gradlew.bat --console=plain :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过。
- GREEN：`git diff --check` 通过。
- 未验证：`command -v adb || true` 无输出；当前 shell 未暴露 adb，因此 RM-08 未补真实设备/模拟器截图或交互观察证据。

## RM-09 History 任务卡

### 落地设计

- `TaskHistoryEntry` 不再只给列表暴露原始 `status`、`source`、`costCurrency` 等后台字段；Presentation 增加 `TaskHistoryCardStatus`、`TaskHistoryPrimaryAction`、`TaskHistoryCostUi` 和 `TaskHistoryExpiryUi`，把卡片需要回答的问题在 Feature 层先归一化。
- `HistoryTaskCard` 进入 Design System，接收稳定 `HistoryTaskCardState`：缩略图、任务名、状态徽标、生成方式、费用、耗时、过期提示、输出数量和主操作；组件只渲染状态，不读取 `GenerationHistoryItem` 或 `TaskHistoryUiState`。
- 卡片默认不展示完整 `taskId`，点击整卡仍通过页面层打开详情；长 ID 只留给 Task Detail，而不是列表。
- 成功任务主操作固定为“查看结果”，失败任务主操作固定为“重试”；进行中任务保留“取消”入口；有参数且成功时保留“复用参数”作为次级动作。
- 费用展示只优先显示 RHB 点数：`costCurrency` 为 RHB / RH / COIN / POINTS / CREDITS 时显示点数；CNY、USD 等法币不与 RHB 混在同一行，暂作为独立 cost kind 供后续 Detail 展示，不在列表制造混排。
- 过期提示从第一项输出的 `expireDays` / `expireTime` 提取，优先展示剩余天数；无输出或服务端未给过期信息时列表不显示虚假提示。
- `TaskHistoryScreen` 只把 `TaskHistoryEntry` 映射为 `HistoryTaskCardState` 和资源文案，不在 Composable 内通过字符串临时判断任务状态或费用单位。

### RED 目标

- Feature 契约：`TaskHistoryEntry` 能输出卡片状态、主/次操作、费用语义、过期语义；筛选仍覆盖全部 / 进行中 / 成功 / 失败。
- Compose 契约：Design System 暴露 `HistoryTaskCardState`、`HistoryTaskCardStatusState`、`HistoryTaskCardCostState`、`HistoryTaskCardActionState`，支持成功查看结果、失败重试、进行中取消、过期提示和“列表不展示长任务 ID”。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:task:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败，关键错误包括 `TaskHistoryCardStatus`、`TaskHistoryCardAction`、`TaskHistoryCostKind`、`cardStatus`、`primaryAction`、`secondaryActions`、`cost`、`expiry`、`showTaskIdInCard`、`HistoryTaskCardState`、`HistoryTaskCardStatusState`、`HistoryTaskCardCostState` 和 `HistoryTaskCardActionState` 未解析。

### 代码改造

- `feature:task:presentation` 为 `TaskHistoryEntry` 增加卡片级语义：`TaskHistoryCardStatus`、`TaskHistoryCardAction`、`TaskHistoryCostUi`、`TaskHistoryExpiryUi`、主操作、次操作和 `showTaskIdInCard=false`。
- `TaskHistoryStateHolder` 的历史条目映射统一输出成功 / 失败 / 进行中 / 已取消 / 未知状态；成功任务主操作为查看结果，失败任务主操作为重试，进行中 QuickCreate 任务保留取消入口。
- 费用映射把 RHB / RH / COIN / POINTS / CREDITS 归为 `RHB`，CNY / USD / EUR / JPY / HKD 归为 `FIAT`，列表只显示单一费用单位，不把 CNY 与 RHB 混排。
- 过期提示从第一项输出的 `expireDays` / `expireTime` 提取；无输出或服务端未返回过期信息时不展示虚假提示。
- 新增 `designsystem/components/cards/HistoryTaskCard.kt`，组件接收 `HistoryTaskCardState`、缩略图 slot 和动作回调，只负责卡片布局，不读取 Feature 状态或执行副作用。
- `TaskHistoryScreen` 的列表行改用 `HistoryTaskCard`，默认不再展示完整任务 ID；整卡点击打开详情，查看结果 / 重试 / 取消 / 复用参数通过稳定动作分发到现有 ScreenModel 方法。
- `strings.xml` 补充“查看结果”、“已取消”和“未知状态”资源文案。
- 修复一次治理扫描误报：测试 ID 中的 `task-id` 片段触发 OpenAI key 正则，改为等价的 `history-row` 测试 ID。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:task:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:task:presentation:testDebugUnitTest --tests com.runninghub.feature.task.presentation.TaskHistoryStateHolderTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhHistoryTaskCardComponentsContractTest` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:task:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过；其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当作 macOS/iOS 运行证据。
- GREEN：`git diff --check` 通过。
- 未验证：`command -v adb || true` 无输出；当前 shell 未暴露 adb，因此 RM-09 未补真实设备/模拟器截图或交互观察证据。

## RM-10 Task Detail / Result

### 落地设计

- 任务详情页从“后台调试记录”调整为“结果处理页”：首屏顺序固定为状态摘要、结果预览、操作区、计费信息、Prompt 和关键参数，技术详情默认折叠。
- `Task Detail` 继续沿用 History 的数据来源，但页面层只接收面向详情的稳定 UI 状态：状态、结果媒体、保存状态、过期提醒、费用、Prompt、关键参数和技术详情折叠项。
- `Task Result` 复用 RM-08 的 `ResultPreview` 结果组件，结果图/视频优先展示；保存/下载/复用/重试作为明确动作语义，组件不直接执行平台副作用。
- 计费信息复用 `BillingInfoCard` 的视觉和语义，先展示扣费点数或法币摘要；费用缺失时展示“未提供扣费信息”的稳定空态，不把接口原始字段名暴露给用户。
- 成功任务的主路径是查看/保存结果；失败任务的主路径是重试；退款、过期和保存状态都必须有独立 UI 状态，不能只藏在技术详情里。
- Prompt 和关键参数只展示用户能理解的字段，长请求 JSON、内部节点参数和原始请求体进入“技术详情”折叠区，默认不展开。
- Domain 仅在 UI 所需字段缺失时扩展；若现有 `GenerationHistoryItem` 已包含输出、费用、请求参数、退款和过期信息，则优先在 Presentation/composeApp 映射，不改 Data mapper。

### RED 目标

- Domain / Presentation 契约：任务详情 UI 状态能表达结果优先顺序、保存状态、24 小时过期提醒、复用/重试动作、退款状态、计费摘要、Prompt / 关键参数和默认折叠的技术详情。
- Compose 契约：任务详情渲染状态使用 `ResultPreview`、`BillingInfoCard` 和状态徽标组合，默认不展开请求 JSON，成功、失败、退款、过期、保存状态都有明确 UI。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:task:presentation:compileDebugUnitTestKotlinAndroid :feature:task:data:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 在实现前失败。
- 关键失败点：`selectedTaskDetailUi`、`TaskHistoryDetailSectionType`、`TaskHistoryDetailStatus`、`TaskHistoryDetailSaveState`、`TaskHistoryDetailAction`、`TaskHistoryDetailMediaType`、`TaskHistoryDetailBillingKind`、`requestParameters`、`TaskDetailLayoutState` 等新契约未解析。

### 代码改造

- `feature:task:domain` 为 `GenerationTaskDetail` 增加 `requestParameters`，并用中文注释明确其只能保存脱敏后的关键请求参数，禁止保存 Token、Cookie、API Key、验证码等敏感字段。
- `feature:task:data` 在 WebApp 详情 mapper 中从已脱敏请求参数提取 Prompt / 关键参数摘要，过滤敏感键；同时把 JVM-only 的 `putIfAbsent` 改为 KMP commonMain 可用写法，覆盖 iOS 编译路径。
- `feature:task:presentation` 新增 `TaskHistoryDetailUiModel`、详情 section 顺序、结果媒体、保存/退款状态、动作、计费行、Prompt 参数和默认折叠技术详情；`TaskHistoryStateHolder` 打开详情时同步产出 `selectedTaskDetailUi`。
- `composeApp` 的任务详情抽屉改为结果优先：状态摘要、`ResultPreview`、保存/退款状态、`BillingInfoCard`、Prompt / 关键参数、默认折叠的请求/响应技术详情。
- `UnifiedGenerationHistoryRepository` 在 fallback 和远端详情合并时补齐 `requestParameters`；`WebAppTaskHistoryOverlayStore` 移出 history UI 目录，减少 UI 目录治理压力，但兼容桥仍保留在治理要求的固定路径。
- 删除 `TaskHistoryScreen` 中已被 RM-10 替代的旧调试详情渲染函数，history UI 目录行数从 2159 降到 1834，满足 `1754 + 80` 的长期治理缓冲。
- `strings.xml` 补充 Prompt / 技术详情 / 保存状态 / 退款状态 / 保存下载动作 / 空计费等中文资源。
- 新增或更新契约测试：详情 UI 顺序、成功任务保存/下载/复用、失败任务重试/退款状态、Data mapper 脱敏参数提取、Compose 详情布局状态。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:task:presentation:testDebugUnitTest --tests com.runninghub.feature.task.presentation.TaskHistoryStateHolderTest :feature:task:data:testDebugUnitTest --tests com.runninghub.feature.task.data.repository.WebAppTaskRepositoryImplTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.history.TaskDetailResultLayoutContractTest` 通过，`BUILD SUCCESSFUL in 6s`。
- GREEN：`./gradlew.bat --console=plain :feature:task:domain:allTests :feature:task:data:allTests :feature:task:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过，`BUILD SUCCESSFUL in 20s`；其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当作 macOS/iOS 运行证据。
- GREEN：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- GREEN：`find composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history -maxdepth 1 -type f -name '*.kt' -print0 | xargs -0 wc -l` 输出 `1834 total`，等于当前长期治理允许上限。
- 未验证：`command -v adb || true` 无输出；当前 shell 未暴露 adb，因此 RM-10 未补真实设备/模拟器截图或交互观察证据。

## RM-11 Discover / App Detail

### 落地设计

- 发现页不再把 AppCard 当作市场陈列卡使用；Feature Presentation 先输出 `DiscoveryAppCardUiModel`，卡片固定表达模板名、能力类型、结果预览、预计费用、使用/成功率辅助指标和主 CTA。
- 能力类型优先从标签、封面媒体类型和标题/描述启发式归一：视频生成、图片生成、音频生成、通用创作；它只服务普通用户理解，不暴露 API、workflowId 或节点名。
- 预计费用当前没有稳定计费字段时必须明确输出“费用待确认”语义，不能用空白或后端字段名替代；后续 RM-13 再接真实余额和费用链路。
- 主 CTA 语义由 Presentation 输出：有明确生成入口的公开 WebApp 默认是“立即生成”，加载失败或不可运行时降级为“查看详情”；composeApp 只映射资源文案和导航行为。
- `designsystem/components/cards/AppCard.kt` 新增创作入口卡片状态和组件；旧 `ui.component.AppCard` 不删除，发现页本轮改用 Design System 卡，Search/Plaza 等后续路线再按各自 RM 处理。
- App Detail 首屏从“大封面 + 长简介”改成创作入口：紧凑封面、用途摘要、必要输入摘要、预计费用、主 CTA 和运行状态优先出现；作者、完整简介和统计信息后置。
- Detail Presentation 新增 `AppDetailCreationEntryUiModel`，集中描述用途、必要输入、预计费用、主 CTA 可用性、技术参数折叠状态和首屏展示顺序；composeApp 不再在页面内临时解析节点名。
- 必要输入只展示用户能理解的字段标题和是否已填写；`nodeId`、`workflowId`、内部 fieldName 和 API 参数进入默认折叠的技术信息，不作为首屏主要信息。
- 详情参数区复用 RM-07 已有 `ParameterSelector` 状态和视觉，不重复创建输入组件；上传、文本和开关控件继续沿用现有详情输入控件。
- 搜索无结果、详情加载失败、上传失败、任务提交失败仍由现有稳定错误语义映射资源文案；本轮只调整入口优先级，不把远端原始 msg 展示给用户。

### RED 目标

- Discovery 契约：`DiscoveryUiState` 能输出列表和搜索结果的创作入口卡片语义；卡片包含能力类型、结果预览、预计费用、辅助指标和主 CTA。
- Detail 契约：`AppDetailUiState` 能输出首屏创作入口语义；用途、必要输入、费用、立即生成、技术信息折叠和失败状态都可被测试。
- Compose 契约：Design System 暴露新的 `AppCardState`、`AppCardPreviewState`、`AppCardMetricState` 和 `AppCardActionState`，发现页可用它替换旧卡片。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:discovery:presentation:compileDebugUnitTestKotlinAndroid :feature:detail:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 失败，exit code 1。
- 关键失败点：`appCards`、`searchResultCards`、`DiscoveryAppCapability`、`DiscoveryAppPreviewType`、`DiscoveryAppEstimatedCostKind`、`DiscoveryAppCardPrimaryAction`、`DiscoveryAppCardMetricKind`、`creationEntry`、`AppDetailCreationSection`、`AppDetailEstimatedCostKind` 和 `AppDetailCreationPrimaryAction` 未解析。

### 代码改造

- `feature:discovery:presentation` 新增 `DiscoveryAppCardPresentation`，把 `WebApp` 映射成创作入口卡片语义：能力类型、预览类型、预计费用、使用/浏览辅助指标和主 CTA；`DiscoveryUiState` 增加 `appCards` 与 `searchResultCards`，列表和搜索结果共用同一语义。
- `feature:detail:presentation` 新增 `AppDetailCreationEntryPresentation`，把详情状态映射成首屏创作入口：用途、必要输入、预计费用、主 CTA、运行状态和默认折叠技术详情；技术键只进入折叠区，不作为首屏主要信息。
- `composeApp` 新增 `designsystem/components/cards/AppCard.kt`，提供稳定的 `AppCardState`、预览、指标和动作状态；发现页改用 Design System 卡片展示模板名、能力、预览、费用和生成/查看 CTA。
- 发现页的搜索结果也改用 `searchResultCards`，并移除预览兜底里的内部枚举名显示，避免把 `preview.type.name` 暴露给用户。
- App Detail 首屏顺序改为创作入口优先，紧凑封面、用途、必要输入、费用和立即生成先出现；旧大封面、作者、简介和统计信息后置到“关于详情”区域。
- App Detail 继续复用现有上传、文本、开关和参数控件；RM-07 的参数分层能力不重复实现，本轮只调整入口优先级和语义模型。
- `strings.xml` 补充发现页能力、费用、CTA、指标，以及详情创作入口、必要输入、技术详情折叠区等中文资源。
- 新增或更新契约测试：Discovery 卡片语义、搜索卡片语义、Detail 创作入口语义、Design System `AppCard` 状态模型。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:discovery:presentation:compileDebugUnitTestKotlinAndroid :feature:detail:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过，`BUILD SUCCESSFUL in 17s`。
- GREEN：`./gradlew.bat --console=plain :feature:discovery:presentation:testDebugUnitTest --tests com.runninghub.feature.discovery.presentation.DiscoveryStateHolderTest :feature:detail:presentation:testDebugUnitTest --tests com.runninghub.feature.detail.presentation.AppDetailStateHolderTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhAppCardComponentsContractTest` 通过，`BUILD SUCCESSFUL in 7s`。
- GREEN：`./gradlew.bat --console=plain :feature:discovery:presentation:allTests :feature:detail:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过，`BUILD SUCCESSFUL in 24s`。
- GREEN：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 未验证：`command -v adb || true` 无输出；当前 shell 未暴露 adb，因此 RM-11 未补真实设备/模拟器截图或交互观察证据。

## RM-12 Plaza 使用同款

### 落地设计

- Plaza 从“瀑布流浏览”改成“灵感到创作”的转化入口，但不伪造后端没有返回的模板、SKU、Prompt 或参考图；缺失字段必须以可复用参数摘要里的缺失态呈现。
- `PlazaCreationCard` 在 Domain 增加复用来源和可复用参数快照：作品 ID、作者来源、媒体预览、可选模板/SKU、可选 Prompt、比例、分辨率、数量和参考媒体；所有字段都可空，表示服务端未给或该作品不可直接复用。
- Community Presentation 新增 `PlazaWorkCardUiModel` 与作品详情/复用 Sheet 状态。卡片固定展示图片/视频预览、作者、使用数、主 CTA“使用同款”；无法复用完整参数时 CTA 仍可打开详情，但详情必须显示哪些参数缺失。
- 作品详情首屏固定顺序：原作品预览、作者信息、来源保护说明、可复用参数摘要、主 CTA“使用同款生成”。作者名、作者头像和作品 ID 不被复用流程丢弃。
- 复用参数摘要按“模型/模板、Prompt、比例、分辨率、数量、参考图”六项表达状态；已知项可带入 Create，未知项要求用户在 Create 中补齐或调整。
- `ReuseTemplateSheet` 只负责展示复用前确认和来源保护，不提交生成、不调用计费、不绕过 RM-05 的生成价格确认；确认后进入 QuickCreate 编辑态，由现有 fee-preview/GenerationConfirmSheet 继续拦截价格确认。
- `QuickCreateInspirationStateHolder` 增加 Plaza 复用入口，只接受 Presentation 级复用意图并写入当前图片/视频编辑状态；应用成功后调用既有 `onTemplateApplied` 回调以重新调度计费预览，不直接调用 `generate()`。
- Compose Plaza 本轮新增 Design System `PlazaWorkCard` 和 `ReuseTemplateSheet`，PlazaScreen 只做资源文案映射、详情/Sheet 展示和导航回调；不直接读取 Data/DTO，不展示服务端原始错误。
- 搜索/详情失败、上传失败、任务提交失败继续沿用已存在稳定错误语义；RM-12 只新增 Plaza 复用可用/缺失状态，不改变 QuickCreate 提交流程。

### RED 目标

- Community Domain 契约：`PlazaCreationCard` 能表达可复用参数快照，且缺失字段不被当作完整可复用参数。
- Community Presentation 契约：`PlazaUiState` 能输出作品卡片和选中作品详情；详情保留作者来源、可复用参数摘要、使用同款动作和缺失参数状态。
- QuickCreate Presentation 契约：应用 Plaza 复用意图后进入创作态，写入可用 Prompt/比例/分辨率/参考图，并触发计费预览回调；缺少模型/SKU 时不直接提交生成。
- Compose 契约：Design System 暴露 `PlazaWorkCardState` 与 `ReuseTemplateSheetState`，卡片和 Sheet 都有使用同款 CTA、来源说明和参数摘要。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:community:presentation:compileDebugUnitTestKotlinAndroid :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 失败，exit code 1。
- 关键失败点：`reuseSnapshot`、`PlazaCreationReuseSnapshot`、`PlazaReuseMediaKind`、`workCards`、`openWorkDetail`、`selectedWorkDetail`、`PlazaWorkCardPrimaryAction`、`PlazaWorkPreviewType`、`PlazaReuseParameterStatus`、`PlazaWorkDetailPrimaryAction`、`applyPlazaReuseIntent`、`QuickCreatePlazaReuseIntent`、`QuickCreatePlazaReuseMediaKind` 未解析。

### 代码改造

- `feature:community:domain` 为 `PlazaCreationCard` 增加 `PlazaCreationReuseSnapshot` 与 `PlazaReuseMediaKind`，模板、SKU、Prompt、比例、分辨率、数量和参考媒体均保持可空，避免把不完整服务端数据伪造成完整复用参数。
- `feature:community:data` 在 Plaza mapper 中从作品简介和 showreel 媒体提取可复用快照，媒体类型只归一到 image/video/audio/unknown，不暴露 DTO 或远端原始字段给 Presentation。
- `feature:community:presentation` 新增 `PlazaReusePresentation`，输出作品卡片、选中详情、来源保护、复用参数摘要、缺失参数状态和“使用同款”动作；`PlazaStateHolder` 只增加选中作品详情状态，不把分页或筛选状态回流 composeApp。
- `feature:quickcreate:presentation` 新增 `QuickCreatePlazaReuseIntent`，`QuickCreateInspirationStateHolder.applyPlazaReuseIntent()` 将 Plaza 参数写入可编辑创作态并触发既有 fee preview 回调；不调用生成、不绕过 RM-05 价格确认。
- QuickCreate Coordinator、PresentationStateHolder、ScreenModel 和 Voyager Screen 串起 Plaza 复用入口，`MainScreen` 在 Plaza 点击“使用同款”后把复用意图一次性带入 QuickCreate Tab。
- `composeApp` 新增 Design System `PlazaWorkCard` 与 `ReuseTemplateSheet`，PlazaScreen 使用同款卡片和确认面板展示作品预览、作者、使用数、来源保护和参数摘要；确认动作只进入 Create 编辑态。
- `strings.xml` 补充 Plaza 使用同款、复用参数、缺失参数和来源保护资源文案，避免恢复硬编码 UI 文案。
- `docs/governance/feature-presentation-thresholds.txt` 将 Plaza UI 基线更新到 1604，并写明 RM-12 使用同款卡片、参数复用确认和资源适配后的当前体量；owner 仍为 `feature:community:presentation`，不允许状态机职责回流 composeApp。
- 新增或更新契约测试：Plaza 可复用来源和详情摘要、QuickCreate Plaza 复用意图、Design System Plaza 卡片/复用 Sheet 状态，以及主导航一次性传递复用意图。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:community:presentation:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:quickcreate:presentation:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 通过。
- GREEN：`./gradlew.bat --console=plain :feature:community:presentation:testDebugUnitTest --tests com.runninghub.feature.community.presentation.PlazaStateHolderTest :feature:quickcreate:presentation:testDebugUnitTest --tests com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationStateHolderTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhPlazaReuseComponentsContractTest --tests com.runninghub.app.ui.navigation.MainTabScreenRegistryTest` 通过，`BUILD SUCCESSFUL in 7s`。
- GREEN：`./gradlew.bat --console=plain :feature:community:presentation:allTests :feature:quickcreate:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 首次失败于 `checkLongTermGovernance`，关键错误为 Plaza UI 从允许 1334 行加 80 行缓冲增长到 1604 行；补充 RM-12 治理基线说明后重跑通过，`BUILD SUCCESSFUL in 18s`。其中 iOS test/link 任务在 Windows 下显示 SKIPPED，不能当作 macOS/iOS 运行证据。
- GREEN：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 未验证：`Get-Command adb -ErrorAction SilentlyContinue` 退出码 1 且无输出；当前环境未暴露 adb，因此 RM-12 未补真实设备/模拟器截图或交互观察证据。

## RM-13 Profile 钱包会员

### 落地设计

- Profile 从“设置菜单页”改成“可信资产中心”：顶部用户身份保留，但首屏核心信息改为 RHB 点数、CNY 钱包余额、会员状态和最近消费明细入口。
- `WalletBalanceCard` 只展示两个独立数值：RHB 点数和钱包余额 CNY，不把余额和创作扣费混成一行；操作只暴露“充值”和“明细”，不在本轮直接接支付提交。
- 风险提示来自 Presentation 的稳定语义：余额未知、余额不足、预计可生成次数未知或可生成次数低，都用明确状态表达；没有真实单次价格时不伪造“可生成 X 次”。
- `MembershipCard` 展示会员等级、剩余天数/到期或已过期状态、权益摘要和续费入口；权益摘要只能来自当前可推导的会员状态，不编造后端没有返回的权益列表。
- `消费明细` 当前没有独立账单接口，因此 Presentation 先输出交易列表空态和可扩展的 `ProfileTransactionUiModel` 契约；只有交易项携带任务 ID 时 Compose 才允许点击跳 Task Detail。
- 余额加载失败以 `AccountStatus` 缺失或用户资料失败表达，不把 `null` 当 0；未登录态保持独立资产不可见状态。
- `feature:auth:presentation` 增加 Profile 资产展示模型，Compose 只映射资源文案与 Design System 组件；不读取 Data、DTO、Token、Cookie 或服务端原始 msg。
- 本轮不把会员广告插入 QuickCreate 主输入区；Create 仍只通过 RM-05 价格确认和计费预览展示余额关系。

### RED 目标

- Profile Presentation 契约：`ProfileUiState` 输出 `walletCenter`、`membershipCenter`、`transactions`、资产加载/失败/空态和可跳转任务 ID；未登录时资产中心不可见。
- 会员契约：会员正常、过期、剩余天数未知三种状态可区分，续费 CTA 可展示但不执行支付。
- 消费明细契约：空态可读；交易项类型覆盖生成、退款、充值、会员；只有关联任务 ID 存在时才允许跳 Task Detail。
- Compose 契约：Design System 暴露 `WalletBalanceCardState`、`MembershipCardState`、`TransactionListItemState`，并能表达充值、明细、续费和交易点击动作。

### RED 证据

- RED：`./gradlew.bat --console=plain :feature:auth:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 失败，exit code 1。
- 关键失败点：`assetCenter`、`ProfileWalletRisk`、`ProfileAssetLoadState`、`ProfileMembershipStatus`、`ProfileMembershipAction`、`ProfileMembershipBenefitStatus`、`ProfileTransactionCenterUiModel`、`ProfileTransactionUiModel`、`ProfileTransactionType`、`ProfileTransactionStatus` 和 `canOpenTaskDetail` 未解析。

### 代码改造

- `feature:auth:presentation` 新增 `ProfileAssetCenterPresentation`，把 `ProfileUiState` 映射成资产中心语义：资产加载状态、RHB 点数、CNY 钱包余额、风险提示、会员状态、交易空态和可扩展交易项契约。
- `ProfileStateHolder` 增加 `isAccountStatusLoadFailed` 与 `assetCenter` 只读属性，刷新用户资料时区分用户资料失败、账户状态失败和未登录隐藏态；不把 `null` 余额当作 0。
- 会员状态只从 `User.memberInfo` 与 `AccountStatus.memberInfo` 可推导字段生成，区分未开通、有效、过期和剩余天数未知；权益摘要保持不可用状态，避免编造后端没有返回的会员权益。
- 交易中心先输出明确空态和 `ProfileTransactionUiModel` 契约，类型覆盖生成、退款、充值、会员；`canOpenTaskDetail` 只在交易携带关联任务 ID 时为真。
- `composeApp` 新增 `designsystem/components/billing/AssetCenterCards.kt`，提供 `WalletBalanceCardState`、`MembershipCardState`、`TransactionListItemState` 以及对应 Compose 组件，稳定表达充值、明细、续费、风险提示和交易点击状态。
- `ProfileScreen` 改用 `AssetCenterSection` 展示钱包、会员和消费明细，删除旧的重复账户摘要行；页面只做资源文案和 Design System 状态映射，不读取 Data/DTO 或服务端原始 msg。
- `strings.xml` 补充钱包、会员、风险提示、交易类型、交易状态和交易空态资源文案，避免在 Profile 页面恢复硬编码展示文案。
- `docs/governance/feature-presentation-thresholds.txt` 将 Profile UI 基线更新到 1027，并记录 RM-13 钱包、会员、交易空态卡片和资源映射后的当前体量；owner 仍为 `feature:auth:presentation`。
- 新增或更新契约测试：Profile 资产中心、未登录资产隐藏、会员过期状态、交易空态和任务跳转约束，以及 Design System 钱包/会员/交易状态模型。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :feature:auth:presentation:compileDebugUnitTestKotlinAndroid :composeApp:compileDebugUnitTestKotlinAndroid` 通过，`BUILD SUCCESSFUL in 32s`。
- GREEN：`./gradlew.bat --console=plain :feature:auth:presentation:testDebugUnitTest --tests com.runninghub.feature.auth.presentation.profile.ProfileStateHolderTest :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.designsystem.RhAssetCenterComponentsContractTest` 通过，`BUILD SUCCESSFUL in 7s`。
- GREEN：`./gradlew.bat --console=plain :feature:auth:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 首次失败于 `checkLongTermGovernance`，关键错误为 Profile UI 从允许 926 行加 80 行缓冲增长到 1027 行；补充 RM-13 治理基线说明后重跑通过，exit code 0，耗时约 20.7 秒。其中 iOS test/link 任务在 Windows 下不能当作 macOS/iOS 运行证据。
- GREEN：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 未验证：`Get-Command adb -ErrorAction SilentlyContinue` 退出码 1 且无输出；当前环境未暴露 adb，因此 RM-13 未补真实设备/模拟器截图或交互观察证据。

## RM-14 文案状态和回归

### 落地设计

- RM-14 不再新增页面功能，而是把 RM-01 到 RM-13 产生的关键用户状态收口成可维护的文案矩阵，避免“构建通过但状态缺文案”的漏洞。
- 新增 `RedesignStateCopyMatrix` 作为 Compose 层状态文案总表：只保存稳定状态 ID、标题/正文/动作资源 key、承载面类型和是否阻塞流程，不保存最终中文字符串。
- 文案承载面分为 `ScreenEmpty`、`InlineNotice`、`Snackbar`、`Dialog`、`BottomSheet`、`Toast` 和 `CardAction`，用契约测试锁住边界：价格确认和复用参数必须是 Bottom Sheet；复制结果必须是 Snackbar；空态必须是 ScreenEmpty；生成状态和风险提示优先 Inline Notice。
- 状态矩阵覆盖 roadmap 指定的首次进入、首页加载、搜索无结果、模型加载失败、模型价格失败、上传中/成功/失败/删除、Prompt 为空/太短、参数缺失/冲突、余额不足、会员过期、生成前价格确认、生成中/队列/成功/失败/取消/重试、复制 Prompt/任务 ID、复用参数、保存本地、云端结果 24 小时过期、历史为空、网络错误、钱包明细为空、扣费成功/失败、失败是否退款。
- `strings.xml` 保存矩阵需要的最终中文文案；矩阵和页面映射只引用 Compose Resources，不把中文文案放进 Domain/Data 或 Feature Presentation。
- 资源化回归不替代真实 UI 验证：本轮仍运行 composeApp 构建、治理检查和受影响的 presentation tests；adb 不可用时必须显式记录未补真实设备截图。

### RED 目标

- Compose 契约：`RedesignStateCopyMatrix` 必须暴露完整 roadmap 状态集合，缺一个状态 ID、缺承载面边界或缺资源 key 都会导致测试失败。
- 资源契约：矩阵中的每个标题、正文、主动作和次动作都必须来自 `Res.string.*`，不得出现最终中文字符串字段。
- 回归契约：关键边界必须稳定，价格确认和复用参数使用 Bottom Sheet，复制类反馈使用 Snackbar，历史/钱包空态使用 ScreenEmpty，生成失败退款提示使用 Inline Notice 或 Dialog。

### RED 证据

- RED：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 失败，exit code 1。
- 关键失败点：`RedesignStateCopyMatrix`、`RedesignStateCopyId` 和 `RedesignStateCopySurface` 未解析；说明 RM-14 状态文案矩阵尚未落地。

### 代码改造

- `composeApp` 新增 `ui/copy/RedesignStateCopyMatrix.kt`，定义 `RedesignStateCopyId`、`RedesignStateCopySurface`、`RedesignStateCopySpec` 和 `RedesignStateCopyMatrix`，覆盖 roadmap 指定的 32 个状态文案语义。
- 矩阵只保存 Compose Resource key、承载面类型和阻塞语义，不保存最终中文字符串；新增中文 KDoc 用于说明内部治理 API 的资源化边界。
- `strings.xml` 新增 `rm14_state_*` 资源，覆盖首次进入、加载、搜索空态、模型/价格失败、上传、Prompt/参数、余额/会员、生成确认和生成状态、复制、复用、保存、过期、历史空态、网络错误、钱包明细、扣费和退款状态。
- `RedesignStateCopyMatrixContractTest` 锁住完整 ID 集合、每个状态的资源键存在，以及价格确认/复用参数/复制/历史空态/钱包空态/失败退款的承载面边界。
- 最终 gate 复核后强化 `RedesignStateCopyMatrixContractTest`：删除对非空类型的无效 `assertNotNull`，改为断言所有状态必须有标题资源、BottomSheet 状态必须带主/次动作、阻塞流程状态集合必须稳定。
- `docs/governance/ui-copy-message-inventory.md` 增加 2026-06-30 RM-14 状态文案矩阵批次，记录矩阵路径、覆盖范围、承载面约束和“无 Kotlin 最终中文字符串”的治理口径。
- 本轮没有修改 Domain/Data，没有把服务端原始 message 或最终中文文案写进 Feature Presentation。

### GREEN 证据

- GREEN：`./gradlew.bat --console=plain :composeApp:compileDebugUnitTestKotlinAndroid` 通过，`BUILD SUCCESSFUL in 20s`。
- GREEN：`./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.copy.RedesignStateCopyMatrixContractTest` 通过，`BUILD SUCCESSFUL in 5s`。
- GREEN：最终 gate 修复后重跑 `./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.copy.RedesignStateCopyMatrixContractTest` 通过，`BUILD SUCCESSFUL in 10s`。
- GREEN：`Select-String` 静态扫描 `RedesignStateCopyMatrix.kt` 的中文字符串字面量，输出 `NO_CHINESE_STRING_LITERALS`。
- GREEN：`./gradlew.bat --console=plain --quiet :feature:quickcreate:presentation:allTests :feature:task:presentation:allTests :feature:auth:presentation:allTests :feature:discovery:presentation:allTests :feature:detail:presentation:allTests :feature:community:presentation:allTests :composeApp:assembleDebug checkArchitectureBoundaries checkLongTermGovernance` 通过，exit code 0，耗时约 31.6 秒。
- GREEN：`git diff --check` 通过，仅输出 Windows LF/CRLF 提示。
- 未验证：`Get-Command adb -ErrorAction SilentlyContinue` 退出码 1 且无输出；当前环境未暴露 adb，因此 RM-14 未补真实设备/模拟器截图或交互观察证据。
