# 架构依赖门禁

本文件记录 AC-08 建立的本地与 CI 共用依赖边界。唯一执行入口是：

```bash
./gradlew checkArchitectureBoundaries
```

## 当前硬规则

- 当前 active 模块不得新增 `project(":shared")`、`projects.shared` 或 `com.runninghub.shared` 引用。
- `feature/*/domain/src/commonMain` 不得导入 Compose、Ktor、DataStore、SQLDelight、Android SDK、iOS 平台 API 或任何 Feature Data 实现包。
- `feature/*/domain/build.gradle.kts` 不得依赖任何 Feature Data 实现模块。
- `feature/*/presentation/src/commonMain` 不得导入 Ktor、DataStore、SQLDelight、`shared.data` 或任何 Feature Data 实现包。
- `feature/*/presentation/build.gradle.kts` 不得依赖 Data 实现模块、Ktor、DataStore、SQLDelight 或 `shared`。
- `feature/*/data` 不得依赖 `composeApp` 或导入 `com.runninghub.app.*`。
- `composeApp/commonMain.dependencies` 不得依赖 Feature Data 实现模块；Data 装配只能放在平台启动 source set。
- `composeApp`、`core` 和 `feature` 源码不得重新引用已退役的 `shared` 包或 Gradle 模块。
- `settings.gradle.kts` 不得重新声明已删除的空壳模块 `:core:designsystem`。
- `settings.gradle.kts` 不得重新 include 已退役的 `:shared` 模块。
- Git 索引不得包含 `shared/` 下的跟踪文件。
- Git 索引不得包含 build 目录、APK/AAB/DEX、签名文件或 `local.properties`。
- 应用交付源码和配置不得包含常见密钥形态，例如 OpenAI/GitHub/Google/AWS token、私钥块或硬编码 Bearer token。

## 维护规则

`shared` 已退役，不再保留 allowlist 或 baseline 旁路。
如果后续确需恢复兼容层，必须先新增独立迁移任务，明确目标归属、删除条件和验收命令，
再同步更新 `settings.gradle.kts`、`docs/migration/shared-ownership.md` 和本门禁。
