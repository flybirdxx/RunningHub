# L1 外部封板证据清单

> 本文件记录 AC-11 最终封板前仍需由外部环境提供的证据。
> 本地 `verifyL1Local` 通过只代表可自动化的本地门禁通过，不能替代本文件列出的外部证据。

## 校验入口

```bash
./gradlew checkL1SealEvidence
```

该任务当前预期失败，直到以下五个证据文件全部存在、对应当前代码 Git `HEAD`、仓库没有未暂存差异，且已暂存差异仅限这些外部证据文件，并通过结构化字段校验。
它不会被 `verifyL1Local`、`verifyL1Android` 或 `verifyL1Ios` 自动调用，避免在外部证据缺失时误报 L1 已封板。

## 必需证据文件

| 证据 | 文件 | 采集方式 | 通过含义 |
|---|---|---|---|
| Android GitHub Actions | `docs/migration/evidence/github-actions-android.json` | 完整补丁提交并推送后，运行 `docs/migration/collect-github-actions-evidence.ps1`；默认绑定当前 Git `HEAD`，也可显式追加 `-HeadSha <commit>`。若目标 run 尚未完成，可追加 `-Wait -WaitTimeoutSeconds 1800 -PollSeconds 30` 等待 completed/success。 | Android CI 在远端实际运行，并且结论为 `success`。 |
| iOS GitHub Actions | `docs/migration/evidence/github-actions-ios.json` | 当前 `iOS CI` 以 `workflow_dispatch` 作为封板证据入口；完成真实 macOS Simulator 登录、退出和 QuickCreate 冒烟并填写 `simulator_smoke_pass=true`、`smoke_notes` 后，再运行 `docs/migration/collect-github-actions-evidence.ps1 -HeadSha <commit>` 或 `finalize-l1-external-evidence.ps1` 采集对应 successful run。 | iOS CI 在 macOS runner 对目标提交实际运行，并且结论为 `success`。 |
| Android 登录态 Tab 网络观察 | `docs/migration/evidence/android-tab-network.json` | 登录 debug 包后，切换 History/QuickCreate 等 Tab，再运行 `docs/migration/observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "<登录态操作路径>"`。 | 至少 120 秒采样和 30 秒稳定窗口内 `started` 不继续增长且 `inFlight` 归零，证明不可见 Tab 没有持续后台请求。 |
| Android 退出登录网络观察 | `docs/migration/evidence/android-logout-network.json` | 从 Profile 执行退出登录并确认回到 Login 根页面后，运行 `docs/migration/observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-logout-network.json -OperationNotes "<退出登录后的 Login 根页面空闲路径>"`。 | 退出登录清空业务主栈后，至少 120 秒采样和 30 秒稳定窗口内 `started` 不继续增长且 `inFlight` 归零，证明 Android 侧业务页面释放后没有持续后台请求。 |
| macOS iOS link 与 Simulator 冒烟 | `docs/migration/evidence/ios-macos-link-and-simulator.md` | 在 macOS runner 或 macOS 开发机完成 iOS Simulator 登录、退出和 QuickCreate 冒烟后，运行 `docs/migration/collect-ios-macos-evidence.sh --simulator-smoke-pass --smoke-notes "<设备、系统和操作路径说明>"`。也可以运行 `docs/migration/request-ios-macos-evidence.ps1 -ConfirmSimulatorSmokePass -SmokeNotes "<设备、系统和操作路径说明>" -Wait` 触发 `iOS CI` 的 `workflow_dispatch` 并等待下载 artifact；该脚本未显式确认和 notes 时会失败。当前 macOS 环境不可用时，允许以 `overallResult: skipped` 留存跳过证据，但必须写明 `skipReason`、`followUpRequired` 和当前 `headSha`。 | `overallResult: pass` 表示 Windows 本地被跳过的 iOS framework link 已在 macOS 对当前待封板提交执行；`overallResult: skipped` 只表示用户确认当前无法测试 macOS，仍需后续补验。 |

## 判定规则

- 五个证据文件缺一不可。
- 证据必须来自当前待封板代码对应的 Git `HEAD`，不能复用旧分支、旧提交或仅存在于本地索引中的代码补丁输出。
- 封板前仓库必须没有未暂存差异；已暂存差异只允许是本文件列出的五个外部证据文件。
- 代码、Gradle、workflow、脚本和文档变更必须先提交并重新采集对应新 `HEAD` 的证据；外部证据文件可以在采集后暂存，用于证明当前代码 `HEAD`。
- GitHub Actions 证据必须是 JSON 对象或 JSON 数组，并至少包含目标 workflow 的一条
  `status=completed`、`conclusion=success` 运行；该运行还必须包含非空 `databaseId`、
  `headSha` 和 `url`。
- Android CI 与 iOS CI 的 `headSha` 必须一致，并且必须等于当前 Git `HEAD`，证明双端远端门禁运行在当前待封板提交上。
- macOS iOS link/Simulator 证据必须包含非空 `capturedAt`、`headSha`、`overallResult`、`skipReason`、`followUpRequired` 和 `## Notes`；
  `headSha` 必须等于当前 Git `HEAD`。`overallResult: pass` 时，`host` 必须包含 `Darwin`，`linkCommand` 必须等于
  `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`，`xcodebuildCommand` 必须等于
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO`，
  `authenticatedSmokeBuildMode` 必须等于 `signed-simulator-or-device`，且 `linkResult`、`xcodebuildResult`、`simulatorSmokeResult` 必须均为 `pass`。
  `overallResult: skipped` 时，三项结果必须均为 `skipped`，并且 `followUpRequired` 不能为 `false`。
- Android Tab 与退出登录网络观察证据必须是 JSON 对象，并满足 `packageName=com.runninghub.app.debug`、
  非空 `operationNotes`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、
  `result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、
  `stableWindowSampleCount` 允许 1 个 adb/logcat 调度抖动样本缺口、`stableWindowStartedDelta=0`、
  `stableWindowMaxInFlight=0`。
- `collect-github-actions-evidence.ps1` 会分别选择 Android CI 和 iOS CI 当前 Git `HEAD` 对应的 completed/success 运行，并写入两个独立 JSON；写入后会立即断言 `databaseId`、`headSha` 和 `url` 非空，且 Android/iOS `headSha` 相同，避免一个汇总输出、不完整运行对象、旧提交或两个不同提交上的成功运行误判双 CI 通过。脚本的 `-Wait` 只在显式传入时轮询，默认仍快速失败，防止本地门禁被远端排队状态长时间阻塞。
- `collect-ios-macos-evidence.sh` 只在 macOS 生成最终证据；未显式传入 `--simulator-smoke-pass` 和非空 `--smoke-notes` 时不会写出可通过 `checkL1SealEvidence` 的 iOS 冒烟证据。脚本会记录当前 Git `HEAD` 并写入 `overallResult: pass`、`skipReason: none`、`followUpRequired: false` 和 `authenticatedSmokeBuildMode: signed-simulator-or-device`，先执行 Compose framework link，再以 `CODE_SIGNING_ALLOWED=NO` 执行 `xcodebuild` 构建 `RunningHub` scheme，防止复用旧提交上的 macOS link 结果、遗漏 iOS 包装工程或被本机签名团队配置影响；真实登录态冒烟必须来自 signed Simulator build、真机或 TestFlight，因为 unsigned build 不能证明 Keychain 恢复。
- `iOS CI` 支持手动 `workflow_dispatch` 证据模式。手动触发前必须先在对应 macOS Simulator 操作登录、退出和 QuickCreate 冒烟；workflow 会校验 `simulator_smoke_pass=true` 和非空 `smoke_notes`，然后运行同一个采集脚本并上传 `ios-macos-link-and-simulator.md` artifact。
- `request-ios-macos-evidence.ps1` 只负责编排手动证据流程：它要求调用方显式传入 `-ConfirmSimulatorSmokePass` 和非空 `-SmokeNotes`，再触发 `iOS CI` 的 `workflow_dispatch`。该脚本不能替代真实 Simulator 操作；没有人工确认时不会触发远端证据 workflow。
- `download-ios-macos-evidence.ps1` 用于把手动 iOS CI 运行的 artifact 落盘到 `docs/migration/evidence/ios-macos-link-and-simulator.md`。它会筛选当前 Git `HEAD` 对应的 successful `workflow_dispatch` iOS CI run，也可用 `-RunId` 绑定指定 run，并在写入后校验 `linkResult: pass`、`xcodebuildResult: pass` 和 `simulatorSmokeResult: pass` 等关键字段。若手动 workflow 正在运行，可显式追加 `-Wait -WaitTimeoutSeconds 1800 -PollSeconds 30` 等待 artifact 所属 run completed/success。
- `finalize-l1-external-evidence.ps1` 是提交后的总编排入口。它会先拒绝未暂存或已暂存的代码、配置和文档改动，
  也会拒绝未跟踪文件，并要求显式传入的 `-HeadSha` 必须等于当前 checkout 的 Git `HEAD`。这样可以确保
  当前 Git `HEAD` 已固定，再调用 `collect-github-actions-evidence.ps1` 重新采集 Android/iOS CI 证据。
  `-IosEvidenceMode skip` 会为当前 `HEAD` 写入显式 skipped 的 macOS/iOS 证据；`request` 或 `download`
  则分别编排真实 Simulator 冒烟后的 workflow_dispatch 或 artifact 下载流程。可追加 `-StageEvidence`
  只暂存五个允许参与封板的外部证据文件；可追加 `-RunSealCheck` 在证据采集后执行
  `./gradlew.bat --console=plain checkL1SealEvidence`。
- 当前补丁提交并推送后，如果仍按用户确认留存 macOS skipped 证据，可在远端 Android/iOS CI 对新 `HEAD`
  完成后运行：
  `powershell -NoProfile -ExecutionPolicy Bypass -File docs\migration\finalize-l1-external-evidence.ps1 -Wait -WaitTimeoutSeconds 1800 -PollSeconds 30 -IosEvidenceMode skip -StageEvidence -RunSealCheck`。
  该命令会拒绝任何非证据 staged/unstaged/untracked 状态，因此必须在代码、配置和文档补丁已经提交后执行。
- 证据中不得包含 Token、Cookie、API Key、Authorization header、请求 Body 或用户隐私数据；`checkL1SealEvidence` 会扫描已落盘证据中的常见凭据形态并拒绝封板。
- 如果登录态 Tab 或退出登录观察失败，应先保留失败证据，再回到 Gate G 修复根因；不要手工编辑 JSON 使其通过。
- 如果 macOS iOS link 在 Windows 本地显示 `SKIPPED`，只能作为本地限制说明，不能作为通过证据。

## 当前状态

- 当前 iOS 补全提交已固定，`./gradlew --console=plain verifyL1Ios` 已在 2026-07-07 复跑通过；其中 `checkMigrationScripts` 不再因证据相关文件未暂存而失败。
- macOS iOS link 与 Simulator 冒烟：`docs/migration/evidence/ios-macos-link-and-simulator.md` 已在当前 Git `HEAD` 上重新采集，字段为 `overallResult=pass`、`linkResult=pass`、`xcodebuildResult=pass`、`simulatorSmokeResult=pass`、`authenticatedSmokeBuildMode=signed-simulator-or-device`。最终封板以该 evidence 文件的 `headSha` 字段和 `git rev-parse HEAD` 一致为准。
- `CODE_SIGNING_ALLOWED=NO` 只用于证明 Xcode wrapper 可构建；authenticated smoke 必须使用 signed Simulator build 或真机/TestFlight 环境，否则 Keychain 可能不可用，不能证明登录态恢复。
- `docs/migration/run-ios-simulator-smoke.sh` 已覆盖临时 Simulator 创建、安装启动、截图和启动崩溃关键词扫描；该脚本只能证明 launch smoke，不会把未登录的自动启动误判为 authenticated Simulator 冒烟。
- `collect-ios-macos-evidence.sh` 的最终通过证据写入格式已与 `checkL1SealEvidence` 对齐：真实传入 `--simulator-smoke-pass` 和非空 `--smoke-notes` 后会写入 `overallResult: pass`、`skipReason: none`、`followUpRequired: false` 和 `authenticatedSmokeBuildMode: signed-simulator-or-device`。
- `./gradlew --console=plain verifyL1Ios` 已在 2026-07-07 复跑通过，覆盖 `checkArchitectureBoundaries`、`checkL1CiWorkflows`、`checkLongTermGovernance`、`checkMigrationScripts` 和 iOS Debug framework link。Gradle 仍输出既有 AGP/KMP 结构迁移警告，不影响本次 iOS gate 结论。
- 2026-07-07 通过 XcodeBuildMCP 执行 signed Simulator `build_run_sim`，结果为 `SUCCEEDED`，bundle id 为 `com.runninghub.app.ios`，进程为 `57226`，构建日志为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/build_run_sim_2026-07-07T04-37-02-069Z_pid54229_c49ed3fc.log`，runtime log 为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/com.runninghub.app.ios_2026-07-07T04-37-25-934Z_helperpid57196_ownerpid54229_6df5d0fa.log`，os log 为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/com.runninghub.app.ios_oslog_2026-07-07T04-37-28-742Z_helperpid57245_ownerpid54229_4c19c935.log`。本次只补非上传人工项：启动后保持登录态，在 QuickCreate 结果保存入口重置 `photos-add` 后触发真实照片权限弹窗并选择“不允许”，页面展示稳定保存失败提示和权限恢复引导；随后选择“限制访问…”进入系统 Limited Photos 管理页并完成返回，页面未新增素材或错误；退出登录后使用测试账号完成密码登录并回到账户页，凭据未写入文档；随后在短信登录路径人工完成 TAC 滑块，短信发送成功并完成 SMS 登录回到账户页；runtime/os log 关键字扫描未命中远端 URL、敏感 token、Cookie、测试手机号、验证码、密码或平台异常原文。该记录仍是 working tree 证据，不是最终 L1 封板证据。
- 2026-07-07 已执行 Release iphoneos archive dry run，命令为 `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-Release.xcarchive CODE_SIGNING_ALLOWED=NO`，结果为 `ARCHIVE SUCCEEDED`。检查结果：`/tmp/RunningHub-iOS-Release.xcarchive/Products/Applications/RunningHub.app/RunningHub` 为 arm64 Mach-O，processed `Info.plist` 包含 `CFBundleIdentifier=com.runninghub.app.ios`、`CFBundleShortVersionString=1.0`、`CFBundleVersion=1`、`MinimumOSVersion=16.0`、四类权限说明和 `PHPhotoLibraryPreventAutomaticLimitedAccessAlert=true`；`PrivacyInfo.xcprivacy` 已打入 app，声明 UserDefaults/FileTimestamp 访问原因，未声明 tracking 或 collected data；Xcode store validation dry run 通过。`ComposeApp.framework` 为 static framework，已静态链接进主二进制，archive app 内无独立 `Frameworks/ComposeApp.framework` 属于当前链接形态。该记录不代表真实签名、IPA 导出、TestFlight 上传或 L1 封板；Release build settings 与 archive metadata 中 Team/SigningIdentity 为空，发布前仍需负责人配置和确认。
- 同日签名环境探测结果：本机 keychain 存在有效 Apple Development/Distribution signing identity，但 `~/Library/MobileDevice/Provisioning Profiles` 下本地 provisioning profile 数量为 0，Xcode Release build settings 中 `DEVELOPMENT_TEAM` 为空。执行签名 archive 探测 `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-SignedProbe.xcarchive` 失败，exit 65，关键错误为 `Signing for "RunningHub" requires a development team`。继续用命令行覆盖 Distribution team 和 Development team 并开启 `-allowProvisioningUpdates` 后，均失败为 `No Accounts` 与没有匹配 `com.runninghub.app.ios` 的 iOS App Development provisioning profile；手动覆盖 Apple Distribution identity 还会触发自动 development signing 与 distribution identity 冲突。这证明当前 TestFlight 阻塞点是 Xcode Accounts 登录、Team/provisioning 配置，不是 Release 编译、Info.plist、PrivacyInfo 或 store validation dry run。
- `checkL1SealEvidence` 当前不应作为完成证明：它只应在提交固定、远端 CI 与登录态 iOS 冒烟证据全部刷新到新 `HEAD` 后用于 L1 封板。
