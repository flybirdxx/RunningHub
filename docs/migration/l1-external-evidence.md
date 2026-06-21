# L1 外部封板证据清单

> 本文件记录 AC-11 最终封板前仍需由外部环境提供的证据。
> 本地 `verifyL1Local` 通过只代表可自动化的本地门禁通过，不能替代本文件列出的外部证据。

## 校验入口

```bash
./gradlew checkL1SealEvidence
```

该任务当前预期失败，直到以下四个证据文件全部存在、已加入 Git 索引、没有未暂存差异，并通过结构化字段校验。
它不会被 `verifyL1Local`、`verifyL1Android` 或 `verifyL1Ios` 自动调用，避免在外部证据缺失时误报 L1 已封板。

## 必需证据文件

| 证据 | 文件 | 采集方式 | 通过含义 |
|---|---|---|---|
| Android GitHub Actions | `docs/migration/evidence/github-actions-android.json` | 完整补丁提交并推送后，运行 `docs/migration/collect-github-actions-evidence.ps1`；默认绑定当前 Git `HEAD`，也可显式追加 `-HeadSha <commit>`。 | Android CI 在远端实际运行，并且结论为 `success`。 |
| iOS GitHub Actions | `docs/migration/evidence/github-actions-ios.json` | 完整补丁提交并推送后，运行 `docs/migration/collect-github-actions-evidence.ps1`；默认绑定当前 Git `HEAD`，也可显式追加 `-HeadSha <commit>`。 | iOS CI 在 macOS runner 实际运行，并且结论为 `success`。 |
| Android 登录态 Tab 网络观察 | `docs/migration/evidence/android-tab-network.json` | 登录 debug 包后，切换 History/QuickCreate 等 Tab，再运行 `docs/migration/observe-tab-network.ps1 -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "<登录态操作路径>"`。 | 至少 120 秒采样和 30 秒稳定窗口内 `started` 不继续增长且 `inFlight` 归零，证明不可见 Tab 没有持续后台请求。 |
| macOS iOS link 与 Simulator 冒烟 | `docs/migration/evidence/ios-macos-link-and-simulator.md` | 在 macOS runner 或 macOS 开发机完成 iOS Simulator 登录、退出和 QuickCreate 冒烟后，运行 `docs/migration/collect-ios-macos-evidence.sh --simulator-smoke-pass --smoke-notes "<设备、系统和操作路径说明>"`。脚本会写入当前 Git `HEAD`。 | Windows 本地被跳过的 iOS framework link 已在 macOS 对当前待封板提交执行，且 iOS 运行时关键装配可启动。 |

## 判定规则

- 四个证据文件缺一不可。
- 证据必须来自当前待封板补丁对应的提交或工作区状态，不能复用旧分支或旧提交的输出。
- 证据文件必须加入 Git 索引，且整个仓库不能存在已跟踪文件的未暂存差异。
- GitHub Actions 证据必须是 JSON 对象或 JSON 数组，并至少包含目标 workflow 的一条
  `status=completed`、`conclusion=success` 运行；该运行还必须包含非空 `databaseId`、
  `headSha` 和 `url`。
- Android CI 与 iOS CI 的 `headSha` 必须一致，并且必须等于当前 Git `HEAD`，证明双端远端门禁运行在当前待封板提交上。
- macOS iOS link/Simulator 证据必须包含非空 `capturedAt`、`headSha`、`host` 和 `linkCommand`；
  `headSha` 必须等于当前 Git `HEAD`，`host` 必须包含 `Darwin`，`linkCommand` 必须等于
  `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`；
  `## Notes` 必须记录非空的 Simulator 设备、系统和操作路径说明。
- Android Tab 网络观察证据必须是 JSON 对象，并满足 `packageName=com.runninghub.app.debug`、
  非空 `operationNotes`、`durationSeconds >= 120`、`stableWindowSeconds >= 30`、
  `result=pass_candidate`、`sampleCount > 0`、`sampleCount` 与 `samples.size` 一致、
  `stableWindowSampleCount >= stableWindowSeconds`、`stableWindowStartedDelta=0`、
  `stableWindowMaxInFlight=0`。
- `collect-github-actions-evidence.ps1` 会分别选择 Android CI 和 iOS CI 当前 Git `HEAD` 对应的 completed/success 运行，并写入两个独立 JSON；写入后会立即断言 `databaseId`、`headSha` 和 `url` 非空，且 Android/iOS `headSha` 相同，避免一个汇总输出、不完整运行对象、旧提交或两个不同提交上的成功运行误判双 CI 通过。
- `collect-ios-macos-evidence.sh` 只在 macOS 生成最终证据；未显式传入 `--simulator-smoke-pass` 和非空 `--smoke-notes` 时不会写出可通过 `checkL1SealEvidence` 的 iOS 冒烟证据。脚本会记录当前 Git `HEAD`，防止复用旧提交上的 macOS link 结果。
- 证据中不得包含 Token、Cookie、API Key、Authorization header、请求 Body 或用户隐私数据；`checkL1SealEvidence` 会扫描已落盘证据中的常见凭据形态并拒绝封板。
- 如果登录态 Tab 观察失败，应先保留失败证据，再回到 Gate G 修复根因；不要手工编辑 JSON 使其通过。
- 如果 macOS iOS link 在 Windows 本地显示 `SKIPPED`，只能作为本地限制说明，不能作为通过证据。

## 当前状态

- Android GitHub Actions：缺失，补丁尚未提交推送。
- iOS GitHub Actions：缺失，补丁尚未提交推送。
- Android 登录态 Tab 网络观察：已落盘并加入 Git 索引，`docs/migration/evidence/android-tab-network.json`
  记录了登录态 `Discover -> History -> Create/QuickCreate -> Plaza -> Profile` 操作路径，
  `durationSeconds=120`、`stableWindowSeconds=30`、`sampleCount=116`、
  `result=pass_candidate`、`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0`。
- macOS iOS link 与 Simulator 冒烟：缺失，Windows 本地 link 仍按平台能力跳过。
