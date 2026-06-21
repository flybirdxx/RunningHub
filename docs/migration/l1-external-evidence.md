# L1 外部封板证据清单

> 本文件记录 AC-11 最终封板前仍需由外部环境提供的证据。
> 本地 `verifyL1Local` 通过只代表可自动化的本地门禁通过，不能替代本文件列出的外部证据。

## 校验入口

```bash
./gradlew checkL1SealEvidence
```

该任务当前预期失败，直到以下五个证据文件全部存在、对应当前 Git `HEAD`、仓库没有未暂存或已暂存差异，并通过结构化字段校验。
它不会被 `verifyL1Local`、`verifyL1Android` 或 `verifyL1Ios` 自动调用，避免在外部证据缺失时误报 L1 已封板。

## 必需证据文件

| 证据 | 文件 | 采集方式 | 通过含义 |
|---|---|---|---|
| Android GitHub Actions | `docs/migration/evidence/github-actions-android.json` | 完整补丁提交并推送后，运行 `docs/migration/collect-github-actions-evidence.ps1`；默认绑定当前 Git `HEAD`，也可显式追加 `-HeadSha <commit>`。 | Android CI 在远端实际运行，并且结论为 `success`。 |
| iOS GitHub Actions | `docs/migration/evidence/github-actions-ios.json` | 完整补丁提交并推送后，运行 `docs/migration/collect-github-actions-evidence.ps1`；默认绑定当前 Git `HEAD`，也可显式追加 `-HeadSha <commit>`。 | iOS CI 在 macOS runner 实际运行，并且结论为 `success`。 |
| Android 登录态 Tab 网络观察 | `docs/migration/evidence/android-tab-network.json` | 登录 debug 包后，切换 History/QuickCreate 等 Tab，再运行 `docs/migration/observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "<登录态操作路径>"`。 | 至少 120 秒采样和 30 秒稳定窗口内 `started` 不继续增长且 `inFlight` 归零，证明不可见 Tab 没有持续后台请求。 |
| Android 退出登录网络观察 | `docs/migration/evidence/android-logout-network.json` | 从 Profile 执行退出登录并确认回到 Login 根页面后，运行 `docs/migration/observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-logout-network.json -OperationNotes "<退出登录后的 Login 根页面空闲路径>"`。 | 退出登录清空业务主栈后，至少 120 秒采样和 30 秒稳定窗口内 `started` 不继续增长且 `inFlight` 归零，证明 Android 侧业务页面释放后没有持续后台请求。 |
| macOS iOS link 与 Simulator 冒烟 | `docs/migration/evidence/ios-macos-link-and-simulator.md` | 在 macOS runner 或 macOS 开发机完成 iOS Simulator 登录、退出和 QuickCreate 冒烟后，运行 `docs/migration/collect-ios-macos-evidence.sh --simulator-smoke-pass --smoke-notes "<设备、系统和操作路径说明>"`。也可以手动触发 `iOS CI` 的 `workflow_dispatch`，填写 `simulator_smoke_pass=true` 和 `smoke_notes`，由 macOS runner 生成并上传 `ios-macos-link-and-simulator.md` artifact，再用 `docs/migration/download-ios-macos-evidence.ps1` 下载并写入证据目录。 | Windows 本地被跳过的 iOS framework link 已在 macOS 对当前待封板提交执行，`iosApp/iosApp.xcodeproj` 的 `RunningHub` scheme 已通过 `xcodebuild`，且 iOS 运行时关键装配可启动。 |

## 判定规则

- 五个证据文件缺一不可。
- 证据必须来自当前待封板补丁对应的 Git `HEAD`，不能复用旧分支、旧提交或仅存在于本地索引中的补丁输出。
- 封板前仓库必须没有未暂存差异，也不能有已暂存但未提交的差异；否则远端 CI 和 macOS 证据只能证明旧 `HEAD`，不能证明当前待交付内容。
- 证据文件必须已经提交到当前 `HEAD`；只加入 Git 索引还不足以作为最终封板证据。
- GitHub Actions 证据必须是 JSON 对象或 JSON 数组，并至少包含目标 workflow 的一条
  `status=completed`、`conclusion=success` 运行；该运行还必须包含非空 `databaseId`、
  `headSha` 和 `url`。
- Android CI 与 iOS CI 的 `headSha` 必须一致，并且必须等于当前 Git `HEAD`，证明双端远端门禁运行在当前待封板提交上。
- macOS iOS link/Simulator 证据必须包含非空 `capturedAt`、`headSha`、`host` 和 `linkCommand`；
  `headSha` 必须等于当前 Git `HEAD`，`host` 必须包含 `Darwin`，`linkCommand` 必须等于
  `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`；
  `xcodebuildCommand` 必须等于
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO`；
  证据必须包含 `xcodebuildResult: pass`，避免只验证 Compose framework link 却没有验证 iOS 包装工程；
  `## Notes` 必须记录非空的 Simulator 设备、系统和操作路径说明。
- Android Tab 与退出登录网络观察证据必须是 JSON 对象，并满足 `packageName=com.runninghub.app.debug`、
  非空 `operationNotes`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、
  `result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、
  `stableWindowSampleCount` 允许 1 个 adb/logcat 调度抖动样本缺口、`stableWindowStartedDelta=0`、
  `stableWindowMaxInFlight=0`。
- `collect-github-actions-evidence.ps1` 会分别选择 Android CI 和 iOS CI 当前 Git `HEAD` 对应的 completed/success 运行，并写入两个独立 JSON；写入后会立即断言 `databaseId`、`headSha` 和 `url` 非空，且 Android/iOS `headSha` 相同，避免一个汇总输出、不完整运行对象、旧提交或两个不同提交上的成功运行误判双 CI 通过。
- `collect-ios-macos-evidence.sh` 只在 macOS 生成最终证据；未显式传入 `--simulator-smoke-pass` 和非空 `--smoke-notes` 时不会写出可通过 `checkL1SealEvidence` 的 iOS 冒烟证据。脚本会记录当前 Git `HEAD`，先执行 Compose framework link，再以 `CODE_SIGNING_ALLOWED=NO` 执行 `xcodebuild` 构建 `RunningHub` scheme，防止复用旧提交上的 macOS link 结果、遗漏 iOS 包装工程或被本机签名团队配置影响。
- `iOS CI` 支持手动 `workflow_dispatch` 证据模式。手动触发前必须先在对应 macOS Simulator 操作登录、退出和 QuickCreate 冒烟；workflow 会校验 `simulator_smoke_pass=true` 和非空 `smoke_notes`，然后运行同一个采集脚本并上传 `ios-macos-link-and-simulator.md` artifact。
- `download-ios-macos-evidence.ps1` 用于把手动 iOS CI 运行的 artifact 落盘到 `docs/migration/evidence/ios-macos-link-and-simulator.md`。它会筛选当前 Git `HEAD` 对应的 successful `workflow_dispatch` iOS CI run，也可用 `-RunId` 绑定指定 run，并在写入后校验 `linkResult: pass`、`xcodebuildResult: pass` 和 `simulatorSmokeResult: pass` 等关键字段。
- 证据中不得包含 Token、Cookie、API Key、Authorization header、请求 Body 或用户隐私数据；`checkL1SealEvidence` 会扫描已落盘证据中的常见凭据形态并拒绝封板。
- 如果登录态 Tab 或退出登录观察失败，应先保留失败证据，再回到 Gate G 修复根因；不要手工编辑 JSON 使其通过。
- 如果 macOS iOS link 在 Windows 本地显示 `SKIPPED`，只能作为本地限制说明，不能作为通过证据。

## 当前状态

- Android GitHub Actions：已落盘并加入 Git 索引，`github-actions-android.json`
  记录当前 `HEAD=f8075fc85639975e0b2829a650720d42cc680703` 上的
  `Android CI` completed/success 运行，run id 为 `27894118245`。
- iOS GitHub Actions：已落盘并加入 Git 索引，`github-actions-ios.json`
  记录当前 `HEAD=f8075fc85639975e0b2829a650720d42cc680703` 上的
  `iOS CI` completed/success 运行，run id 为 `27894118250`。
- Android 登录态 Tab 网络观察：已落盘并加入 Git 索引，`docs/migration/evidence/android-tab-network.json`
  记录了登录态 `Discover -> History -> Create/QuickCreate -> Plaza -> Profile` 操作路径，
  `durationSeconds=120`、`stableWindowSeconds=30`、`sampleCount=116`、
  `result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
- Android 退出登录网络观察：已落盘，`docs/migration/evidence/android-logout-network.json`
  记录了 Profile 退出登录完成后 Login 根页面空闲路径，
  `durationSeconds=125`、`stableWindowSeconds=30`、`sampleCount=118`、
  `result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
- macOS iOS link 与 Simulator 冒烟：缺失，Windows 本地 link 仍按平台能力跳过；
  需要在 macOS runner 或 macOS 开发机执行 `collect-ios-macos-evidence.sh`，或手动触发
  `iOS CI` 的证据采集模式，并完成 Xcode build、Simulator 登录、退出和 QuickCreate
  冒烟说明后生成最终 Markdown 证据。若使用 workflow artifact，下载落盘命令为
  `powershell -NoProfile -ExecutionPolicy Bypass -File docs/migration/download-ios-macos-evidence.ps1 -RunId <run id>`。
- 当前工作区仍有完整迁移补丁处于 Git 索引中，所以上述 Android/iOS CI 证据只能证明
  `f8075fc85639975e0b2829a650720d42cc680703`，不能证明后续提交后的最终内容。
  最终封板前必须先把补丁提交并重新采集对应新 `HEAD` 的 Android/iOS CI 与 macOS iOS 证据。
