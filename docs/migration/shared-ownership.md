# shared 模块退役记录

`shared` 已在 2026-06-22 中期架构整理中整体退役，不再包含在当前 Gradle 模块图中。
新增业务代码必须进入 `feature/*` 或 `core/*`，不得重新 include `:shared`、声明
`projects.shared` / `project(":shared")` 依赖或导入 `com.runninghub.shared.*`。

## 退役结果

| 遗留区域 | 退役状态 | 当前归属 | 防回归要求 |
|---|---|---|---|
| 业务 API / DTO / Repository 实现 | 已迁入对应 Feature Data 或删除。 | `feature:*:data`、`core:network`。 | Data 实现不得依赖 `composeApp`，Feature 不得重新引用 `shared`。 |
| 业务模型 / Repository 契约 | 已迁入 `core:model`、`core:storage` 或对应 Feature Domain。 | `core:*`、`feature:*:domain`。 | Domain 和 Presentation 不得导入 Data、平台 SDK 或 `com.runninghub.shared.*`。 |
| 本地存储与平台工厂 | 已迁入 `core:storage` 和平台 runtime module。 | `core:storage`、`composeApp/src/*Main` 启动层。 | 敏感凭据继续通过 Keystore / Keychain 边界，不得回退到 shared 兼容层。 |
| Koin 组合入口 | `sharedModule` 已删除，生产启动图直接装配 Core 和 Feature Data 模块。 | `composeApp` 只做平台装配。 | 不得恢复 `sharedModule` 作为启动旁路。 |
| Platform / MD5 兼容文件 | 最后 8 个 Git 跟踪文件已删除，`settings.gradle.kts` 不再 include `:shared`。 | 旧登录 MD5 入口已由 `core:common` 承接。 | `verifyL1Ios`、CI 和本地门禁不再显式编译 `:shared`。 |

## 自动门禁

- `./gradlew checkArchitectureBoundaries` 会拒绝 `settings.gradle.kts` 重新 include `:shared`。
- 同一门禁会拒绝任何 Git 跟踪文件落在 `shared/` 目录下。
- Feature、Presentation、composeApp 源码和 Gradle 文件中出现 `project(":shared")`、
  `projects.shared` 或 `com.runninghub.shared` 会失败。
- `verifyL1Ios` 已移除 `:shared:compileKotlinIosSimulatorArm64` 依赖；iOS 验证只覆盖当前
  composeApp 和 Feature/Core 模块图。

## 历史 androidApp 清理

`androidApp/` 的历史 Android 入口源码已删除，Git 跟踪文件不再保留旧
`RunningHubApplication.kt`、`MainActivity.kt`、`AndroidManifest.xml` 或
`build.gradle.kts`。`settings.gradle.kts` 当前没有 include `:androidApp`，
当前 Android 生产入口以 `composeApp/src/androidMain` 为唯一来源。

本地 `androidApp/build` 可能因历史构建残留而继续存在；它属于忽略的构建产物，
不代表 Gradle 模块、生产启动图或 `sharedModule` 依赖。后续如果决定恢复独立
Android App 模块，必须先新增独立 AC，并同步更新 `settings.gradle.kts`、本文件和
对应验证命令。
