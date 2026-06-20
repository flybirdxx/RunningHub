# RunningHub Repository Instructions

## Scope

本文件适用于整个仓库。

子目录中的 `AGENTS.md` 可以补充或覆盖本文件中与该目录相关的规则。
用户在当前任务中的明确要求优先于本文件。

## Project Overview

RunningHub 是一个面向 Android 和 iOS 的 Kotlin Multiplatform 客户端，
UI 使用 Compose Multiplatform。

当前 Gradle 模块以 `settings.gradle.kts` 为准：

- `:shared`：共享业务模型、Repository 接口与实现、Ktor 网络层、
  SQLDelight/DataStore、本地存储和平台抽象。
- `:composeApp`：Compose Multiplatform UI、导航、ScreenModel、
  应用入口和平台 UI 实现。

不要假设历史模块仍然有效。修改构建配置前，先检查：

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- 目标模块的 `build.gradle.kts`

## Sources of Truth

以下文件分别是对应信息的唯一事实来源：

- 模块清单：`settings.gradle.kts`
- 依赖及插件版本：`gradle/libs.versions.toml`
- Gradle 版本：`gradle/wrapper/gradle-wrapper.properties`
- API 接口及产品资料：`doc/` 和相关接口文档
- 数据库结构：SQLDelight `.sq` 文件
- Android 配置：`composeApp/src/androidMain/AndroidManifest.xml`

不要在本文件、源码注释或其他说明文档中复制具体依赖版本。
版本发生变化时只修改 Version Catalog。

## Architecture Direction

项目采用：

- Kotlin Multiplatform
- Feature-first 业务组织
- Repository abstraction
- 单向数据流
- 对复杂业务使用 UseCase / Interactor
- Koin 依赖注入

依赖方向必须保持为：

`Presentation -> Domain <- Data`

具体要求：

1. UI 和 ScreenModel 可以依赖 Domain model、Repository interface 或 UseCase。
2. UI 和 ScreenModel 不得直接调用 Ktor API、SQLDelight Query、
   DataStore 或其他 DataSource。
3. Domain 不得依赖 Compose、Ktor、SQLDelight、DataStore、
   Android SDK、iOS SDK 或具体 API endpoint。
4. Data 层负责实现 Domain Repository，并完成 DTO、Entity、
   Domain Model 之间的映射。
5. Data 层不得依赖 `composeApp` 或任何 Presentation 类型。
6. 新业务代码不得继续扩大无边界的通用 `shared` 包；
   应放入明确的业务功能或核心能力目录。
7. 简单 Repository 转发不需要机械创建 UseCase。
   跨 Repository、复杂校验、计费、任务编排、轮询等逻辑应放入 UseCase 或 Interactor。

## Kotlin Multiplatform Rules

### commonMain

`commonMain` 只能使用跨平台 API。

禁止：

- `android.*`
- `java.awt.*`
- UIKit、Foundation 等直接平台调用
- Android `Context`、`Uri`、`Application`
- 仅 JVM 可用的库或类型

跨平台状态中使用平台无关表示，例如：

- URI 使用 `String` 或专用值对象
- 文件使用项目定义的共享模型
- 时间使用 `kotlinx-datetime`

### Platform source sets

平台相关代码放入：

- `androidMain`
- `iosMain`

优先使用接口和依赖注入隔离平台能力。
仅在编译期平台实现确实必要时使用 `expect` / `actual`。

不要因为 Android 实现方便，就把 Android 类型泄漏到 `commonMain`。

## Presentation Rules

Compose 页面使用单向数据流：

`UI -> Action -> ScreenModel -> UseCase/Repository -> State -> UI`

要求：

- 页面状态使用不可变 `data class`。
- 对外暴露只读 `StateFlow`。
- UI 通过明确的 Action 或回调发送事件。
- Composable 尽量保持无状态。
- 业务判断、网络调用、持久化和任务轮询不得放在 Composable 中。
- 导航行为与持久业务状态分离。
- 不在 UI 中直接读取 Token、Cookie、API Key 或 DataStore。
- 用户可见文案优先使用 Compose Resources，不在 Data 层生成最终 UI 文案。
- 新文件原则上保持单一职责。
- 不得继续向已经过大的 Screen 或 ScreenModel 添加无关职责。

`QuickCreateScreenModel` 是当前重点治理对象：

- 不继续加入新的业务子系统。
- 草稿、上传、计费、生成、轮询、历史、项目和灵感模板应逐步拆分。
- 修改该功能时优先提取局部 Interactor、Coordinator 或 StateHolder。
- 不在一次变更中无测试地整体重写该功能。

## Data and Network Rules

- Repository interface 放在 Domain。
- Repository implementation 放在 Data。
- DTO 不得直接暴露给 Presentation。
- API 路径、请求头和环境地址不得定义在 Domain model 中。
- 统一映射网络错误、认证错误和业务错误。
- 不要静默吞掉异常。
- 不使用空的 `catch`。
- 不使用 `runBlocking` 处理生产业务流程。
- Token 刷新必须防止并发重复刷新和无限 401 重试。
- 日志不得输出 Token、Cookie、API Key、密码或完整认证请求头。

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

## 中文注释强制规范

中文注释属于代码交付内容，而不是可选项。新增或修改代码时，
如果缺少符合本节要求的中文注释，则任务视为未完成。

### 基本要求

- 所有新增或修改的生产代码必须补充必要的简体中文注释。
- 注释应解释设计意图、业务规则、数据流、边界条件和异常处理，不能只翻译代码语句。
- 类名、函数名、参数名、协议名以及通用技术术语可以保留英文。
- 修改已有逻辑时，必须同步检查并更新相关注释。
- 与实际实现不一致的注释视为代码缺陷。
- 不允许为了减少工作量而删除仍然有效的注释。
- 完成任务前必须检查本次修改的全部文件，确认注释覆盖符合要求。

### 必须使用 KDoc 的位置

以下新增或修改的 Kotlin 声明必须使用 `/** ... */` 中文 KDoc：

- public、internal 的类、接口、枚举和 sealed class/interface
- Repository、UseCase、Interactor、Coordinator、ScreenModel
- public、internal 的函数和扩展函数
- 具有业务语义的数据模型
- `expect` / `actual` 平台抽象
- 非显而易见的配置对象和常量

KDoc 根据实际情况说明：

1. 该类型或函数承担什么职责。
2. 为什么需要它，以及它位于哪一层。
3. 参数含义、单位、格式和有效范围。
4. 返回值以及可能出现的状态。
5. 副作用，例如网络请求、数据库写入、文件操作或状态更新。
6. 并发、协程、线程和生命周期约束。
7. 可能发生的业务错误及调用方处理方式。
8. Android 与 iOS 的平台差异。

### 必须添加块注释的位置

下列逻辑必须在代码附近添加中文块注释或行注释，重点解释“为什么”：

- 多阶段业务流程
- 复杂条件和状态转换
- 分页、刷新、去重和缓存策略
- 协程并发、Job 取消、Mutex 和防抖
- Token 刷新、401 重试和会话失效
- 任务提交、轮询、超时、取消和失败恢复
- 计费预览、余额判断和价格计算
- DTO、Entity 与 Domain Model 映射
- 兼容旧接口或异常服务端数据的处理
- 安全相关处理和敏感数据脱敏
- Android/iOS 平台行为差异
- 临时规避方案以及无法立即消除的技术债务

复杂函数应按执行阶段添加注释，例如：

```kotlin
// 第一阶段：校验用户输入，避免无效参数进入计费和任务提交链路。

// 第二阶段：取消旧的价格预览请求，防止旧响应覆盖用户刚刚修改的新参数。

// 第三阶段：只有价格预览通过后才允许提交任务，避免余额不足时仍产生远程任务。
```

### 禁止的注释方式

禁止添加没有维护价值的注释：

```kotlin
// 设置 loading 为 true
state.loading = true

// 调用接口
api.loadData()

// 返回结果
return result
```

应解释代码本身无法表达的信息：

```kotlin
// 在请求发出前立即进入加载状态，避免用户连续点击导致同一页被重复提交。
state.loading = true

// 此处必须复用 Repository 的分页入口，确保搜索结果与首页列表采用相同的错误映射规则。
repository.loadPage()

// 保留服务端返回顺序，因为该顺序包含运营侧配置的推荐权重，不能在客户端重新排序。
return result
```

### TODO 和临时方案

`TODO`、`FIXME` 和临时兼容逻辑必须包含：

- 产生原因
- 当前风险
- 后续移除条件
- 相关任务编号；没有任务编号时写明待办内容

示例：

```kotlin
// TODO(RH-124): 服务端暂时会返回空的 skuId。
// 当前使用 bindingId 作为降级标识；服务端完成数据修复后应删除此兼容分支。
```

不得使用以下无信息量写法：

```kotlin
// TODO: later
// FIXME
// 临时处理
```

### Kotlin KDoc 示例

```kotlin
/**
 * 协调快捷创作页面的价格预览流程。
 *
 * 当用户修改模型、分辨率、数量或动态参数时，本类会取消上一次尚未完成的预览请求，
 * 并在防抖时间结束后提交最新参数。这样可以避免旧请求晚于新请求返回时覆盖正确价格。
 *
 * 并发约束：
 * - 同一时间只允许存在一个有效的价格预览任务。
 * - 已取消请求的结果不得写入页面状态。
 * - ScreenModel 销毁后，预览任务必须随作用域一起取消。
 *
 * @param repository 快捷创作数据仓库，负责调用远程计费接口。
 * @param debounceMillis 用户连续修改参数时的防抖时长，单位为毫秒。
 */
class FeePreviewInteractor(
    private val repository: QuickCreateRepository,
    private val debounceMillis: Long,
) {

    /**
     * 根据当前创作参数获取价格预览。
     *
     * @return 计费成功时返回价格信息；余额不足仍属于有效业务结果，
     * 网络异常或响应格式错误则返回失败结果。
     */
    suspend fun preview(
        request: ImageGenerationRequest,
    ): Result<QuickCreationFeePreview> {
        // Repository 统一完成网络异常和服务端错误码映射，
        // 此处不捕获异常，以免丢失调用方需要展示的具体失败原因。
        return repository.previewImageQuickCreationFee(request)
    }
}
```

### 修改完成检查

提交结果前逐项确认：

- [ ] 新增和修改的核心类型具有中文 KDoc。
- [ ] public、internal 函数具有必要的中文 KDoc。
- [ ] 复杂流程已经按阶段添加中文注释。
- [ ] 并发、状态转换和异常降级策略已有说明。
- [ ] 注释解释的是原因和约束，而不是复述语法。
- [ ] 没有过时、错误或与实现冲突的注释。
- [ ] 没有泄露 Token、Cookie、API Key、密码等敏感信息。
- [ ] 最终回复中说明本次新增或更新了哪些注释。

## Build and Verification

使用仓库内 Gradle Wrapper：

```bash
./gradlew projects
```

常用检查：

```bash
# 构建 Android 应用
./gradlew :composeApp:assembleDebug

# 检查共享模块 Android 编译
./gradlew :shared:compileKotlinAndroid

# 运行可用的 JVM / Android 单元测试
./gradlew test

# Android lint
./gradlew :composeApp:lintDebug
```

在 macOS 上修改 iOS 代码后，还应执行对应的 iOS framework 或 Xcode 构建检查。

按变更范围选择验证：

- 修改 `shared/commonMain`：
  编译 `:shared`，并运行相关 `commonTest`。
- 修改 `composeApp/commonMain`：
  至少执行 `:composeApp:assembleDebug`。
- 修改 `androidMain`：
  执行 Android assemble 和 lint。
- 修改 `iosMain`：
  在 macOS 上执行对应 iOS 编译。
- 修改 Gradle 配置或 Version Catalog：
  至少执行 `./gradlew projects` 和受影响模块编译。
- 修改 Repository、UseCase、状态转换或计费逻辑：
  添加或更新单元测试。

如果某项检查因环境限制无法运行，必须在最终结果中明确说明：
未运行的命令、原因和仍存在的风险。

## Testing Rules

测试优先覆盖：

- Repository 与 DTO-to-Domain 映射
- UseCase / Interactor 业务规则
- ScreenModel 状态转换
- 分页、刷新和重复请求
- Token 过期及刷新
- 任务轮询的成功、失败、取消和超时
- 计费预览与余额不足
- 草稿保存及恢复
- Android/iOS 平台实现差异

测试命名应表达行为，例如：

```kotlin
fun `refresh replaces existing items when request succeeds`() 
fun `expired session emits logged out state when refresh fails`()
```

不要只断言实现细节。

## Change Discipline

开始修改前：

1. 阅读目标目录和相关调用链。
2. 检查附近是否存在更具体的 `AGENTS.md`。
3. 确认代码应该属于 Presentation、Domain、Data 还是平台层。
4. 搜索同类实现，避免重复抽象。
5. 确定最小验证命令。

修改过程中：

- 保持变更范围聚焦。
- 不进行无关格式化、重命名或目录移动。
- 不覆盖用户尚未提交的修改。
- 修复根因，不通过重复重试或吞异常掩盖问题。
- 除非任务明确要求，否则不进行全项目重写。
- 行为变化必须同步修改测试和相关文档。
- 不提交生成文件和构建产物。

除非用户明确要求，否则不要执行：

- `git commit`
- `git push`
- `git reset --hard`
- force push
- 大规模删除
- 数据库破坏性迁移

## Generated and Local Files

不得提交：

- 任意模块的 `build/`
- APK、AAB、DEX、framework 构建产物
- `.gradle/`
- `.kotlin/`
- `local.properties`
- IDE 本地配置
- 日志和临时文件
- 签名文件
- API Key、Token、Cookie 和其他凭据

发现已跟踪的构建产物时，应将其从 Git 索引移除，
而不只是修改 `.gitignore`。

## Code Style

- 使用 Kotlin 官方代码风格。
- 4 空格缩进。
- JVM target 保持与 Gradle 配置一致。
- 包名保持在 `com.runninghub` 下。
- 禁止 wildcard import。
- 使用有业务意义的命名，避免 `Util`、`Manager`、`Helper`
  等缺乏边界的通用类名。
- 常量使用 `UPPER_SNAKE_CASE`。
- Domain model 不使用 DTO 或 Entity 后缀。
- 网络模型使用 `Dto` 后缀。
- 数据库模型使用 `Entity` 后缀。
- Repository 实现使用 `Impl` 后缀。
- 页面状态使用 `*UiState`。
- 用户事件使用 `*Action` 或 `*Intent`，同一 Feature 内保持一致。

## Security

- 所有凭据必须放在安全的本地配置或平台安全存储中。
- Android 敏感凭据使用安全存储方案。
- iOS 敏感凭据使用 Keychain。
- 不把秘密写入源码、注释、测试 Fixture 或 Git 历史。
- 日志和错误报告中必须脱敏。
- Release 构建不得启用详细网络 Body 日志。
- 不自行更改生产接口、签名配置或认证协议。

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

提交应保持单一目的。

Pull Request 或任务结果必须说明：

- 修改了什么
- 为什么修改
- 影响哪些模块和平台
- 执行了哪些验证命令
- 哪些检查没有执行及原因
- 是否包含行为、API、数据库或配置变化
- 本次新增或更新了哪些中文注释

## Definition of Done

变更完成前确认：

- 代码位于正确架构层。
- `commonMain` 没有新增平台类型。
- 没有新增 Data-to-Presentation 反向依赖。
- 没有新增硬编码凭据或环境地址。
- 中文注释符合“中文注释强制规范”。
- 相关模块能够编译。
- 相关测试已经添加或更新。
- 没有提交构建产物。
- 没有进行无关修改。
- 最终说明包含实际验证结果。
