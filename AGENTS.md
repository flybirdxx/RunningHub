# RunningHub Repository Instructions

## Scope

本文件适用于整个仓库。

子目录中的 `AGENTS.md` 可以补充或覆盖本文件中与该目录相关的规则。
用户在当前任务中的明确要求优先于本文件。

## Context Entry Points

日常开发先读取当前态文档，避免每次加载完整迁移历史：

- `ARCHITECTURE.md`：当前模块边界、依赖方向和运行期装配。
- `DEVELOPMENT.md`：日常验证命令、PR 交付信息和长期治理入口。
- `docs/governance/long-term-priorities.md`：复核建议收敛后的 10 条长期优先级。
- `docs/governance/ai-task-template.md`：AI 任务启动时需要提供的边界和验收字段。
- `docs/governance/chinese-commenting.md`：中文 KDoc、字段注释和复杂流程注释规则。

只有追溯 Gate/AC、证据来源或封板风险时，才读取 `docs/migration/`。

## Project Overview

RunningHub 是面向 Android 和 iOS 的 Kotlin Multiplatform 客户端，
UI 使用 Compose Multiplatform。

当前 Gradle 模块以 `settings.gradle.kts` 为准：

- `:core:model`：跨功能共享的纯业务模型和值对象。
- `:core:common`：跨平台结果类型、错误语义、日志和基础工具。
- `:core:network`：Ktor 客户端、认证插件、DTO 编解码和网络错误映射。
- `:core:storage`：会话、凭据、偏好、草稿等存储抽象及平台实现。
- `:feature:*:domain`：业务模型、Repository interface、UseCase 和业务契约。
- `:feature:*:data`：API、DTO、Mapper、Repository 实现和 Data DI。
- `:feature:quickcreate:presentation`：快捷创作 Coordinator、StateHolder、
  Interactor、ScreenModel 门面和 UI 状态。
- `:composeApp`：应用壳、根导航、DI 组装、平台入口和仍待拆分页面。
历史 `:shared` 模块已退役，不得重新 include、依赖 `projects.shared` 或导入 `com.runninghub.shared.*`。

## Sources of Truth

- 模块清单：`settings.gradle.kts`
- 依赖及插件版本：`gradle/libs.versions.toml`
- Gradle 版本：`gradle/wrapper/gradle-wrapper.properties`
- API 接口及产品资料：`doc/` 和相关接口文档
- 数据库结构：SQLDelight `.sq` 文件
- Android 配置：`composeApp/src/androidMain/AndroidManifest.xml`
- 当前架构说明：`ARCHITECTURE.md`
- 日常开发规范：`DEVELOPMENT.md`
- 迁移状态：`docs/migration/current-state.yaml`
- 迁移 Gate/AC 跟踪：`docs/migration/acceptance.md`

不要在本文件、源码注释或其他说明文档中复制具体依赖版本。
版本发生变化时只修改 Version Catalog。

## Architecture Direction

项目采用 Kotlin Multiplatform、Feature-first、Repository abstraction、
单向数据流和 Koin 依赖注入。

依赖方向必须保持为：

```text
Presentation -> Domain <- Data
Platform bootstrap -> Data modules
```

要求：

- UI 和 ScreenModel 可以依赖 Domain model、Repository interface 或 UseCase。
- UI 和 ScreenModel 不得直接调用 Ktor API、SQLDelight Query、DataStore 或 DataSource。
- Domain 不得依赖 Compose、Ktor、SQLDelight、DataStore、Android/iOS SDK 或 endpoint。
- Data 层负责实现 Domain Repository，并完成 DTO、Entity、Domain Model 映射。
- Data 层不得依赖 `composeApp` 或任何 Presentation 类型。
- 新业务代码不得继续扩大 `shared`；应放入明确 Feature 或 Core 边界。
- 简单 Repository 转发不机械创建 UseCase；跨仓库、计费、任务编排和轮询使用 UseCase/Interactor。

## Kotlin Multiplatform

`commonMain` 只能使用跨平台 API。

禁止：

- `android.*`
- `java.awt.*`
- `platform.UIKit.*`
- `platform.Foundation.*`
- Android `Context`、`Uri`、`Application`
- 仅 JVM 可用的库或类型

平台相关代码放入 `androidMain` 或 `iosMain`，优先通过接口和依赖注入隔离。
不要把 Android/iOS 类型泄漏到 `commonMain` 状态、模型或 Domain 契约中。

## Presentation Rules

Compose 页面使用单向数据流：

```text
UI -> Action -> ScreenModel/StateHolder -> UseCase/Repository -> State -> UI
```

要求：

- 页面状态使用不可变 `data class`，对外暴露只读 `StateFlow`。
- UI 通过明确 Action 或回调发送事件，Composable 尽量保持无状态。
- 业务判断、网络调用、持久化和任务轮询不得放在 Composable 中。
- 导航行为与持久业务状态分离。
- 不在 UI 中直接读取 Token、Cookie、API Key 或 DataStore。
- 用户可见文案优先使用 Compose Resources，不在 Data 层生成最终 UI 文案。
- 不继续向已经过大的 Screen 或 ScreenModel 添加无关职责。

`QuickCreateScreenModel` 是重点治理对象：草稿、上传、计费、生成、轮询、
历史、项目和灵感模板应逐步拆分到 Interactor、Coordinator 或 StateHolder；
不得在一次无测试变更中整体重写。

## Data, Network, Storage

- Repository interface 放在 Domain，Repository implementation 放在 Data。
- DTO 不得暴露给 Presentation。
- API 路径、请求头和环境地址不得定义在 Domain model 中。
- 网络、认证和业务错误统一映射，不直接把服务端 `msg` 当最终 UI 文案。
- 不使用空 `catch`、生产 `runBlocking` 或裸 `println`。
- Authorization/Cookie 只能发送到精确主机白名单。
- Token 刷新必须用 Mutex 去重，401 后最多重试原请求一次。
- 日志不得输出 Token、Cookie、API Key、密码、验证码 Token、完整认证头或请求体。
- 敏感凭据使用 Android Keystore / iOS Keychain；普通草稿、余额缓存和偏好继续使用非敏感存储。

## Dependency Injection

KMP 和 Compose Multiplatform 代码统一使用 Koin。

- Repository 通过接口绑定实现。
- ScreenModel 使用 factory，除非明确需要应用级共享状态。
- 会话状态、认证状态等应使用可注入对象，不使用难以重置的全局 `object`。
- 不在 Composable 中创建 Repository、HttpClient 或数据库实例。
- 不在新 KMP 代码中引入 Hilt。

新增第三方依赖前：

1. 确认现有依赖不能满足需求。
2. 确认目标平台均受支持。
3. 通过 `gradle/libs.versions.toml` 添加。
4. 只添加到需要它的 source set。
5. 在变更说明中解释引入原因。

## Chinese Comments

中文注释属于代码交付内容。新增或修改生产 Kotlin 代码时，必须遵守
`docs/governance/chinese-commenting.md`。

最低要求：

- public/internal 业务类型、函数、Repository、UseCase、Interactor、Coordinator、
  ScreenModel、`expect`/`actual` 和非显而易见配置对象使用中文 KDoc。
- UiState、Domain model、DTO、Entity、Request、Response、Action、Intent、导航参数
  必须逐字段说明业务语义、来源、默认值、空值、单位、生命周期和安全属性。
- Boolean 必须解释 `true`/`false`；可空字段必须解释 `null`；数值必须说明单位和特殊值。
- 复杂流程、并发、取消、分页、缓存、Token 刷新、计费、任务轮询、DTO 映射和兼容分支
  必须在代码附近解释原因和约束。
- 注释与实现不一致视为缺陷；不得添加只复述语法的无价值注释。

## Build and Verification

常用命令：

```bash
./gradlew projects
./gradlew checkArchitectureBoundaries
./gradlew checkLongTermGovernance
./gradlew verifyL1Android
./gradlew verifyL1Ios
```

按变更范围选择验证：

- 修改 `commonMain`：运行架构边界、长期治理和相关模块测试。
- 修改 Android 平台或 release 行为：执行 `verifyL1Android`。
- 修改 iOS、`expect`/`actual`、Keychain、WKWebView 或平台权限：在 macOS 或 macOS CI 执行 `verifyL1Ios`。
- 修改 Repository、UseCase、状态转换、计费或轮询：添加或更新单元测试。
- 文档-only 变更可不跑完整测试，但最终说明未运行原因。

## Change Discipline

开始修改前：

1. 阅读目标目录和相关调用链。
2. 检查附近是否存在更具体的 `AGENTS.md`。
3. 确认代码应该属于 Presentation、Domain、Data、Core 还是平台层。
4. 搜索同类实现，避免重复抽象。
5. 确定最小验证命令。

修改过程中：

- 保持变更范围聚焦，不做无关格式化、重命名或目录移动。
- 不覆盖或回滚用户已有改动。
- 修复根因，不通过重复重试或吞异常掩盖问题。
- 行为变化必须同步修改测试和相关文档。
- 不提交生成文件、构建产物、本地配置或凭据。

除非用户明确要求，否则不要执行 `git commit`、`git push`、`git reset --hard`、
force push、大规模删除或破坏性数据库迁移。

## Generated and Local Files

不得提交构建产物、本地配置、IDE 配置、日志、临时文件、签名文件或任何凭据。
发现已跟踪的构建产物时，应将其从 Git 索引移除，而不只是修改 `.gitignore`。

## Code Style

- 使用 Kotlin 官方代码风格，4 空格缩进。
- JVM target 保持与 Gradle 配置一致。
- 包名保持在 `com.runninghub` 下。
- 禁止 wildcard import。
- 使用有业务意义的命名，避免无边界的 `Util`、`Manager`、`Helper`。
- 常量使用 `UPPER_SNAKE_CASE`。
- Domain model 不使用 DTO/Entity 后缀；网络模型用 `Dto`，数据库模型用 `Entity`。
- Repository 实现用 `Impl`；页面状态用 `*UiState`；用户事件用 `*Action` 或 `*Intent`。

## Git and Pull Requests

使用 Conventional Commits：

```text
feat(scope): ...
fix(scope): ...
refactor(scope): ...
test(scope): ...
docs(scope): ...
chore(scope): ...
```

PR 或任务结果必须说明：

- 修改内容、原因、影响模块和平台。
- 实际验证命令、未执行检查及原因。
- 是否包含行为、API、数据库、配置、发布、隐私或安全变化。
- 本次新增或更新了哪些中文注释。

## Definition of Done

变更完成前确认：

- 代码位于正确架构层。
- `commonMain` 没有新增平台类型。
- 没有新增 Data-to-Presentation 反向依赖。
- 没有新增硬编码凭据或环境地址。
- 中文注释符合 `docs/governance/chinese-commenting.md`。
- 相关模块能够编译，相关测试已经添加或更新。
- 没有提交构建产物，没有进行无关修改。
- 最终说明包含实际验证结果、未验证项和剩余风险。
