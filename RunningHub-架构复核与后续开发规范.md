# RunningHub 架构复核与后续开发规范

> 审查分支：`feature/kmp-refactoring`  
> 审查 HEAD：`03ab929958cab94473aaade0a3c52dd8d794d2b6`  
> 审查范围：Gradle 模块图、依赖方向、平台组合根、认证与会话、QuickCreate、CI、iOS 启动壳、迁移门禁及长期维护风险。

---

# 一、复核结论

## 1. 总体判断

当前项目的 **L1 架构迁移主体已经完成**：

- 已形成 `core + feature domain/data + composeApp` 的模块边界。
- `composeApp/commonMain` 只依赖 Core、Domain 和 QuickCreate Presentation。
- Android/iOS Data 实现均在平台启动层装配。
- Android 生产启动图不再依赖 `sharedModule`。
- 会话根导航已统一到 `SessionManager`。
- Auth、Discovery、Task、Community、QuickCreate 均已建立较清晰的领域与数据边界。
- 已存在架构边界检查、Android CI 和 iOS CI 定义。
- QuickCreate 的状态、计费、上传、轮询、历史和项目等高风险逻辑已有较多自动化测试。

但当前只能判定为：

> **结构迁移条件通过，当前 HEAD 尚未完成正式封板；iOS 侧不仅缺少运行验证，仍存在明确的占位实现。**

因此不建议把当前状态标记为“生产级双端迁移全部完成”。

## 2. 当前评级

| 维度 | 结果 |
|---|---|
| 模块与依赖方向 | 通过 |
| Android 生产组合根 | 通过 |
| iOS 生产组合根结构 | 基本通过，未运行验证 |
| 会话与根导航唯一事实来源 | 通过 |
| 认证 401 刷新与单次重试 | 基本通过 |
| QuickCreate 架构治理 | 通过 |
| Discovery 架构治理 | 通过 |
| 当前 HEAD CI 证据 | 未通过／证据过期 |
| iOS 媒体权限与选择 | 未完成 |
| 安全存储 | 未完成 |
| Release 构建治理 | 未完成 |
| 遗留 `shared`/`androidApp` 清理 | 部分完成 |
| 长期构建脚本可维护性 | 需要治理 |

---

# 二、必须先处理的高优先级问题

## P0-1 当前 HEAD 的 CI 与验收证据需要重新生成

当前 HEAD 为：

```text
03ab929958cab94473aaade0a3c52dd8d794d2b6
```

仓库中的 Android/iOS GitHub Actions 证据仍绑定：

```text
85f134d23ac58768e8a40c3172f3fbb34ca90699
```

迁移文档中还存在 `d7510d...` 的旧 HEAD 记录。

根工程验收器明确要求外部证据的 `headSha` 等于当前 HEAD，因此当前提交不能直接复用旧证据封板。

### 验收动作

```bash
# Windows / Linux
./gradlew verifyL1Android

# macOS
./gradlew verifyL1Ios

# 当前 HEAD 推送后，确认 Android CI 与 iOS CI 均绑定同一 SHA。
```

同时更新：

```text
docs/migration/evidence/github-actions-android.json
docs/migration/evidence/github-actions-ios.json
docs/migration/current-state.yaml
docs/migration/acceptance.md
docs/migration/l1-seal-audit.md
```

## P0-2 修复认证请求域名判断

当前网络拦截器通过：

```kotlin
host.contains("runninghub.cn")
```

判断是否附加 Authorization 和 Cookie。

这会把类似以下域名也视为可信：

```text
runninghub.cn.example.com
evilrunninghub.cn
```

只要应用将来请求了这类 URL，就存在敏感认证头被发送到错误主机的风险。

### 必须改为精确白名单

```kotlin
private val trustedHosts = setOf(
    "www.runninghub.cn",
)

private fun isTrustedRunningHubHost(host: String): Boolean =
    host.lowercase() in trustedHosts
```

如果确实需要多个子域名，应显式列举，不建议使用宽泛的 `contains`。

## P0-3 iOS 权限与媒体选择不是“未验证”，而是尚未实现

当前 iOS PermissionController：

- `pickMedia()` 没有任何实现。
- `checkAndRequest()` 无条件调用 `onGranted()`。
- `openAppSettings()` 没有实现。

iOS PermissionDataStore 也固定返回 `GRANTED`，所有写入函数为空。

这意味着媒体选择、权限拒绝、永久拒绝和系统设置跳转在 iOS 上都无法按真实状态工作。

### 完成标准

- 图片选择使用 PHPicker 或适合当前 Compose/FilePicker 栈的系统选择器。
- 视频和音频选择能够返回可读取的安全作用域 URL。
- 正确处理取消、拒绝、有限照片权限和永久拒绝。
- `openAppSettings()` 可以跳转应用设置。
- 不再将 iOS 权限固定视为 GRANTED。
- 补齐需要的 Info.plist 权限说明。
- 在 Simulator 和至少一台真机上完成上传回归。

## P0-4 最新新增的 iOS 图形验证码必须单独验证

当前 HEAD 新增 WKWebView 图形验证码实现，至少需要验证：

- TAC JS/CSS 能正常加载。
- 成功后能回传 `validToken`。
- 关闭回调有效。
- 多次打开不会残留旧 handler。
- 弹窗销毁后没有 retain cycle。
- 网络失败和脚本失败有可理解的降级状态。
- ATS、Cookie、同源策略和跨域请求符合预期。
- Android WebView 与 iOS WKWebView 的行为一致。

---

# 三、架构边界规范

## 1. 模块职责

```text
core:model
  纯业务模型和值对象；不得依赖 UI、网络、存储和平台 SDK。

core:common
  通用错误、结果类型、调度器抽象和无业务归属的纯工具。

core:network
  Ktor Client、环境配置、认证头、Token 刷新、网络错误映射。

core:storage
  存储端口、非敏感偏好、草稿、余额缓存和平台存储工厂。

feature:<name>:domain
  领域模型、Repository 接口、UseCase、业务错误和业务规则。

feature:<name>:data
  API、DTO、Mapper、DataSource、Repository 实现和 Data DI。

feature:<name>:presentation
  UiState、Action、Reducer、StateHolder、Interactor 和纯 Presentation 逻辑。

composeApp
  应用入口、根导航、平台装配、Compose 页面，以及尚未独立成模块的轻量 Presentation。
```

## 2. 强制依赖方向

```text
Presentation -> Domain <- Data
Platform bootstrap -> Data modules
```

禁止：

```text
Domain -> Data
Domain -> Compose/Ktor/DataStore/SQLDelight/platform SDK
Presentation -> Data implementation
Data -> composeApp
Feature Data -> 其他 Feature Data
新 Feature -> shared
composeApp commonMain -> Feature Data
```

跨 Feature 协作只能通过：

- 对方 Domain 接口；
- 独立 integration/adapter；
- 应用组合根中的适配器。

## 3. 新模块判定

只有满足以下任一条件才新增 Core 模块：

- 被至少两个 Feature 使用；
- 语义稳定，不属于某个具体业务；
- 拥有独立测试与版本边界。

不要为了“看起来整齐”创建空壳模块。

Feature 满足以下任一条件时，应建立独立 Presentation 模块：

- ScreenModel/Coordinator 超过 400 行；
- 注入依赖超过 8 个；
- 有三个以上页面或多个复用 StateHolder；
- UI 状态被多个 App 入口复用；
- 页面测试显著拖慢 composeApp。

---

# 四、Presentation 开发规范

## 1. 单向数据流

统一采用：

```text
UI -> Action -> ScreenModel/StateHolder -> UseCase/Repository -> UiState -> UI
```

推荐契约：

```kotlin
data class FeatureUiState(...)

sealed interface FeatureAction {
    data object Retry : FeatureAction
    data class QueryChanged(val value: String) : FeatureAction
}

class FeatureScreenModel(...) {
    val uiState: StateFlow<FeatureUiState>
    fun onAction(action: FeatureAction)
}
```

## 2. 状态规则

- UiState 必须不可变。
- 对外只暴露 `StateFlow`。
- 不把 Context、Uri、UIViewController、NSURL 等平台对象放进 commonMain 状态。
- 一次性事件不得长期保存在普通 Boolean 中；使用明确消费语义。
- Loading 状态必须对应具体流程，避免一个 `isLoading` 控制多个请求。
- 每个错误字段必须说明来源、展示位置、重试方式和清理时机。
- UiState 超过约 30 个字段时，应拆成有业务意义的子状态。

## 3. ScreenModel 规则

- 不直接使用 Ktor、DataStore、SQLDelight。
- 不直接读取 Token、Cookie 或 API Key。
- 不在 `init` 中启动不可控的永久任务。
- 所有 Job 绑定页面作用域。
- 必须定义取消、超时和页面销毁行为。
- 不可见 Tab 不得持续轮询，除非有明确产品需求。
- 页面销毁必须释放上传、轮询、防抖和自动保存任务。

---

# 五、Domain 与 Data 规范

## 1. Domain

- Domain 不保存 endpoint、Header、DTO 字段名或服务端路径。
- 排序、状态、错误、计费模式使用枚举或 sealed 类型，不使用裸协议字符串。
- ID、金额、时间、尺寸等高风险 String 应逐步迁移为值对象。
- 业务错误使用稳定错误码，由 Presentation 映射最终文案。
- 简单单仓库转发不强制创建 UseCase；跨仓库编排必须使用 UseCase/Interactor。

## 2. Data

每个远程功能保持：

```text
Api -> Dto -> Mapper -> RepositoryImpl -> Domain
```

要求：

- DTO 不得暴露到 Presentation。
- 服务端 `msg` 不直接作为最终 UI 文案。
- Mapper 必须有契约测试。
- 网络、序列化、业务错误分开映射。
- 不使用空 catch，不静默吞异常。
- 不使用裸 `println`。
- 远端兼容分支必须写清删除条件。
- 一个 Repository 实现覆盖过多聚合时，按模型、计费、生成、历史、项目等职责拆分。

---

# 六、网络与认证规范

## 1. Host 与凭据

- Authorization/Cookie 只允许发送到精确主机白名单。
- 日志不得包含 Token、Cookie、API Key、密码、验证码 Token。
- Refresh Client 与 Main Client 必须隔离。
- Token 刷新必须使用 Mutex 去重。
- 401 后最多重试原请求一次。
- 对非幂等 POST/上传请求，必须确认请求体可重放且不会造成重复副作用。
- Logout 与 refresh 竞态必须有测试。

## 2. 环境配置

不要长期只保留硬编码 Production 环境。建立：

```kotlin
data class ApiEnvironment(
    val webBaseUrl: String,
    val userCenterBaseUrl: String,
    val taskBaseUrl: String,
)
```

由平台启动层注入：

```text
debug -> dev/staging
release -> production
```

## 3. JSON

当前宽松 JSON 配置允许容错，但可能隐藏接口漂移。

建议：

- Debug/Contract Test 使用更严格配置。
- Release 可保留必要的 `ignoreUnknownKeys`。
- 对核心登录、计费、提交和历史 DTO 增加真实样本契约测试。
- 不使用正则解析 Token 刷新 JSON，改用 kotlinx.serialization DTO。

---

# 七、平台开发规范

## 1. commonMain

禁止：

```text
android.*
platform.UIKit.*
platform.Foundation.*
java.awt.*
Context
android.net.Uri
Application
UIViewController
NSURL
```

平台能力通过接口或 expect/actual 暴露。

## 2. Android/iOS 对称性

每新增一个平台能力必须检查：

```text
[ ] commonMain 端口
[ ] androidMain 实现
[ ] iosMain 实现
[ ] Android 测试
[ ] iOS 编译
[ ] iOS 运行验证或明确风险记录
```

禁止用“始终授权”“空函数”“直接成功”充当长期 iOS 实现。

## 3. iOS 最低验收矩阵

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme RunningHub \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build CODE_SIGNING_ALLOWED=NO
```

还应显式执行可用的 Native 测试任务：

```bash
./gradlew tasks --all
./gradlew :core:network:iosSimulatorArm64Test
./gradlew :feature:auth:domain:iosSimulatorArm64Test
./gradlew :feature:auth:data:iosSimulatorArm64Test
./gradlew :feature:quickcreate:presentation:iosSimulatorArm64Test
```

实际任务名以 Gradle 输出为准。

---

# 八、存储与安全规范

## 1. 敏感数据

下列内容不得继续存入普通 Preferences DataStore：

```text
access token
refresh token
Cookie
API Key
企业 API Key
```

目标实现：

```text
Android -> Keystore 支持的安全存储
iOS -> Keychain
```

余额快照、草稿、普通偏好继续保留在 DataStore。

## 2. Logout

Logout 只清理认证凭据和用户级缓存，不应无差别删除用户草稿，除非产品明确要求。

## 3. Release

正式发布前必须：

- 开启 R8/minify 和资源压缩。
- 验证反射、序列化、Koin、Coil、Ktor 的 keep 规则。
- 配置签名和密钥管理。
- 执行 Release 构建和安装回归。
- 扫描敏感日志和硬编码秘密。
- 检查 iOS Privacy Usage Description 和隐私清单。

---

# 九、测试规范

## 1. 分层测试

### Domain

- 业务规则；
- 错误语义；
- 状态机；
- 值对象边界。

### Data

- API 路径与请求体；
- DTO 反序列化；
- DTO-to-Domain Mapper；
- Token 失效重试；
- 空数据和兼容字段；
- 分页与错误映射。

### Presentation

- Action 到 State 的转换；
- 防抖；
- 旧响应隔离；
- 分页去重；
- 上传失败；
- 计费和提交一致性；
- 轮询终态；
- dispose 取消任务。

### Platform

- Android 权限、文件选择和存储；
- iOS PHPicker/WKWebView/Keychain；
- 双端媒体读取；
- 应用前后台和进程恢复。

## 2. PR 最低验证

普通 commonMain 变更：

```bash
./gradlew checkArchitectureBoundaries verifyL1Android
```

涉及 iOS/expect-actual/平台依赖：

```bash
./gradlew verifyL1Ios
```

涉及 UI：

- Android 截图或录屏；
- iOS 具备环境时同步截图；
- 说明未验证平台及风险。

## 3. CI 改进

现有 iOS CI 主要覆盖编译和 Framework Link，后续应增加：

- `iosSimulatorArm64Test`；
- Xcode build；
- 自动启动 Simulator；
- XCTest/XCUITest 冒烟；
- 构建和测试报告 artifact。

同时：

- 固定 macOS/Xcode 大版本，不长期依赖漂移的 `macos-latest`。
- 添加 concurrency，自动取消同分支旧运行。
- Branch Protection 强制 Android/iOS 检查通过。
- 每周运行一次完整双端夜间回归。

---

# 十、中文注释规范

- public/internal 业务类型使用中文 KDoc。
- UiState、Domain Model、DTO、Request、Response 必须逐字段解释。
- Boolean 说明 true/false 的业务语义。
- 可空字段说明 null 与空字符串/空集合的区别。
- 数值说明单位、范围和特殊值。
- 复杂流程解释“为什么”和约束，不复述语法。
- 注释与实现不一致视为缺陷。
- 不要求对明显的一行赋值写无价值注释。
- 临时兼容逻辑必须包含任务编号、风险和删除条件。

---

# 十一、Git 与 PR 规范

## Commit

```text
feat(scope): ...
fix(scope): ...
refactor(scope): ...
test(scope): ...
docs(scope): ...
chore(scope): ...
```

一个提交只做一个目的。

## PR 必须包含

```text
变更目标
影响模块
影响平台
架构边界说明
实际执行命令
测试结果
未验证项
截图或录屏
兼容性/数据迁移
安全影响
回滚方案
```

禁止把“未执行”写成“通过”。

---

# 十二、Definition of Done

功能只有满足以下条件才算完成：

```text
[ ] 代码位于正确模块
[ ] commonMain 没有平台类型
[ ] Domain 没有数据/平台依赖
[ ] Presentation 没有 Data 实现依赖
[ ] 没有新增 shared 业务代码
[ ] UiState 和业务模型注释完整
[ ] 错误语义稳定，不直接展示服务端原文
[ ] 并发、取消、超时和旧响应已处理
[ ] 相关单元测试通过
[ ] Android 构建/lint 通过
[ ] iOS 编译通过
[ ] 平台功能完成对应运行验证
[ ] 未泄露敏感信息
[ ] 文档与实际代码一致
[ ] CI 绑定当前提交 SHA
```

---

# 十三、长期维护路线

## 近期：1～2 个迭代

1. 修复可信 Host 判断。
2. 为当前 HEAD 重新生成 Android/iOS CI 证据。
3. 实现真实 iOS 权限和媒体选择。
4. 验证新 WKWebView 验证码流程。
5. 统一迁移文档结论，移除“已完成/待完成”矛盾。
6. 删除或隔离旧 Create 页面，避免 AI 和开发者误改。
7. 给 iOS CI 增加 Native Tests。

## 中期：1～2 个月

1. Android Keystore + iOS Keychain。
2. Staging/Production 环境注入。
3. 开启 Android Release minify。
4. 删除历史 `androidApp`。
5. 继续把 Audio/ModelCatalog/ModelInvocation 从 shared 迁出。
6. 将根 `build.gradle.kts` 中超过千行的验收逻辑迁入：
   - `build-logic` 自定义插件；
   - 或独立 verification 脚本。
7. 将迁移证据归档，不再让正常构建长期解析迁移历史。

## 长期：持续治理

1. 防止 `composeApp` 变成新的 UI 单体。
2. Feature 达到规模阈值后建立独立 Presentation 模块。
3. 建立结构化日志、脱敏和崩溃监控。
4. 建立 API 契约测试与 SQLDelight migration 测试。
5. UI 文案迁移到 Compose Resources，清理乱码和硬编码。
6. 使用 Dependabot/Renovate 定期升级依赖，重大版本必须人工回归。
7. 维护 Android/iOS 性能基线：启动、内存、图片缓存、视频和长轮询。
8. 建立 App Store/TestFlight 与 Android Release 自动化流水线。

---

# 十四、AI 协作防上下文失控规则

迁移完成后，不应让 AI 每次加载全部迁移历史。

建议文档结构：

```text
AGENTS.md                    # 只保留 150～250 行强规则
ARCHITECTURE.md              # 当前架构，不写迁移过程
DEVELOPMENT.md               # 日常开发命令和 DoD
docs/adr/                    # 长期有效的架构决策
docs/archive/migration-2026/ # 已完成迁移的历史证据
```

每次 AI 任务必须提供：

```text
任务 ID
唯一目标
允许修改目录
禁止修改目录
验收命令
最大重试次数
完成后更新的文档
```

不要让 AI 自动重新规划已封板的架构；只有明确回归证据才能修改既有边界。
