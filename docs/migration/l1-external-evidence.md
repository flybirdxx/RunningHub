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
  且 `linkResult`、`xcodebuildResult`、`simulatorSmokeResult` 必须均为 `pass`。
  `overallResult: skipped` 时，三项结果必须均为 `skipped`，并且 `followUpRequired` 不能为 `false`。
- Android Tab 与退出登录网络观察证据必须是 JSON 对象，并满足 `packageName=com.runninghub.app.debug`、
  非空 `operationNotes`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、
  `result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、
  `stableWindowSampleCount` 允许 1 个 adb/logcat 调度抖动样本缺口、`stableWindowStartedDelta=0`、
  `stableWindowMaxInFlight=0`。
- `collect-github-actions-evidence.ps1` 会分别选择 Android CI 和 iOS CI 当前 Git `HEAD` 对应的 completed/success 运行，并写入两个独立 JSON；写入后会立即断言 `databaseId`、`headSha` 和 `url` 非空，且 Android/iOS `headSha` 相同，避免一个汇总输出、不完整运行对象、旧提交或两个不同提交上的成功运行误判双 CI 通过。脚本的 `-Wait` 只在显式传入时轮询，默认仍快速失败，防止本地门禁被远端排队状态长时间阻塞。
- `collect-ios-macos-evidence.sh` 只在 macOS 生成最终证据；未显式传入 `--simulator-smoke-pass` 和非空 `--smoke-notes` 时不会写出可通过 `checkL1SealEvidence` 的 iOS 冒烟证据。脚本会记录当前 Git `HEAD`，先执行 Compose framework link，再以 `CODE_SIGNING_ALLOWED=NO` 执行 `xcodebuild` 构建 `RunningHub` scheme，防止复用旧提交上的 macOS link 结果、遗漏 iOS 包装工程或被本机签名团队配置影响。
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

- 当前 Git `HEAD` 为 `11652fa627c9fa1d715190db75383b7018cd7bd7`，且该提交已推送到
  `origin/feature/kmp-refactoring`。
- 当前工作区在本次补证前无已暂存或未暂存业务补丁；本轮只刷新证据和状态文档。
- Android GitHub Actions：`github-actions-android.json` 已绑定当前
  `HEAD=11652fa627c9fa1d715190db75383b7018cd7bd7`，远端 run `28512594215`
  为 `completed/success`。
- iOS GitHub Actions：`github-actions-ios.json` 仍绑定旧
  `HEAD=85f134d23ac58768e8a40c3172f3fbb34ca90699`，不能用于当前 HEAD 封板；
  `gh run list` 当前只找到 Android CI 在 `11652fa627c9fa1d715190db75383b7018cd7bd7`
  上的 successful run，没有同一 HEAD 的 iOS CI successful run。
- Android 登录态 Tab 网络观察：`docs/migration/evidence/android-tab-network.json`
  已记录登录态 `Discover -> History -> Create/QuickCreate -> Plaza -> Profile` 操作路径，
  `durationSeconds=120`、`stableWindowSeconds=30`、`sampleCount=116`、
  `result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
- Android 退出登录网络观察：`docs/migration/evidence/android-logout-network.json`
  已记录 Profile 退出登录完成后 Login 根页面空闲路径，
  `durationSeconds=125`、`stableWindowSeconds=30`、`sampleCount=118`、
  `result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
- macOS iOS link 与 Simulator 冒烟：`ios-macos-link-and-simulator.md` 已刷新到当前
  `HEAD=11652fa627c9fa1d715190db75383b7018cd7bd7`，但当前仍为 `overallResult=skipped`。
  Windows 本地 link 仍按平台能力跳过；后续如需补齐真实通过证据，需要在 macOS runner
  或 macOS 开发机执行 `collect-ios-macos-evidence.sh`，或手动触发 `iOS CI` 的证据采集模式，
  并完成 Xcode build、Simulator 登录、退出和 QuickCreate 冒烟说明后生成最终 Markdown 证据。
- `./gradlew.bat --console=plain checkL1SealEvidence` 本轮复跑按预期失败：补证前失败项为
  Android GitHub Actions、iOS GitHub Actions 和 macOS/iOS skipped 证据均绑定旧 HEAD。
  本轮已刷新 Android CI 与 macOS/iOS skipped 证据；剩余封板阻塞是当前 HEAD 缺少
  iOS CI `completed/success` 证据，且 skipped 仍不能作为 iOS runtime pass 证明。
- `finalize-l1-external-evidence.ps1 -SelfTest` 已覆盖 skipped 证据必填字段、脏工作区拒绝、
  未跟踪文件拒绝，以及 `-StageEvidence` 使用的五个外部证据路径清单。
