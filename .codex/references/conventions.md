# RunningHub 编码约定

生成时间：2026-06-27

## 架构约定

- Feature 按 `domain`、`data`、`presentation` 拆分。
- Domain 保存稳定模型、错误语义和 Repository 接口。
- Data 保存 Ktor API、DTO、Mapper、RepositoryImpl 和 Koin data module。
- Presentation 保存 `StateHolder`、`UiState`、`Coordinator`、Interactor 和用户可见语义。
- `composeApp` 保存 Compose UI、Voyager Screen、ScreenModel facade、平台 runtime module 和平台 expect/actual。

## 命名约定

- 模块：`core:{name}`、`feature:{area}:domain|data|presentation`。
- 包名：`com.runninghub.{core|feature|app}...`，与模块层级对齐。
- API：`{Area}Api`。
- DTO：`{Area}{Purpose}Dto` 或 `{Purpose}RequestDto` / `{Purpose}ResponseDto`。
- Repository 接口：`{Area}Repository` 或具体能力名。
- Repository 实现：`{RepositoryName}Impl`。
- Presentation 状态：`{Area}StateHolder`、`{Area}UiState`、`{Area}Coordinator`。
- Voyager ScreenModel：`{Screen}ScreenModel`，只做应用壳 facade。
- 测试：`{被测类}Test`。

## Kotlin/KMP 约定

- `commonMain` 不导入 Android/iOS 平台 API。
- 平台差异使用 `expect` / `actual` 或 `androidMain` / `iosMain` 装配。
- 生产代码使用结构化协程；禁止 `runBlocking`、`GlobalScope` 和空 `catch`。
- 公共 API 和复杂业务边界保留中文 KDoc，具体规则参考 `docs/governance/chinese-commenting.md`。

## Compose/UI 约定

- UI 文案优先进入 `composeApp/src/commonMain/composeResources/values/strings.xml`。
- 颜色、尺寸、排版优先通过主题 token 或局部语义变量表达。
- 页面级复杂状态下沉到 Feature Presentation。
- Compose 页面只做状态收集、事件转发和可视结构。

## 数据与安全约定

- API 层固定 endpoint 和远端字段，Repository 负责映射和错误归一。
- Token、Cookie、API Key、验证码 token 和请求体不得写入日志、截图说明或错误文案。
- 敏感凭据走 Keystore/Keychain，非敏感设置走 DataStore。
- token refresh JSON 用 DTO 解析，不用正则。

## 构建约定

- KMP 库模块统一走约定插件 `runninghub.android.library`（`AndroidLibraryConventionPlugin`）。
- 库模块的 Android 集成使用 `com.android.kotlin.multiplatform.library`，不再使用 `com.android.library`（AGP 9.0 起后者与 KMP 不兼容）。该插件由 `kotlin { android { ... } }` 提供 android target，因此库模块不调用 `androidTarget()`。
- 库模块 `namespace` 由 `AndroidLibraryConventionPlugin` 按 Gradle 路径自动推导：`:feature:auth:data` → `com.runninghub.feature.auth.data`；模块 build 脚本不再声明 `android { namespace = ... }`。新增库模块的包名必须与其 Gradle 路径对齐，否则推导出的 namespace 会错位。
- `compileSdk` / `minSdk` / `jvmTarget(17)` 统一在约定插件配置，模块脚本只保留自身 target 列表与依赖。
- `composeApp` 仍使用 `com.android.application` + 经典 `androidTarget()`（`KotlinMultiplatformConventionPlugin` 保留该分支）；该组合当前仍会触发唯一一条弃用告警，**已评估后决定暂缓处理**，详见下方「composeApp KMP-AGP 弃用告警」决策记录。

### composeApp KMP-AGP 弃用告警（决策：暂缓重构）

**告警**：`w: The 'org.jetbrains.kotlin.multiplatform' plugin deprecated compatibility with Android Gradle plugin: 'com.android.application'`（来源 `composeApp` 应用 `runninghub.android.application` + `runninghub.kotlin.multiplatform`）。

**调研结论（官方，AGP 9.1.1）**：不存在 `com.android.kotlin.multiplatform.application` 之类的 application 侧 KMP 插件，AGP 只提供**库侧**的 `com.android.kotlin.multiplatform.library`。官方（[kotl.in/kmp-project-structure-migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)）对 application 模块唯一给出的路径是**项目结构重构**：
1. 把 Android 入口（`MainActivity`/`RunningHubApplication` 等）抽到独立 `androidApp` 模块，该模块用**经典 `com.android.application`**（纯 Android App，不套 KMP 插件，告警随之消失）；
2. 把 `composeApp` 从 application **降级为 KMP 库**，改用 `com.android.kotlin.multiplatform.library` 的 `kotlin { androidLibrary { } }`。

**时间线（硬约束）**：AGP 9 仍以 legacy 兼容让旧组合可编译，仅告警；legacy API 将在 **AGP 10（预计 2026 下半年）彻底移除**。短期逃生阀为 `gradle.properties` 加 `android.enableLegacyVariantApi=true`（能否静音**本条**告警未实测，不建议直接进主干）。

**为何暂缓**（重构成本高、部分本地不可验证）：
- KMP 库插件**不支持 build variants**，而 `composeApp` 强依赖 `debug/release` 双 variant：各注入 6 个环境 URL 的 `buildConfigField`、`applicationIdSuffix=.debug`、`isMinifyEnabled`/`isShrinkResources`。这些必须整体迁到新 `androidApp` 模块。
- `androidMain/.../di/AndroidRuntimeModule.kt` 直接 `import com.runninghub.app.BuildConfig` 读 `RUNNINGHUB_*`；BuildConfig 随 variant 迁走后此处会断链，需改成 expect/actual 或配置注入间接层。
- 需精确切分 `androidMain`：入口进 `androidApp`，`expect/actual` 与平台实现留 `composeApp` 库；边界易错。
- 影响 iOS 目标（`iosX64/iosArm64/iosSimulatorArm64` framework）装配与 `iosApp` 消费，**Windows 本地无法验证 iOS link**，须留 macOS/CI。
- 收益仅"消一条 deprecation 告警"，远低于上述风险。

**重启触发点**：准备升级 AGP 10 之前，或具备 macOS/CI 全程验证 iOS 时，按上述两步执行重构。

**验证**：本条为文档-only 变更，未改动任何 build 配置，不触发 `checkArchitectureBoundaries`/`verifyL1Android`；`composeApp` 的插件应用与 `KotlinMultiplatformConventionPlugin` 分支保持不变。

## 验证约定

- 依赖或 source set 变更：`checkArchitectureBoundaries`。
- 长期治理文档或发布配置变更：`checkLongTermGovernance`。
- Android 交付路径：`verifyL1Android`。
- iOS 交付路径：macOS `verifyL1Ios`。
- 文档-only：占位符扫描 + references 模块文档数量检查。
