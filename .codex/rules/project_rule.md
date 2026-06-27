# RunningHub 项目主规则

更新时间：2026-06-27  
适用范围：`F:\Program Files\RunningHub` 全仓库。

## 1. 行为准则

- 修改代码前必须阅读根 `AGENTS.md`、本文件和目标目录最近的局部 `AGENTS.md`。
- 探索代码结构时优先使用 CodeGraph：`codegraph status`、`codegraph query`、`codegraph explore`、`codegraph node`。
- `.codex/references/_scan.json` 采用 CodeGraph 轻量模式，保存模块与依赖元数据；源码细节以 CodeGraph 和模块文档为准。
- 禁止虚构不存在的类、方法、Gradle task、API endpoint 或外部验证结果。
- 工作区已有大量业务改动时，只处理当前任务范围内的 `.codex` 与入口文档，不回滚业务代码。
- 文档-only 改动也必须执行占位符扫描和文档数量检查；代码改动必须运行相关 Gradle verifier。

## 2. 项目事实

- 项目：RunningHub。
- 平台：Kotlin Multiplatform + Compose Multiplatform；Android 应用 + iOS SwiftUI 包装工程。
- 构建：Gradle Kotlin DSL，Java 17 toolchain，Kotlin 2.3.21，AGP 9.1.1。
- Android：`applicationId = "com.runninghub.app"`，`minSdk = 26`，`targetSdk = 35`，`compileSdk = 37`。
- 主命名空间：`com.runninghub`。
- 依赖注入：Koin。
- 网络：Ktor + kotlinx.serialization。
- 导航/状态：Voyager `ScreenModel` + Feature Presentation `StateHolder` / `Coordinator`。
- 存储：DataStore Preferences、Android Keystore、iOS Keychain。
- CodeGraph：版本 1.1.1，索引当前包含约 608 个文件、11099 个节点。

## 3. 模块依赖规则

- `:composeApp` 是应用壳，`commonMain` 只依赖 Core、Feature Domain 和 Feature Presentation。
- `:composeApp` 的 `androidMain` / `iosMain` 才能依赖 Feature Data、`core:network` 和平台运行期实现。
- `:core:*` 不得依赖 `:feature:*` 或 `:composeApp`。
- `:feature:*:domain` 只能依赖 Core 模型或其他稳定 Domain 契约，不得依赖 Data、Presentation、Compose、Ktor、Koin 或平台 SDK。
- `:feature:*:data` 实现对应 Domain Repository，可依赖 `core:network`、`core:storage` 和必要的 Domain 契约。
- `:feature:*:presentation` 只依赖 Domain，不直接依赖 Data 实现。
- 跨 Feature 调用优先通过 Domain 接口、应用壳导航和平台组合根协调，避免 Feature 之间直接触达实现层。
- `:shared` 已退役，不得重新加入 `settings.gradle.kts`、Gradle 依赖、源码导入或 Git 跟踪文件。

## 4. 禁止模式表

| # | 禁止模式 | 应使用方式 | 原因 |
|---|---|---|---|
| 1 | `projects.shared`、`project(":shared")`、`com.runninghub.shared.*` | 迁移到 `core:*` 或对应 `feature:*` | `shared` 已退役，根门禁会拒绝回归 |
| 2 | `composeApp/commonMain` 依赖 Feature Data | 平台 source set 装配 Data，commonMain 依赖 Domain/Presentation | 保持 KMP 平台隔离 |
| 3 | Domain/Presentation 导入 Data 实现包 | 通过 Domain Repository 接口 | 防止分层反向依赖 |
| 4 | `android.*`、`platform.UIKit.*` 等平台 API 出现在 `commonMain` | expect/actual 或平台 source set | 保持共享源码可跨平台编译 |
| 5 | 生产源码 `runBlocking`、`GlobalScope`、空 `catch` | 结构化协程、明确错误映射 | 避免阻塞、泄漏和吞错 |
| 6 | `SessionManager()` 裸构造 | 注入带 `SessionRestoreRepository` 的实例 | 进程重启后必须恢复会话事实 |
| 7 | Token/Cookie/API Key/验证码 token 写入日志或文档 | 脱敏日志和稳定错误语义 | 防止敏感信息泄漏 |
| 8 | 正则解析 token refresh JSON | kotlinx.serialization DTO | 避免 escape 语义和字段误采 |
| 9 | 恢复 `CreateVoyagerScreen` / `CreateScreenModel` | 使用 QuickCreate 新架构 | 旧创作页状态机已退役 |
| 10 | Compose 页面散落硬编码十六进制颜色 | 收敛到主题 token 或局部语义色 | 当前已有存量，新增需避免扩大 |
| 11 | Release 关闭 R8、资源压缩或移除 ProGuard 规则 | 保持 `isMinifyEnabled`、`isShrinkResources` 与 `proguard-rules.pro` | 发布包安全和体积门禁 |
| 12 | 构建产物、local 配置、keystore、APK/AAB 入 Git | 保持未跟踪或通过安全发布流程 | 保护仓库可复现性和敏感资产 |

## 5. 命名规范

- Gradle 模块：`core:{name}`、`feature:{area}:domain|data|presentation`、`composeApp`。
- 包名：与模块层级一致，例如 `com.runninghub.feature.quickcreate.presentation`。
- Domain Repository：`{Feature}Repository` 或具体领域名，例如 `QuickCreationGenerationRepository`。
- Data 实现：`{RepositoryName}Impl`，API 类以 `Api` 结尾，DTO 以 `Dto` 结尾。
- Presentation 状态：`{Feature}StateHolder`、`{Feature}UiState`、`{Feature}ActionMessage`、`{Feature}Coordinator`。
- Compose 页面：顶层 `Screen` / `Content` 与拆分组件保持功能名后缀。
- 测试：被测类名 + `Test`，路径位于对应 source set 的 `commonTest`、`androidUnitTest` 等目录。

## 6. 平台专项规则

- Android release 必须启用 R8 与资源压缩，并保留 Ktor、serialization、Koin 等 ProGuard keep 规则。
- iOS framework link 与 Xcode build 只能在 macOS 环境作为真实通过证据。
- 平台权限、媒体选择器、WebView/WKWebView 验证码和文件上传必须在真实设备或可说明的模拟环境下验证。
- `RunningHubApiEnvironment` 由 Android/iOS runtime module 在启动期配置，Data 层不得自行决定 base URL。
- Android debug 使用 `.debug` applicationId 后缀，便于与正式包并存验收。

## 7. 推荐验证矩阵

- 文档/规则初始化：占位符扫描、模块文档数量检查、CodeGraph status。
- 架构边界：`.\gradlew.bat --console=plain checkArchitectureBoundaries`。
- 长期治理：`.\gradlew.bat --console=plain checkLongTermGovernance`。
- Android 回归：`.\gradlew.bat --console=plain verifyL1Android`。
- iOS 回归：macOS 上运行 `./gradlew --console=plain verifyL1Ios`。
- 代码审查触发：修改 2 个及以上项目文件时执行 `.codex/skills/code_review/SKILL.md`。
- 架构审查触发：修改 3 个及以上 Gradle 模块或依赖方向时执行 `.codex/agents/arch-review.md`。
