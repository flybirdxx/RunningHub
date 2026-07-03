<!-- .codex-version: v1.0.0 (2026.06.27) -->
# AGENTS.md

本文件是 RunningHub 仓库的 AI 协作入口。所有任务先读本文件，再按需读取 `.codex/rules/project_rule.md`、局部 `AGENTS.md`、`ARCHITECTURE.md`、`DEVELOPMENT.md` 与相关源码。

## 1. 项目身份

- 项目名称：RunningHub。
- 项目类型：Kotlin Multiplatform + Compose Multiplatform 应用，Android 为主交付端，iOS 由 `iosApp` 包装 Compose framework。
- 根包名：`com.runninghub`；Android applicationId：`com.runninghub.app`。
- 构建系统：Gradle Kotlin DSL，启用 `TYPESAFE_PROJECT_ACCESSORS` 与 `build-logic` convention plugins。
- 模块数量：25 个 Gradle 模块，见 `.codex/references/dependencies.md`。
- CodeGraph：已安装并完成索引；优先用 `codegraph explore/query/node/status` 定位符号，再读取必要文件。

## 2. 沟通与协作

- 默认使用中文沟通，除非用户明确要求其他语言。
- 先确认目标、边界、成功标准和当前工作区状态，再修改系统行为。
- 用户中途补充的新指令优先于旧方向；如果冲突，立即按最新要求收束。
- 工作区可能已有用户改动，禁止回滚、覆盖或删除非本任务改动。
- 重要项目事实写入仓库文档或 `.codex/references/`，不要只留在聊天记录里。
- 非平凡任务必须给出 verifier；没有验证证据时，只能说明完成范围和剩余风险。

## 3. 任务启动流程

- 运行或查看 `git status --short`，区分用户已有改动和本次改动。
- 阅读最近的局部规则：从目标文件向上查找 `AGENTS.md`。
- 需要架构背景时读 `ARCHITECTURE.md`、`DEVELOPMENT.md` 和 `.codex/rules/project_rule.md`。
- 需要模块定位时先查 `.codex/references/dependencies.md` 与 `.codex/references/{module}.md`。
- 需要符号级上下文时优先使用 CodeGraph，例如 `codegraph explore "QuickCreateCoordinator"`。
- 目标涉及长期治理、迁移状态或外部证据时，读取 `docs/governance/long-term-priorities.md`、`docs/governance/ai-task-template.md`、`docs/governance/chinese-commenting.md` 和 `docs/migration/`。

## 4. 架构边界

- `composeApp/commonMain` 只能依赖 Core、Feature Domain 和 Feature Presentation 入口。
- `composeApp/androidMain` 与 `composeApp/iosMain` 负责装配运行期 Data 模块、平台 HTTP 引擎、存储和权限实现。
- Feature Domain 只放稳定业务模型和 Repository 接口，不导入 Data、Ktor、Koin、DataStore、Compose 或平台 SDK。
- Feature Data 实现 Domain 接口，可依赖 `core:network`、`core:storage` 和必要的其他 Domain 契约。
- Feature Presentation 依赖自身 Domain 和跨 feature 的 Domain 契约，不直接依赖 Data 实现。
- Core 模块不得依赖 Feature 或 `composeApp`。
- `shared` 模块已退役；不得重新 include、声明 `projects.shared` / `project(":shared")` 或导入 `com.runninghub.shared.*`。
- 新增跨模块能力优先通过 Domain 接口、Koin 绑定和平台组合根装配，不引入全局单例旁路。

## 5. UI 与状态

- UI 使用 Compose Multiplatform，导航使用 Voyager，依赖注入使用 Koin。
- `composeApp` 中的 `ScreenModel` 应保持应用壳职责，复杂页面状态下沉到对应 Feature Presentation 的 `StateHolder`、`Coordinator` 或 Interactor。
- QuickCreate 的页面级状态所有权属于 `feature:quickcreate:presentation`；`QuickCreateScreenModel` 只做 Voyager facade。
- UI 文案优先进入 Compose Resources；业务层不得保存最终展示文案。
- 公共 UI 组件放在 `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/component` 或主题目录，避免复制粘贴到页面文件。
- UI 修改必须验证真实渲染或至少运行相关 compose/viewmodel 单元测试；构建通过不等于体验正确。

## 6. 网络、数据与凭据

- Ktor API 类只维护远端路径和 DTO 边界，Repository 负责 DTO-to-Domain 映射和错误归一化。
- Token、Cookie、Authorization、API Key、请求体和验证码 `validToken` 不得进入日志、截图说明、异常展示或提交内容。
- 敏感凭据必须通过 Android Keystore / iOS Keychain 边界；Preferences/DataStore 只允许作为非敏感配置或迁移源。
- `RunningHubApiEnvironment` 必须由平台启动层配置，不得在 Data 层写死生产地址。
- 刷新 token 响应必须用 `kotlinx.serialization` DTO 解析，不使用正则提取敏感字段。
- 上传、生成、轮询、取消等流程必须有可取消的结构化协程边界。

## 7. 禁止模式

- 禁止在生产源码使用 `runBlocking`、`GlobalScope`、空 `catch` 或阻塞式 `Thread.sleep`。
- 禁止在 `commonMain` 导入 Android/iOS 平台 API；平台能力必须通过 expect/actual 或平台 source set 注入。
- 禁止在 Domain/Presentation 依赖 Feature Data 模块或导入 Data 实现包。
- 禁止在 `composeApp/commonMain` 直接依赖 Data 模块。
- 禁止恢复旧 `CreateVoyagerScreen` / `CreateScreenModel` 创作入口。
- 禁止把远端原始错误、服务端 message 或内部诊断文本直接展示给用户。
- 禁止在发布配置关闭 R8、资源压缩或移除 `proguard-rules.pro`。
- 禁止把 `build/`、`.gradle/`、`.kotlin/`、`local.properties`、keystore、APK/AAB 等产物或敏感文件加入 Git。
- 禁止在没有证据的情况下声称 L1 封板、iOS 真机构建、远端 CI 或登录态运行观察已完成。

## 8. 构建与验证命令

- Windows 本地优先使用 `.\gradlew.bat --console=plain <task>`。
- 架构边界快速检查：`.\gradlew.bat --console=plain checkArchitectureBoundaries`。
- 长期治理检查：`.\gradlew.bat --console=plain checkLongTermGovernance`。
- Android 侧聚合验证：`.\gradlew.bat --console=plain verifyL1Android`。
- iOS 侧聚合验证需要 macOS runner 或 macOS 开发机：`./gradlew --console=plain verifyL1Ios`。
- 本地总入口：`.\gradlew.bat --console=plain verifyL1Local`，但 Windows 上 iOS link 结果不能替代 macOS 证据。
- 快速定位任务：`.\gradlew.bat --console=plain tasks --all` 或 `.\gradlew.bat --console=plain help --task <task>`。
- 文档-only 初始化变更可用占位符扫描、文档数量检查和 CodeGraph 状态作为 verifier。

## 9. 测试策略

- 小范围代码变更至少运行相关模块测试或目标类测试。
- 架构边界、依赖、source set 或 DI 装配变更必须运行 `checkArchitectureBoundaries`。
- 影响 QuickCreate、History、Auth、Task、Model 等用户流程时，优先运行对应 feature 的 commonTest。
- Data/DTO/API 路径变更必须补或更新契约测试，尤其是 Ktor MockEngine 路径和请求体断言。
- UI 变更需要截图、运行观察或可复现的交互检查；仅运行 Gradle 不足以证明体验正确。
- 外部 CI、商店上传、生产签名、真机权限矩阵等高风险动作只能作为人工确认项，不自动执行。

## 10. 文档与记忆

- 当前态规则写入 `AGENTS.md`、`.codex/rules/`、`ARCHITECTURE.md` 或 `DEVELOPMENT.md`。
- 模块事实写入 `.codex/references/{module}.md`。
- 迁移历史、封板证据和外部状态写入 `docs/migration/`。
- 长期治理和任务模板写入 `docs/governance/`，尤其是 `docs/governance/long-term-priorities.md` 与 `docs/governance/ai-task-template.md`。
- 注释和中文文案规范以 `docs/governance/chinese-commenting.md` 为准。
- 没有实质新信息时不要制造文档噪音。

## 11. CodeGraph 使用

- 项目结构探索优先 `codegraph status`、`codegraph files`、`codegraph query <symbol>`。
- 需要调用关系或源码上下文时使用 `codegraph explore "<query>"`。
- 单个符号或文件深读使用 `codegraph node "<symbol-or-path>"`。
- CodeGraph 输出是当前磁盘源码快照；若涉及最近编辑，必要时再读文件确认。
- `.codex/references/dependencies.md` 为模块依赖与清单索引；细节以 CodeGraph 和模块文档补充（历史 `_scan.json` 轻量索引可选，缺失时以上述来源为准）。

## 12. 文件编辑规则

- 小补丁聚焦解决当前目标，不顺手重构无关模块。
- 先读上游调用点和下游使用点，再改共享接口、数据结构或状态模型。
- 生成或更新规则文档时必须去掉模板占位符，尤其是双大括号形式的变量。
- 脚本和 hooks 中不得保留项目模板变量。
- 遇到既有脏文件，先判断是否与任务有关；无关则忽略，有关则在现有改动上继续，不回滚。
- 不使用破坏性 Git 操作，除非用户明确要求。

## 13. 交付口径

- 完成后说明改了什么、验证了什么、未验证什么和剩余风险。
- 如果工作区有无关改动，交付时说明未触碰。
- 如果验证失败，给出失败命令、关键错误和下一步可行路径。
- 文档-only 改动可不跑完整 Gradle，但必须说明原因并执行轻量 verifier。
- 任何声称“初始化完成”的结论必须同时满足 `.codex/rules`、`.codex/skills`、`.codex/agents`、`.codex/references` 和 hooks 无占位符残留。

## 14. 常用参考入口

- `.codex/rules/project_rule.md`：AI 主规则和当前架构约束。
- `.codex/rules/conflict_resolution.md`：规则冲突裁决顺序。
- `.codex/skills/plan_mode/SKILL.md`：规划任务模板。
- `.codex/skills/code_review/SKILL.md`：代码审查清单。
- `.codex/skills/performance_check/SKILL.md`：性能与安全检查。
- `.codex/agents/arch-review.md`：架构审查 agent 规则。
- `.codex/agents/resource-sync.md`：资源同步 agent 规则。
- `.codex/agents/proactive-correction.md`：主动纠错 agent 规则。
- `.codex/references/dependencies.md`：模块依赖图。
- `.codex/references/conventions.md`：编码和命名约定。

## 15. 常见改动路线

- Auth 登录、短信验证码、用户资料：先看 `feature/auth/domain`，再看 `feature/auth/data` 和 `feature/auth/presentation`。
- Discovery/Search/WebApp 列表：先看 `feature/discovery/domain`，再看 Data mapper 和 Presentation state holder。
- Plaza/Community：先看 `feature/community/domain`、`PlazaRepositoryImpl` 和 `PlazaStateHolder`。
- App detail 与任务运行：先看 `feature/detail/presentation` 和 `feature/task/domain|data`。
- History：先看 `feature/task/presentation/TaskHistoryStateHolder.kt`，应用壳只做资源文案映射和 Compose 展示。
- QuickCreate：先看 `feature/quickcreate/domain` 和 `feature/quickcreate/presentation`，再看 `feature/quickcreate/data`。
- 标准模型目录与调用：先看 `feature/model/domain` 和 `feature/model/data`。
- 音频生成：先看 `feature/audio/domain` 和 `feature/audio/data`。
- 网络环境或认证：先看 `core/network` 与平台 runtime module。
- 本地存储或权限状态：先看 `core/storage` 与 `composeApp/platform`。

## 16. 变更分层检查

- 只改 UI 布局：确认没有把业务状态拉回 `composeApp`。
- 只改 Presentation：确认没有新增 Data import 或远端 DTO import。
- 只改 Data：确认没有暴露 DTO 给 Domain/Presentation。
- 只改 Domain：确认模型仍平台无关，且 Data mapper 同步更新。
- 改 source set：确认 commonMain 没有平台 API。
- 改 build file：确认依赖方向没有反转，运行架构边界检查。
- 改发布配置：确认 release minify、shrink 和 ProGuard 仍在。
- 改资源文案：确认 Compose Resources、Android res、iOS 隐私说明按需同步。
- 改脚本或 CI：确认命令可在目标平台执行，不能用本地成功替代远端证据。
- 改 `.codex` 规则：确认占位符扫描和模块文档数量检查通过。

## 17. 验证记录

- 最终回复必须写明实际运行的命令。
- 命令失败时必须写明 exit code、关键错误和下一步。
- Windows 本地无法证明 macOS iOS link 通过，只能记录未验证。
- 没有真实设备时，不声称权限、媒体选择器、WebView/WKWebView 或长轮询运行观察通过。
- 文档-only 改动不需要跑完整 Gradle，但需要说明原因。
- 初始化或规则变更至少运行 CodeGraph 状态和 references 一致性检查。
- 如果用户要求继续实现业务代码，再按业务风险补 Gradle/截图/运行验证。

## 18. 提交前自查

- `git diff --name-only` 中没有意外业务文件。
- 根入口和 `.codex` 文件不含脚手架占位内容。
- 新增文档没有“待补充”作为事实占位。
- references 模块文档覆盖 `settings.gradle.kts` 声明的全部模块。
- hooks 命令适配当前平台；Windows 使用 PowerShell hook，类 Unix 可使用 `.sh`。
- 未触碰用户已有业务改动，除非当前任务明确要求。
- 未把构建产物、临时输出或敏感文件加入交付范围。
- 最终说明清楚哪些验证未运行以及为什么。
- 需要后续协作者接手时，明确留下当前决策、阻塞点和下一步命令。
