# composeApp

## 模块概述

`:composeApp` 是 RunningHub 应用壳，包含 Compose Multiplatform UI、Voyager 导航、ScreenModel facade、主题、平台 runtime module 和 Android/iOS 平台实现。它负责把 Core、Feature Domain/Presentation 与平台 Data 实现装配成可运行应用。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:composeApp` |
| 路径 | `composeApp` |
| 类型 | application |
| 命名空间 | `com.runninghub.app` |
| applicationId | `com.runninghub.app` |
| minSdk / targetSdk | 26 / 35 |
| 完整扫描基线源文件数 | 114 |
| commonMain 依赖 | Core Model/Storage + Feature Domain/Presentation + Compose/Voyager/Koin/Coil |
| platform 依赖 | Core Network + Feature Data + Android/iOS runtime libraries |

## 关键源码

- `App.kt`：根 Compose 应用。
- `AppRootNavigationPolicy.kt`：根导航策略。
- `di/AppModule.kt`：commonMain Koin binding。
- `androidMain/RunningHubApplication.kt`、`MainActivity.kt`：Android 启动入口。
- `androidMain/di/AndroidRuntimeModule.kt`：Android 运行期 Data、网络、存储和平台服务装配。
- `iosMain/di/IosRuntimeModule.kt`、`MainViewController.kt`：iOS 运行期装配和 Compose framework 入口。
- `ui/navigation/MainScreen.kt`：主 Tab 导航。
- `ui/feature/**`：各页面 Compose UI 与 ScreenModel facade。
- `platform/**`：权限、媒体选择器、图片加载、系统返回等 expect/actual 边界。
- `ui/theme/**`：主题、颜色、窗口尺寸和设计 token。

## 架构边界

- `commonMain` 不依赖 Feature Data；只消费 Domain/Presentation。
- `androidMain` 与 `iosMain` 负责装配 Feature Data 和平台网络/存储。
- `ScreenModel` 不应承载复杂业务状态；复杂状态下沉 Feature Presentation。
- 用户可见文案优先进入 Compose Resources。
- Android release 必须保持 R8、资源压缩和 ProGuard 规则。

## 推荐验证

- UI/状态小改：相关 `commonTest` 或页面状态测试。
- Android 构建：`.\gradlew.bat --console=plain :composeApp:assembleDebug`。
- Android release：`.\gradlew.bat --console=plain :composeApp:assembleRelease`。
- iOS framework：macOS 上 `./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64`。
