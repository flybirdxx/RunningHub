# iOS App Instructions

## Scope

适用于 `iosApp/` Xcode 包装工程和 SwiftUI 壳。

## Responsibility

本目录只负责承载 ComposeApp Framework、启动 iOS Koin 运行期模块、声明 Info.plist 权限文案和 Xcode 构建配置。
业务逻辑、Repository、UiState 和网络调用不得写在 Swift 壳中。

## Rules

- SwiftUI App 初始化必须先调用 `startRunningHubKoin()`，再创建 Compose root view。
- Xcode Build Phase 必须调用 `:composeApp:embedAndSignAppleFrameworkForXcode`。
- Info.plist 权限说明必须与实际平台能力一致。
- 涉及媒体、相册、文件、WKWebView、Keychain、后台模式或网络权限时，必须在 macOS 上运行验证。
- 不在 Swift 文件中硬编码 Token、Cookie、API Key、环境 URL 或业务请求。
- Xcode 工程改动必须保持可由命令行 `xcodebuild` 构建。

## Verification

修改本目录后在 macOS 执行：

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

涉及 UI/权限/媒体时，还必须在 Simulator 或真机执行冒烟。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
