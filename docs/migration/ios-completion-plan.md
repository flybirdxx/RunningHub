# iOS 功能补全剖析与执行计划

更新时间：2026-07-07
当前基线：当前 Git `HEAD`；macOS/iOS evidence 已按该 `headSha` 重采，具体提交以 evidence 文件为准。
当前环境：macOS 26.5.1，Xcode 26.6 (17F113)，iOS Simulator SDK 26.5，已安装 iOS Simulator runtime 26.4 与 26.5
任务边界：先完成仓库剖析和文档落地，不在本文件完成前直接修改业务代码。

## 1. 目标与停止线

Android 侧当前已有较完整的构建、运行和外部证据链；iOS 侧此前主要停留在
`overallResult: skipped` 的风险记录，不能作为真实运行通过。当前已经切到 macOS 环境，
下一阶段目标是把 iOS 从“可编译/可包装”推进到“主要用户流程可在 Simulator 或真机上闭环”。

本计划只定义补全顺序、成功标准、允许修改面和验证证据。任何实现任务开始前，必须先完成：

- 确认工作区已有改动，尤其是 `composeApp/src/iosMain/kotlin/com/runninghub/app/platform/MediaSaver.ios.kt` 当前已有未提交修改。
- 在 macOS 上跑通最小 iOS 构建基线，或把失败命令和错误写回本文档或对应迁移证据。
- 明确本轮只改 iOS 平台边界和必要的共享接口，不借机重构 Android 已完成链路。

## 2. 本次剖析入口

本计划基于以下当前源码和文档：

| 类型 | 入口 |
|---|---|
| 根规则 | `AGENTS.md`、`.codex/rules/project_rule.md` |
| 架构与开发 | `ARCHITECTURE.md`、`DEVELOPMENT.md`、`.codex/references/dependencies.md` |
| iOS 包装工程 | `iosApp/AGENTS.md`、`iosApp/README.md`、`iosApp/iosApp.xcodeproj/project.pbxproj`、`iosApp/iosApp/*.swift`、`iosApp/iosApp/Info.plist`、`iosApp/iosApp/PrivacyInfo.xcprivacy` |
| iOS 运行期 | `composeApp/src/iosMain/kotlin/com/runninghub/app/di/IosRuntimeModule.kt`、`MainViewController.kt` |
| 平台能力 | `PermissionController.ios.kt`、`MediaResolver.ios.kt`、`MediaSaver.ios.kt`、`SmsCaptchaDialog.ios.kt` |
| 存储与凭据 | `core/storage/src/iosMain/kotlin/com/runninghub/core/storage/*` |
| 证据与门禁 | `docs/migration/l1-external-evidence.md`、`docs/migration/evidence/ios-macos-link-and-simulator.md`、`docs/governance/release-readiness-checklist.md` |

CodeGraph 当前可用，最新索引统计为 712 个文件、13722 个节点、27505 条边；但索引提示有 4 个
pending added 文件和 30 个 pending modified 文件，因此涉及本轮 iOS 补丁的结论以磁盘源码为准。

## 3. 当前 iOS 架构事实

### 3.1 包装工程与启动链

- `iosApp` 是 SwiftUI 薄壳，`RunningHubIosApp.init()` 先调用 `startRunningHubKoin()`。
- `ContentView` 只嵌入 `MainViewControllerKt.MainViewController()`，业务逻辑不写在 Swift 壳中。
- Xcode target 的 build phase 调用 `:composeApp:embedAndSignAppleFrameworkForXcode`。
- 当前 bundle id 为 `com.runninghub.app.ios`，deployment target 为 iOS 16.0。
- `Info.plist` 已声明 Photo Library 读取、相册写入、Camera、Microphone 四类 Usage Description，并设置 `PHPhotoLibraryPreventAutomaticLimitedAccessAlert=true`，由客户端手动打开 Limited Photos 管理页，避免系统自动弹窗和应用内权限恢复入口重复；`PrivacyInfo.xcprivacy` 已加入 Xcode resources，并声明 UserDefaults 与文件时间戳访问原因；`checkLongTermGovernance` 会阻止权限说明、Limited Photos 自动弹窗配置、privacy manifest 字段或 Xcode Resources 引用退化。

### 3.2 KMP 运行期装配

`IosRuntimeModule.kt` 已装配：

- `ApiEnvironment`，通过 `RUNNINGHUB_*` 进程环境变量覆盖公开 base URL 和可信认证 host。
- `Json`、`HttpClient`、`TokenRefresher`、`SessionManager`、`NetworkActivityTracker`。
- iOS Keychain backed `CredentialStore`，并通过 `MigratingCredentialStore` 懒迁移旧 DataStore 凭据。
- DataStore backed 非敏感缓存，包括余额、QuickCreate 草稿、模型选择和 UI snapshot。
- `audio/auth/community/discovery/model/task/quickCreate` Data 模块和 `appModule`。

结论：iOS 不是缺少总入口；当前 macOS 下的 link、Xcode build 和 signed Simulator 主流程已经有运行证据，`verifyL1Ios` 已在当前补全补丁上通过，且 `ios-macos-link-and-simulator.md` 已按当前 `HEAD` 重采。剩余风险集中在远端 CI 外部证据、TestFlight/真机边界和需要人工操作的系统弹窗。

### 3.3 已存在的平台能力

| 能力 | 当前实现 | 当前风险 |
|---|---|---|
| Keychain | `IosKeychainCredentialStore` 保存 API Key、Cookie、access token、refresh token | 已验证 signed Simulator 登录后可恢复会话；2026-07-07 已再次退出登录并使用测试账号完成密码登录，回到账户页，凭据未写入文档；Keychain unavailable 时已改为显式失败；`IosKeychainCredentialStoreTest` 覆盖 token/refresh token 写入、读回和 `clearAll` 清理，仍需真机/TestFlight 升级与重装边界验证 |
| 非敏感 DataStore | iOS Documents 目录下 `PreferenceDataStoreFactory.createWithPath` | 需要确认升级和卸载边界，不作为敏感凭据存储 |
| 权限状态 | `PermissionDataStoreImpl.ios.kt` 用 `NSUserDefaults` 记录授权轨迹 | PhotoKit 允许、拒绝和 Limited 三种授权结果写入 `PermissionStateStore` 已由 `IosPermissionAuthorizationMappingTest` 覆盖；真实权限仍以系统状态为准 |
| 图片/视频选择 | `PHPickerViewController`，选中结果先复制到应用临时文件再回传 URI | 已覆盖视频 PHPicker 打开、选中后形成素材卡和取消后回到页面；`IosPickedMediaCopyTest` 覆盖图片/视频临时文件复制到 app-owned URI 后仍可读取；`PermissionControllerContractTest` 覆盖图片/视频/音频取消不会进入权限拒绝；治理门禁阻止图片/视频选择重新预申请整库 PhotoKit 权限，并守住 PHPicker 空结果、类型不匹配和空 URI 的取消分支；QuickCreate 上传会保留 picker 返回的 `.mov/.webm/.mp4` 扩展名并提交匹配 MIME，避免 iOS 视频被统一伪装成 mp4；2026-07-07 已在 signed Simulator 选择“限制访问…”进入系统 Limited Photos 管理页并完成返回，页面未新增素材或错误；权限恢复入口已在 PhotoKit Limited 状态下路由到系统有限照片管理页，仍需远端上传 200 |
| 音频选择 | `UIDocumentPickerViewController` import 模式 | 已覆盖入口和取消后回到页面，且取消不会再误弹音频权限说明；QuickCreate 上传会保留 `.m4a/.wav/.aac/.ogg/.mp3` 扩展名并提交匹配 MIME，避免音频被统一伪装成 mp3；真实文件导入和远端上传 200 仍需后续人工或专门环境验证 |
| 媒体读取 | `MediaResolver.ios.kt` 尝试 security-scoped access 后读取 `NSData`；QuickCreate/AppDetail 上传前会先读取文件大小 | 已在 Presentation 层拦截超过服务端字段上限或 100MiB 本地保护上限的文件，避免先读入 `ByteArray`；允许范围内仍是一次性读入，后续若要支持更大视频需流式上传改造 |
| 图片保存 | `MediaSaver.ios.kt` 当前工作区已有 PhotoKit 保存实现改动，`Info.plist` 已声明相册写入权限 | 已随本轮 iOS 编译/link 通过；图片结果保存到 Simulator Photo Library 已通过 signed Simulator smoke；2026-07-07 在 iPhone 17 Pro Simulator 上重置 `photos-add` 后触发保存，真实系统弹窗拒绝路径通过，页面展示稳定失败提示并出现权限恢复引导，日志扫描未命中远端 URL、敏感 token 或平台异常原文；仍缺真实远端视频保存运行验证 |
| 短信图形验证码 | `SmsCaptchaDialog.ios.kt` 使用 WKWebView、message handler 和 custom scheme 兜底 | TAC 滑块渲染已通过 smoke；Android/iOS Web 容器 base URL 跟随 `RunningHubApiEnvironment.WEB_BASE_URL`，`/tac/js`、`/tac/css` 与 `/uc` 相对路径保持同源的静态契约已有平台测试；关闭后旧 token 触发短信重试和旧 Web 容器回调串到当前状态层已有自动化防线；网络/script 失败、图片解码失败和超时的可重试降级状态已有 HTML 契约测试与治理门禁；2026-07-07 已在 signed Simulator 人工完成 TAC 滑块、短信发送和 SMS 登录，runtime/os log 脱敏扫描未命中 token、Cookie、手机号、验证码、远端 URL 或平台异常原文；Android 真实 WebView、真机/TestFlight 仍需发布前专项复核 |
| Camera/Notifications | `PermissionController.ios.kt` 已接入 AVFoundation 相机授权和 UserNotifications 通知授权 | 已随 iOS Simulator 编译通过；源码复核显示当前产品 UI 未暴露 Camera/Notifications 调用点，不能为验证新增用户不可见入口；有产品入口后再做真实系统弹窗允许/拒绝运行验证 |

## 4. 缺口分级

### P0：先证明当前 iOS 能构建

当前本机 `./gradlew` 没有可执行位，直接执行会得到 `permission denied`；`bash ./gradlew --version`
可运行并显示 Gradle 9.4.1。进入实现前应先处理这个前置差异，推荐在 macOS 上恢复可执行位后使用
仓库文档中的标准命令。

P0 成功标准：

- `./gradlew --console=plain checkArchitectureBoundaries checkLongTermGovernance`
- `./gradlew --console=plain :composeApp:compileKotlinIosSimulatorArm64`
- `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO`

若任一失败，先修编译或包装工程，不进入功能补全。

2026-07-06 当前执行记录：

- 已恢复 `gradlew` 可执行位；标准 macOS Gradle 命令可直接运行。
- `./gradlew --console=plain checkArchitectureBoundaries checkLongTermGovernance` 已通过。
- `./gradlew --console=plain :composeApp:compileKotlinIosSimulatorArm64` 已通过。
- `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64` 已通过；已在 `composeApp/build.gradle.kts` 显式配置 `binaryOption("bundleId", "com.runninghub.app.compose")`，最新 link 输出不再出现 Kotlin/Native bundleId 推断警告。
- `xcodebuild ... -sdk iphonesimulator ...` 已通过，SwiftUI 壳能消费当前 Compose framework。
- 首次 Simulator 启动暴露 `CADisableMinimumFrameDurationOnPhone` 缺失导致的 Compose iOS 启动异常；已在 `iosApp/iosApp/Info.plist` 补齐该 key。
- 补齐后通过 XcodeBuildMCP 重新 build/install/launch `com.runninghub.app.ios` 到 iPhone 17 Pro / iOS 26.5 Simulator；截图确认 RunningHUB 登录页渲染，手机号输入框获得焦点并弹出数字键盘，新 runtime/os log 未出现启动崩溃关键字。

### P1：补齐 iOS 运行证据

需要把 `docs/migration/evidence/ios-macos-link-and-simulator.md` 从 skipped 证据替换为当前提交的 pass 证据；2026-07-07 已按当前 `HEAD` 重采。
采集入口仍使用：

```bash
docs/migration/collect-ios-macos-evidence.sh --simulator-smoke-pass --smoke-notes "<设备、系统、登录/退出/QuickCreate 操作路径>"
```

注意：`--simulator-smoke-pass` 只能在真实操作完成后传入，不能用脚本启动成功替代登录、退出和 QuickCreate 冒烟。

2026-07-06 当前执行记录：

- `docs/migration/collect-ios-macos-evidence.sh --simulator-smoke-pass --smoke-notes "<...>"` 已在 macOS 上重新采集并写入 `docs/migration/evidence/ios-macos-link-and-simulator.md`。
- 该 evidence 记录为 `overallResult: pass`、`simulatorSmokeResult: pass`、`authenticatedSmokeBuildMode: signed-simulator-or-device`。
- 2026-07-07 已在当前 `HEAD` 上重新采集 iOS evidence，字段为 `overallResult=pass`、`linkResult=pass`、`xcodebuildResult=pass`、`simulatorSmokeResult=pass`。

### P2：补齐用户主流程

iOS 功能补全至少覆盖以下用户流程：

| 流程 | 必须验证的 iOS 行为 | 主要文件 |
|---|---|---|
| 启动与会话恢复 | 首启、已有 token 恢复、token 失效回 Login、退出后无业务栈残留 | `IosRuntimeModule.kt`、`App.kt`、Auth Data/Presentation |
| 登录验证码 | WKWebView 加载 TAC，成功回传 `validToken`，关闭和连续打开不串回调 | `SmsCaptchaDialog.ios.kt`、`SmsCaptchaHtml.kt` |
| 个人中心凭据 | API Key/Cookie 写入 Keychain，退出清理，重启不丢登录态 | `IosKeychainCredentialStore.kt`、Auth/Profile |
| 发现/广场/详情 | 列表、搜索、详情打开、任务提交和输出轮询 | Discovery、Community、Detail、Task Feature |
| QuickCreate | 模型/参数/计费预览/素材上传/生成/轮询/历史 | QuickCreate Domain/Data/Presentation、iOS media actual |
| 历史任务 | 普通历史刷新、详情、取消、复用参数 | Task Domain/Data/Presentation |
| 媒体上传 | 图片、视频、音频选择、取消、读取、上传失败不继续提交 | `PermissionController.ios.kt`、`MediaResolver.ios.kt` |
| 图片保存 | 输出结果保存到相册，拒绝权限和下载失败有稳定失败语义 | `MediaSaver.ios.kt` |

2026-07-06 已完成静态补强：

- `PermissionController.pickMedia` 已区分“用户取消/临时拒绝”和“系统永久拒绝/需进设置页”；`PermissionControllerContractTest` 覆盖图片、视频、音频三类 picker 取消不会触发权限拒绝或永久拒绝；iOS PhotoKit 图片/视频权限被拒绝时，AppDetail 和 QuickCreate 会进入权限说明/设置页引导，不再静默等同取消选择器。
- `PermissionController.ios.kt` 已接入真实 iOS 相机和通知授权：相机通过 AVFoundation 查询/请求 `AVMediaTypeVideo`，通知通过 `UNUserNotificationCenter` 查询/请求 alert、sound、badge；拒绝或 restricted 统一映射到需进设置页的永久拒绝状态。
- `iosApp/iosApp/Info.plist` 已补齐 `NSPhotoLibraryAddUsageDescription` 和 `PHPhotoLibraryPreventAutomaticLimitedAccessAlert=true`，并由 `checkLongTermGovernance` 守住 Photo Library 读写、Camera、Microphone 权限说明和 Limited Photos 手动管理配置。
- 新增 `PermissionControllerContractTest` 覆盖媒体选择永久拒绝回调契约，并已编译到 iOS Simulator test target。
- QuickCreate 默认紧凑输入条已按当前 tab 分流素材入口：图片 tab 的加号打开图片 picker，视频 tab 的加号打开视频 picker，避免视频创作仍误走图片素材入口；新增 `QuickCreateCompactComposerContractTest` 防止该接线回退。
- QuickCreate 参数面板已把滚动内容固定在显式 viewport 中，并为参数上传字段提高专用高度上限；视频模型中的 `imageUrls`、`videoUrls`、`audioUrls` 上传按钮在 iOS Simulator 上均可见，避免音频上传入口被面板底部裁切。
- 新增 `IosMediaResolverTest` 覆盖 iOS actual 的 file URL 读取路径：真实临时文件通过 `NSURL.fileURLWithPath(...).absoluteString` 进入 `MediaResolver.ios.kt`，断言 `readBytes`、`getDisplayName`、`getFileSizeBytes` 均返回预期值。
- AppDetail 上传适配新增音频和视频 MIME 契约测试：`AppDetailScreenModel` 从平台 `MediaResolver` 读取媒体 bytes，按 `.m4a` 推断 `audio/mp4`、按 iOS 常见 `.mov` 推断 `video/quicktime`，调用 `WebAppTaskRepository.uploadFile` 后把远端文件名写回输入值。
- QuickCreate 和 AppDetail 已补上传前大小门禁与失败提交阻断：`QuickCreateMediaUploadCoordinatorTest` 覆盖全局素材超过 100MiB 保护上限、字段素材超过服务端 `maxUploadSize` 时不调用 `readBytes`/远端上传，并覆盖失败素材在 `awaitPendingUploads` 阶段阻断生成提交；`AppDetailStateHolderTest` 与 `AppDetailScreenModelTest` 覆盖 AppDetail 超过 100MiB 时不读取平台 bytes、不调用上传仓库，同时覆盖上传中或上传失败时 `runTask` 不调用远端提交、不提交旧远端文件名、本地 URI 或空素材参数。
- AppDetail 详情页已把运行按钮改为列表内操作项，并将参数区优先于长简介展示；新增 `AppDetailLayoutMetricsTest` 固定末尾安全余量契约，避免短详情页的单个图片/视频/音频上传入口被底部固定操作栏遮挡。
- QuickCreate 和 History 的治理拆分只移动 UI/横幅瞬态 effect，不改变生成、轮询、上传或状态机 owner。

2026-07-06 已完成运行前置验证：

- Xcode Debug Simulator build passed。
- Simulator install/launch passed，bundle id 为 `com.runninghub.app.ios`；当前最近一次 XcodeBuildMCP signed Simulator build/run 启动进程 pid 为 30892。
- 登录页渲染 passed。
- 使用 signed Simulator build 和测试账号完成密码登录 passed；Profile 显示脱敏手机号 `176****8045`、余额和会员信息。
- 停止并重新启动 `com.runninghub.app.ios` 后，Keychain backed 会话恢复 passed。
- 主 Tab 非破坏性 smoke passed：创作、发现、广场/灵感、历史、账户均能加载；历史页显示 61 条记录。
- 首次用 `CODE_SIGNING_ALLOWED=NO` 构建做登录验证时暴露 Keychain unavailable 后静默丢凭据问题：页面会短暂进入 Main，但 Profile 判断未登录。已修复为 `IosKeychainCredentialStore` 写入失败时抛出稳定本地错误，AuthRepository 不会再把凭据持久化失败误判为认证成功。
- 退出登录 passed：Profile 点击退出后回到短信登录根页面。
- 短信验证码 WebView smoke passed：输入测试手机号后点击获取验证码，WKWebView 完整渲染 TAC 滑块验证；本轮补齐验证码关闭后的状态层防线，旧 token 不会在用户关闭弹窗后触发短信重试，Android/iOS Web 容器 dispose 后也会停用旧回调，避免连续打开时串到当前状态层。HTML 包装层已用 `SmsCaptchaHtmlTest` 覆盖网络/script 失败、图片解码失败和超时的可重试降级状态，并确认失败状态不暴露底层 URL 或平台诊断文本。2026-07-07 已在 iPhone 17 Pro signed Simulator 人工完成 TAC 滑块，短信发送成功并完成 SMS 登录；runtime/os log 脱敏扫描未命中 token、Cookie、手机号、验证码、远端 URL 或平台异常原文。
- QuickCreate side-effect smoke passed：图片模式输入提示词，计费确认弹层显示预计消耗和余额，确认后提交任务并轮询到成功；历史详情中任务 `2074005268555067394` 显示绿色圆形结果图。
- iOS 图片 picker/media upload passed：从 Photo Library 选择绿色圆形图片后回到创作页，日志显示 `/openapi/v2/media/upload/binary` 返回 200，请求体约 923 KB。
- iOS 图片保存 passed：历史详情保存图片触发 PhotoKit 写入，日志显示 `performChanges` 成功；本轮同时补齐保存成功后的详情 UI 状态回写，最新 signed Simulator build 验证标签从“未保存到本地”更新为“已保存到本地”。`IosMediaSaverTest` 已覆盖图片/视频下载失败不先请求 PhotoKit 写权限、PhotoKit 写权限拒绝不继续写入、PhotoKit 写入失败归一化为 `WRITE_FAILED`，并覆盖视频保存成功语义；`TaskHistoryMediaSaveActionTest` 覆盖历史详情图片输出走图片保存、视频输出走视频保存、文件输出不调用平台保存。
- QuickCreate 视频 tab 素材入口 smoke passed：最新 signed Simulator build 中切到视频 tab 后点击紧凑输入条加号，系统 picker 顶部显示“视频”，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_72ac8ab4-c44d-4abd-8978-0bcf65e7de74.jpg`。随后用 `ffmpeg` 生成 2.3 KB、1 秒 MP4，通过 `xcrun simctl addmedia` 导入相册，选中后回到 QuickCreate 并出现视频素材卡，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_7b234011-1f7b-4fe8-a408-3960ce06c15c.jpg`；当前 runtime log 未输出上传 HTTP 细节，因此只记录视频选择、URI 回传和素材引用通过，不把视频上传 200 写成已验证。继续提交 `seedance2.0` 视频模型时，旧实现先暴露红色 toast `conversionSlots 选项无效`；按 RunningHub 官方接口文档复核后，`sparkvideo-2.0/text-to-video` body 不含 `conversionSlots`，多模态文档中的 `conversionSlots` 语义为真人素材槽位。本项目已有 `realPersonMode` 真人开关，因此 Seedance2.0 全接口族在客户端忽略 `conversionSlots`，不展示也不下发；文生/图生/多模态字段按官方 body 白名单过滤。`duration` 仍按官方 4-15 秒补齐 UI 与校验，上传字段默认占位 `9/3/3` 不进入 `quickCreationParams`，只有真实上传结果进入数组参数；v2 视频请求也已移除本地附加的 `creationMode/creationSubModeId/creationSubModeKey`。2026-07-06 重新 signed Simulator build 后，视频紧凑入口已从旧的 `12 个参数` 变为 `10 个参数`，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_e0d1bf62-1e84-412f-878c-d682c390430d.jpg`；参数面板首屏显示 `resolution/imageUrls/videoUrls/audioUrls/ratio`，未再暴露 `conversionSlots`，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_c327748f-9af3-4fb8-8503-4909443ca172.jpg`。本轮随后将 iOS 图片/视频选择器迁到 `PHPickerViewController`，不再为选择素材预先申请整库 PhotoKit 权限；最新 signed Simulator build 进程 `9991` 打开 PHPicker 视频页的证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_5d62c71f-ede9-443e-b9e1-6d5d7243b5f7.jpg`，选中 1 秒绿色测试视频后回到 QuickCreate 并出现视频素材卡，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_90437ea9-a10e-4782-a8a7-93139c9d034c.jpg`。本轮未确认扣费级视频生成，远端视频上传 200 和视频生成任务提交仍未验证。
- QuickCreate 参数音频入口 smoke passed：最新 signed Simulator build 进程 `14219` 打开视频模型参数面板后，`audioUrls` 上传按钮完整可见，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_64ce380c-7846-425a-9bda-02e50e34238f.jpg`；点击后打开 iOS Files/Document Picker“最近项目”，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_d7592cb1-610d-49c1-b060-392719eae2a9.jpg`。本轮只验证音频选择器入口，不声明音频文件读取或上传 200。
- AppDetail 图片参数入口 smoke passed：最新 signed Simulator build 进程 `75369` 打开“全能图片PRO-图生图-低价渠道版”详情后，`配置参数 7 个参数`、`上传图片 *` 和三个 `点击上传...` 图片上传卡片在首屏可见，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_2bc8d772-e300-4213-b057-e0b7e547ebb7.jpg`；点击第一个上传卡后打开 iOS Photo Picker，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_81eb2157-fd06-46f5-9084-911d4f948a30.jpg`；选择图片后回到详情并显示本地预览，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_2b2ccd80-abe7-41b4-9d49-4df8a3f6a7fd.jpg`；点击“立即运行”后按钮进入“提交中”并转为“生成中”，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_ed98c66d-e7cb-4aab-ad41-60da9ee58349.jpg`，等待后页面回到“重新运行”，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_329dc8ba-6550-449f-a503-a46565e57032.jpg`。后续截图已观察到同一详情页进入 `生成完成` 并显示生成结果图，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_8011d0fc-3cfa-43bb-8442-35e498dd4f34.jpg`。runtime log 仍未输出上传接口明细，因此本轮可声明 AppDetail 图片选择、本地预览、任务提交和结果 UI 链路通过，但不声明远端上传 200。

### P3：发布前人审

TestFlight 或生产发布前不能自动完成，必须由负责人确认：

- 证书、签名、App Store Connect 账号、bundle id、版本号和构建号。
- 隐私表单与 `Info.plist`、`PrivacyInfo.xcprivacy` 一致；当前自动化只覆盖 privacy manifest 打包和 accessed API reason 静态字段。
- 真机 Keychain、Photo Library Limited、文档选择器、大视频上传、Android 真实 WebView 验证码和 TestFlight WKWebView 边界。

## 5. 推荐执行顺序

### 阶段 0：冻结本轮边界

允许修改：

- `docs/migration/ios-completion-plan.md`
- 后续实现阶段按任务切片修改 `composeApp/src/iosMain`、`iosApp/`、`core/storage/src/iosMain` 和必要测试。

禁止修改：

- Android 已完成路径，除非 iOS 修复暴露共享接口缺陷。
- Feature Domain/Data/Presentation 的业务语义，除非 iOS 编译或运行实证证明共享契约缺失。
- 任何凭据、构建产物、签名文件和外部证据的手工伪造。

### 阶段 1：macOS 构建基线

1. 恢复 `gradlew` 可执行位或统一以 `bash ./gradlew` 临时运行，并记录选择。已完成：当前选择恢复可执行位。
2. 运行 P0 命令。Gradle 架构/治理、iOS compile、iOS framework link、Xcode Debug Simulator build 已通过。
3. 如果 `MediaSaver.ios.kt` 编译失败，先判断当前未提交实现是否接纳、修正或暂存隔离。当前结果：该文件随 iOS compile/link 通过，图片保存到 Simulator Photo Library 也已通过 signed Simulator smoke；真实系统拒绝弹窗和真实远端视频保存仍按人工验收模板补证。
4. 生成失败摘要，避免在未 link 通过时继续做 UI/权限修复。当前没有构建失败摘要；剩余未验证项是 TestFlight/真机边界、系统权限弹窗和人工 CAPTCHA 成功回传。

### 阶段 2：Simulator 启动与登录链

1. 用 Xcode 或 `run-ios-simulator-smoke.sh` 安装启动 Debug app；脚本会创建临时 Simulator、安装 app、启动 bundle、截图并扫描启动崩溃关键词。
2. 手动验证登录页渲染、短信验证码弹窗、token 回传、关闭和连续打开。
3. 验证登录后主 Tab、退出登录后根页面回 Login。
4. 补充或更新 `ios-macos-link-and-simulator.md`，只在真实操作后写 pass。

2026-07-06 当前进展：

- Signed Simulator build 下密码登录、Keychain 会话恢复和主 Tab 加载已通过。
- 退出登录回 Login 已通过。
- SMS captcha WKWebView 加载已通过，滑块验证正常渲染；验证码关闭后旧 token 被 `LoginStateHolder` 忽略，Android WebView 与 iOS WKWebView 容器销毁时会停用旧回调，降低连续打开串回调风险；网络/script 失败、图片解码失败和超时已进入可重试降级状态并由 `checkLongTermGovernance` 守住。2026-07-07 已在 iPhone 17 Pro signed Simulator 人工完成 TAC 滑块，短信发送成功并完成 SMS 登录；runtime/os log 脱敏扫描未命中 token、Cookie、手机号、验证码、远端 URL 或平台异常原文。Android 真实 WebView 和 TestFlight 边界仍需发布前专项复核。

### 阶段 3：媒体与任务链

1. 验证图片、视频、音频三类选择器。
2. 验证 `MediaResolver` 读取字节、文件名和文件大小；大视频先验证上传前大小门禁，若需支持超过 100MiB 的视频再做流式上传和内存观察。
3. 在 AppDetail 和 QuickCreate 各完成一次素材上传和任务提交。
4. 验证任务成功、失败、取消和轮询停止。
5. 验证结果图片保存到相册；拒绝权限和网络失败不透传 URL 或平台错误。

2026-07-06 当前进展：

- QuickCreate 图片生成、计费确认、远端提交、轮询、历史列表和详情均已通过 signed Simulator smoke。
- 图片素材选择、读取和上传已通过 signed Simulator smoke。
- 图片结果保存到 Simulator Photo Library 已通过，且保存成功后的历史详情标签已修正为“已保存到本地”。
- AppDetail signed Simulator smoke 已补一轮：发现页可进入 WebApp 详情；“全能视频X-图生视频-官方稳定版-v1.5”点击“立即运行”后进入排队阶段，证明详情默认参数提交流程可触发远端任务。另打开“废土笔触重绘废土笔触重绘7.4”可见 `配置参数 3 个参数`，初始证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_af907929-e054-4acd-a08b-624d1a2d647b.jpg`；继续向下滚动后 `cfg`、`strength_model`、`steps` 三个输入框完整可见，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_46dfe7da-d378-4e9f-bead-640bf9c625d4.jpg`。最新布局修复后，“全能图片PRO-图生图-低价渠道版”详情可见图片上传卡并能打开 iOS Photo Picker；后续 signed Simulator 截图已观察到 `生成完成` 和结果图显示；本轮补齐图片/视频/音频 picker 取消不会进入权限拒绝的自动化契约，并由治理门禁守住 iOS PHPicker 空结果、类型不匹配和空 URI 的取消分支。2026-07-07 已补齐 AppDetail/WebAppTask 上传 multipart file part 的 `Content-Type`，避免图片、视频或音频仅靠普通 `fileType` 字段导致服务端或网关媒体识别不稳定；但 AppDetail 远端媒体上传 200 仍未在 runtime log 中确认。
- 视频选择器入口和本地视频引用已通过 signed Simulator smoke：QuickCreate 视频 tab 的紧凑输入条加号打开系统 PHPicker 视频页，导入测试 MP4 后可选择并回到页面形成视频素材卡；`IosPickedMediaCopyTest` 覆盖 PHPicker 返回的图片和视频临时文件会复制到 app-owned 临时 URI，源文件移除后仍可由 iOS `MediaResolver` 读取；`checkLongTermGovernance` 已防止 `presentPhotoPicker` 重新调用 `PHPhotoLibrary.authorizationStatus()` 或 `PHPhotoLibrary.requestAuthorization` 做整库权限预检。Seedance2.0 已改为按官方文档字段白名单展示和提交参数，`conversionSlots` 作为真人素材槽位由客户端忽略，`realPersonMode` 继续作为真人开关保留。2026-07-06 最新 signed Simulator build 已确认视频紧凑入口显示过滤后的 `10 个参数`，参数面板首屏不再展示 `conversionSlots`；随后在进程 `22401` 打开视频 PHPicker 后点击取消，页面回到 QuickCreate 视频 tab、输入区仍为空、底部恢复素材加号，未出现新增素材卡或错误 toast，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_46798e17-b717-4d87-8066-76e9fd5057fb.jpg`。2026-07-07 补齐 QuickCreate 上传扩展名/MIME 契约：视频会保留 `.mov/.webm/.mp4` 并提交 `video/quicktime`、`video/webm` 或 `video/mp4`，上传文件名仍使用通用前缀，避免把用户本地文件名透传给服务端。本轮未确认扣费级视频生成，视频远端上传 200 和视频生成任务提交仍未通过。
- 音频选择器入口与取消路径已通过 signed Simulator smoke：QuickCreate 视频模型参数面板中的 `audioUrls` 按钮可见，点击后打开 iOS Files/Document Picker。旧实现把 Document Picker 取消回调接到权限拒绝，进程 `26951` 从“我的 iPhone”空浏览页关闭后会误弹“需要读取您的音频文件”授权面板；本轮已在 `PermissionController.pickMedia` 增加独立 `onPickerCancelled` 回调，并让 iOS `UIDocumentPicker`/`PHPicker` 取消走取消分支。最新 signed Simulator build 进程 `43716` 复测同一路径后回到更多参数面板，`audioUrls` 仍显示文件 0MB，未新增素材、未出现错误提示、未再弹权限说明，证据截图为 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_cf8815a6-21e0-409b-83f2-48452e17562b.jpg`。音频文件读取在 `IosMediaResolverTest` 中覆盖 file URL actual 路径，AppDetail 音频上传在 `AppDetailScreenModelTest` 中覆盖 bytes/MIME/远端文件名写回契约；2026-07-07 进一步补齐 QuickCreate 音频上传扩展名/MIME 契约，`.m4a/.wav/.aac/.ogg/.mp3` 会映射到匹配 MIME，且上传文件名仍使用通用前缀。真实文档选择器文件导入、远端音频上传 HTTP 结果和 AppDetail 远端媒体上传 200 仍未在本轮 smoke 中覆盖；已尝试 Simulator LocalStorage、全局 Documents 和 `simctl openurl file://...` 放入测试音频，Files 索引/打开行为不稳定，不作为当前阻塞项。
- 大视频/大音频已先补读入前门禁和失败提交阻断：QuickCreate 字段素材遵循服务端 `maxUploadSize` 和 100MiB 本地保护上限，QuickCreate 全局素材与 AppDetail 素材遵循 100MiB 本地保护上限；QuickCreate 上传失败会在生成前阻断，AppDetail 上传中或上传失败会阻断 `runTask`，避免继续提交缺失素材参数。100MiB 以内仍会一次性 `NSData` -> `ByteArray`，更大文件能力需要后续流式上传专项。

### 阶段 4：发布就绪补强

1. 对照 `docs/governance/release-readiness-checklist.md` 补齐 iOS TestFlight 项。
2. 若产品需要相机拍摄或通知，新增真实 iOS 实现；若不需要，清理入口或调整权限说明。
3. 图片/视频选择器已迁到 `PHPickerViewController`，避免选择素材前申请整库 PhotoKit 权限；治理门禁已阻止 `presentPhotoPicker` 重新做 PhotoKit 授权预检；`IosPickedMediaCopyTest` 已覆盖图片/视频临时文件复制到 app-owned URI 后仍可读取；视频 PHPicker 取消路径已通过 signed Simulator smoke；权限恢复入口已在 PhotoKit Limited 状态下路由到系统有限照片管理页，后续仍需补 Limited Photos 管理页真实追加/取消运行验证。
4. 对 Keychain 升级、卸载、重装和退出登录后的真机/TestFlight 边界做补验。

## 6. 验证矩阵

| 层级 | 命令或动作 | 通过含义 |
|---|---|---|
| 文档 | 对新文档和 `docs/README.md` 执行模板占位符与空事实关键词扫描 | 新文档无模板占位和空事实 |
| CodeGraph | `codegraph status` | 记录当前索引和 pending changes，不把旧索引当新改动事实 |
| 架构门禁 | `./gradlew --console=plain checkArchitectureBoundaries` | shared、commonMain 平台 API、依赖方向未回退 |
| 治理门禁 | `./gradlew --console=plain checkLongTermGovernance` | iOS 权限、媒体、Keychain、验证码和发布清单防退化规则仍在 |
| iOS Kotlin | `./gradlew --console=plain :composeApp:compileKotlinIosSimulatorArm64` | 共享 UI 与 iOS actual 可编译 |
| iOS framework | `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64` | Compose framework 在 macOS 真实 link |
| Xcode | `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO` | SwiftUI 壳能消费当前 Compose framework |
| Simulator launch | `docs/migration/run-ios-simulator-smoke.sh` | 证明 Debug app 可安装启动、产出截图且未命中启动崩溃关键词；不替代登录态用户流程 |
| Authenticated Simulator | 登录、退出、QuickCreate、媒体上传、验证码、图片保存 | 证明主要用户流程不是只有构建通过 |
| 封板证据 | `./gradlew --console=plain checkL1SealEvidence` | 仅在五类外部证据全部绑定当前 HEAD 时才可通过 |

## 7. 当前明确风险

- `MediaSaver.ios.kt` 图片保存已通过 signed Simulator 运行验证；图片/视频下载失败、写权限拒绝和写入失败已有 `IosMediaSaverTest` 自动化契约覆盖，其中写权限拒绝会返回独立 `PHOTO_PERMISSION_DENIED`，再由 `recoverablePermission()` 让 QuickCreate 与 History 复用权限底部弹窗，并在永久拒绝时调用 `openAppSettings()`；历史详情视频输出保存分发已有 `TaskHistoryMediaSaveActionTest` 覆盖。真实系统弹窗拒绝路径和真实远端视频保存仍未做专项运行覆盖。
- `./gradlew` 可执行位已恢复；这会作为本轮文件模式变更留在工作区。
- iOS 证据文件已更新为当前 `HEAD` 的 pass 记录，并包含 signed Simulator 登录、退出、QuickCreate、图片上传 200、视频 picker 入口、音频 Document Picker 入口和图片保存 smoke；远端 video/audio/AppDetail 上传 200、TestFlight 和真机升级边界仍不在该证据声明范围内。
- Release checklist 中的 Keychain、WKWebView、PHPicker 和文档选择器路径冒烟项已按上述 signed Simulator evidence 标记为当前工作区已覆盖；这不等同于真实音频文件导入、video/audio 上传 200、Android 真实 WebView、真机或 TestFlight 通过。
- 2026-07-07 通过 XcodeBuildMCP 执行当前 default profile 的 iOS Simulator build，结果为 `SUCCEEDED`，构建日志为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/build_sim_2026-07-07T02-22-38-026Z_pid88023_dd3982a2.log`；本次只证明 Xcode 构建，不替代新的登录或媒体运行 smoke。
- 2026-07-07 通过 XcodeBuildMCP 执行 signed Simulator `build_run_sim`，结果为 `SUCCEEDED`，进程为 `22624`，构建日志为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/build_run_sim_2026-07-07T03-40-44-748Z_pid88023_a201ea79.log`，runtime log 为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/com.runninghub.app.ios_2026-07-07T03-41-20-511Z_helperpid22576_ownerpid88023_daadd8a0.log`；截图 `/var/folders/nv/4qwbj0z538jbxgb62zmhcf6w0000gn/T/screenshot_optimized_27ef2dca-4e71-48d1-b3b2-333baf3ee8a6.jpg` 显示应用保持在已登录的发现页，启动崩溃关键词扫描通过。本次只记录启动与登录态保持，不继续扩展远端上传 smoke。
- `CODE_SIGNING_ALLOWED=NO` 的 Simulator 构建可证明 Xcode wrapper build，不应用来证明 Keychain 登录态；authenticated smoke 需要 signed Simulator build 或真机/TestFlight 环境。
- `./gradlew --console=plain verifyL1Ios` 已在 2026-07-07 复跑通过；最新一次覆盖 `checkArchitectureBoundaries`、`checkL1CiWorkflows`、`checkLongTermGovernance`、`checkMigrationScripts` 和 iOS Debug framework link。
- Camera 和 Notifications 已有 iOS 平台授权实现和编译证据；`IosPermissionAuthorizationMappingTest` 已覆盖 PhotoKit limited/denied/restricted、AVFoundation denied/restricted、UserNotifications provisional/denied 等系统状态到 granted/denied/permanently denied 的映射，并覆盖 PhotoKit 允许、拒绝和 Limited 三种结果写入 `PermissionStateStore` 的记录路径。2026-07-07 新增 PhotoKit Limited 管理入口静态契约：图片、视频和 StorageRead 权限在 Limited 状态下打开系统有限照片管理页，Camera 等其他权限仍进入 App Settings；`Info.plist` 同步设置 `PHPhotoLibraryPreventAutomaticLimitedAccessAlert=true`，避免系统自动 limited alert 绕过应用内恢复入口。2026-07-07 signed Simulator 已真实触发照片权限弹窗并选择“不允许”，页面未崩溃，展示稳定保存失败提示和权限恢复引导，日志扫描未命中远端 URL、敏感 token 或平台异常原文；同日已选择“限制访问…”进入系统 Limited Photos 管理页并完成返回，QuickCreate 页面未新增素材或错误。源码复核显示当前产品 UI 未暴露 Camera/Notifications 调用点，不能为验证新增用户不可见入口；有产品入口后再做允许、拒绝和设置页运行验证。
- iOS `MediaResolver` 的普通 file URL 字节、文件名和大小读取已由 `IosMediaResolverTest` 覆盖；QuickCreate/AppDetail 已在上传前按文件大小阻断超过服务端字段上限或 100MiB 本地保护上限的文件，并在上传失败或上传中阻断生成任务提交。100MiB 以内媒体读取当前仍一次性加载 `NSData` 到 `ByteArray`，如果产品要求更大视频上传，需要先改领域上传接口为流式传输再做真机内存专项。
- WKWebView 验证码 TAC 资源加载和滑块渲染已通过真实运行验证；Android `SmsCaptchaEnvironmentTest` 与 iOS `SmsCaptchaEnvironmentTest` 已覆盖平台 base URL 跟随 `RunningHubApiEnvironment.WEB_BASE_URL`，配合 `SmsCaptchaHtmlTest` 锁定 `/tac/js`、`/tac/css` 和 `/uc` 相对路径，防止 debug/staging 验证码和短信接口跨环境；验证码关闭和连续打开的旧回调隔离已有 `LoginStateHolderTest`、Android WebView dispose gate 和 iOS WKWebView handler/delegate dispose gate 覆盖；网络/script 失败、图片解码失败和超时的可重试降级状态已有 `SmsCaptchaHtmlTest` 与治理门禁覆盖；2026-07-07 新增静态脱敏门禁，阻止验证码 HTML、Android WebView 回调、iOS WKWebView 回调或登录状态层把 `validToken`、原始 TAC 响应写入 Web console/native log；同日已在 iPhone 17 Pro signed Simulator 人工完成 TAC 滑块，短信发送成功并完成 SMS 登录，runtime/os log 脱敏扫描未命中 token、Cookie、手机号、验证码、远端 URL 或平台异常原文。Android 真实 WebView 和 TestFlight 边界仍需发布前专项复核。
- AppDetail 图片参数详情已验证选择器、本地预览、提交状态链和结果 UI；2026-07-07 已补齐上传请求体契约，multipart file part 会携带 `Content-Type`，并由 `WebAppTaskApiTest` 与 `checkLongTermGovernance` 守住；`AppDetailScreenModelTest` 已覆盖 `.m4a -> audio/mp4` 和 `.mov -> video/quicktime` 从平台 URI 回调到上传仓库的 MIME 传递；runtime log 未输出上传接口明细，因此远端媒体上传 200 仍需专项补验。
- QuickCreate Seedance2.0 已按官方接口族白名单过滤高级参数：文生视频不展示或下发 `conversionSlots`，多模态视频也将 `conversionSlots` 作为真人素材槽位忽略，避免把资产化槽位暴露给普通用户；`realPersonMode` 作为真人开关保留。当前代码已按文档补齐 `duration` 4-15、过滤上传字段占位默认值，并移除 v2 视频请求的本地附加 `creationMode/creationSubModeId/creationSubModeKey`；最新 signed Simulator smoke 已确认高级参数入口不再暴露 `conversionSlots`，视频 PHPicker 和音频 Document Picker 取消路径不污染页面状态；图片/视频 PHPicker 不预申请整库 PhotoKit 权限和 app-owned 临时 URI 复制已由治理门禁与 `IosPickedMediaCopyTest` 守住；2026-07-07 已补齐 QuickCreate 上传扩展名/MIME 契约，视频和音频不再统一伪装成 `.mp4` 或 `.mp3`。远端视频上传 200、真实音频文件导入和扣费级视频生成任务提交仍需补验。
- 2026-07-07 用户报告“所有视频都不显示”后已完成根因修复：历史详情、QuickCreate 对话结果和 QuickCreate 历史详情不再把视频原文件 URL 交给 `SmartAsyncImage` 当图片解码，而是按稳定媒体类型走 `VideoThumbnail`；`GenerationHistoryOutput` 会识别带 query/hash 的 `.mp4/.mov/.webm/.m4v` URL，视频无独立封面时 `displayThumbnailUrl` 返回 null，列表缩略图不会再回退到视频原地址；AppDetail 任务结果也支持签名视频 URL 后缀识别。已用 `GenerationHistoryTest`、`TaskHistoryStateHolderTest` 和 `AppDetailTaskResultTest` 覆盖签名视频 URL、无封面视频缩略图和 AppDetail 视频判断，并复跑 `:feature:task:domain:iosSimulatorArm64Test :feature:task:presentation:iosSimulatorArm64Test :composeApp:iosSimulatorArm64Test checkLongTermGovernance` 通过。该修复证明视频结果不再因图片解码路径空白；不等同于远端视频上传 200 或扣费级视频生成通过。
- 2026-07-07 已复跑 `:composeApp:iosSimulatorArm64Test` 的关键 iOS 自动化集合，覆盖 `IosPermissionAuthorizationMappingTest`、`IosMediaSaverTest`、`IosMediaResolverTest`、`IosPickedMediaCopyTest`、`PermissionControllerContractTest`、`MediaSaveResultRecoveryTest` 和 `SmsCaptchaEnvironmentTest`，命令通过。这组测试证明权限映射、PhotoKit 保存失败语义、file URL 读取、PHPicker app-owned 临时文件复制、picker 取消分支、保存权限恢复映射和验证码环境配置没有回退；它不替代真实权限弹窗、真实 Files 音频导入、Android 真实 WebView 或远端 video/audio 上传 200。
- 2026-07-07 已复跑非上传目标测试：`:composeApp:iosSimulatorArm64Test --tests com.runninghub.app.platform.IosMediaSaverTest --tests com.runninghub.app.platform.IosPermissionAuthorizationMappingTest --tests com.runninghub.app.platform.MediaSaveResultRecoveryTest --tests com.runninghub.app.ui.feature.login.SmsCaptchaEnvironmentTest --tests com.runninghub.app.ui.feature.login.SmsCaptchaHtmlTest --tests com.runninghub.feature.auth.presentation.login.LoginStateHolderTest`，命令通过。这组测试证明 PhotoKit 保存拒绝/写入失败语义、Camera/Notifications/PhotoKit 授权映射、保存权限恢复入口、验证码环境同源、验证码失败降级、token 静态脱敏和关闭后旧 token 隔离没有回退；它不替代 Limited Photos 管理页真实交互、系统权限弹窗允许/拒绝、Android 真实 WebView 或 TestFlight 人审。
- 2026-07-07 已复跑 `:core:storage:iosSimulatorArm64Test --tests com.runninghub.core.storage.IosKeychainCredentialStoreTest`，命令通过。这组测试证明 iOS Keychain backed `CredentialStore` 在当前 Simulator 环境下可写入、读回并清理 access token/refresh token；若 Keychain unavailable，写入失败必须显式暴露为稳定本地错误，不能静默丢凭据。它不替代真机/TestFlight 的升级、卸载、重装或系统 Keychain 访问组边界验证。
- 2026-07-07 已复跑 `:feature:quickcreate:presentation:iosSimulatorArm64Test` 与 `:feature:detail:presentation:iosSimulatorArm64Test`，命令通过。这组测试证明 QuickCreate 参数过滤、生成请求工厂、上传协调器失败阻断和 AppDetail 上传中/上传失败提交防线没有回退；它不替代 AppDetail 远端媒体上传 200、QuickCreate 远端视频上传 200 或扣费级视频生成提交。
- 2026-07-07 已复跑覆盖当前 iOS 主链路的 Simulator 自动化集合：`:composeApp:iosSimulatorArm64Test :core:storage:iosSimulatorArm64Test :feature:quickcreate:domain:iosSimulatorArm64Test :feature:quickcreate:data:iosSimulatorArm64Test :feature:quickcreate:presentation:iosSimulatorArm64Test :feature:detail:presentation:iosSimulatorArm64Test :feature:task:domain:iosSimulatorArm64Test :feature:task:data:iosSimulatorArm64Test :feature:task:presentation:iosSimulatorArm64Test :feature:auth:presentation:iosSimulatorArm64Test :feature:auth:data:iosSimulatorArm64Test`，命令通过。这组测试证明 composeApp 平台层、iOS Keychain/storage、QuickCreate domain/data/presentation、AppDetail presentation、Task domain/data/presentation、Auth data/presentation 在当前工作区没有 iOS Simulator 自动化回归；它不替代真实音频 Files 导入、远端 video/audio/AppDetail 上传 200、Android 真实 WebView、真机或 TestFlight。
- 2026-07-07 已执行 Release iphoneos archive dry run：`xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-Release.xcarchive CODE_SIGNING_ALLOWED=NO`，命令通过。该历史证据证明当时 Release `iosArm64` ComposeApp framework link、Xcode wrapper archive、asset catalog、processed `Info.plist`、`PrivacyInfo.xcprivacy` 打包和 Xcode store validation dry run 没有失败；archive 内 `RunningHub.app/RunningHub` 是 arm64 Mach-O。`ComposeApp.framework` 当前为 static framework，已静态链接进主二进制，archive app 内没有独立 `Frameworks/ComposeApp.framework` 属于当前链接形态。视频显示修复后，当前工作区又复跑 `:composeApp:linkReleaseFrameworkIosArm64` 与 `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' build CODE_SIGNING_ALLOWED=NO`，两条命令均通过，证明当前代码的 Release KMP framework link 和 Xcode wrapper arm64 app build 没有回退；一次完整 archive 复跑因耗时被人工中断，不能作为通过证据。上述 dry run 不执行真实签名、IPA 导出或 TestFlight 上传；Release build settings 与 archive metadata 均显示 Team/SigningIdentity 为空。本机存在有效 Apple Development/Distribution signing identity，但本地 provisioning profile 数量为 0；签名 archive 探测未带 `CODE_SIGNING_ALLOWED=NO` 时 exit 65，错误为 `Signing for "RunningHub" requires a development team`。继续用命令行覆盖 Distribution team 和 Development team 并开启 `-allowProvisioningUpdates` 后，均失败为 `No Accounts` 与没有匹配 `com.runninghub.app.ios` 的 iOS App Development provisioning profile；手动覆盖 Apple Distribution identity 还会触发自动 development signing 与 distribution identity 冲突。发布前仍需负责人在 Xcode Accounts 登录可管理该 bundle id 的 Apple Developer 账号，并配置 Team/provisioning 后再确认真实签名。

## 8. 下一步执行队列

当前仍不能把 iOS/L1 标记为封板。剩余工作按是否能继续自动化推进分为两组：

### 8.1 可继续自动化推进

1. 推送当前 iOS 补全提交后，重新采集 Android/iOS GitHub Actions 证据；macOS iOS evidence 已在当前 `HEAD` 上重采。
2. 运行 `checkL1SealEvidence` 前，确保工作区只暂存允许的外部证据文件；不要为了通过门禁暂存业务源码、构建脚本或文档补丁。
3. 上传链路不再作为当前自动化主线继续扩展：QuickCreate 图片上传 200、视频/音频扩展名与 MIME、AppDetail multipart file part `Content-Type`、AppDetail `.m4a/.mov` MIME 传递、上传中/上传失败阻断提交均已有自动化或 signed Simulator 证据。只有真实运行中出现明确用户可感知失败，例如上传实际返回错误、任务因此无法提交、崩溃或敏感信息泄漏时，才回到上传实现修复。
4. 若要支持超过 100MiB 的真实视频或音频，先把领域上传接口改为流式传输，再做真机或专门环境内存专项 smoke；不要在当前 `NSData` -> `ByteArray` 路径上放宽保护上限。

### 8.2 需要真实环境或负责人确认

1. 真实音频文件导入与远端上传 200：当前已验证 Document Picker 入口、取消路径和 iOS file URL 读取 actual；Simulator Files 注入音频多次尝试后索引/打开行为不稳定，后续应在人工可控 Files 环境或真机上验证。
2. Android 真实 WebView 验证码和 TestFlight WKWebView 边界：iOS signed Simulator 已完成 TAC、短信发送和 SMS 登录；Android 真机/WebView 与 TestFlight 仍需发布前专项复核。
3. Camera/Notifications 系统弹窗：相册写入拒绝和 Limited Photos 管理页返回已在 signed Simulator 补验；Camera 和 Notifications 目前没有产品 UI 入口，不能为验证新增用户不可见入口。当前 `xcrun simctl privacy` 只支持 `photos`/`photos-add` 的 grant/revoke/reset，不支持设置 Limited Photos 状态，不能用它替代受限照片管理页真实交互。
4. TestFlight 账号、证书、Team、provisioning profile、bundle id、版本号、构建号和隐私表单：Release iphoneos archive dry run 有历史通过记录，视频显示修复后的当前代码已通过 Release `iosArm64` framework link 和 Xcode Release iphoneos build，但当前 Xcode 配置 `DEVELOPMENT_TEAM` 为空，archive metadata 中 `SigningIdentity` 和 `Team` 为空；2026-07-07 复核显示本机仍有有效 Apple Development/Distribution signing identity，但本地 provisioning profile 数量为 0；未带 `CODE_SIGNING_ALLOWED=NO` 的 signed archive 探测仍 exit 65，错误为 `Signing for "RunningHub" requires a development team`。必须由负责人配置并确认真实签名、导出、上传和 App Store Connect 隐私表单，AI 代理不得自动代签、上传或替负责人确认，也不得把 dry run 等同 TestFlight 通过。

### 8.3 人工验收记录模板

后续补人工证据时，优先把结果写回 `docs/migration/evidence/ios-macos-link-and-simulator.md` 或 `docs/migration/l1-external-evidence.md`，再同步本计划和 release checklist。每条记录至少包含设备、系统版本、构建来源、账号状态、截图或日志路径、通过/失败结论和未覆盖项。

| 项目 | 执行动作 | 通过证据 | 失败时处理 |
|---|---|---|---|
| Limited Photos 管理页 | 2026-07-07 已在 iPhone 17 Pro Simulator 上选择“限制访问…”进入系统 Limited Photos 管理页，完成返回后 QuickCreate 页面未新增素材或错误 | 系统有限照片管理页出现，返回应用后页面状态未新增错误或素材；后续真机发布前可专项复核追加选择的明确勾选反馈 | 若恢复入口进入 App Settings 或污染页面状态，再回到 `PermissionController.ios.kt` 修复 |
| 相册写入拒绝 | 2026-07-07 已在 iPhone 17 Pro Simulator 上重置 `photos-add` 后触发保存结果，选择“不允许” | 系统权限弹窗出现；拒绝后页面展示“保存失败，请重试”和权限恢复引导；runtime/os log 关键字扫描未命中远端 URL、敏感 token 或平台异常原文 | 已覆盖；若后续真机出现崩溃、静默失败或暴露 URL/异常文本，再回到 `MediaSaver.ios.kt` 与调用页面修复 |
| Camera/Notifications 弹窗 | 从实际 UI 入口触发相机或通知授权，分别验证允许、拒绝、再次进入设置 | 系统弹窗截图、允许/拒绝后的页面状态和 `PermissionStateStore` 行为记录 | 如果当前产品未暴露入口，保持人工项，不为测试新增用户不可见入口 |
| 验证码成功回传 | 2026-07-07 已在 iPhone 17 Pro signed Simulator 使用测试账号触发短信验证码并人工完成 TAC 滑块 | 短信发送成功，SMS 登录进入账号页；runtime/os log 脱敏扫描未命中 `validToken`、原始 TAC 响应、token、Cookie、手机号、验证码、远端 URL 或平台异常原文 | Android 真实 WebView 和 TestFlight 边界仍需发布前专项复核；若 token 回调失败、跨域或 Cookie/ATS 异常，记录 WebView/OS log 后回到 `SmsCaptchaDialog.ios.kt`、`SmsCaptchaHtml.kt` 或环境配置修复 |
| TestFlight 人审 | 负责人确认 App Store Connect 账号、证书、Team、provisioning profile、bundle id、版本号、构建号、隐私表单和导出合规项；当前代码已确认 Release framework link 和不签名 Xcode build 通过，但 Team/SigningIdentity 为空，签名 archive 探测因缺 Xcode account/profile 失败 | 负责人签字或工单链接、构建号、隐私表单截图或审核记录；真实签名 archive、IPA 导出和 TestFlight 上传记录 | AI 代理不得代签、代上传或替负责人确认；缺证据时不得标记 L1/iOS 封板 |

### 8.4 当前自动化停线

在未收到新的真实失败证据或负责人授权前，当前 iOS 自动化实现工作到此收口。后续不再因为“证据更完整”而扩展上传、权限弹窗或验证码自动化；只允许继续做以下三类动作：

1. 推送后按当前 `HEAD` 重采 Android/iOS GitHub Actions 外部证据。
2. 人工或专门环境补齐 8.3 模板中的真实交互证据，并把证据路径和结论写回文档。
3. 如果真实运行出现用户可感知失败、崩溃、敏感信息泄漏、任务无法提交或封板门禁新增非证据类失败，再回到对应实现修复。

已运行的 macOS iOS 构建基线：

```bash
chmod +x gradlew
./gradlew --console=plain checkArchitectureBoundaries checkLongTermGovernance
./gradlew --console=plain :composeApp:compileKotlinIosSimulatorArm64
./gradlew --console=plain :composeApp:compileTestKotlinIosSimulatorArm64 :composeApp:iosSimulatorArm64Test
./gradlew --console=plain :composeApp:iosSimulatorArm64Test --tests com.runninghub.app.platform.IosMediaSaverTest
./gradlew --console=plain :composeApp:iosSimulatorArm64Test --tests com.runninghub.app.ui.feature.history.TaskHistoryMediaSaveActionTest
./gradlew --console=plain :composeApp:iosSimulatorArm64Test --tests com.runninghub.app.platform.IosPermissionAuthorizationMappingTest
./gradlew --console=plain :composeApp:iosSimulatorArm64Test --tests com.runninghub.app.platform.IosPermissionAuthorizationMappingTest --tests com.runninghub.app.platform.IosMediaSaverTest --tests com.runninghub.app.platform.IosMediaResolverTest --tests com.runninghub.app.platform.PermissionControllerContractTest
./gradlew --console=plain :composeApp:iosSimulatorArm64Test --tests com.runninghub.app.platform.IosPermissionAuthorizationMappingTest --tests com.runninghub.app.platform.IosMediaSaverTest --tests com.runninghub.app.platform.IosMediaResolverTest --tests com.runninghub.app.platform.IosPickedMediaCopyTest --tests com.runninghub.app.platform.PermissionControllerContractTest --tests com.runninghub.app.platform.MediaSaveResultRecoveryTest --tests com.runninghub.app.ui.feature.login.SmsCaptchaEnvironmentTest
./gradlew --console=plain :core:storage:iosSimulatorArm64Test --tests com.runninghub.core.storage.IosKeychainCredentialStoreTest
./gradlew --console=plain :feature:quickcreate:presentation:iosSimulatorArm64Test
./gradlew --console=plain :feature:quickcreate:presentation:iosSimulatorArm64Test :feature:detail:presentation:iosSimulatorArm64Test
./gradlew --console=plain :composeApp:iosSimulatorArm64Test :core:storage:iosSimulatorArm64Test :feature:quickcreate:domain:iosSimulatorArm64Test :feature:quickcreate:data:iosSimulatorArm64Test :feature:quickcreate:presentation:iosSimulatorArm64Test :feature:detail:presentation:iosSimulatorArm64Test :feature:task:domain:iosSimulatorArm64Test :feature:task:data:iosSimulatorArm64Test :feature:task:presentation:iosSimulatorArm64Test :feature:auth:presentation:iosSimulatorArm64Test :feature:auth:data:iosSimulatorArm64Test
./gradlew --console=plain :feature:detail:presentation:iosSimulatorArm64Test :composeApp:iosSimulatorArm64Test
./gradlew --console=plain :feature:task:domain:iosSimulatorArm64Test :feature:task:presentation:iosSimulatorArm64Test :composeApp:iosSimulatorArm64Test checkLongTermGovernance
./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew --console=plain :composeApp:linkReleaseFrameworkIosArm64
git diff --check
codegraph status
./gradlew --console=plain verifyL1Ios # 2026-07-07 通过，覆盖 checkMigrationScripts 与 iOS Debug framework link
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' build CODE_SIGNING_ALLOWED=NO
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-Release.xcarchive CODE_SIGNING_ALLOWED=NO
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-SignedProbe.xcarchive # 当前失败: DEVELOPMENT_TEAM 为空，不能签名
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-Signed-Probe.xcarchive # 2026-07-07 复核仍失败: DEVELOPMENT_TEAM 为空，本地 provisioning profile 数量为 0
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-TeamProbe.xcarchive DEVELOPMENT_TEAM=67F6T9YV37 -allowProvisioningUpdates # 当前失败: No Accounts 且无匹配 provisioning profile
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-DevTeamProbe.xcarchive DEVELOPMENT_TEAM=TZ7VDX5TN6 -allowProvisioningUpdates # 当前失败: No Accounts 且无匹配 provisioning profile
# XcodeBuildMCP build_run_sim, extraArgs: CODE_SIGNING_ALLOWED=NO
```
