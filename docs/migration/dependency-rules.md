# 架构依赖门禁

本文件记录 AC-08 建立的本地与 CI 共用依赖边界。唯一执行入口是：

```bash
./gradlew checkArchitectureBoundaries
```

## 当前硬规则

- `feature/*` 不得新增 `project(":shared")`、`projects.shared` 或 `com.runninghub.shared` 引用。
- `feature/*/domain/src/commonMain` 不得导入 Compose、Ktor、DataStore、SQLDelight、Android SDK、iOS 平台 API 或任何 Feature Data 实现包。
- `feature/*/domain/build.gradle.kts` 不得依赖任何 Feature Data 实现模块。
- `feature/*/presentation/src/commonMain` 不得导入 Ktor、DataStore、SQLDelight、`shared.data` 或任何 Feature Data 实现包。
- `feature/*/presentation/build.gradle.kts` 不得依赖 Data 实现模块、Ktor、DataStore、SQLDelight 或 `shared`。
- `feature/*/data` 不得依赖 `composeApp` 或导入 `com.runninghub.app.*`。
- `composeApp/commonMain.dependencies` 不得依赖 Feature Data 实现模块；Data 装配只能放在平台启动 source set。
- `composeApp` 中仍需要保留的 `shared` 使用必须登记在 `docs/migration/shared-allowlist.txt`。
- `settings.gradle.kts` 不得重新声明已删除的空壳模块 `:core:designsystem`。
- `shared` 不得新增 Git 跟踪文件；当前基线见 `docs/migration/shared-baseline.txt`。
- Git 索引不得包含 build 目录、APK/AAB/DEX、签名文件或 `local.properties`。
- 应用交付源码和配置不得包含常见密钥形态，例如 OpenAI/GitHub/Google/AWS token、私钥块或硬编码 Bearer token。

## 维护规则

`shared-allowlist.txt` 是迁移期兼容清单，不是扩展点。新增条目必须说明对应业务归属、删除条件和验收 Gate；
如果只是为了让检查通过而扩大清单，应优先把依赖迁移到对应 Feature 或 Core 模块。

`shared-baseline.txt` 是 AC-11 时的遗留文件基线。删除 `shared` 文件不需要更新基线；确属遗留维护时新增文件，
必须同步更新 `docs/migration/shared-ownership.md` 的目标归属和删除条件。
