# L1 架构封板审计

审计日期：2026-06-21
审计目标：根据 `doc/RunningHub-KMP-架构迁移验收标准.md` 判断 L1 是否可以封板。

## 结论

当前尚不能封板 L1。

本轮已经补齐本地可自动化的构建、依赖、安全、Android 安装启动、Gate E Discovery 证据，
并继续补齐 Gate F QuickCreate Data 的无裸日志、认证刷新依赖、任务状态错误码、历史/项目分页、
上传失败拦截、单一生成任务、成功/失败/取消/超时轮询终态、模板应用一致性和计费提交一致性证据；
本轮进一步把认证、用户资料、会话恢复、个人中心凭据和余额快照的数据实现迁入 `feature:auth:data`；
并把 Plaza API、DTO、Repository、DI 和兼容测试迁入 `feature:community:data`；
同时把 WebApp 公开目录、搜索、标签树、用户发布列表和详情的数据实现迁入 `feature:discovery:data`；
并把 WebApp 任务详情、提交、输出、上传和历史数据实现迁入 `feature:task:data`；
本轮继续把 Android 生产启动图从 `sharedModule` 切到 `androidRuntimeModule`，
并把 DataStore-backed 余额、QuickCreate 草稿和权限状态存储实现迁入 `core:storage`，
生产 `CredentialStore` 切到 Android Keystore backed 密文存储与 iOS Keychain，
使 `composeApp` 对 `shared` 的 allowlist 归零；
macOS runner 或 macOS 开发机生成的 iOS framework link、Xcode build 和 iOS Simulator
运行验收当前按用户说明留存 `overallResult: skipped` 证据；该记录不是通过证明，
但已作为 L1 风险记录被 `checkL1SealEvidence` 接受。后续具备 macOS 环境后仍可补验并替换为
`overallResult: pass`。2026-06-21
继续发现仓库中的 `iosApp` 此前只有 README，没有可打开的 Xcode 工程；
本轮已补齐 `iosApp/iosApp.xcodeproj`、SwiftUI 壳、Info.plist 和 asset catalog，
并在 `composeApp/src/iosMain` 增加 `MainViewController` 与 `iosRuntimeModule`，
使 iOS Simulator 验收具备真实启动目标。2026-06-21
已将 debug 构建改为独立 applicationId，使其可与用户已安装的正式包并存，并在
Pixel_10_Pro AVD 上完成安装和冷启动；随后在连接实体设备 2211133C 上
`:composeApp:installDebug` 成功，并确认登录态主界面包含 Discover/Create/Plaza/History/Profile
一级 Tab。本轮新增 Android debug
网络活动计数通道，logcat 只输出 `started/completed/inFlight` 聚合数量，不输出 URL、
Header、Body 或凭据；AVD 冷启动已证明 `RunningHubNetwork` 标签可用。本轮已使用
`docs/migration/observe-tab-network.ps1` 在登录态设备上记录 Tab 切换后的稳定窗口网络计数：
`docs/migration/evidence/android-tab-network.json` 中 `durationSeconds=120`、
`stableWindowSeconds=30`、`sampleCount=116`、`result=pass_candidate`、
`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
本轮继续使用同一观察器验证退出登录后的业务页面释放：从 Profile 触发退出登录并确认 UI 回到
Login 根页面后，`docs/migration/evidence/android-logout-network.json` 记录空闲 125 秒，
30 秒稳定窗口内 `stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`、
`stableWindowSampleCount=29`，证明 Android 侧退出后没有持续后台网络请求。
本轮继续加固该观察脚本：新增 `-SelfTest` 离线自检，覆盖 `RunningHubNetwork` 日志样本解析、
稳定窗口 `pass_candidate` 判定和 JSON 证据文件写入读取；脚本支持 `-OutputPath` 输出结构化
观察证据。脚本执行提示改为 ASCII，避免 Windows PowerShell 按本地代码页读取无 BOM UTF-8
时损坏中文字符串。PowerShell 语法检查和 `-SelfTest` 均已通过。
本轮进一步新增 `checkMigrationScripts` Gradle 门禁，用纯文本方式检查观察脚本存在、
保留 `-SelfTest`、`-OutputPath`、解析/汇总/证据输出函数、`pass_candidate` 输出和
`RunningHubNetwork` 标签，并禁止脚本运行文本重新出现非 ASCII 字符；该任务已接入
`verifyL1Android` 与 `verifyL1Ios`，dry-run 任务图确认两个入口均包含该任务。
本轮继续收紧 `checkL1CiWorkflows`：除了检查 workflow 已被 Git 跟踪，还会拒绝
`.github/workflows` 下的未暂存差异，避免本地工作区文件和后续提交到远端实际运行的
workflow 内容不一致。当前 `git status --short .github/workflows` 仅显示两个新增
workflow 已加入索引，无未暂存修改。
本轮同时把 L1 自动化门禁拆为 Android 与 iOS 两个 CI 可复用入口：Android workflow
调用 `verifyL1Android`，不再依赖可能空跑的根 `test`；iOS workflow 调用
`verifyL1Ios`，由 macOS runner 负责产生真实 framework link 记录。2026-06-21
复查 `.github/workflows/android-ci.yml` 和 `.github/workflows/ios-ci.yml` 均存在，
且本地复跑 `verifyL1Android`、`verifyL1Ios` 均通过；`.github/workflows`
已加入本地 Git 索引。当时 `HEAD=d7510d8e398134dab92ce5a3ac38d42ff9762df2` 的远端
`Android CI` run `27896312527` 与 `iOS CI` run `27896312519` 均已 completed/success，
对应运行证据已落盘到
`docs/migration/evidence/github-actions-android.json` 和
`docs/migration/evidence/github-actions-ios.json`。
本轮继续把完整架构迁移补丁加入 Git 索引，范围覆盖 `composeApp`、`core`、`feature`、
`shared`、Gradle 配置、workflow、验收标准和迁移证据文件；`git diff --name-only`
为空，说明当前没有未暂存残留。当前补丁尚未提交，因此远端 CI 证据仍只能证明旧 `HEAD`。
完整补丁进入索引后复跑 `git diff --cached --check`、
`checkMigrationScripts`、`checkL1CiWorkflows`、`checkArchitectureBoundaries`、
`verifyL1Android` 和 `verifyL1Ios` 均通过，证明索引级 shared 增长、构建产物、
密钥、workflow 和迁移证据跟踪检查没有发现违规。
本轮进一步新增 `checkL1CiWorkflows`，把 workflow 文件存在性、PR/push 触发、JDK 17、
chmod、runner 和 `verifyL1Android` / `verifyL1Ios` 调用纳入 Gradle 门禁；Android
和 iOS 两个 L1 入口均会执行该自检。本轮继续收紧该任务，新增 Git 跟踪检查：
首次运行按预期发现 `.github/workflows/android-ci.yml` 和 `.github/workflows/ios-ci.yml`
仅存在于工作区、未进入 Git 索引；随后已将两个 workflow 文件加入索引，复跑
`checkL1CiWorkflows`、`verifyL1Android` 和 `verifyL1Ios` 均通过。
本轮继续收口会话唯一事实来源：个人中心注销不再直接操作 Voyager 根导航，
只委托 AuthRepository 清理会话；根 App 仍是唯一根据 SessionManager 状态替换 Main/Login
根页面的位置。本轮进一步把根会话导航映射提取为 `AppRootNavigationPolicy` 纯策略，
并用 `AppRootNavigationPolicyTest` 覆盖 `Restoring` 暂不建栈、`Authenticated` 进入
Main、`Unauthenticated` 进入 Login、`Expired` 进入 Login 且消费失效标记。App 根入口
仍通过 `navigator.replaceAll` 执行替换，因此策略测试可以证明退出或会话失效不会映射回业务主栈。
本轮继续把主导航 Tab 到 Voyager Screen 的映射提取为 `MainTabScreenRegistry`，
并用 `MainTabScreenRegistryTest` 覆盖同一 Tab 切换返回后仍使用同一个 Screen 实例、
不同一级 Tab 不共享 Screen 实例、创作 Tab 固定映射到迁移后的 `QuickCreateVoyagerScreen`。
该证据补强了 Gate G 中“重组不会重复初始化同一个 ScreenModel”和“每个 Tab 的 Screen 实例和
状态所有权明确”的本地自动化验证，但仍不能替代登录态下真实网络请求数量观察。
本轮继续补齐 QuickCreate 页面销毁证据：`QuickCreateScreenModelTest.dispose cancels active image generation polling`
构造一个会持续等待的图片生成状态流，并验证 `onDispose` 会取消该 Flow，防止不可见或已离开的
QuickCreate 页面继续轮询远端任务并回写失效状态。本轮进一步新增
`QuickCreateScreenModelTest.dispose cancels active media upload`，构造一个挂起的媒体上传并验证
`onDispose` 会取消上传协程，且取消后素材不会进入上传完成态，也不会触发后续计费预览请求。
本轮继续收紧生产代码门禁：`checkArchitectureBoundaries` 新增生产源码 `runBlocking`、`GlobalScope`
和空 `catch` 扫描，首次运行暴露 Android 媒体权限处理、`feature:auth:data` logout 和 `shared`
遗留 logout 的空 `catch`。这些位置已改为显式 no-log 降级处理，并补充中文注释说明为什么不记录
URI、路径或认证请求细节；随后 `checkArchitectureBoundaries` 以及相关 Android 编译均通过。
本轮继续把 Gate C 的生产构造约束固化为自动门禁：`checkArchitectureBoundaries` 会拒绝生产源码中
无 `SessionRestoreRepository` 的 `SessionManager()` 裸构造，避免会话恢复事实来源被绕过；当前静态搜索
确认裸构造只存在于 commonTest。本轮还把 `App.kt` 之外构造 Main/Login 根 Screen 纳入同一门禁，
防止登录页、个人中心或其他 Feature 重新绕过根 App 建立第二套根导航入口；当前 Main/Login 根
Screen 构造只保留在 `App.kt`。
根据验收标准，
“未验证项不得标记通过”，因此 AC-11
保持待完成。

## 本轮新增门禁

- `verifyL1UnitTests` 聚合所有 Gradle `Test` 类型任务：
  - 覆盖 Android/KMP 子模块的 `testDebugUnitTest` 等任务。
  - 避免根工程 `test` 无测试时被误认为整仓单元测试已通过。
- `verifyL1Android` 聚合 Android 侧 L1 可自动化验证：
  - `checkArchitectureBoundaries`
  - `verifyL1UnitTests`
  - `:composeApp:lintDebug`
  - `:composeApp:assembleDebug`
- `verifyL1Ios` 聚合 iOS 侧 L1 可自动化验证：
  - `checkArchitectureBoundaries`
  - `:shared:compileKotlinIosSimulatorArm64`
  - `:composeApp:compileKotlinIosSimulatorArm64`
  - `:composeApp:linkDebugFrameworkIosSimulatorArm64`
- `verifyL1Local` 聚合 `verifyL1Android` 和 `verifyL1Ios`，用于本地封板前总检查。
- `checkMigrationScripts` 校验 Gate G 运行观察脚本仍具备可追溯采样能力：
  - `doc/RunningHub-KMP-架构迁移验收标准.md` 必须进入 Git 索引，保证远端 CI 和后续协作者使用同一份验收基准。
  - `docs/migration/observe-tab-network.ps1` 必须存在。
  - 脚本必须保留 `-SelfTest`、`-OutputPath`、日志解析函数、稳定窗口汇总函数、
    结构化证据输出函数和 `pass_candidate` 输出。
  - 脚本运行文本必须保持 ASCII，避免 Windows PowerShell 5 按本地代码页执行时解析失败。
  - `docs/migration` 下的 L1 证据文件必须进入 Git 索引且没有未暂存差异，避免远端 CI
    缺少观察脚本、shared 基线、allowlist 或当前 Gate 状态。
- `checkL1SealEvidence` 额外要求最终封板时没有未暂存差异，并且已暂存差异只能是五个外部证据文件；
  所有外部证据必须绑定到已提交的当前代码 Git `HEAD`，不能用旧 `HEAD` 的远端 CI 结果证明仍停留在索引中的代码补丁。
- `checkArchitectureBoundaries` 现在同时检查：
  - Feature / Domain / Presentation 依赖边界。
  - `commonMain` 是否导入 Android、UIKit、Foundation、java.awt 等平台 API，或泄漏 Android
    `Context`、`Uri`、`Application` 等平台类型。
  - 生产源码是否重新引入 `runBlocking`、`GlobalScope` 或空 `catch`。
  - 生产源码是否无恢复仓库地构造 `SessionManager()`。
  - `App.kt` 之外的生产源码是否构造 Main/Login 根 Screen。
  - `composeApp` 对 `shared` 的 allowlist。
  - `shared` 新增 Git 跟踪文件是否超过 `docs/migration/shared-baseline.txt`。
  - Git 索引中是否存在 build 目录、APK/AAB/DEX、签名文件或 `local.properties`。
  - 应用交付源码和配置中是否存在常见密钥形态。
- `checkL1CiWorkflows` 现在同时检查：
  - Android 和 iOS workflow 文件存在。
  - Android 和 iOS workflow 文件已被 Git 跟踪，能够随下一次提交和推送进入远端。
  - Android 和 iOS workflow 文件没有未暂存差异，避免本地校验的 YAML 与将来提交内容不一致。
  - workflow 同时包含 `pull_request` 与 `push` 触发。
  - Android workflow 使用 `ubuntu-latest` 并调用 `./gradlew verifyL1Android`。
  - iOS workflow 使用固定 `macos-15` runner 并调用 `./gradlew verifyL1Ios`，避免 `macos-latest`
    随 GitHub 托管镜像策略漂移。
  - Android 和 iOS workflow 均配置 `concurrency.cancel-in-progress`，新提交会取消同分支旧运行。
  - Android 和 iOS workflow 均配置每周 UTC 定时回归，分别错开 30 分钟触发。
  - iOS workflow 在执行 Gradle/Xcode 前先对 iOS 迁移脚本运行 `bash -n` 和 `--self-test`，
    防止脚本语法或离线自检退化到远端安装/采证阶段才暴露。
  - iOS workflow 执行 `xcodebuild` Debug 模拟器构建并输出 `.xcresult`。
  - iOS workflow 执行自动 Simulator launch smoke，安装并启动 `RunningHub.app`。
  - Android 和 iOS workflow 均上传 Gradle reports/test-results artifact，iOS 额外上传 Xcode result bundle。
  - Dependabot 每周巡检 Gradle 和 GitHub Actions 依赖，防止依赖升级治理只停留在一次性修复。
  - Dependency Submission workflow 提交 Gradle dependency graph，支撑 GitHub Dependency Graph 和 Dependabot Alerts。
  - PR 模板要求补丁说明变更目标、影响平台、验证、Android/iOS CI 状态、L1 封板证据状态、
    剩余风险和回滚方案；真正的 Branch Protection required checks 仍需 GitHub 远端设置生效。
  - iOS workflow 提供 `workflow_dispatch` 证据采集入口，要求 `simulator_smoke_pass`、
    `smoke_notes`、`collect-ios-macos-evidence.sh` 和 `actions/upload-artifact@v4`，
    使 macOS runner 能上传 `ios-macos-link-and-simulator.md`。
  - 两个 workflow 均使用 JDK 17 并执行 `chmod +x gradlew`。
- `checkMigrationScripts` 现在同时检查 iOS 包装工程：
  - `iosApp/iosApp.xcodeproj/project.pbxproj`、SwiftUI App、ContentView 和 Info.plist 已进入 Git。
  - Xcode build phase 仍调用 `:composeApp:embedAndSignAppleFrameworkForXcode`。
  - Swift 入口仍调用 `MainViewControllerKt.startRunningHubKoin()` 和 `MainViewControllerKt.MainViewController()`。
  - iOS Koin 入口仍装配运行期 Data 模块和 `appModule`。
  - `download-ios-macos-evidence.ps1` 仍能筛选 `workflow_dispatch` iOS CI run、下载 artifact、
    复制 `ios-macos-link-and-simulator.md` 并校验 link/xcodebuild/Simulator 通过字段。
- `docs/migration/shared-ownership.md` 定义了 `shared` 的兼容模块身份、目标归属和删除条件。

## 已通过命令

```bash
./gradlew.bat --console=plain help --task verifyL1UnitTests
./gradlew.bat --console=plain help --task verifyL1Android
./gradlew.bat --console=plain help --task verifyL1Ios
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
./gradlew.bat --console=plain verifyL1Local
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
gh workflow list --repo flybirdxx/RunningHub
gh run list --repo flybirdxx/RunningHub --limit 10 --json databaseId,workflowName,headBranch,headSha,status,conclusion,createdAt,updatedAt,url,event
./gradlew.bat --console=plain checkL1CiWorkflows
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
./gradlew.bat --console=plain checkL1CiWorkflows
git add -- .github/workflows/android-ci.yml .github/workflows/ios-ci.yml
./gradlew.bat --console=plain checkL1CiWorkflows
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.AppRootNavigationPolicyTest"
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.navigation.MainTabScreenRegistryTest" --tests "com.runninghub.app.AppRootNavigationPolicyTest"
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active image generation polling" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.second image generation is blocked while current task is active"
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active media upload" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active image generation polling" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels pending draft autosave"
./gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat --console=plain :composeApp:assembleDebug
./gradlew.bat --console=plain checkArchitectureBoundaries
rg -n "catch\s*\([^)]*\)\s*\{\s*\}" composeApp/src/androidMain composeApp/src/commonMain feature shared core -g "*.kt"
./gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid :feature:auth:data:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid
rg -n "SessionManager\s*\(\s*\)" composeApp/src core feature shared -g "*.kt"
rg -n "replaceAll\(MainVoyagerScreen|replaceAll\(LoginVoyagerScreen|MainVoyagerScreen\(|LoginVoyagerScreen\(" composeApp/src/commonMain/kotlin -g "*.kt"
$parseErrors = $null; [System.Management.Automation.PSParser]::Tokenize((Get-Content -Raw 'docs/migration/observe-tab-network.ps1'), [ref]$parseErrors) | Out-Null; if ($parseErrors) { $parseErrors; exit 1 } else { 'PowerShell parse passed' }
./docs/migration/observe-tab-network.ps1 -SelfTest
./gradlew.bat --console=plain checkMigrationScripts
./gradlew.bat --console=plain verifyL1Android --dry-run
./gradlew.bat --console=plain verifyL1Ios --dry-run
git status --short .github/workflows
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.profile.ProfileScreenModelTest"
./gradlew.bat --console=plain checkArchitectureBoundaries :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat --console=plain :composeApp:assembleDebug
./gradlew.bat --console=plain :composeApp:compileKotlinIosSimulatorArm64
rg "replaceAll\(MainVoyagerScreen\(|replaceAll\(LoginVoyagerScreen\(|LoginVoyagerScreen|MainVoyagerScreen" composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature composeApp/src/commonMain/kotlin/com/runninghub/app/App.kt -g "*.kt"
rg "SessionManager\(" composeApp/src feature/auth -g "*.kt"
rg "CreateScreenModel|QuickCreateScreenModel|QuickCreateVoyagerScreen|CreateVoyagerScreen" composeApp/src/commonMain composeApp/src/androidMain -g "*.kt"
./gradlew.bat projects help checkArchitectureBoundaries test :shared:compileDebugKotlinAndroid :composeApp:lintDebug :composeApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.discovery.DiscoveryScreenModelTest"
./gradlew.bat checkArchitectureBoundaries :composeApp:assembleDebug
./gradlew.bat :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat checkArchitectureBoundaries :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64
rg "println\(" feature/quickcreate/data/src/commonMain -n
rg "AuthRepository\?|authRepository\?" feature/quickcreate/data/src/commonMain feature/quickcreate/data/src/commonTest -n
rg "任务失败|任务超时|任务查询失败|价格预览失败|余额不足|任务预提交失败|任务提交失败|未知错误|提交失败" feature/quickcreate/data/src/commonMain/kotlin/com/runninghub/feature/quickcreate/data/repository/QuickCreateRepositoryImpl.kt -n
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingControllerTest"
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:data:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:data:compileKotlinIosSimulatorArm64 :feature:quickcreate:presentation:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolderTest" --tests "com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectStateHolderTest"
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:presentation:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.second image generation is blocked while current task is active"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:presentation:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImplVideoV2Test.quick creation polling stops when task is cancelled" --tests "com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImplVideoV2Test.legacy openapi polling stops when query task is cancelled" :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingControllerTest.collect maps cancelled task to cancelled terminal display" --tests "com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusUiTest.cancelled task status uses cancelled terminal text"
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImplVideoV2Test.quick creation polling stops when task fails" --tests "com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImplVideoV2Test.quick creation polling emits timeout after max attempts without terminal record" --tests "com.runninghub.feature.quickcreate.data.repository.QuickCreateRepositoryImplVideoV2Test.legacy openapi polling stops when query task fails"
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:data:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template refreshes fee preview with applied model params and media"
./gradlew.bat :feature:quickcreate:data:testDebugUnitTest :feature:quickcreate:presentation:testDebugUnitTest :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
./gradlew.bat checkArchitectureBoundaries :feature:quickcreate:domain:compileKotlinIosSimulatorArm64 :feature:quickcreate:data:compileKotlinIosSimulatorArm64 :feature:quickcreate:presentation:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to service upload field"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when upload completes without matching fee preview snapshot"
./gradlew.bat :feature:quickcreate:presentation:testDebugUnitTest checkArchitectureBoundaries :feature:quickcreate:presentation:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
gh auth status
gh run list --repo flybirdxx/RunningHub --limit 10 --json databaseId,workflowName,headBranch,headSha,status,conclusion,createdAt,updatedAt,url,event
gh workflow list --repo flybirdxx/RunningHub
git ls-files .github
git status --short .github
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest.onDispose cancels active polling before next refresh"
./gradlew.bat :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolderTest.dispose cancels polling before next history refresh"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest" :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolderTest" checkArchitectureBoundaries :composeApp:assembleDebug
rg "println\(" composeApp/src/commonMain feature/quickcreate -g "*.kt"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateMediaUploadCoordinatorTest" --tests "com.runninghub.app.ui.feature.create.CreateScreenModelTest"
./gradlew.bat checkArchitectureBoundaries :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
rg "^(data class|enum class|class|interface|sealed)" core/model/src/commonMain/kotlin -g "*.kt"
./gradlew.bat :core:model:compileDebugKotlinAndroid :core:model:compileKotlinIosSimulatorArm64
rg "com\.runninghub\.shared\.domain\.model\.Permission|com\.runninghub\.shared\.domain\.permission|PermissionAndroidMapping|androidManifestPermission|fromAndroidManifestPermission" shared/src composeApp/src core/storage/src -g "*.kt"
./gradlew.bat :core:storage:testDebugUnitTest :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid
./gradlew.bat checkArchitectureBoundaries :core:storage:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat :shared:testDebugUnitTest :composeApp:assembleDebug
git diff --check
rg "com\.runninghub\.shared\.domain\.model\.(TaskResult|TaskOutput|TaskFailedReason|UploadResult|TaskHistoryItem|TaskHistoryOutput|TaskExecutionStatus|isTerminal|isFailed|isSuccessful)" shared/src composeApp/src core -g "*.kt"
./gradlew.bat :core:model:testDebugUnitTest :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid
./gradlew.bat checkArchitectureBoundaries :core:model:testDebugUnitTest :shared:testDebugUnitTest :core:model:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
(Get-Content 'docs/migration/shared-allowlist.txt' | Where-Object { $_.Trim() -ne '' -and -not $_.Trim().StartsWith('#') }).Count
rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' composeApp/src/commonMain/kotlin/com/runninghub/app/di/AppModule.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/history -g '*.kt'
rg 'com\.runninghub\.shared\.domain\.(model|repository).*GenerationHistory' composeApp/src shared/src feature/task/domain -g '*.kt'
./gradlew.bat :feature:task:domain:testDebugUnitTest :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest" --tests "com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapterTest" checkArchitectureBoundaries :feature:task:domain:compileKotlinIosSimulatorArm64 :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat :feature:task:domain:testDebugUnitTest :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest" --tests "com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapterTest" checkArchitectureBoundaries
./gradlew.bat projects checkArchitectureBoundaries :feature:task:domain:testDebugUnitTest :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest" --tests "com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapterTest" :feature:task:domain:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/detail/AppDetailScreenModel.kt -g '*.kt'
rg 'com\.runninghub\.shared\.domain\.repository\.WebAppTaskRepository|WebAppTaskRepository' composeApp/src shared/src feature/task/domain -g '*.kt'
./gradlew.bat :feature:task:domain:testDebugUnitTest :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid checkArchitectureBoundaries :feature:task:domain:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/plaza -g '*.kt'
rg 'com\.runninghub\.shared\.domain\.(model|repository)\.Plaza|shared\.domain\.(model|repository).*Plaza' composeApp/src shared/src feature/community/domain -g '*.kt'
./gradlew.bat :feature:community:domain:testDebugUnitTest :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.plaza.PlazaScreenModelTest" :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid checkArchitectureBoundaries :feature:community:domain:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
rg 'com\.runninghub\.shared' composeApp/src/commonMain -g '*.kt'
rg 'project\(":shared"\)|projects\.shared' composeApp/build.gradle.kts
./gradlew.bat :composeApp:dependencyInsight --configuration iosSimulatorArm64CompileKlibraries --dependency shared
./gradlew.bat checkArchitectureBoundaries :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
rg "com\.runninghub\.shared\.data\.local\.initDataStore|shared\.data\.local\.createDataStore|DATASTORE_FILE_NAME" composeApp shared core -g "*.kt"
./gradlew.bat :core:storage:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid checkArchitectureBoundaries
./gradlew.bat :core:storage:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat --console=plain :core:common:testDebugUnitTest :core:common:compileKotlinIosSimulatorArm64 :feature:auth:data:testDebugUnitTest :feature:auth:data:compileKotlinIosSimulatorArm64
$matches = rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' feature/auth/data -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'feature auth data has no shared refs' } else { $matches; exit $LASTEXITCODE }
./gradlew.bat --console=plain :feature:community:data:testDebugUnitTest :feature:community:data:compileKotlinIosSimulatorArm64
$matches = rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' feature/community/data -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'feature community data has no shared refs' } else { $matches; exit $LASTEXITCODE }
$matches = rg -n 'PlazaApi|PlazaRepositoryImpl|PlazaDto|single<PlazaRepository>|com\.runninghub\.feature\.community\.domain\.PlazaRepository' shared/src/commonMain/kotlin/com/runninghub/shared/di shared/src/commonMain/kotlin/com/runninghub/shared/data shared/src/commonTest; if ($LASTEXITCODE -eq 1) { 'shared has no Plaza data bindings or tests' } else { $matches; exit $LASTEXITCODE }
./gradlew.bat --console=plain :feature:discovery:data:testDebugUnitTest :feature:discovery:data:compileKotlinIosSimulatorArm64
$matches = rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' feature/discovery/data -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'feature discovery data has no shared refs' } else { $matches; exit $LASTEXITCODE }
$matches = rg 'single<WebAppCatalogRepository>|WebAppCatalogRepository|CatalogQuery|CatalogError|CatalogTagRange|getWebAppList|getCarefullyChosenList|getCustomMadeWebappList|getWebAppUserList|getTagTree\(|getWebAppDetail' shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/WebAppRepositoryImpl.kt shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/RunningHubApi.kt; if ($LASTEXITCODE -eq 1) { 'shared has no WebApp catalog bindings or public catalog endpoints' } else { $matches; exit $LASTEXITCODE }
./gradlew.bat --console=plain :feature:task:data:testDebugUnitTest :feature:task:data:compileKotlinIosSimulatorArm64
$matches = rg 'com\.runninghub\.shared|project\(":shared"\)|projects\.shared' feature/task/data -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'feature task data has no shared refs' } else { $matches; exit $LASTEXITCODE }
$matches = rg 'WebAppTaskRepository|WebAppTaskHistoryRepository|WebAppRepositoryImpl|WebAppDetailDto|InputNodeDto|TaskRunRequest|TaskRunResponseDto|TaskStatusRequest|TaskOutputDto|UploadResponseDto|TaskHistoryItemDto|TaskHistoryOutputDto|WebAppDto|TagDto|projects\.feature\.task\.domain|getApiCallDemo|runTask\(|getTaskOutputs|uploadFile\(|getTaskHistory\(' shared/src shared/build.gradle.kts -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'shared has no WebApp task data bindings, DTOs, endpoints, or tests' } else { $matches; exit $LASTEXITCODE }
./gradlew.bat --console=plain :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid checkArchitectureBoundaries
$matches = rg 'com\.runninghub\.shared|sharedModule|project\(":shared"\)|projects\.shared' composeApp -g '*.kt' -g '*.kts'; if ($LASTEXITCODE -eq 1) { 'composeApp has no shared refs' } else { $matches; exit $LASTEXITCODE }
(Get-Content 'docs\migration\shared-allowlist.txt' | Where-Object { $_.Trim() -ne '' -and -not $_.Trim().StartsWith('#') }).Count
./gradlew.bat --console=plain :composeApp:dependencyInsight --configuration debugCompileClasspath --dependency shared
./gradlew.bat --console=plain :core:storage:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid checkArchitectureBoundaries
./gradlew.bat --console=plain :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat --console=plain :core:storage:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat --console=plain projects :shared:testDebugUnitTest :composeApp:assembleDebug
./gradlew.bat --console=plain :core:network:testDebugUnitTest --tests "com.runninghub.core.network.NetworkActivityTrackerTest" :composeApp:compileDebugKotlinAndroid
./gradlew.bat --console=plain checkArchitectureBoundaries :core:network:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
$parseErrors = $null; [System.Management.Automation.PSParser]::Tokenize((Get-Content -Raw 'docs/migration/observe-tab-network.ps1'), [ref]$parseErrors) | Out-Null; if ($parseErrors) { $parseErrors; exit 1 }
./gradlew.bat --console=plain checkL1CiWorkflows checkMigrationScripts checkArchitectureBoundaries
git status --short .github/workflows
git diff --check
gh run list --repo flybirdxx/RunningHub --limit 10 --json databaseId,workflowName,headBranch,headSha,status,conclusion,createdAt,updatedAt,url,event
gh workflow list --repo flybirdxx/RunningHub
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
git add -- .
git diff --cached --check
git diff --name-only
./gradlew.bat --console=plain checkMigrationScripts checkL1CiWorkflows checkArchitectureBoundaries
./gradlew.bat --console=plain verifyL1Android
./gradlew.bat --console=plain verifyL1Ios
```

结果：通过。

说明：

- `verifyL1Android` 已在本地通过，执行日志包含 `composeApp`、`shared`、`core:*` 和各
  `feature:*` 模块的 `testDebugUnitTest`，不再只依赖根工程空 `test`。
- `verifyL1Ios` 已在本地通过，执行了架构门禁、`shared` 与 `composeApp` 的 iOS Simulator
  Kotlin 编译路径；Windows 本地 framework link 仍按平台能力跳过。
- `verifyL1Local` 已在本地通过，证明 Android 与 iOS 两个聚合入口可以作为本地总门禁组合执行。
- 2026-06-21 继续复核 `checkL1CiWorkflows`、`checkMigrationScripts`、`checkArchitectureBoundaries`、
  `verifyL1Android`、`verifyL1Ios` 和 `git diff --check`，结果均通过。
- 2026-06-21 继续收紧 `checkMigrationScripts` 后，首次运行按预期暴露 9 个
  `docs/migration` L1 证据文件未被 Git 跟踪；将这些文件加入索引后，
  `checkMigrationScripts`、`checkL1CiWorkflows` 和 `checkArchitectureBoundaries` 均通过。
- 2026-06-21 继续把 `doc/RunningHub-KMP-架构迁移验收标准.md` 纳入
  `checkMigrationScripts` 必跟踪清单，首次运行按预期暴露该验收标准源文件未跟踪；
  加入索引后可避免远端 CI 或后续协作者缺少 AC-11 的唯一验收基准。
- `ProfileScreenModelTest` 已覆盖 `logout` 只委托 `AuthRepository` 并清空个人中心状态。
- 静态搜索确认业务 Feature 不再直接 `replaceAll(MainVoyagerScreen())` 或
  `replaceAll(LoginVoyagerScreen())`，根 `App` 是唯一根导航切换位置。
- `:composeApp:linkDebugFrameworkIosSimulatorArm64` 在 Windows 本地为 `SKIPPED`，真实 link 仍需 macOS runner。
- `gh auth status` 确认当前登录 `flybirdxx`；早前在 workflow 尚未进入远端时，
  `gh workflow list` 无输出，`gh run list` 返回 `[]`。
- 2026-06-21 旧补丁轮次复查远端 GitHub Actions，当时 `HEAD=d7510d8e398134dab92ce5a3ac38d42ff9762df2`
  已有 `Android CI` run `27896312527` 和 `iOS CI` run `27896312519`，两者均 completed/success；
  证据 JSON 已写入 `docs/migration/evidence/` 并加入 Git 索引。
  `.github/workflows` 下只有两个已加入索引的新增 workflow 文件，没有未暂存差异。
- 2026-06-21 将完整迁移补丁加入 Git 索引后，`git diff --name-only` 为空；
  `git diff --cached --check`、`checkMigrationScripts`、`checkL1CiWorkflows`、
  `checkArchitectureBoundaries`、`verifyL1Android`、`verifyL1Ios` 和 `verifyL1Local`
  均通过。
  这证明当前待提交补丁本身，而不只是 workflow/迁移证据文件，已通过本地 L1 自动化门禁。
- 2026-06-21 本轮发现 `iosApp` 只有 README，没有 `iosApp.xcodeproj`；
  已补齐最小 SwiftUI 包装工程，并在 `composeApp/src/iosMain` 新增 iOS Compose
  `UIViewController` 导出入口和 Koin 运行期装配。`composeApp` 的 iOS source set
  现在只在平台启动层依赖 Feature Data 模块，commonMain 仍只依赖领域接口。
  `:composeApp:compileKotlinIosSimulatorArm64` 和 `:composeApp:compileDebugKotlinAndroid`
  均通过；Windows 本地仍不能验证 Xcode build 或 Simulator 运行。
- 2026-06-21 本轮继续加固 iOS 包装工程证据链：新增 shared `RunningHub.xcscheme`，
  修正 Swift 入口为 `IosRuntimeModuleKt.startRunningHubKoin()`，并让
  `collect-ios-macos-evidence.sh` 在 macOS 上除 Compose framework link 外还执行
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO`。
  `checkL1SealEvidence` 已同步要求 `xcodebuildCommand` 和 `xcodebuildResult: pass`，
  避免只证明 KMP framework link 而没有证明 iOS 包装工程可构建。
- 本轮新增 `docs/migration/l1-external-evidence.md` 和独立任务 `checkL1SealEvidence`。
  该任务不接入 `verifyL1Local`，只在准备宣称 L1 封板时执行，用于确认 Android GitHub Actions、
  iOS GitHub Actions、Android 登录态 Tab 网络观察、Android 退出登录网络观察、
  macOS iOS link/Simulator 五个外部证据文件已经落盘、
  对应当前代码 Git `HEAD`，且仓库没有未暂存差异，已暂存差异仅限这些外部证据文件。
  当前缺失这些外部证据时该任务预期失败，因此不能用本地 `verifyL1Local` 结果替代最终封板。
- 本轮继续加固 `checkL1SealEvidence`：Android CI 与 iOS CI 远端运行记录拆为两个独立 JSON 文件，
  避免一个汇总 `success` 字段被误判为双 CI 均通过；同时最终封板检查会拒绝整个仓库的未暂存差异和
  staged 的非证据变更，避免外部证据证明的是旧 `HEAD`，而不是当前待交付代码。
- 本轮继续把 `checkL1SealEvidence` 从文本片段检查升级为结构化 JSON 校验：GitHub Actions
  证据必须能解析为对象或运行数组，并包含目标 workflow 的 `completed/success` 运行以及非空
  `databaseId`、`headSha`、`url`；Android Tab 与退出登录网络观察证据必须能解析为对象，并满足
  `packageName=com.runninghub.app.debug`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、
  `result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、
  `stableWindowSampleCount` 允许 1 个 adb/logcat 调度抖动样本缺口、`stableWindowStartedDelta=0` 和
  `stableWindowMaxInFlight=0`。
  这避免只包含 `success` 或 `pass_candidate` 文本的手工文件被误当成 L1 封板证据。
- 本轮继续收紧 `checkL1SealEvidence`：Android CI 和 iOS CI 的 `headSha` 必须一致，且必须等于
  当前 Git `HEAD`，确保双端远端门禁证明的是当前待封板提交，而不是旧提交或两个不同提交上的成功运行。
- 本轮继续收紧 `checkL1SealEvidence`：若 `git diff --cached --name-only` 输出了外部证据文件之外的路径，
  任务会失败并要求先提交代码或配置补丁，再重新采集对应新 `HEAD` 的 CI 与 macOS iOS 证据。证据文件本身允许暂存，
  因为它们只能在外部运行完成后落盘，用于证明当前已提交代码 `HEAD`。
- 本轮继续收紧 Android 登录态 Tab 网络观察证据：`observe-tab-network.ps1` 新增 `-OperationNotes`
  参数并写入 JSON；`checkL1SealEvidence` 要求 `operationNotes` 非空。这样最终证据不仅包含
  `RunningHubNetwork` 稳定窗口计数，也会记录登录态 Tab 操作路径，避免未登录冷启动样本被误当成
  Gate G 运行验收。
- 本轮继续收紧 Android 登录态 Tab 网络观察证据：`checkL1SealEvidence` 要求最终 JSON 至少包含
  `durationSeconds=120` 和 `stableWindowSeconds=30`。短窗口冷启动样本仍可用于验证采集通道，
  但不能作为 History/QuickCreate 登录态 Tab 后台请求停止的封板证据。
- 本轮继续加固 Android debug 观察器：`RunningHubNetwork` 改为每秒输出一次当前计数心跳。
  `checkMigrationScripts` 会校验该心跳实现仍存在，`checkL1SealEvidence` 会要求最终 JSON 的
  `stableWindowSampleCount` 接近覆盖完整稳定窗口，避免单个静态日志点或稳定窗口外样本通过封板检查。
- 本轮继续把 Android 退出登录网络观察纳入 `checkL1SealEvidence`：`android-logout-network.json`
  与登录态 Tab 证据使用相同结构化规则，`operationNotes` 必须记录 logout 路径，稳定窗口样本数
  允许 1 个 adb/logcat 调度抖动样本缺口，但仍必须满足 `stableWindowStartedDelta=0` 和
  `stableWindowMaxInFlight=0`。
- 早前复跑 `./gradlew.bat --console=plain checkL1SealEvidence` 时，任务按预期失败，失败原因为
  Android GitHub Actions、iOS GitHub Actions、Android 登录态 Tab 网络观察、macOS iOS
  link/Simulator 四个外部证据文件缺失；这确认结构化校验已接入，但当时仍没有把 L1 标记为封板。
- 本轮新增 `docs/migration/collect-github-actions-evidence.ps1`，提交推送后可用同一入口分别生成
  `github-actions-android.json` 和 `github-actions-ios.json`。脚本支持 `-HeadSha` 与 `-Branch`
  收窄证据来源，并通过 `-SelfTest` 离线验证成功运行筛选、JSON 写入和字段断言。
- 本轮继续加固 `collect-github-actions-evidence.ps1`：生成 Android/iOS CI 证据后立即断言
  `databaseId`、`headSha` 和 `url` 为非空字段；`checkMigrationScripts` 也会检查该断言函数和字段名，
  使采集脚本与 `checkL1SealEvidence` 的结构化校验保持同一口径。
- 本轮继续加固 CI 证据采集脚本：生成两个 workflow 证据后会调用
  `Assert-MatchingWorkflowHeadSha`，如果 Android 与 iOS 成功运行来自不同 `headSha`，脚本会失败并提示使用
  `-HeadSha` 绑定目标提交。
- 本轮继续加固 CI 证据采集脚本：未显式传入 `-HeadSha` 时会自动解析当前 Git `HEAD` 并输出
  `targetHeadSha=`，避免默认选择最新成功运行时拿到旧提交证据。
- 本轮继续加固 CI 证据采集脚本：新增显式 `-Wait`、`-WaitTimeoutSeconds` 与 `-PollSeconds`
  参数。默认调用仍只选择已完成的成功 run 并快速失败；封板补证时可以显式等待目标 `HEAD`
  的 Android/iOS workflow 完成，避免远端 run 仍在 `in_progress` 时需要人工反复执行采集命令。
- 早前复跑 `powershell -NoProfile -ExecutionPolicy Bypass -File docs/migration/collect-github-actions-evidence.ps1 -SelfTest`
  通过；随后复跑 `checkMigrationScripts`、`checkL1CiWorkflows` 和 `checkArchitectureBoundaries`
  通过。后续 Android 登录态 Tab 网络观察证据已补齐，当前 Android/iOS CI 证据已重新绑定
  `d7510d8e398134dab92ce5a3ac38d42ff9762df2`；macOS iOS link/Simulator 已按用户说明留存
  `overallResult: skipped`，后续具备 macOS 环境后再补齐 pass 证据。
- 本轮新增 `docs/migration/collect-ios-macos-evidence.sh`。该脚本只允许在 macOS 生成最终 iOS 证据，
  会执行 `:composeApp:linkDebugFrameworkIosSimulatorArm64`，并要求人工完成 Simulator 登录、
  退出和 QuickCreate 冒烟后显式传入 `--simulator-smoke-pass`，才会写出
  `simulatorSmokeResult: pass`。
- 本轮继续加固 `collect-ios-macos-evidence.sh` 和 `checkL1SealEvidence`：macOS 证据会写入
  当前 Git `HEAD`，最终封板检查要求 `ios-macos-link-and-simulator.md` 的 `headSha` 等于当前
  `git rev-parse HEAD`，且 `## Notes` 非空，避免旧提交的 iOS link 结果或无操作说明的冒烟记录被复用。
- 本轮继续加固 macOS iOS 证据结构：`checkL1SealEvidence` 要求
  `ios-macos-link-and-simulator.md` 包含非空 `capturedAt`，`host` 必须包含 `Darwin`，
  `linkCommand` 必须等于 `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`，
  避免 Windows 本地 `SKIPPED` link 或其他命令输出被手工包装成 macOS 通过证据。
- 用户已明确当前无法测试 macOS 环境，因此本轮将 `ios-macos-link-and-simulator.md`
  调整为可记录 `overallResult: skipped` 的显式跳过证据。`checkL1SealEvidence`
  只在 `headSha` 等于当前 Git `HEAD`、`skipReason` 非空、`followUpRequired` 非空且不是
  `false`、`## Notes` 非空，并且 `linkResult`、`xcodebuildResult`、`simulatorSmokeResult`
  全部为 `skipped` 时接受该状态。该状态不是 iOS runtime 通过证明；具备 macOS runner
  或 macOS 开发机后仍应替换为 `overallResult: pass` 的真实 link、Xcode build 和 Simulator
  冒烟证据。
- 本轮在 skipped 证据和门禁规则暂存后复跑
  `./gradlew.bat --console=plain checkArchitectureBoundaries verifyL1Ios` 通过；
  Windows 本地仍将 `:composeApp:linkDebugFrameworkIosSimulatorArm64` 标记为 `SKIPPED`。
  随后复跑 `./gradlew.bat --console=plain verifyL1Android` 通过，覆盖 Android
  单元测试、lint 和 debug 构建。由于这些验证仍发生在未提交补丁上，最终封板仍要求先提交，
  再基于新 `HEAD` 重新采集 Android/iOS CI 和外部证据。
- 本轮新增 `docs/migration/finalize-l1-external-evidence.ps1`，作为提交后的补证总入口。
  该脚本会先拒绝 staged 或 unstaged 的代码、配置和文档改动，再按当前 `HEAD` 调用
  `collect-github-actions-evidence.ps1`，并根据 `-IosEvidenceMode skip/request/download`
  生成 skipped 证据、请求真实 iOS workflow_dispatch 证据或下载既有 artifact。脚本
  `-SelfTest` 已通过，用于防止 skipped 证据缺少 `skipReason` 或 `followUpRequired`。
  直接运行 `finalize-l1-external-evidence.ps1 -IosEvidenceMode none` 时，脚本按预期拒绝
  当前 staged 的非证据补丁，没有继续采集或覆盖最终外部证据。
  本轮继续加固该脚本：显式 `-HeadSha` 必须等于当前 checkout 的 Git `HEAD`，
  且预采集清洁检查会拒绝 untracked 文件，避免在错误提交或遗漏新增文件时生成封板证据。
  本轮继续补齐该脚本的提交后收尾能力：`-StageEvidence` 只会暂存
  Android/iOS CI、Android 登录态 Tab、Android 退出登录和 macOS/iOS 五个外部证据文件；
  `-RunSealCheck` 可在采集后立即执行 `checkL1SealEvidence`，脚本 `-SelfTest`
  已覆盖该暂存路径清单。
- 本轮复跑 `./gradlew.bat --console=plain checkL1SealEvidence`，任务按预期失败；
  失败项只剩当前 staged 的非证据门禁、脚本和文档补丁尚未提交。五个外部证据文件在当前结构化校验中
  没有再报告字段、`headSha`、敏感信息或 skipped 证据格式问题。最终封板仍必须先提交这些非证据补丁，
  再基于新 `HEAD` 重新采集 Android/iOS CI 和外部证据。
- 本轮继续复跑 `./gradlew.bat --console=plain verifyL1Local` 通过，确认当前暂存补丁在
  Windows 可执行范围内同时通过 `verifyL1Android` 与 `verifyL1Ios`。其中
  `:composeApp:linkDebugFrameworkIosSimulatorArm64` 仍为 Windows 平台下的 `SKIPPED`，
  不能替代 macOS runner 的真实 pass 证据。
- 本轮继续加固 `checkL1SealEvidence`：所有已落盘外部证据会逐行扫描常见凭据形态，
  包括 Authorization header、Cookie header、access/refresh token、API Key、私钥块和请求 Body 字段；
  命中时拒绝 L1 封板，避免 CI 输出、logcat 或 Simulator 说明把认证材料带入 Git 索引。
- 本轮继续加固 `checkArchitectureBoundaries`：`composeApp/commonMain.dependencies` 不得依赖
  Feature Data 实现模块，Domain 和 Presentation 不得导入或声明任何 Feature Data 实现依赖，
  `feature/*/data` 不得依赖 `composeApp` 或导入 `com.runninghub.app.*`。当前规则通过本地验证，
  证明 Data 实现只由平台启动层装配，没有回流到 Domain 或共享 UI 层。
- `:composeApp:dependencyInsight --configuration iosSimulatorArm64CompileKlibraries --dependency shared` 返回
  `No dependencies matching given input were found`，证明 iOS compile 配置没有继续依赖 `shared`。
- 构建仍输出既有 AGP/KMP 兼容性警告、SDK processing 警告和旧 CreateScreen 图标弃用警告。
  本轮已清理 `QuickCreateRepositoryImpl` 中 3 个 Kotlin 非空冗余警告，并通过强制重编验证。

## Android 运行证据

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew.bat --console=plain :composeApp:installDebug
adb shell am start -W -n com.runninghub.app.debug/com.runninghub.app.MainActivity
adb shell pidof com.runninghub.app.debug
adb logcat -d -s RunningHubNetwork
```

结果：

- `composeApp` debug 构建使用 `applicationIdSuffix=".debug"` 和 `versionNameSuffix="-debug"`。
- `installDebug` 在 `Pixel_10_Pro(AVD) - 17` 成功安装 `com.runninghub.app.debug`。
- `MainActivity` 冷启动成功，`LaunchState: COLD`，`TotalTime: 1303`。
- `pidof com.runninghub.app.debug` 返回进程号 `3553`。
- `dumpsys package com.runninghub.app.debug` 返回 `versionName=1.0-debug`。
- 冷启动后执行 `uiautomator dump`，界面文本包含 `RunningHUB`、`手机号`、`验证码`、
  `登录`、`使用密码登录` 和用户协议提示，说明当前 AVD 没有可恢复会话并停留在登录页。
- 本轮重新安装并冷启动后，`RunningHubNetwork` 输出
  `started=0 completed=0 inFlight=0`，证明 debug 计数日志通道可用且未输出任何接口路径或认证信息。
- `docs/migration/observe-tab-network.ps1` 已提供登录态 Tab 观察流程：脚本只读取
  `RunningHubNetwork` 标签，并在稳定窗口内计算 `started` 增量和最大 `inFlight`。
- 本轮再次后台启动 `Pixel_10_Pro` AVD，`ANDROID_SERIAL=emulator-5554 ./gradlew.bat --console=plain :composeApp:installDebug`
  成功安装 debug 包；`am start` 返回 `LaunchState: COLD`、`TotalTime: 864`，UI dump 仍显示
  `RunningHUB`、`手机号`、`验证码`、`登录`、`使用密码登录`，说明 AVD 仍无可恢复会话。
- 本轮短窗口执行 `observe-tab-network.ps1 -DurationSeconds 20 -StableWindowSeconds 10 -Launch`
  时，因应用已在前台且 logcat 已被清空，没有采集到 `RunningHubNetwork` 样本；随后新增
  `-ForceStopBeforeLaunch` 参数，并用 `-DurationSeconds 10 -StableWindowSeconds 5 -Launch -ForceStopBeforeLaunch`
  复跑短窗口采样，捕获
  `started=0 completed=0 inFlight=0`，输出 `result=pass_candidate`。该结果只证明冷启动登录页
  观察通道可用，不作为登录态 Tab 后台请求封板证据。

说明：该证据只证明 debug 包可安装和冷启动，不等价于完整人工业务回归。

### 2026-06-21 连接设备复测

```bash
adb devices
./gradlew.bat --console=plain :composeApp:installDebug
adb shell pm path com.runninghub.app
adb shell dumpsys package com.runninghub.app
```

结果：

- `adb devices` 返回一台连接设备：`adb-5d692d82-J3ioHJ._adb-tls-connect._tcp`。
- `:composeApp:installDebug` 失败，Android 返回
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.runninghub.app signatures do not match newer version`。
- 设备上已有 `com.runninghub.app`，`lastUpdateTime=2026-06-18 17:29:07`，
  且签名与本分支 debug 包不同。
- 未执行 `adb uninstall com.runninghub.app`，因为这会清除当前设备上的用户应用数据。

后续处理：

- 已为 debug 构建增加 `.debug` applicationId 后缀，避免与 `com.runninghub.app` 正式包签名冲突。
- 后续再次执行 `:composeApp:installDebug` 已成功安装 `com.runninghub.app.debug` 到连接设备
  2211133C；应用启动后 UI dump 确认处于登录态主界面，并完成 Tab 网络观察采样。
- 结论：实体设备安装阻塞已解除，Android 登录态 Tab 后台请求观察证据已落盘到
  `docs/migration/evidence/android-tab-network.json`。

## Gate 审计

| Gate | 当前判定 | 证据 | 剩余缺口 |
|---|---|---|---|
| Gate A 构建系统唯一性 | 通过 | `projects`、`help`、模块清单和 AC-01/AC-07 变更。 | 无。 |
| Gate B 模块依赖方向 | 通过 | `checkArchitectureBoundaries`、QuickCreate Data 去 `shared`、allowlist。新增门禁阻止 `composeApp/commonMain` 依赖 Feature Data 实现，阻止 Domain/Presentation 导入或声明 Feature Data 实现依赖，并阻止 Feature Data 反向依赖 `composeApp`。权限模型和权限状态边界已迁移到 `core:storage`；WebApp 任务模型和任务执行状态已迁移到 `core:model`；统一生成历史模型、历史仓库契约和 `WebAppTaskRepository` 已迁移到 `feature:task:domain`；Plaza 模型和仓库契约已迁移到 `feature:community:domain`，Plaza API/DTO/Repository/DI 和测试已迁移到 `feature:community:data`；WebApp 公开目录、搜索、标签树、用户发布列表和详情数据实现已迁移到 `feature:discovery:data`；Auth 数据实现、DTO、API 封装和 Koin 绑定已迁移到 `feature:auth:data`；WebApp Task API、DTO、Repository、DI 和测试已迁移到 `feature:task:data`。静态搜索确认 Auth/Community/Discovery/Task Data 模块无 `shared` 引用。History、Detail、Plaza 页面和 `AppModule` 不再引用 `shared`，`composeApp/commonMain` 已移除 `project(":shared")`，Android 启动层不再装配 `sharedModule`，`composeApp` 静态搜索无 shared 引用，`debugCompileClasspath` 无 `shared` 依赖，allowlist 已清零。 | 无。 |
| Gate C 唯一事实来源 | 通过 | 创作入口统一到 QuickCreate；旧 `CreateScreenModel` 已从生产 Koin 图移除；登录页和个人中心注销均不再直接替换根导航；生产 DI 使用 `SessionManager(get())` 注入恢复仓库；静态搜索确认业务 Feature 不再直接替换 Main/Login 根页面，根 `App` 是唯一根导航切换位置；`checkArchitectureBoundaries` 已禁止生产源码裸 `SessionManager()` 构造和 `App.kt` 之外构造 Main/Login 根 Screen，防止绕过 `SessionRestoreRepository` 或根 App。 | 无。 |
| Gate D 认证与 401 | 通过 | `core:network`、`feature:auth:domain` 单元测试覆盖刷新、重试、并发和 logout 竞态；本轮按复核建议把 `TokenRefresher` 的刷新响应解析从正则改为 kotlinx.serialization DTO，新增 JSON unicode escape token 测试，避免 Authorization 写入未解码的 token 文本；`checkArchitectureBoundaries` 会拒绝在该文件重新引入 Regex/toRegex；本轮继续新增 `ApiEnvironment`，`RunningHubApiEnvironment` 改为平台启动层可配置门面，Android/iOS runtime module 显式注入当前生产环境，后续 staging/dev 真实地址登记后可在启动层替换，不再改 Data endpoint；`feature:auth:data` 和 `shared` 遗留 Auth logout 的远端失败降级已从空 `catch` 改为显式 no-log 处理，避免认证请求细节进入日志。 | 无。staging/dev 真实地址尚未登记，debug 到 staging/dev 的实际切换归后续环境配置补齐。 |
| Gate E Discovery | 通过 | Discovery 依赖窄 `WebAppCatalogRepository`；Domain 已提供 `CatalogQuery`、`CatalogSort`、`CatalogTagRange` 和 `CatalogError`；Data 层集中映射远端协议参数；Presentation 使用 `toCatalogErrorMessage` 映射文案。`DiscoveryScreenModelTest` 已覆盖 index 0 语义、分类/排序/搜索旧响应隔离、分页去重、加载更多失败不推进页码、刷新保留筛选和目录错误文案映射。 | 无。 |
| Gate F QuickCreate | 通过 | 唯一入口、Data 去 `shared`、Coordinator/StateHolder 结构、相关测试和构建通过；本轮已移除 QuickCreate Data 层裸 `println`，并将 `QuickCreateRepositoryImpl` 的 `AuthRepository` 改为必需依赖，测试统一使用 `FakeAuthRepository` 覆盖刷新语义。生成任务状态流本地兜底已迁移为 `QuickCreateTaskIssueCode`，`QuickCreateTaskPollingController` 负责映射中文展示文案，相关 Data 与 Presentation 测试通过。非任务状态 Data 本地兜底已迁移为 `QuickCreateRepositoryIssueCode` 和 `QuickCreateRepositoryException`，历史、项目、灵感、模型、上传和计费预览的最终展示文案由 `QuickCreateErrorMessages` 统一映射；静态搜索确认 Data commonMain 不再保留这些本地兜底异常文案。`QuickCreateHistoryStateHolderTest` 覆盖最近历史和项目任务按 `taskId` 去重、旧最近历史不覆盖选中项目任务；`QuickCreateProjectStateHolderTest` 覆盖项目分页按 `projectId` 去重和删除选中项目后的回调边界。`QuickCreateScreenModelTest` 覆盖上传超时/失败不提交远端任务，并覆盖活跃生成 Job 期间重复点击不会产生第二次远端提交；后续任务状态推进会清理重复点击产生的临时提示；本轮继续覆盖页面 `onDispose` 会取消正在收集的图片生成状态流。旧 OpenAPI queryTask 与 QuickCreation 任务列表收到 CANCELED/CANCELLED 后均映射为取消终态并停止轮询，Presentation 展示“任务已取消”。QuickCreation V2 失败只查询一次任务列表，旧 OpenAPI 失败只查询一次 queryTask；V2 长时间没有终态时在固定次数后发出 `TASK_TIMEOUT` 并结束 Flow。灵感视频模板应用后，编辑状态与计费预览请求共享同一份模型、动态参数和模板媒体。生成提交流程在点击生成时捕获状态快照，等待上传期间继续编辑 prompt 不会污染当前远端提交，上传完成只回填快照素材的远端 URL。计费预览成功后保存请求指纹，正式提交会按最终请求重新计算并匹配；上传完成但未重新计费的请求会以“价格待确认”拦截。成功提交进入 Queuing 后会清理本地草稿。composeApp commonMain 与 feature/quickcreate 源码静态搜索无裸 `println`。本轮继续按复核建议隔离旧 Create 页面：旧 `CreateVoyagerScreen` 已变为 internal 且 ERROR 级废弃，架构门禁会阻止其他生产 commonMain 文件重新构造旧入口。登录态 Tab 网络观察已证明切换 History/QuickCreate 等一级 Tab 后稳定窗口内无持续新增网络请求；Android 退出登录后 Login 根页面空闲观察已证明稳定窗口内没有持续后台请求。 | 无。macOS iOS Simulator 运行路径以 `overallResult: skipped` 记录风险，后续具备 macOS 环境后可补齐真实 pass 证据。 |
| Gate G 生命周期与导航 | 通过 | `MainScreen` 改为只组合当前 Tab；`tab-lifecycle.md` 记录策略；Android 冷启动通过。`TaskHistoryScreenModelTest` 和 `QuickCreateHistoryStateHolderTest` 已覆盖 dispose 后轮询 Job 不触发下一次刷新。2026-06-21 debug 包已通过独立 applicationId 在 AVD 和连接设备 2211133C 上安装并启动；本轮新增 `NetworkActivityTracker` 和 Android debug `RunningHubNetwork` 聚合计数日志，单元测试覆盖成功和异常请求都会清空 inFlight，AVD 冷启动已看到 `started=0 completed=0 inFlight=0`；`observe-tab-network.ps1` 已沉淀为登录态 Tab 网络观察脚本，本轮新增 `-SelfTest` 与 `-OutputPath`，离线自检覆盖日志样本解析、稳定窗口判定和 JSON 证据写入读取；`checkMigrationScripts` 已把观察脚本完整性纳入 Gradle 门禁；当前 `android-tab-network.json` 已记录登录态 Discover/History/Create/Plaza/Profile 切换后 120 秒采样和 30 秒稳定窗口，结果为 `pass_candidate`、稳定窗口请求增量 0、最大 in-flight 0；Profile 注销不再直接操作 Voyager 根栈，根 App 统一根据 `SessionManager` 清空业务页面栈；`AppRootNavigationPolicyTest` 覆盖 Restoring/Authenticated/Unauthenticated/Expired 的根目标，证明退出或会话失效不会映射回业务主栈，Expired 会在进入 Login 后消费失效标记；`MainTabScreenRegistryTest` 覆盖同一 Tab 的 Screen 实例稳定、不同 Tab 的状态所有权隔离，以及创作 Tab 固定到 `QuickCreateVoyagerScreen`。本轮继续从 Profile 执行退出登录，UI dump 确认回到 Login 根页面，`android-logout-network.json` 记录退出完成后空闲 125 秒，结果为 `pass_candidate`、稳定窗口请求增量 0、最大 in-flight 0。 | 无。macOS iOS Simulator 登录、退出和 QuickCreate 冒烟以 skipped 证据记录风险，后续仍可在 macOS 补验。 |
| Gate H Core 与 Shared 收口 | 通过 | 删除空壳 designsystem；`core:model` 的 User、WebApp、Tag、PageData、AppDetail 及嵌套业务模型已补齐字段级中文 KDoc，并通过 Android 与 iOS Simulator Kotlin 编译；`core:common` 新增旧登录协议专用 `md5` 跨平台入口；权限模型、权限状态、权限存储边界、Android Manifest 映射、DataStore 平台工厂、DataStore-backed 余额/QuickCreate 草稿和权限状态存储实现已迁入 `core:storage`；AC-11 复核补丁已把生产 `CredentialStore` 切换到 Android Keystore backed 密文存储与 iOS Keychain，并通过 `MigratingCredentialStore` 懒迁移旧 DataStore 凭据；WebApp 任务提交、输出、历史和任务执行状态模型已迁入 `core:model`；统一生成历史模型、`GenerationHistoryRepository` 和 `WebAppTaskRepository` 已迁入 `feature:task:domain`；Plaza 模型与 `PlazaRepository` 已迁入 `feature:community:domain`，Plaza API/DTO/Repository/DI 和测试已迁入 `feature:community:data`，shared 不再保留 Plaza Data 绑定；WebApp 公开目录、搜索、标签树、用户发布列表和详情 API/DTO/Repository/DI 已迁入 `feature:discovery:data`，shared 不再保留公开目录 endpoint 或 `WebAppCatalogRepository` 绑定；认证、用户资料、会话恢复、个人中心凭据和余额快照实现已迁入 `feature:auth:data`；WebApp Task API/DTO/Repository/DI 和测试已迁入 `feature:task:data`，shared 不再保留 Task endpoint、DTO、Repository 实现或 Data 绑定；Android 生产启动图不再装配 shared，新增 shared baseline 和 ownership 文档。2026-06-21 复核 `composeApp` debugCompileClasspath 无 `shared` 依赖，`composeApp`、`feature` 和 `core` 当前生产模块无实际 `shared` 引用；历史 `androidApp` 目录仍有旧 `shared` 引用，但未被 `settings.gradle.kts` include，不属于当前 L1 生产启动图。 | 无。`shared` 剩余 Audio、ModelCatalog、ModelInvocation 和旧兼容文件已登记归属与删除条件，属于 L1 后继续瘦身项；真机 Keychain/Keystore 升级回归、备份/卸载/系统还原风险检查归 AC-12。 |
| Gate I Android + iOS 编译 | 本地通过，待 macOS 运行证据 | Android assemble/lint 和 AVD install/launch 通过；2026-06-21 debug 构建增加 `.debug` applicationId 后缀，`com.runninghub.app.debug` 已在 Pixel_10_Pro AVD 上安装并冷启动成功；随后 `:composeApp:installDebug` 在连接设备 2211133C 上安装成功，并完成登录态主界面启动和 Tab 网络采样；`verifyL1Ios` 本地通过，覆盖 iOS Simulator Kotlin 编译；`checkArchitectureBoundaries` 已把 commonMain 平台 API 禁用自动化，覆盖 Android、UIKit、Foundation、java.awt 导入和 Android Context/Uri/Application 泄漏，本地复跑通过；本轮生产空 `catch` 修复后复跑 `:composeApp:compileDebugKotlinAndroid`、`:feature:auth:data:compileDebugKotlinAndroid` 和 `:shared:compileDebugKotlinAndroid` 通过。本轮补齐 `iosApp/iosApp.xcodeproj`、SwiftUI 壳和 iOS Koin runtime module；`:composeApp:compileKotlinIosSimulatorArm64` 与 `:composeApp:compileDebugKotlinAndroid` 通过。本轮按复核建议开启 Android release R8 minify 和 resource shrink，更新迁移后的 ProGuard 规则，并通过 `:composeApp:assembleRelease` 验证；`verifyL1Android` 已纳入 `:composeApp:assembleRelease` 防止 release 构建治理回退。 | Windows 本地 `linkDebugFrameworkIosSimulatorArm64` 仍为 `SKIPPED`；iOS Xcode build、Simulator 登录/退出/QuickCreate 和媒体上传运行回归仍需 macOS runner 或 macOS 开发机补证；正式签名和 Release 安装回归归 AC-12。 |
| Gate J CI | 待当前 HEAD 外部证据 | Android/iOS workflow 文件已存在，分别调用 `verifyL1Android` 和 `verifyL1Ios`；本地门禁覆盖依赖、commonMain 平台类型、生产 `runBlocking`/`GlobalScope`/空 `catch`、生产 `SessionManager()` 裸构造、根 Screen 构造唯一入口、迁移脚本完整性、测试、lint、构建、shared 增长、秘密和构建产物；`verifyL1Android` 现在同时覆盖 Android lint、debug 构建和开启 R8/资源压缩后的 release 构建；`checkArchitectureBoundaries` 已纳入环境注入防退化检查，阻止 `ApiEnvironment`、平台 runtime module 环境绑定或运行期 URL 配置能力被删除；新增 `checkL1CiWorkflows` 校验 workflow 文件、Git 跟踪状态、无未暂存差异、触发器、runner、JDK 17、chmod、L1 Gradle 入口、Dependabot 依赖巡检配置、Dependency Submission 和 PR 模板；本轮按复核建议补强 iOS workflow，`verifyL1Ios` 后会在 macOS runner 上显式运行 `core:network`、`feature:auth:domain`、`feature:auth:data` 和 `feature:quickcreate:presentation` 的 `iosSimulatorArm64Test`，随后执行 `xcodebuild` Debug 模拟器构建、安装到临时 Simulator 并启动 bundle，上传 `.xcresult` 和 launch smoke 输出；Android/iOS workflow 均上传 Gradle reports/test-results artifact；iOS runner 已固定为 `macos-15`，Android/iOS workflow 已增加同分支旧运行自动取消和每周定时回归，Dependabot 已每周巡检 Gradle 与 GitHub Actions，Dependency Submission 会提交 Gradle dependency graph，PR 模板已固化变更、验证、风险和回滚信息，且 `checkL1CiWorkflows` 已纳入防退化检查并拒绝 `macos-latest`；P0-4 本地包装层继续补强，TAC 脚本加载失败和超时会显示可重试中文降级状态，`SmsCaptchaHtmlTest` 已覆盖；iOS workflow 已提供 `workflow_dispatch` 证据采集入口，手动确认 `simulator_smoke_pass` 和 `smoke_notes` 后会运行 `collect-ios-macos-evidence.sh` 并上传 `ios-macos-link-and-simulator.md` artifact；`checkMigrationScripts` 校验 Gate G 运行观察脚本、GitHub Actions 证据采集脚本和 macOS/iOS 证据采集脚本。 | 当前工作区存在已暂存的非证据修复补丁，且 Android/iOS/macOS 证据仍绑定旧 HEAD `85f134d23ac58768e8a40c3172f3fbb34ca90699`；提交并推送后必须按新 HEAD 重新采集证据，并让 `checkL1SealEvidence` 通过。 |

## 下一步

AC-11 的本地修复和 Windows 可执行门禁已完成，但 L1 仍未封板。下一步应先完成当前 HEAD 外部证据补采：

1. 提交并推送当前已暂存的非证据修复补丁，然后按新 HEAD 重新采集 Android/iOS GitHub Actions 证据。
2. 当前已按用户说明留存 macOS 不可测的 `overallResult: skipped` 证据；具备 macOS runner
   或 macOS 开发机后，执行 `docs/migration/collect-ios-macos-evidence.sh`，
   由脚本验证 Compose framework link 和 `RunningHub` scheme 的 `xcodebuild`，并替换为
   `overallResult: pass`。
3. 也可以手动触发 `iOS CI` 的 `workflow_dispatch` 证据模式，填写
   `simulator_smoke_pass=true` 和 `smoke_notes`，下载生成的
   `ios-macos-link-and-simulator.md` artifact，或运行
   `docs/migration/download-ios-macos-evidence.ps1 -RunId <run id>` 自动落盘到
   `docs/migration/evidence/`。
3. 在 macOS Simulator 冒烟时覆盖登录、退出和 QuickCreate 操作路径；Android 登录态 Tab 网络计数
   已由 `docs/migration/evidence/android-tab-network.json` 记录，Android 退出登录后的空闲网络观察
   已由 `docs/migration/evidence/android-logout-network.json` 记录。
