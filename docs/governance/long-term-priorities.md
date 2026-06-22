# Long-term Governance Priorities

本文件把复核建议收敛为 10 条可执行长期优先级。每一项都区分本地可完成内容和不可本地完成内容。

| 优先级 | 主题 | 本地落地内容 | 完成口径 | 不可本地完成 |
|---|---|---|---|---|
| P1 | 上下文入口收敛 | 将 `AGENTS.md` 收敛为 150-250 行强规则入口，维护 `ARCHITECTURE.md`、`DEVELOPMENT.md`、ADR、`docs/governance/ai-task-template.md` 和 `docs/governance/chinese-commenting.md`，并拦截生产 Kotlin/KTS 乱码注释回流 | 新任务无需读取完整迁移历史即可理解当前边界；AI 任务有固定边界和验收字段；详细中文注释规则可按需单独读取；生产源码出现常见 mojibake 片段会失败 | 无 |
| P2 | 构建逻辑收口 | 使用 `build-logic` 承载 `checkLongTermGovernance`，并用 `build-script-baseline.txt` 锁定根构建脚本行数 | 根工程不再继续堆叠长期治理文本检查；旧 L1 Gate 迁出顺序有明确记录 | 完整迁出所有旧 Gate 任务需单独重构 |
| P3 | 迁移证据归档 | 建立 `docs/archive/migration-2026/` 归档边界 | L1 封板后有明确移动条件 | 当前 HEAD 外部证据未完成前不能移动 |
| P4 | `composeApp` 单体控制 | 用 allowlist 阻止新增超大 commonMain Kotlin 文件，并防止已退役旧 Create 源目录重新承载实现；QuickCreate 应用壳只能保留 Voyager 门面、Compose UI 叶子组件和资源映射；History 迁移期兼容桥只能保留在唯一隔离文件中 | 新增超阈值文件会被 `checkLongTermGovernance` 拦截；旧 Create 页面或状态机 Kotlin 源文件回流会失败；QuickCreate 仓库聚合、状态所有权、Coordinator/Interactor/StateHolder 装配回流到 composeApp 会失败；QuickCreate 历史模型映射不得在 composeApp 扩散，Task Data 未正式绑定 `GenerationHistoryRepository` 前不得误删桥 | 既有历史大文件拆分需分 Feature 迭代 |
| P5 | Feature Presentation 阈值 | `checkLongTermGovernance` 按 `feature-presentation-thresholds.txt` 约束无同名 Presentation 模块的大体量 UI Feature，并校验真实 ownerPresentation 模块存在 | 超过基线继续增长会失败；baseline 缺少 `feature:<owner>:presentation` 或 owner 模块不存在会失败；除非拆分同名 Presentation 模块或更新治理说明 | 具体模块拆分需产品迭代配合 |
| P6 | 日志、凭据、环境与平台安全 | `checkLongTermGovernance` 扫描敏感日志、自定义 debug 调用、认证 Host 白名单、认证 JSON/JWT 解析、401 后请求重放边界、Android/iOS 环境覆盖入口、Android Keystore/iOS Keychain 凭据存储、已迁移 Data 错误消息收口、iOS 权限/媒体选择和短信验证码 Web 容器防退化 | 明显敏感日志会失败；认证头不得发送到相似域名；token 刷新响应和 JWT payload 必须按 JSON 语义解析，不得用正则截取敏感协议字段；401 刷新后默认只重放安全方法，POST/上传/任务提交必须显式标记可重放；debug/release 可在平台启动层切换环境；`CredentialStore` 不得退回普通 Preferences；已迁移 Feature Data 不得把服务端 `msg/message` 直接作为异常消息或任务状态错误；iOS 权限轨迹通过 NSUserDefaults 持久化，且不得退回固定授权或空选择器；验证码 bridge/handler 清理不得删除 | 接入第三方监控、真实 staging/dev 地址、Simulator/真机上传回归需外部环境 |
| P7 | 契约测试 | `checkLongTermGovernance` 校验 API/数据库契约测试基线 | 新增 DTO、Request、Response 必须登记测试入口；新增 SQLDelight schema 必须登记真实数据库测试入口，不得继续使用 `legacy-debt` | 依赖服务端真实契约变更确认 |
| P8 | UI 文案资源化 | `checkLongTermGovernance` 校验 composeApp 与 Feature Presentation 硬编码 UI 文案基线，并防止已清理的 Domain 兼容模型、QuickCreate 任务状态区或 History 标题启发式重新承载展示文案 | 新增硬编码文案文件或超过基线会失败；减少基线可分批推进 Compose Resources；旧 QuickCreate Domain 模型不得重新定义展示名或说明文案字段，任务状态区不得恢复原文透传通道，History Compose 页面不得按任务标题推断费用或输出数量 | 全量清理旧硬编码文案需分批 |
| P9 | 性能基线 | `checkLongTermGovernance` 校验性能指标目标表和证据锚点 | 启动、首屏、内存、图片、视频和长轮询指标都有平台、证据、责任域，且证据路径可追溯 | 真机/iOS 性能数据需设备或 CI 支持 |
| P10 | 发布与依赖自动化 | `checkLongTermGovernance` 校验发布就绪清单、Android release 构建配置、ProGuard 规则、iOS `PrivacyInfo.xcprivacy`、Dependabot、Dependency Submission 和 PR 模板 | Android Release、iOS TestFlight、release 构建压缩、iOS 隐私清单、依赖升级巡检、重大版本升级人工回归和人工确认停止条件保留在仓库 | 商店上传、签名、生产发布和重大版本升级回归必须人工授权 |

## 本地门禁映射

本节把 10 条长期优先级绑定到仓库内可重复执行的检查入口。后续协作者新增、删除或调整治理规则时，
必须同步更新本表和 `checkLongTermGovernance`，避免文档说法与实际门禁脱节。

| 优先级 | 主要文件 | 本地验证入口 |
|---|---|---|
| P1 | `AGENTS.md`、`ARCHITECTURE.md`、`DEVELOPMENT.md`、`docs/governance/ai-task-template.md`、`docs/governance/chinese-commenting.md` | `checkContextEntryGuard`、`checkProductionTodoGuard`、`checkMojibakeTextGuard`、`requireDocumentSnippets` |
| P2 | `build-logic/src/main/kotlin/com/runninghub/buildlogic/LongTermGovernancePlugin.kt`、根 `build.gradle.kts`、`docs/governance/build-logic-migration.md`、`docs/governance/build-script-baseline.txt` | `checkLongTermGovernance`、`checkRootBuildScriptSize` |
| P3 | `docs/archive/migration-2026/README.md`、`docs/migration/acceptance.md`、`docs/migration/shared-ownership.md` | `requireDocumentSnippets`、`checkMigrationScripts`、`checkLegacyAndroidAppGuard` |
| P4 | `docs/governance/composeapp-file-size-allowlist.txt`、`composeApp/src/commonMain/kotlin/` | `checkComposeAppFileSize`、`checkRetiredCreateGuard`、`checkHistoryCompatibilityBridgeGuard`、`checkQuickCreateLegacyImplementationGuard`、`checkQuickCreateScreenModelFacadeGuard` |
| P5 | `docs/governance/feature-presentation-thresholds.txt`、`feature/*/presentation` | `checkFeaturePresentationThresholds`、`feature|maxLines|ownerPresentation|reason` |
| P6 | `core/network`、`core/storage`、`composeApp/src/androidMain`、`composeApp/src/iosMain`、`feature/*/data` | `checkTrustedAuthHostGuard`、`checkAuthReplayGuard`、`checkAuthJsonParsingGuard`、`checkRuntimeEnvironmentOverrideGuard`、`checkSecureCredentialStorageGuard`、`checkMigratedDataErrorMessageGuard`、`checkIosPermissionAndMediaGuard`、`checkSmsCaptchaGuard`、`checkSensitiveLogging`、`checkMediaUploadErrorPrivacyGuard` |
| P7 | `docs/governance/api-contract-test-baseline.txt`、`docs/governance/database-contract-test-baseline.txt` | `checkApiContractBaseline`、`checkDatabaseContractBaseline`、`checkCoreAuthContractSamples` |
| P8 | `docs/governance/ui-copy-hardcoded-baseline.txt`、`docs/governance/ui-copy-resources.md`、`feature/quickcreate/domain/src/commonMain/kotlin/com/runninghub/feature/quickcreate/domain/QuickCreationModels.kt`、`feature/quickcreate/presentation/src/commonMain/kotlin/com/runninghub/feature/quickcreate/presentation/result/QuickCreateTaskStatusUi.kt`、`feature/task/presentation/src/commonMain/kotlin/com/runninghub/feature/task/presentation/TaskHistoryStateHolder.kt` | `checkUiCopyBaseline`、`checkQuickCreateDomainModelTextGuard`、`checkQuickCreateTaskStatusTextGuard`、`checkTaskHistoryPresentationTextGuard` |
| P9 | `docs/governance/performance-baseline-targets.txt`、`docs/governance/performance-baselines.md` | `checkPerformanceBaselineTargets` |
| P10 | `.github/dependabot.yml`、`.github/workflows/dependency-submission.yml`、`.github/pull_request_template.md`、`composeApp/build.gradle.kts`、`iosApp/iosApp/PrivacyInfo.xcprivacy`、`docs/governance/release-readiness-checklist.md` | `checkAndroidReleaseBuildGuard`、`checkReleaseReadinessChecklist`、`checkDependencyMaintenanceGuard` |
