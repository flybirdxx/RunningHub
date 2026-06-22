# RunningHub Architecture

本文档描述当前有效架构，不记录迁移过程。迁移历史和旧证据保留在 `docs/migration/`
以及 `docs/archive/migration-2026/`，日常开发应优先读取本文件和 `DEVELOPMENT.md`。

## 模块边界

RunningHub 是 Kotlin Multiplatform 客户端，当前模块清单以 `settings.gradle.kts` 为准。
核心依赖方向保持为：

```text
Presentation -> Domain <- Data
```

- `core:model` 只承载跨功能业务模型和值对象。
- `core:common` 承载跨平台结果类型、错误语义、日志和基础工具。
- `core:network` 承载 Ktor 客户端、认证插件、DTO 编解码和网络错误映射。
- `core:storage` 承载会话、凭据、偏好、草稿等存储抽象及平台实现。
- `feature:*:domain` 承载业务模型、Repository interface、UseCase 或 Interactor 契约。
- `feature:*:data` 实现对应 Domain Repository，并负责 DTO/Entity/Domain 映射。
- `feature:quickcreate:presentation` 承载快捷创作的 Coordinator、StateHolder、Interactor、ScreenModel 门面和 UI 状态。
- `composeApp` 是应用壳、根导航、DI 组装、平台入口和仍待拆分页面的落点，不应继续成为新的 UI 单体。

历史 `shared` 模块已经从当前 Gradle 模块图退役。生产源码、测试和构建脚本不得重新
include `:shared`、依赖 `projects.shared` 或导入 `com.runninghub.shared.*`；
如确需恢复兼容层，必须先新增独立迁移任务并同步更新架构门禁。

## 运行期装配

Android 与 iOS 平台入口负责装配 Koin runtime module、当前 `ApiEnvironment`、
安全凭据存储和 Feature Data 模块。UI、ScreenModel 和 Domain 不直接读取 Token、
Cookie、API Key、DataStore、Keychain、Keystore 或 Ktor API。

## 长期治理

长期治理检查由 `runninghub.long-term-governance` 插件提供，入口为：

```bash
./gradlew checkLongTermGovernance
```

该任务校验架构文档、开发文档、ADR、迁移归档说明、日志/契约测试/文案/性能/发布规范，
并阻止 `AGENTS.md` 重新膨胀为迁移历史合集或 `composeApp` 新增未登记的超大 commonMain Kotlin 文件。无独立 Presentation 模块的
大体量 UI Feature 也必须遵守 `docs/governance/feature-presentation-thresholds.txt` 基线。
认证头只能发送到 `ApiEnvironment.trustedAuthHosts` 中的精确主机，禁止用 `contains`、
后缀匹配或通配符判断 RunningHub 主机。
Android 通过 build type 注入的 `RUNNINGHUB_*` BuildConfig 字段选择 API 环境；iOS 通过
同名进程环境变量选择 API 环境。未配置 staging/dev 时默认回退 production，但不得把平台启动层退回
不可覆盖的固定 production 调用。
敏感凭据只能通过平台安全存储边界读写：Android 使用 Keystore backed 存储，iOS 使用
Keychain；旧 Preferences 凭据只允许通过 `MigratingCredentialStore` 按字段懒迁移，余额缓存
和快捷创作草稿继续留在非敏感 Preferences 边界。
iOS 权限和媒体选择不得退回固定授权、空函数或直接成功；平台层必须保留真实系统授权、
文件/媒体选择器、设置页跳转和可读取 URI。
短信图形验证码统一由受控 TAC HTML、Android WebView 和 iOS WKWebView 承载，token/关闭事件
必须通过最小 bridge 或自定义 scheme 回到 Compose 状态层，并在弹窗销毁时清理平台 handler。
