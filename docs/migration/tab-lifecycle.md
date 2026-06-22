# Tab 生命周期策略

本文记录 AC-10 对主导航生命周期的验收结论。唯一实现入口位于
`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/navigation/MainScreen.kt`。

## 当前策略

- 主导航只组合当前选中的一级 Tab。
- 每个 Tab 的 Voyager Screen 实例由 `MainTabScreenRegistry` 统一持有，避免重组或 Tab 切换返回时重复创建导航对象。
- `SaveableStateHolder` 按 Tab 名称保存可保存 UI 状态，例如滚动位置、输入框这类 `rememberSaveable` 状态。
- 不可见 Tab 会离开 Composition，其 `LaunchedEffect`、Voyager ScreenModel scope 和页面 Job 会随生命周期释放。
- QuickCreate 的草稿、上传、计费、生成轮询和历史刷新仍由 `feature:quickcreate:presentation/coordinator` 的 `QuickCreateCoordinator.dispose()` 统一取消，composeApp 的 ScreenModel 只负责在 Voyager 生命周期结束时转发释放动作。
- `QuickCreateScreenModelTest.dispose cancels active image generation polling` 覆盖页面销毁时取消活跃生成状态流。
- `QuickCreateScreenModelTest.dispose cancels active media upload` 覆盖页面销毁时取消仍在进行的媒体上传，
  并验证取消不会把素材推进到上传完成态或触发计费预览请求。
- Android debug 运行图安装了 `NetworkActivityTracker`，logcat 标签为 `RunningHubNetwork`，
  只输出 `started`、`completed` 和 `inFlight` 聚合计数，不输出 URL、Header、Body 或凭据。
- Android debug 观察器每秒输出一次当前聚合计数；即使稳定窗口内没有新请求，也会保留连续心跳样本。
  这可以证明采样窗口持续存在，避免单个静态日志点被误判为后台请求已经停止。
- `docs/migration/observe-tab-network.ps1` 是 AC-11 登录态 Tab 网络观察的标准脚本。
  它会清空并读取 `RunningHubNetwork` 标签，按稳定窗口计算请求是否继续增长，并在最终 JSON
  中写入 `stableWindowSampleCount`，证明稳定窗口内确实存在连续采样。

## 验收映射

| 验收项 | 当前处理 |
|---|---|
| 非当前 Tab 不执行轮询、自动刷新或上传 | 非当前 Tab 不再进入 Composition，相关协程随页面离开取消。 |
| Tab 切换后滚动和输入状态可恢复 | 使用 `SaveableStateHolder` 保存可序列化 UI 状态；长生命周期业务状态通过草稿或仓库恢复。 |
| 页面离开导航栈后所有 Job 被取消 | 主页面退出时当前 Tab 离开 Composition；QuickCreate 由 ScreenModel/Coordinator 释放，History 的普通历史轮询由 `feature:task:presentation` 持有，`composeApp` Voyager 适配在 `onDispose` 中调用释放入口；QuickCreate 活跃生成状态流和媒体上传已有取消测试。 |
| 重组不会重复初始化同一个 ScreenModel | `MainTabScreenRegistryTest` 验证同一 Tab 切换返回后仍获得同一个 Screen 实例。 |
| 每个 Tab 的 Screen 实例和状态所有权明确 | `MainTabScreenRegistry` 集中维护 Tab 到 Screen 的映射；测试验证不同一级 Tab 不共享 Screen，创作 Tab 固定到 `QuickCreateVoyagerScreen`。 |
| 会话失效清空所有业务页面栈 | 根入口由 `SessionManager.state` 切换登录/主页面，主页面离开后当前业务 Tab 被释放。 |

## 已验证命令

```bash
./gradlew.bat checkArchitectureBoundaries :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest.onDispose cancels active polling before next refresh"
./gradlew.bat --console=plain :feature:task:presentation:testDebugUnitTest --tests "com.runninghub.feature.task.presentation.TaskHistoryStateHolderTest.dispose cancels active polling before next refresh"
./gradlew.bat :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolderTest.dispose cancels polling before next history refresh"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest" :feature:quickcreate:presentation:testDebugUnitTest --tests "com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolderTest" checkArchitectureBoundaries :composeApp:assembleDebug
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.navigation.MainTabScreenRegistryTest" --tests "com.runninghub.app.AppRootNavigationPolicyTest"
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active image generation polling" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.second image generation is blocked while current task is active"
./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active media upload" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels active image generation polling" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.dispose cancels pending draft autosave"
./gradlew.bat --console=plain :core:network:testDebugUnitTest --tests "com.runninghub.core.network.NetworkActivityTrackerTest" :composeApp:compileDebugKotlinAndroid
./gradlew.bat --console=plain checkArchitectureBoundaries :core:network:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug
ANDROID_SERIAL=emulator-5554 ./gradlew.bat --console=plain :composeApp:installDebug
adb -s emulator-5554 shell am start -W -n com.runninghub.app.debug/com.runninghub.app.MainActivity
adb -s emulator-5554 logcat -d -s RunningHubNetwork
powershell -NoProfile -ExecutionPolicy Bypass -File docs/migration/observe-tab-network.ps1 -Serial emulator-5554 -DurationSeconds 120 -StableWindowSeconds 30 -Launch -ForceStopBeforeLaunch
powershell -NoProfile -ExecutionPolicy Bypass -File docs/migration/observe-tab-network.ps1 -Serial adb-5d692d82-J3ioHJ._adb-tls-connect._tcp -DurationSeconds 120 -StableWindowSeconds 30 -OutputPath docs/migration/evidence/android-tab-network.json -OperationNotes "logged-in Discover -> History -> Create/QuickCreate -> Plaza -> Profile; final Profile idle on Android debug device 2211133C API 35/Android 16"
```

最新 AVD 冷启动后，`RunningHubNetwork` 已输出 `started=0 completed=0 inFlight=0`，
证明 debug 计数通道可用。该证据只证明观察工具可工作；因为当前 AVD 仍停留在登录页，
尚不能替代登录态 Tab 切换后的真实后台请求验收。
本轮补充了 `-ForceStopBeforeLaunch` 参数，用于采样前重启 debug 进程，避免应用已经在前台时
`am start` 只投递 intent 而不产生新的 `RunningHubNetwork` 样本。
随后在连接设备 2211133C 的登录态主界面执行标准观察脚本，按
`Discover -> History -> Create/QuickCreate -> Plaza -> Profile` 操作后静置，生成
`docs/migration/evidence/android-tab-network.json`。该证据记录 `durationSeconds=120`、
`stableWindowSeconds=30`、`sampleCount=116`、`result=pass_candidate`、
`stableWindowStartedDelta=0`、`stableWindowMaxInFlight=0` 和 `stableWindowSampleCount=30`。

## AC-11 观察步骤

1. 安装并启动 `com.runninghub.app.debug`，完成一次测试登录或恢复已有测试会话。
2. 运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File docs/migration/observe-tab-network.ps1 -Serial emulator-5554 -DurationSeconds 120 -StableWindowSeconds 30 -Launch -ForceStopBeforeLaunch
```

3. 在采样窗口内依次进入 History、QuickCreate，再切回其他 Tab，并等待页面静置。
4. 如果脚本输出 `result=pass_candidate`，将完整命令、关键输出、操作路径和设备信息贴入
   `docs/migration/l1-seal-audit.md`。如果输出 `result=needs_review`，需要结合当时页面和任务状态判断是否仍有后台轮询。

## 剩余风险

- 本地测试已证明普通 History 和 QuickCreate History 在 `onDispose`/`dispose` 后不会触发下一次轮询刷新。
- 登录态 Android 设备网络计数已落盘通过；如果后续需要模拟器专项证据，应使用同一脚本在可恢复测试会话的 AVD 上复跑。
- AC-11 双端回归时仍需人工观察退出登录后业务页面栈被释放。
