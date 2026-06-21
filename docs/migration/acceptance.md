# RunningHub KMP 架构迁移验收跟踪

> 唯一验收标准见 `doc/RunningHub-KMP-架构迁移验收标准.md`。
> 本文件只记录当前仓库可追溯状态，避免迁移上下文只存在于聊天记录。

## 当前基线

- 审查基线：`5f62cca0f1b2c0ad24a8ec12e963185d259f3636`
- 当前目标：中期架构整理，继续按复核长期建议收敛 Presentation/Data/Core 边界；L1 封板外部证据补采暂停。
- 当前任务：中期架构整理，按复核长期建议逐步拆分 Presentation/Data/Core 边界；暂不补上线 CI、Release 或 L1 封板证据。
- 状态文件：`docs/migration/current-state.yaml`

## L1 Gate 状态

| Gate | 状态 | 当前证据或阻塞 |
|---|---|---|
| Gate A 构建系统唯一性 | 完成 | AC-01 已统一 Kotlin JVM 插件来源和 JVM toolchain；AC-02 更新模块文档；AC-07 已删除无人依赖且无源码资源的 `core:designsystem` 空壳模块，并通过 `projects`、Android 构建和 iOS Kotlin 编译验证。 |
| Gate B 模块依赖方向 | 完成 | AC-06 已解除 `feature:quickcreate:data -> shared`；AC-08 已建立 `checkArchitectureBoundaries` 与 `shared-allowlist.txt`，阻止已迁移 Feature 回流 `shared`。本轮继续加固架构门禁：`composeApp/commonMain.dependencies` 不得依赖 Feature Data 实现模块；Domain 和 Presentation 不得导入或声明任何 Feature Data 实现依赖；`feature/*/data` 不得依赖 `composeApp` 或导入 `com.runninghub.app.*`，Data 装配只能留在平台启动 source set。权限模型和权限状态边界已迁移到 `core:storage`；WebApp 任务模型和任务执行状态已迁移到 `core:model`；统一生成历史模型、历史仓库契约和 `WebAppTaskRepository` 已迁移到 `feature:task:domain`；Plaza 模型与仓库契约已迁移到 `feature:community:domain`，Plaza API、DTO、Repository、DI 和兼容测试已迁移到 `feature:community:data`；WebApp 公开目录、搜索、标签树、用户发布列表和详情数据实现已迁移到 `feature:discovery:data`；认证数据实现、DTO、API 封装和 Koin 绑定已迁移到 `feature:auth:data`；WebApp Task API、DTO、Repository、DI 和测试已迁移到 `feature:task:data`。已迁移出的 Auth/Community/Discovery/Task Data 模块均无 `shared` 引用。History、Detail、Plaza 页面和 `AppModule` 不再引用 `shared`，`composeApp/commonMain` 已移除 `project(":shared")`，Android 启动层不再装配 `sharedModule`，`composeApp` 源码和 Gradle 文件已无 `shared` 引用，allowlist 已清零。 |
| Gate C 唯一事实来源 | 完成 | 创作入口已统一到 QuickCreate；旧 Create 页面和旧创作 ScreenModel 已从 composeApp 删除；登录页和个人中心注销均不再直接替换根导航，根页面切换只由 `SessionManager.state` 驱动；生产 DI 使用 `SessionManager(get())` 注入会话恢复仓库；AC-11 已把生产源码裸 `SessionManager()` 构造、`App.kt` 之外构造 Main/Login 根 Screen 纳入 `checkArchitectureBoundaries`，防止后续绕过会话恢复仓库或新增第二套根导航入口。 |
| Gate D 认证与 401 | 完成 | 401 refresh 成功后的单次重试、并发刷新去重、重试仍 401、logout 竞态和敏感日志扫描已有本地验证；本轮把 Auth logout 的远端失败降级从空 `catch` 改为显式 no-log 兼容处理，避免吞异常模式重新进入生产代码。AC-11 复核补丁已新增 `ApiEnvironment` 并把 `RunningHubApiEnvironment` 改为平台启动层可配置门面；Android 通过 build type 注入的 `RUNNINGHUB_*` BuildConfig 字段覆盖环境，iOS 通过同名进程环境变量覆盖环境，未配置时回退 production。仓库尚未登记真实 staging/dev 地址，后续只需补构建参数或 Xcode scheme，不再改 Data endpoint。 |
| Gate E Discovery | 完成 | 领域仓库已收窄；AC-11 已补齐 `CatalogQuery`、`CatalogSort`、`CatalogTagRange` 和 `CatalogError`，Presentation 不再维护目录排序/标签范围协议字符串，也不直接展示目录 `Throwable.message`。Discovery 主列表 ScreenModel 测试覆盖状态语义、旧响应隔离、分页去重、分页失败、刷新保留筛选和错误文案映射；Search 独立页状态机已迁入 `feature:discovery:presentation`，测试覆盖输入防抖、热门标签保留、分页去重和旧关键词响应隔离。 |
| Gate F QuickCreate | 完成 | Presentation 已拆出多个组件；唯一入口、Data 去 shared、Data 层无裸 `println`、`AuthRepository` 非可空依赖已完成；生成任务状态流和非任务状态 Data 本地兜底均已改为 Domain 稳定错误码，并由 Presentation 映射展示文案；历史/项目分页去重和项目任务状态覆盖已有 StateHolder 测试；上传失败/超时拦截、单一生成任务，成功、失败、取消、超时轮询终态、页面 `onDispose` 取消活跃图片生成状态流和活跃媒体上传、模板应用一致性、生成提交快照漂移，以及计费预览与正式提交请求指纹一致性已有测试覆盖；composeApp commonMain 与 feature/quickcreate 源码静态搜索无裸 `println`。P8 UI 文案资源化本轮继续收缩 QuickCreate 叶子 UI：结果页标题/清空结果入口、灵感页空状态/加载更多入口已迁入 commonMain Compose Resources，对应硬编码文案基线从 1 下调到 0；模型选择面板标题、加载态、空态和关闭按钮无障碍描述也已迁入 commonMain Compose Resources，`QuickCreateModelSelectorContent` 基线从 1 下调到 0；模型名称、分组、副标题、模板标题、标签和任务状态文案仍由 Presentation 状态/文案映射提供，后续应按独立文案端口切片处理。登录态 Tab 网络观察已证明切换 History/QuickCreate 等一级 Tab 后稳定窗口内无持续新增网络请求；Android 退出登录后 Login 根页面空闲观察已证明稳定窗口内无新增请求和 in-flight 请求。macOS iOS Simulator 路径按用户确认以 skipped 风险证据留存。 |
| Gate F2 Community Presentation | 进行中 | 2026-06-22 中期瘦身继续推进：新增 `feature:community:presentation`，把 Plaza 状态机、筛选、分页、短片加载和 fallback 内容逻辑迁出 `composeApp`；`PlazaScreenModel` 现在只负责 Voyager 生命周期适配，测试迁入 `PlazaStateHolderTest`。本轮继续把 Community 工具页的 `CommunityTool`、`CommunityUiState` 和静态工具目录迁入同一 Presentation 模块；`CommunityScreenModel` 现在只负责 Voyager/Koin 适配，工具点击后的真实导航仍保留在应用壳。P8 UI 文案资源化本轮先收缩 Community 页头标题/副标题：新增 commonMain Compose Resources 字符串，`CommunityScreen` 改用 `stringResource`，并把该文件硬编码 UI 文案基线从 2 下调到 0；工具卡片目录文案仍留给后续 Presentation 文案端口或资源化切片。 |
| Gate F3 Task History Presentation | 进行中 | 2026-06-22 中期框架整理继续推进：新增 `feature:task:presentation`，把 History 的状态、列表筛选、详情加载、参数复用、取消任务和普通历史轮询迁出 `composeApp`；`TaskHistoryScreenModel` 现在只负责 Voyager 生命周期适配，测试迁入 `TaskHistoryStateHolderTest`。本轮按中期开发范围验证新模块单测、Android 编译、composeApp Android 编译和 iOS Simulator Kotlin 编译，不补上线 CI/Release 封板证据。 |
| Gate F4 Auth Profile Presentation | 进行中 | 2026-06-22 中期框架整理继续推进：新增 `feature:auth:presentation`，把 Profile 页面状态、资料刷新、凭据绑定、解绑和注销状态清理迁出 `composeApp`；`ProfileScreenModel` 现在只负责 Voyager 生命周期适配，测试迁入 `ProfileStateHolderTest`。本轮继续把 CreatorProfile 的 `CreatorProfileUiState`、用户资料/关注状态/作品列表并发加载、重复加载去重、关注切换和粉丝数本地修正迁入 `feature:auth:presentation` 的 `creator` 边界；`CreatorProfileScreenModel` 现在只负责 Voyager 生命周期适配。本轮按中期开发范围验证 Auth Presentation 单测、Android/iOS Kotlin 编译、composeApp Android/iOS Kotlin 编译、架构边界和长期治理门禁，不补上线 CI/Release 封板证据。 |
| Gate F5 Discovery Presentation | 进行中 | 2026-06-22 中期框架整理继续推进：新增 `feature:discovery:presentation`，把 Discovery 的 UiState、排序展示名、目录错误文案、分类/排序/分页/搜索状态机和旧响应隔离迁出 `composeApp`；`DiscoveryScreenModel` 现在只负责 Voyager 生命周期适配，测试迁入 `DiscoveryStateHolderTest`。本轮继续把独立 Search 页的 `SearchUiState`、热门标签加载、输入防抖搜索、分页、错误文案映射和旧响应隔离迁入同一 Presentation 模块；`SearchScreenModel` 现在只负责 Voyager 生命周期适配。本轮按中期开发范围验证新模块单测、Android 编译、composeApp Android 编译和 iOS Simulator Kotlin 编译，不补上线 CI/Release 封板证据。 |
| Gate F6 Auth Login Presentation | 进行中 | 2026-06-22 中期框架整理继续推进：把 Login 的 `LoginUiState`、短信验证码发送、图形验证码 token 重试、短信/密码登录、倒计时和错误文案映射迁入 `feature:auth:presentation`；`LoginScreenModel` 现在只负责 Voyager 生命周期适配，`SmsCaptchaDialog` expect/actual、TAC HTML 和回调解析继续保留在 `composeApp` 平台边界。本轮按中期开发范围验证 Auth Presentation 单测、Android/iOS Kotlin 编译、验证码保留测试、Android debug 构建、架构边界和长期治理门禁，不补上线 CI/Release 封板证据。 |
| Gate F7 AppDetail Presentation | 进行中 | 2026-06-22 中期框架整理继续推进：新增 `feature:detail:presentation`，把 AppDetail 的 `AppDetailUiState`、媒体选择/上传状态、详情加载、任务提交、输出轮询和失败文案映射迁出 `composeApp`；`AppDetailScreenModel` 现在只负责 Voyager 生命周期、平台媒体读取适配和 UI 事件转发，AppDetail 页面继续保留权限、媒体选择器和渲染逻辑。本轮新增 `AppDetailStateHolderTest` 并保留 composeApp 适配测试，按开发中期范围验证新模块单测、composeApp 适配单测、Android Kotlin 编译和 Detail Presentation iOS Simulator Kotlin 编译；不补上线 CI/Release/L1 封板证据。 |
| Gate G 生命周期与导航 | 完成 | AC-10 已把主导航改为只组合当前 Tab，并用 `SaveableStateHolder` 保存可保存 UI 状态；AC-11 已补普通 History 和 QuickCreate History 的 dispose 后轮询停止测试。2026-06-21 Android debug 运行图新增 `NetworkActivityTracker`，logcat 标签 `RunningHubNetwork` 只输出 started/completed/inFlight 聚合计数；AVD 冷启动已看到 `started=0 completed=0 inFlight=0`。本轮继续把 Android debug 观察器改为每秒输出当前计数心跳，即使稳定窗口内没有新请求也会保留连续样本；`checkMigrationScripts` 会校验心跳实现未被删除。`docs/migration/observe-tab-network.ps1` 已沉淀为登录态 Tab 网络观察脚本，本轮新增 `-SelfTest` 离线自检并通过，覆盖 logcat 样本解析、稳定窗口判定和 JSON 证据文件写入读取；脚本支持 `-OutputPath` 产出结构化观察证据，且运行提示已改为 ASCII，避免 Windows PowerShell 按本地代码页执行时损坏中文字符串；本轮继续为脚本新增 `-ForceStopBeforeLaunch`，避免应用已在前台时清空 logcat 后没有新样本；根工程 `checkMigrationScripts` 已把观察脚本存在性、`-SelfTest`、解析/汇总/证据输出函数、`pass_candidate` 输出和 ASCII 运行文本纳入 L1 Gradle 门禁。2026-06-21 已在连接设备 2211133C 上安装 `com.runninghub.app.debug`，UI dump 确认处于登录态主界面并包含 `Discover/Create/Plaza/History/Profile` Tab；随后运行 `observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "logged-in Discover -> History -> Create/QuickCreate -> Plaza -> Profile; final Profile idle on Android debug device 2211133C API 35/Android 16"`，结果为 `sampleCount=116`、`result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`、`stableWindowSampleCount=30`，证明登录态 Tab 切换后稳定窗口内不可见 Tab 没有持续新增网络请求。Profile 注销不再直接操作 Voyager 根栈，根 App 统一根据 `SessionManager` 清空业务页面栈；本轮新增 `AppRootNavigationPolicyTest`，覆盖 Restoring 不建栈、Authenticated 进入 Main、Unauthenticated 进入 Login、Expired 进入 Login 并消费失效标记，证明退出/失效不会映射回业务主栈；本轮继续新增 `MainTabScreenRegistry` 和 `MainTabScreenRegistryTest`，验证同一 Tab 切换返回后仍使用同一个 Screen 实例、每个一级 Tab 拥有不同 Screen、创作 Tab 固定映射到迁移后的 QuickCreate 入口；2026-06-21 继续在 Android debug 设备上从 Profile 执行退出登录，UI dump 确认回到 Login 根页面，`android-logout-network.json` 记录退出完成后空闲 125 秒，结果为 `pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`、`stableWindowSampleCount=29`，证明 Android 侧业务主栈释放后没有持续后台网络请求。macOS iOS Simulator 运行以 skipped 风险证据留存。 |
| Gate H Core 与 Shared 收口 | 完成 | `core:designsystem` 空壳已删除；`core:model` 字段级中文 KDoc 已补齐；`core:common` 新增旧登录协议专用 `md5` 跨平台入口；权限模型、权限状态、权限状态存储边界和 DataStore 平台工厂已从 `shared` 迁移到 `core:storage`；DataStore-backed 余额缓存、QuickCreate 草稿和权限状态存储实现已迁入 `core:storage`；AC-11 复核补丁已把生产 `CredentialStore` 切换为 Android Keystore backed 密文存储与 iOS Keychain，并通过 `MigratingCredentialStore` 懒迁移旧 DataStore 凭据，架构门禁会阻止 RuntimeModule 退回普通 Preferences 绑定；WebApp 任务提交、输出、历史和任务执行状态模型已迁入 `core:model`；统一生成历史模型、`GenerationHistoryRepository` 和 `WebAppTaskRepository` 已迁入 `feature:task:domain`；Plaza 模型与 `PlazaRepository` 已迁入 `feature:community:domain`，Plaza API/DTO/Repository/DI 和测试已迁入 `feature:community:data`；WebApp 公开目录、搜索、标签树、用户发布列表和详情数据实现已迁入 `feature:discovery:data`；认证、用户资料、会话恢复、个人中心凭据和余额快照 Data 实现已迁入 `feature:auth:data`，shared 中旧 RunningHubApi、Auth/User DTO、Auth/User/Session/ProfileCredential/BalanceSnapshot Repository 和对应自测已删除；WebApp Task API、DTO、Repository、DI 和测试已迁入 `feature:task:data`，`sharedModule` 不再注册这些绑定。Android 生产启动图改为 `androidRuntimeModule` 和各 Feature Data 模块，`composeApp` 不再依赖 `shared`；AC-11 已新增 `shared` baseline 和 ownership 文档，并由架构门禁阻止 `shared` 新增文件。2026-06-22 中期瘦身继续推进：历史 `androidApp` 跟踪源码已删除；Audio 领域模型和仓库契约已迁入 `feature:audio:domain`，Audio API/DTO/Repository/DI 和测试已迁入 `feature:audio:data`；标准模型目录模型、调用模型和仓库契约已迁入 `feature:model:domain`，标准模型 API/DTO/Repository/DI 和测试已迁入 `feature:model:data`；User/WebApp/Tag/PageData/AppDetail 的 shared typealias 兼容层已删除，shared 不再保留业务 Domain 或业务 Data 实现。 |
| Gate I Android + iOS 编译 | 完成 | Android 构建、lint、既有 AVD installDebug 和冷启动已有本地证据；2026-06-21 `composeApp` debug 构建增加 `.debug` applicationId 后缀，`com.runninghub.app.debug` 已在 `Pixel_10_Pro` AVD 上安装并冷启动成功；随后在连接设备 2211133C 上 `:composeApp:installDebug` 成功，当前 debug 包可在真实登录态设备上启动并采集 Tab 网络证据。AC-09 本地通过 `shared` 与 `composeApp` 的 iOS Simulator Kotlin 编译，并将 iOS framework link 纳入 macOS CI；AC-11 已把 commonMain 禁止 Android/UIKit/Foundation/java.awt 导入和 Android Context/Uri/Application 泄漏纳入 `checkArchitectureBoundaries`，且本地复跑通过；本轮继续复跑 `:composeApp:compileDebugKotlinAndroid`、`:feature:auth:data:compileDebugKotlinAndroid` 和 `:shared:compileDebugKotlinAndroid`，验证生产空 `catch` 修复后 Android 编译仍通过；2026-06-21 复跑 `verifyL1Ios` 通过，但 Windows 本地 `linkDebugFrameworkIosSimulatorArm64` 仍为 `SKIPPED`。本轮发现 `iosApp` 此前只有 README，已补齐 `iosApp/iosApp.xcodeproj`、SwiftUI 壳、Info.plist 和 asset catalog；`composeApp/src/iosMain` 新增 `MainViewController` 与 `iosRuntimeModule`，iOS 启动层现在会装配 Feature Data 模块和 AppModule。`:composeApp:compileKotlinIosSimulatorArm64` 与 `:composeApp:compileDebugKotlinAndroid` 已通过；本轮按复核建议开启 Android release R8 minify 和 resource shrink，更新迁移后的 ProGuard 规则，并通过 `:composeApp:assembleRelease` 验证；`verifyL1Android` 已纳入 `:composeApp:assembleRelease` 防止 release 构建治理回退。iOS Xcode build 和 Simulator 运行按用户确认以 skipped 风险证据留存。 |
| Gate J CI | 待当前 HEAD 外部证据 | AC-08 已新增 Android CI；AC-09 已新增 iOS CI；AC-11 本地门禁已覆盖依赖、commonMain 平台类型、生产 `runBlocking`/`GlobalScope`/空 `catch`、生产 `SessionManager()` 裸构造、根 Screen 构造唯一入口、迁移脚本完整性、`shared` 增长、秘密和构建产物检查。根工程新增 `verifyL1UnitTests`、`verifyL1Android`、`verifyL1Ios`、`verifyL1Local`、`checkL1CiWorkflows` 和 `checkMigrationScripts`；本轮按复核长期建议新增 `runninghub.long-term-governance` build-logic 插件与 `checkLongTermGovernance`，把 `ARCHITECTURE.md`、`DEVELOPMENT.md`、ADR、迁移归档边界、日志/契约测试/文案/性能/发布治理文档、`composeApp` 超大文件新增风险、无 Presentation 模块的大体量 UI Feature 增长、认证头精确 Host 白名单防回退、iOS 权限/媒体选择防回退、短信图形验证码 Web 容器防回退、生产日志敏感凭据语义扫描、API/数据库契约测试基线、硬编码 UI 文案基线、性能指标目标表和发布就绪清单纳入本地门禁。`verifyL1Android` 和 `verifyL1Ios` 已接入该长期治理门禁；`verifyL1Android` 现在同时覆盖 Android lint、debug 构建和开启 R8/资源压缩后的 release 构建。`checkArchitectureBoundaries` 已纳入环境注入防退化检查，阻止 `ApiEnvironment`、平台 runtime module 环境绑定或运行期 URL 配置能力被删除。本轮按复核建议补强 iOS CI：`verifyL1Ios` 前会先对 iOS 迁移脚本执行 `bash -n` 和 `--self-test`，随后在 macOS runner 上显式运行 `:core:network:iosSimulatorArm64Test`、`:feature:auth:domain:iosSimulatorArm64Test`、`:feature:auth:data:iosSimulatorArm64Test` 和 `:feature:quickcreate:presentation:iosSimulatorArm64Test`，再执行 `xcodebuild` Debug 模拟器构建、安装到临时 iOS Simulator 并启动 bundle，输出 `.xcresult` 与 `simulator-launch-smoke.txt`；Android/iOS workflow 均上传 Gradle reports/test-results artifact。iOS workflow 已从浮动 `macos-latest` 固定到 `macos-15`；Android/iOS workflow 均新增同分支 `concurrency.cancel-in-progress` 和每周 UTC 定时回归；新增 `.github/dependabot.yml` 以每周巡检 Gradle 与 GitHub Actions 依赖；新增 `.github/workflows/dependency-submission.yml`，在 push/schedule/workflow_dispatch 上提交 Gradle dependency graph，便于 GitHub Dependency Graph/Dependabot Alerts 覆盖传递依赖；新增 `.github/pull_request_template.md` 固化 PR 交付信息，并要求 PR 明确 Android/iOS CI 状态和 L1 封板证据状态。`checkL1CiWorkflows` 会校验迁移脚本语法/自检、Native Test、Xcode build、Simulator launch smoke、报告 artifact、固定 macOS runner、concurrency、schedule、Dependabot、Dependency Submission 和 PR 模板配置没有被移除。GitHub Branch Protection 的 required checks 仍需仓库管理员在远端设置，本地补丁只能固化 workflow、PR 模板和防退化门禁。`checkL1SealEvidence` 是最终封板入口，要求 Android GitHub Actions、iOS GitHub Actions、Android 登录态 Tab 网络观察、Android 退出登录网络观察、macOS iOS link/Simulator 五个外部证据文件全部对应当前 Git `HEAD`，且仓库没有未暂存差异、已暂存差异仅限这些外部证据文件。当前工作区仍有已暂存的非证据修复补丁，且 Android/iOS/macOS 证据仍绑定旧 HEAD `85f134d23ac58768e8a40c3172f3fbb34ca90699`，因此 `checkL1SealEvidence` 按预期失败；提交并推送本轮补丁后，必须重新采集新 HEAD 的外部证据并复跑通过，才可宣称 L1 封板。 |

Gate J 补证脚本说明：`docs/migration/collect-github-actions-evidence.ps1` 默认只选择已完成且成功的目标 run 并快速失败；需要等待当前 `HEAD` 的远端 Android/iOS workflow 时，必须显式传入 `-Wait`，可配合 `-WaitTimeoutSeconds` 和 `-PollSeconds` 控制等待边界。当前工作区存在已暂存的非证据修复补丁，尚不能复用旧提交 `d7510d8e398134dab92ce5a3ac38d42ff9762df2` 或 `85f134d23ac58768e8a40c3172f3fbb34ca90699` 的证据；本轮补丁提交并推送后，需按新 `HEAD` 重新采集 Android/iOS CI 证据，并重新生成或明确留存 macOS iOS link/xcodebuild/Simulator 证据。

Gate J 长期治理补充：`checkLongTermGovernance` 已继续纳入 Android Keystore、iOS Keychain、
`MigratingCredentialStore` 和双端 runtime module 凭据绑定检查，防止 `CredentialStore`
退回普通 Preferences；旧凭据只能按字段懒迁移，不能误删余额缓存或快捷创作草稿。
本轮继续把已迁移 Feature Data 的服务端错误消息收口扩展为全量门禁：`feature/*/data`
不得把服务端 `msg/message` 直接作为异常消息或 QuickCreate 任务状态错误传播；
Task Data、QuickCreate Data、QuickCreate Presentation 和 composeApp 相关测试已覆盖该边界。
P8 UI 文案资源化按中期节奏继续收缩：AppBarLogo、AppCard、CollapsibleSection、
ErrorState、SmartAsyncImage、TaskProgressIndicator、ImageUploadButton 和 PermissionBottomSheet
的静态文案、格式化单位/进度、状态徽标、步骤标签、权限弹窗操作文案和无障碍描述已迁入 commonMain Compose Resources，
对应硬编码文案基线均下调为 0；
QuickCreateModelSelectorContent 的面板标题、加载态、空态和关闭按钮无障碍描述也已迁入
commonMain Compose Resources，基线从 1 下调为 0。
MainScreen 的一级 Tab 标签、guest banner 文案和余额角标符号也已迁入
commonMain Compose Resources，基线从 3 下调为 0。
CreatorProfileScreen 的标题兜底、返回无障碍描述、作品区标题、空态、关注按钮、
用户兜底名、统计标签、使用/点赞格式和指标分隔符也已迁入 commonMain Compose Resources，
基线从 5 下调为 0；CreatorProfileStateHolder 的错误状态文案仍保留给后续 Presentation
文案端口治理。
ProfileScreen 的个人中心默认用户名、设置/头像/会员/菜单/未登录空态等静态标签和无障碍描述，
以及会员剩余单位/到期格式也已迁入 commonMain Compose Resources，硬编码 UI 文案基线从 2 下调为 0；
ProfileScreen 仍是迁移期留在 composeApp 的 UI 壳，体量基线同步记录本次资源导入增长，
SettingsDialog 和 ProfileStateHolder 文案留给后续独立切片。本轮不补 CI、Release 或 L1 封板证据。

## AC 收口顺序

| AC | 任务 | 状态 | 说明 |
|---|---|---|---|
| AC-01 | 统一 Kotlin/JVM/Gradle 配置 | 完成 | 结果记录在 `docs/migration/current-state.yaml` 的 Gate A 历史中。 |
| AC-02 | 更新 AGENTS 与迁移状态文档 | 完成 | 根 `AGENTS.md`、本文件和当前状态文件已更新。 |
| AC-03 | 确定唯一创作入口，冻结另一套实现 | 完成 | 主导航只挂载 `QuickCreateVoyagerScreen`；旧 Create 页面、旧创作 ScreenModel 和对应旧测试已从 composeApp 删除。 |
| AC-04 | 消除登录页与 SessionManager 双重导航 | 完成 | 登录页不再直接 `replaceAll(MainVoyagerScreen())`，根导航只由 `SessionManager.state` 驱动。 |
| AC-05 | 完成 401 刷新后的单次请求重试 | 完成 | `core:network` 已实现 refresh 后单次 retry，并补充并发、失败和 logout 竞态测试。 |
| AC-06 | 解除 `feature:quickcreate:data -> shared` | 完成 | QuickCreate 通用历史适配器迁移到 composeApp 组合层；data 模块移除 `project(":shared")` 并通过源码搜索、data 测试、Android 构建和 iOS Simulator Kotlin 编译验证。 |
| AC-07 | 决定 `core:designsystem` 接入或删除 | 完成 | `core:designsystem` 只有构建文件、没有源码资源且无人依赖；已从 `settings.gradle.kts` 删除并同步 `AGENTS.md` 模块清单。 |
| AC-08 | 增加架构依赖检查和 Android CI | 完成 | 根工程新增 `checkArchitectureBoundaries`，`composeApp` shared 使用纳入 `docs/migration/shared-allowlist.txt`，并新增 `.github/workflows/android-ci.yml`。AC-11 已将 workflow 收敛到 `verifyL1Android`，本地通过架构门禁、所有子项目 Test 类型任务、Android lint 和 debug 构建。 |
| AC-09 | 增加 macOS/iOS CI | 完成 | 新增 `.github/workflows/ios-ci.yml`，在固定 `macos-15` runner 执行 `verifyL1Ios`，覆盖 `checkArchitectureBoundaries`、`:shared:compileKotlinIosSimulatorArm64`、`:composeApp:compileKotlinIosSimulatorArm64` 和 `:composeApp:linkDebugFrameworkIosSimulatorArm64`；本轮继续在 workflow 中显式加入 iOS Native 单元测试步骤，覆盖 `core:network`、`feature:auth:domain`、`feature:auth:data` 和 `feature:quickcreate:presentation` 的 `iosSimulatorArm64Test`，并新增 `xcodebuild` Debug 模拟器构建、临时 Simulator launch smoke 和 `.xcresult` artifact。两个 CI workflow 均增加 `chmod +x gradlew`。本地 iOS Kotlin 编译通过，framework link 在 Windows 上为 `SKIPPED`，等待 macOS CI 实际运行记录。 |
| AC-10 | 验证 Tab 生命周期和后台任务 | 完成 | `MainScreen` 不再让所有 Tab 常驻 Composition；不可见 Tab 离开 Composition 后释放 `LaunchedEffect`、ScreenModel scope 和页面 Job。策略与剩余运行观察记录在 `docs/migration/tab-lifecycle.md`。 |
| AC-11 | 执行双端回归并封板 L1 | 待补证 | 本地回归、Android lint/debug 构建、iOS Simulator Kotlin 编译、架构门禁、登录态 Tab 网络观察、Android 退出登录后 Login 根页面空闲网络观察，以及多项 ScreenModel/StateHolder/Repository 测试已通过；本轮复核后继续修复认证 Host 精确白名单、iOS 权限/媒体选择占位、Android/iOS 图形验证码回调一致性和相关测试，补强 TAC 脚本加载失败/超时的可重试降级状态，把 Token 刷新响应从正则解析改为 kotlinx.serialization DTO 解码，把旧 Create 兼容实现从 composeApp 删除并纳入 `checkArchitectureBoundaries` 防回归检查，并把生产 `CredentialStore` 切换到 Android Keystore / iOS Keychain backed 安全存储，旧 DataStore 凭据通过迁移包装器逐项迁移清理。当前未完成项是封板证据：补丁仍处于 Git 索引中，远端 Android/iOS CI 与 macOS iOS link/Simulator 证据尚未绑定提交后的新 `HEAD`，`checkL1SealEvidence` 因此按预期失败。 |
| AC-12 | 完成安全存储、Release 和生产验收 L2 | 部分前置完成 | 平台安全凭据存储代码已在 AC-11 复核补丁中接入；Android release minify/resource shrink 和 ProGuard 规则已在 AC-11 复核补丁中接入并通过 `:composeApp:assembleRelease`；`iosApp/iosApp/PrivacyInfo.xcprivacy` 已加入 Xcode 资源并纳入本地门禁。L2 仍需真机 Keychain/Keystore 升级回归、备份/卸载/系统还原风险检查、正式签名、Release 安装回归、App Store 隐私表单人工确认和生产验收。 |

## 本轮补充说明

2026-06-21 继续加固 iOS 运行验收前置条件：`iosApp` 已补齐 shared
`RunningHub` scheme，Swift 入口改为通过 `IosRuntimeModuleKt.startRunningHubKoin()`
启动 iOS Koin 运行期装配。`collect-ios-macos-evidence.sh` 现在会在 macOS 上先执行
Compose framework link，再执行
`xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO`。
最终 L1 证据在 macOS 可用时应包含 `overallResult: pass`、`linkResult: pass`、
`xcodebuildResult: pass` 和 `simulatorSmokeResult: pass`；当前 macOS 环境不可用时，
按用户要求留存 `overallResult: skipped`、`skipReason` 和 `followUpRequired`，
后续具备 macOS 环境后仍可替换为真实 pass 证据。
也可以手动触发 `iOS CI` 的 `workflow_dispatch` 证据模式，提供
`simulator_smoke_pass=true` 和 `smoke_notes`，由 macOS runner 运行同一脚本并上传
`ios-macos-link-and-simulator.md` artifact；可运行
`docs/migration/download-ios-macos-evidence.ps1 -RunId <run id>` 下载并写入
`docs/migration/evidence/`，然后加入 Git 索引。

2026-06-21 继续补齐 Android 退出登录运行观察：在连接设备上从 Profile 执行退出登录后，
UI dump 确认根页面回到 Login；随后生成
`docs/migration/evidence/android-logout-network.json`，记录退出完成后 Login 根页面空闲
125 秒，30 秒稳定窗口内请求增量为 0、最大 in-flight 为 0。该证据已纳入
`checkL1SealEvidence` 和 `checkMigrationScripts`。macOS iOS
link/xcodebuild/Simulator 当前以 skipped Markdown 证据留存，不能被解读为真实 iOS 运行通过。

2026-06-21 在 macOS skipped 证据和门禁规则暂存后复跑
`./gradlew.bat --console=plain checkArchitectureBoundaries verifyL1Ios` 通过；
Windows 本地 `:composeApp:linkDebugFrameworkIosSimulatorArm64` 仍为 `SKIPPED`，
真实 Xcode build 和 Simulator 冒烟留待 macOS 环境补验。同轮复跑
`./gradlew.bat --console=plain verifyL1Android` 通过，覆盖 Android 单元测试、
lint 和 debug 构建。提交后补证流程会对最终 `HEAD` 重新采集外部证据，并由
`checkL1SealEvidence` 作为 L1 封板判定。

2026-06-21 新增 `docs/migration/finalize-l1-external-evidence.ps1` 作为提交后补证总入口。
该脚本会拒绝带有 staged 或 unstaged 非证据改动的工作区，再按当前 `HEAD`
调用 GitHub Actions 证据采集脚本，并根据 `-IosEvidenceMode` 生成 skipped
证据、请求真实 iOS workflow_dispatch 证据或下载既有 artifact。脚本 `-SelfTest`
已覆盖 skipped 证据生成和必填字段保护。
本轮继续加固该脚本：它会拒绝未跟踪文件，并要求显式 `-HeadSha` 与当前 Git
`HEAD` 一致，避免在错误 checkout 上采集旧提交或遗漏本地新增文件。
本轮继续补齐提交后补证收尾能力：`-StageEvidence` 只暂存
`github-actions-android.json`、`github-actions-ios.json`、`android-tab-network.json`、
`android-logout-network.json` 和 `ios-macos-link-and-simulator.md` 五个外部证据文件；
`-RunSealCheck` 会在采集后执行 `checkL1SealEvidence`。脚本自检已覆盖该暂存清单。
随后复跑 `checkL1SealEvidence`，任务按预期失败；当前失败项只剩 staged 的非证据
门禁、脚本和文档补丁尚未提交，未再报告外部证据字段或 skipped 证据格式问题。
提交并推送这些非证据补丁后，下一轮应等待新 `HEAD` 的远端 Android/iOS CI 完成，
再运行
`powershell -NoProfile -ExecutionPolicy Bypass -File docs\migration\finalize-l1-external-evidence.ps1 -Wait -WaitTimeoutSeconds 1800 -PollSeconds 30 -IosEvidenceMode skip -StageEvidence -RunSealCheck`。
该命令只应留下五个外部证据文件作为 staged 差异。

2026-06-21 继续复跑 `./gradlew.bat --console=plain verifyL1Local` 通过，确认当前暂存补丁
在 Windows 可执行范围内同时通过 `verifyL1Android` 与 `verifyL1Ios`。其中
`linkDebugFrameworkIosSimulatorArm64` 仍为 Windows 平台下的 `SKIPPED`，不能替代 macOS
真实 pass 证据。

## 执行规则

- 每次只处理一个 `AC-*` 任务。
- 已完成 Gate 不重复实现，除非有明确回归证据。
- 每个 AC 结束时必须更新修改文件、验证命令、实际结果、未完成项和下一步唯一动作。
- 未验证项不得标记通过。
