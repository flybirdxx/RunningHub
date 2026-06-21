# RunningHub KMP 架构迁移验收跟踪

> 唯一验收标准见 `doc/RunningHub-KMP-架构迁移验收标准.md`。
> 本文件只记录当前仓库可追溯状态，避免迁移上下文只存在于聊天记录。

## 当前基线

- 审查基线：`5f62cca0f1b2c0ad24a8ec12e963185d259f3636`
- 当前目标：L1 架构迁移封板
- 当前任务：AC-11 执行双端回归并封板 L1
- 状态文件：`docs/migration/current-state.yaml`

## L1 Gate 状态

| Gate | 状态 | 当前证据或阻塞 |
|---|---|---|
| Gate A 构建系统唯一性 | 完成 | AC-01 已统一 Kotlin JVM 插件来源和 JVM toolchain；AC-02 更新模块文档；AC-07 已删除无人依赖且无源码资源的 `core:designsystem` 空壳模块，并通过 `projects`、Android 构建和 iOS Kotlin 编译验证。 |
| Gate B 模块依赖方向 | 完成 | AC-06 已解除 `feature:quickcreate:data -> shared`；AC-08 已建立 `checkArchitectureBoundaries` 与 `shared-allowlist.txt`，阻止已迁移 Feature 回流 `shared`。本轮继续加固架构门禁：`composeApp/commonMain.dependencies` 不得依赖 Feature Data 实现模块；Domain 和 Presentation 不得导入或声明任何 Feature Data 实现依赖；`feature/*/data` 不得依赖 `composeApp` 或导入 `com.runninghub.app.*`，Data 装配只能留在平台启动 source set。权限模型和权限状态边界已迁移到 `core:storage`；WebApp 任务模型和任务执行状态已迁移到 `core:model`；统一生成历史模型、历史仓库契约和 `WebAppTaskRepository` 已迁移到 `feature:task:domain`；Plaza 模型与仓库契约已迁移到 `feature:community:domain`，Plaza API、DTO、Repository、DI 和兼容测试已迁移到 `feature:community:data`；WebApp 公开目录、搜索、标签树、用户发布列表和详情数据实现已迁移到 `feature:discovery:data`；认证数据实现、DTO、API 封装和 Koin 绑定已迁移到 `feature:auth:data`；WebApp Task API、DTO、Repository、DI 和测试已迁移到 `feature:task:data`。已迁移出的 Auth/Community/Discovery/Task Data 模块均无 `shared` 引用。History、Detail、Plaza 页面和 `AppModule` 不再引用 `shared`，`composeApp/commonMain` 已移除 `project(":shared")`，Android 启动层不再装配 `sharedModule`，`composeApp` 源码和 Gradle 文件已无 `shared` 引用，allowlist 已清零。 |
| Gate C 唯一事实来源 | 完成 | 创作入口已统一到 QuickCreate；旧 `CreateScreenModel` 已从生产 Koin 图移除；登录页和个人中心注销均不再直接替换根导航，根页面切换只由 `SessionManager.state` 驱动；生产 DI 使用 `SessionManager(get())` 注入会话恢复仓库；AC-11 已把生产源码裸 `SessionManager()` 构造、`App.kt` 之外构造 Main/Login 根 Screen 纳入 `checkArchitectureBoundaries`，防止后续绕过会话恢复仓库或新增第二套根导航入口。 |
| Gate D 认证与 401 | 完成 | 401 refresh 成功后的单次重试、并发刷新去重、重试仍 401、logout 竞态和敏感日志扫描已有本地验证；本轮把 Auth logout 的远端失败降级从空 `catch` 改为显式 no-log 兼容处理，避免吞异常模式重新进入生产代码。 |
| Gate E Discovery | 完成 | 领域仓库已收窄；AC-11 已补齐 `CatalogQuery`、`CatalogSort`、`CatalogTagRange` 和 `CatalogError`，Presentation 不再维护目录排序/标签范围协议字符串，也不直接展示目录 `Throwable.message`。ScreenModel 测试覆盖状态语义、旧响应隔离、分页去重、分页失败、刷新保留筛选和错误文案映射。 |
| Gate F QuickCreate | 完成 | Presentation 已拆出多个组件；唯一入口、Data 去 shared、Data 层无裸 `println`、`AuthRepository` 非可空依赖已完成；生成任务状态流和非任务状态 Data 本地兜底均已改为 Domain 稳定错误码，并由 Presentation 映射展示文案；历史/项目分页去重和项目任务状态覆盖已有 StateHolder 测试；上传失败/超时拦截、单一生成任务，成功、失败、取消、超时轮询终态、页面 `onDispose` 取消活跃图片生成状态流和活跃媒体上传、模板应用一致性、生成提交快照漂移，以及计费预览与正式提交请求指纹一致性已有测试覆盖；composeApp commonMain 与 feature/quickcreate 源码静态搜索无裸 `println`。登录态 Tab 网络观察已证明切换 History/QuickCreate 等一级 Tab 后稳定窗口内无持续新增网络请求；Android 退出登录后 Login 根页面空闲观察已证明稳定窗口内无新增请求和 in-flight 请求。macOS iOS Simulator 路径按用户确认以 skipped 风险证据留存。 |
| Gate G 生命周期与导航 | 完成 | AC-10 已把主导航改为只组合当前 Tab，并用 `SaveableStateHolder` 保存可保存 UI 状态；AC-11 已补普通 History 和 QuickCreate History 的 dispose 后轮询停止测试。2026-06-21 Android debug 运行图新增 `NetworkActivityTracker`，logcat 标签 `RunningHubNetwork` 只输出 started/completed/inFlight 聚合计数；AVD 冷启动已看到 `started=0 completed=0 inFlight=0`。本轮继续把 Android debug 观察器改为每秒输出当前计数心跳，即使稳定窗口内没有新请求也会保留连续样本；`checkMigrationScripts` 会校验心跳实现未被删除。`docs/migration/observe-tab-network.ps1` 已沉淀为登录态 Tab 网络观察脚本，本轮新增 `-SelfTest` 离线自检并通过，覆盖 logcat 样本解析、稳定窗口判定和 JSON 证据文件写入读取；脚本支持 `-OutputPath` 产出结构化观察证据，且运行提示已改为 ASCII，避免 Windows PowerShell 按本地代码页执行时损坏中文字符串；本轮继续为脚本新增 `-ForceStopBeforeLaunch`，避免应用已在前台时清空 logcat 后没有新样本；根工程 `checkMigrationScripts` 已把观察脚本存在性、`-SelfTest`、解析/汇总/证据输出函数、`pass_candidate` 输出和 ASCII 运行文本纳入 L1 Gradle 门禁。2026-06-21 已在连接设备 2211133C 上安装 `com.runninghub.app.debug`，UI dump 确认处于登录态主界面并包含 `Discover/Create/Plaza/History/Profile` Tab；随后运行 `observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "logged-in Discover -> History -> Create/QuickCreate -> Plaza -> Profile; final Profile idle on Android debug device 2211133C API 35/Android 16"`，结果为 `sampleCount=116`、`result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`、`stableWindowSampleCount=30`，证明登录态 Tab 切换后稳定窗口内不可见 Tab 没有持续新增网络请求。Profile 注销不再直接操作 Voyager 根栈，根 App 统一根据 `SessionManager` 清空业务页面栈；本轮新增 `AppRootNavigationPolicyTest`，覆盖 Restoring 不建栈、Authenticated 进入 Main、Unauthenticated 进入 Login、Expired 进入 Login 并消费失效标记，证明退出/失效不会映射回业务主栈；本轮继续新增 `MainTabScreenRegistry` 和 `MainTabScreenRegistryTest`，验证同一 Tab 切换返回后仍使用同一个 Screen 实例、每个一级 Tab 拥有不同 Screen、创作 Tab 固定映射到迁移后的 QuickCreate 入口；2026-06-21 继续在 Android debug 设备上从 Profile 执行退出登录，UI dump 确认回到 Login 根页面，`android-logout-network.json` 记录退出完成后空闲 125 秒，结果为 `pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`、`stableWindowSampleCount=29`，证明 Android 侧业务主栈释放后没有持续后台网络请求。macOS iOS Simulator 运行以 skipped 风险证据留存。 |
| Gate H Core 与 Shared 收口 | 完成 | `core:designsystem` 空壳已删除；`core:model` 字段级中文 KDoc 已补齐；`core:common` 新增旧登录协议专用 `md5` 跨平台入口；权限模型、权限状态、权限状态存储边界和 DataStore 平台工厂已从 `shared` 迁移到 `core:storage`；DataStore-backed 凭据、余额缓存、QuickCreate 草稿和权限状态存储实现已迁入 `core:storage`；WebApp 任务提交、输出、历史和任务执行状态模型已迁入 `core:model`；统一生成历史模型、`GenerationHistoryRepository` 和 `WebAppTaskRepository` 已迁入 `feature:task:domain`；Plaza 模型与 `PlazaRepository` 已迁入 `feature:community:domain`，Plaza API/DTO/Repository/DI 和测试已迁入 `feature:community:data`；WebApp 公开目录、搜索、标签树、用户发布列表和详情数据实现已迁入 `feature:discovery:data`；认证、用户资料、会话恢复、个人中心凭据和余额快照 Data 实现已迁入 `feature:auth:data`；WebApp Task API、DTO、Repository、DI 和测试已迁入 `feature:task:data`，`sharedModule` 不再注册这些绑定。Android 生产启动图改为 `androidRuntimeModule` 和各 Feature Data 模块，`composeApp` 不再依赖 `shared`；AC-11 已新增 `shared` baseline 和 ownership 文档，并由架构门禁阻止 `shared` 新增文件。2026-06-21 复核 `settings.gradle.kts` 当前模块图未 include 历史 `androidApp` 目录；该目录仍有旧 `shared` 引用，但不属于当前 L1 生产启动图。`shared` 剩余 Audio、ModelCatalog、ModelInvocation 和旧兼容文件已登记归属与删除条件，属于 L1 后继续瘦身项。 |
| Gate I Android + iOS 编译 | 完成 | Android 构建、lint、既有 AVD installDebug 和冷启动已有本地证据；2026-06-21 `composeApp` debug 构建增加 `.debug` applicationId 后缀，`com.runninghub.app.debug` 已在 `Pixel_10_Pro` AVD 上安装并冷启动成功；随后在连接设备 2211133C 上 `:composeApp:installDebug` 成功，当前 debug 包可在真实登录态设备上启动并采集 Tab 网络证据。AC-09 本地通过 `shared` 与 `composeApp` 的 iOS Simulator Kotlin 编译，并将 iOS framework link 纳入 macOS CI；AC-11 已把 commonMain 禁止 Android/UIKit/Foundation/java.awt 导入和 Android Context/Uri/Application 泄漏纳入 `checkArchitectureBoundaries`，且本地复跑通过；本轮继续复跑 `:composeApp:compileDebugKotlinAndroid`、`:feature:auth:data:compileDebugKotlinAndroid` 和 `:shared:compileDebugKotlinAndroid`，验证生产空 `catch` 修复后 Android 编译仍通过；2026-06-21 复跑 `verifyL1Ios` 通过，但 Windows 本地 `linkDebugFrameworkIosSimulatorArm64` 仍为 `SKIPPED`。本轮发现 `iosApp` 此前只有 README，已补齐 `iosApp/iosApp.xcodeproj`、SwiftUI 壳、Info.plist 和 asset catalog；`composeApp/src/iosMain` 新增 `MainViewController` 与 `iosRuntimeModule`，iOS 启动层现在会装配 Feature Data 模块和 AppModule。`:composeApp:compileKotlinIosSimulatorArm64` 与 `:composeApp:compileDebugKotlinAndroid` 已通过；iOS Xcode build 和 Simulator 运行按用户确认以 skipped 风险证据留存。 |
| Gate J CI | 完成 | AC-08 已新增 Android CI；AC-09 已新增 iOS CI；AC-11 本地门禁已覆盖依赖、commonMain 平台类型、生产 `runBlocking`/`GlobalScope`/空 `catch`、生产 `SessionManager()` 裸构造、根 Screen 构造唯一入口、迁移脚本完整性、`shared` 增长、秘密和构建产物检查。根工程新增 `verifyL1UnitTests`、`verifyL1Android`、`verifyL1Ios`、`verifyL1Local`、`checkL1CiWorkflows` 和 `checkMigrationScripts`，其中 workflow 自检会校验 Android/iOS workflow 文件、PR/push 触发、JDK 17、chmod、runner、Git 跟踪状态、workflow 无未暂存差异和对应 `verifyL1*` 调用；iOS workflow 已新增 `workflow_dispatch` 证据采集入口，手动确认 `simulator_smoke_pass` 和 `smoke_notes` 后会运行 `collect-ios-macos-evidence.sh` 并上传 `ios-macos-link-and-simulator.md` artifact；迁移脚本自检会校验 Gate G 观察脚本仍保留离线解析、稳定窗口判定和 JSON 证据输出能力，校验 Android debug 观察器仍按秒输出计数心跳，校验 GitHub Actions 证据采集脚本仍分别生成 Android/iOS 两个 JSON，校验 macOS/iOS 证据在 `overallResult: pass` 时必须包含 `linkResult: pass`、`xcodebuildResult: pass`、`simulatorSmokeResult: pass`、当前 `headSha` 和非空 Notes；当前 macOS 不可用时允许以 `overallResult: skipped`、非空 `skipReason` 和 `followUpRequired` 留存跳过证据，并要求验收标准源文件和 13 个 `docs/migration` L1 证据文件已被 Git 跟踪且没有未暂存差异；本轮新增 `docs/migration/l1-external-evidence.md` 和独立 `checkL1SealEvidence`，用于最终封板前校验 Android GitHub Actions、iOS GitHub Actions、Android 登录态 Tab 网络观察、Android 退出登录网络观察、macOS iOS link/Simulator 五个外部证据文件已经落盘并对应当前 Git `HEAD`，且仓库没有未暂存差异、已暂存差异仅限这五个外部证据文件；本轮继续加固 `checkL1SealEvidence`，GitHub Actions 证据会解析 JSON 对象或数组，并要求目标 workflow 存在 `status=completed`、`conclusion=success`、非空 `databaseId/headSha/url`，且 Android/iOS CI 的 `headSha` 一致并等于当前 Git `HEAD`；Android 网络观察证据会解析 JSON 对象并要求 `packageName=com.runninghub.app.debug`、非空 `operationNotes`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、`result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、稳定窗口样本数允许 1 个调度抖动样本缺口、稳定窗口请求增量和最大 in-flight 均为 0；macOS iOS link/Simulator 证据必须包含等于当前 Git `HEAD` 的 `headSha`、非空 `capturedAt`、`overallResult` 和非空 Notes；`pass` 证据要求 macOS Darwin host、固定 link/xcodebuild 命令与三项 pass，`skipped` 证据要求跳过原因、后续补验要求和三项 skipped，避免旧提交或 Windows 本地限制被误读为通过证据；所有外部证据还会扫描 Authorization、Cookie、Token、API Key、私钥和请求 Body 等常见敏感形态。Android workflow 调用 `verifyL1Android`，避免根 `test` 空跑；iOS workflow 调用 `verifyL1Ios`。最终证据由 `finalize-l1-external-evidence.ps1` 采集并通过 `checkL1SealEvidence` 校验。 |

Gate J 补证脚本说明：`docs/migration/collect-github-actions-evidence.ps1` 默认只选择已完成且成功的目标 run 并快速失败；需要等待当前 `HEAD` 的远端 Android/iOS workflow 时，必须显式传入 `-Wait`，可配合 `-WaitTimeoutSeconds` 和 `-PollSeconds` 控制等待边界。当前 `HEAD=d7510d8e398134dab92ce5a3ac38d42ff9762df2` 的 `Android CI` run `27896312527` 与 `iOS CI` run `27896312519` 已 completed/success 并已重新落盘；macOS iOS link/xcodebuild/Simulator 因当前环境不可用已按用户要求留存 `overallResult: skipped` 证据，后续具备 macOS 环境时仍可替换为 pass 证据。

## AC 收口顺序

| AC | 任务 | 状态 | 说明 |
|---|---|---|---|
| AC-01 | 统一 Kotlin/JVM/Gradle 配置 | 完成 | 结果记录在 `docs/migration/current-state.yaml` 的 Gate A 历史中。 |
| AC-02 | 更新 AGENTS 与迁移状态文档 | 完成 | 根 `AGENTS.md`、本文件和当前状态文件已更新。 |
| AC-03 | 确定唯一创作入口，冻结另一套实现 | 完成 | 主导航只挂载 `QuickCreateVoyagerScreen`；旧 `CreateScreenModel` 已从生产 Koin 图移除。 |
| AC-04 | 消除登录页与 SessionManager 双重导航 | 完成 | 登录页不再直接 `replaceAll(MainVoyagerScreen())`，根导航只由 `SessionManager.state` 驱动。 |
| AC-05 | 完成 401 刷新后的单次请求重试 | 完成 | `core:network` 已实现 refresh 后单次 retry，并补充并发、失败和 logout 竞态测试。 |
| AC-06 | 解除 `feature:quickcreate:data -> shared` | 完成 | QuickCreate 通用历史适配器迁移到 composeApp 组合层；data 模块移除 `project(":shared")` 并通过源码搜索、data 测试、Android 构建和 iOS Simulator Kotlin 编译验证。 |
| AC-07 | 决定 `core:designsystem` 接入或删除 | 完成 | `core:designsystem` 只有构建文件、没有源码资源且无人依赖；已从 `settings.gradle.kts` 删除并同步 `AGENTS.md` 模块清单。 |
| AC-08 | 增加架构依赖检查和 Android CI | 完成 | 根工程新增 `checkArchitectureBoundaries`，`composeApp` shared 使用纳入 `docs/migration/shared-allowlist.txt`，并新增 `.github/workflows/android-ci.yml`。AC-11 已将 workflow 收敛到 `verifyL1Android`，本地通过架构门禁、所有子项目 Test 类型任务、Android lint 和 debug 构建。 |
| AC-09 | 增加 macOS/iOS CI | 完成 | 新增 `.github/workflows/ios-ci.yml`，在 `macos-latest` 执行 `verifyL1Ios`，覆盖 `checkArchitectureBoundaries`、`:shared:compileKotlinIosSimulatorArm64`、`:composeApp:compileKotlinIosSimulatorArm64` 和 `:composeApp:linkDebugFrameworkIosSimulatorArm64`；两个 CI workflow 均增加 `chmod +x gradlew`。本地 iOS Kotlin 编译通过，framework link 在 Windows 上为 `SKIPPED`，等待 macOS CI 实际运行记录。 |
| AC-10 | 验证 Tab 生命周期和后台任务 | 完成 | `MainScreen` 不再让所有 Tab 常驻 Composition；不可见 Tab 离开 Composition 后释放 `LaunchedEffect`、ScreenModel scope 和页面 Job。策略与剩余运行观察记录在 `docs/migration/tab-lifecycle.md`。 |
| AC-11 | 执行双端回归并封板 L1 | 完成 | 本地回归、AVD Android installDebug、冷启动、登录页 UI dump、真实设备 debug 包安装、登录态 Tab 网络观察、Android 退出登录后 Login 根页面空闲网络观察、远端 Android/iOS CI completed/success、Gate C 会话唯一事实来源收口、Gate E Discovery 验收、Gate F Data/任务状态/历史项目/上传拦截/单一生成任务/轮询终态/计费提交一致性，以及 History/QuickCreate dispose 后轮询停止测试已通过；本轮新增根会话导航策略测试，证明 Expired/Unauthenticated 会进入 Login 根目标且 Expired 会消费失效标记；本轮继续新增主导航 Tab Screen registry 测试，证明同一 Tab 的 Screen 所有者稳定、不同 Tab 的状态所有权隔离且创作 Tab 固定到 QuickCreate；本轮继续新增 QuickCreate 页面销毁测试，证明 onDispose 会取消活跃图片生成状态流和活跃媒体上传；本轮继续把 Gate I commonMain 平台 API 禁用、生产 `runBlocking`、`GlobalScope`、空 `catch` 禁用、生产 `SessionManager()` 裸构造禁用，以及根 Screen 构造唯一入口纳入 `checkArchitectureBoundaries` 并复跑通过；2026-06-21 已通过 `.debug` applicationId 让 debug 包可与正式包并存安装，并新增无敏感信息的 Android debug 网络活动计数日志和 `observe-tab-network.ps1` 脚本用于 Tab 运行观察；`docs/migration/evidence/android-tab-network.json` 已记录 120 秒登录态 `Discover -> History -> Create/QuickCreate -> Plaza -> Profile` 操作路径，稳定窗口内 `started` 无增长且最大 `inFlight=0`；`docs/migration/evidence/android-logout-network.json` 已记录退出登录后 Login 根页面空闲 125 秒，稳定窗口内 `started` 无增长且最大 `inFlight=0`；`collect-ios-macos-evidence.sh` 要求 macOS 上完成 Simulator 登录、退出和 QuickCreate 冒烟后才允许生成 `overallResult: pass` 证据；当前 macOS 环境不可用，已按用户要求留存 `overallResult: skipped`、`skipReason` 和 `followUpRequired` 风险证据；本轮补齐可由 macOS 打开的 `iosApp/iosApp.xcodeproj` 和 SwiftUI 壳，并新增 iOS Koin runtime module，避免 iOS Simulator 启动时缺少数据层绑定；最终封板由 `finalize-l1-external-evidence.ps1` 重新采集当前 HEAD 外部证据，并由 `checkL1SealEvidence` 校验通过。审计记录在 `docs/migration/l1-seal-audit.md`。 |
| AC-12 | 完成安全存储、Release 和生产验收 L2 | 未开始 | 不属于 L1 封板前置完成项。 |

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
