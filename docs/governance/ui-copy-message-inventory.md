# UI Copy and Message Inventory

> 盘点日期：2026-06-22  
> 当前口径：中期治理阶段，先全量盘点并按语义批量收口；不再按单个页面零散修补。

## 范围

本清单覆盖生产代码中可能进入用户界面的文案来源：

- `composeApp/src/commonMain/kotlin` 中的 `Text(...)`、`text = ...`、`contentDescription`、`placeholder`、按钮和空态文案。
- `feature/*/presentation/src/commonMain/kotlin` 中保存最终中文提示、错误、fallback 或 `Throwable.message` 派生展示内容的状态和文案端口。
- `core`、`feature/*/domain`、`feature/*/data` 中可能被上层透传到 UI 的异常 message、权限描述、服务端 `errorMessage` 或运行时标题。

以下内容不按固定 UI 文案处理，但必须在盘点中说明边界：

- Compose Resource key、资源导入和 `stringResource(...)` 调用。
- 日志、测试 fixture、Preview mock、协议字段、DTO 字段名、分隔符、颜色、尺寸和动画 label。
- 服务端运行时返回的模型名、模板名、字段标题、字段选项、项目名、任务标题和用户输入。
- Domain/Data 中仅保存服务端原始 `msg/message/errorMessage` 的运行时数据，前提是 Presentation 不直接展示原文。

## 当前治理基线

`docs/governance/ui-copy-hardcoded-baseline.txt` 当前没有正数匹配文件：

| 文件 | 匹配数 | 分类 |
|---|---:|---|
| 无 | 0 | 新增用户可见中文硬编码会作为未登记新增文件失败。 |

该基线只能防止匹配数增长，不能替代人工分类。后续修改必须先更新本清单，再按同一语义批量覆盖调用点、测试和基线。

## 已完成批次

### 2026-06-30 RM-14 状态文案矩阵

已把产品重设 RM-01 到 RM-13 新增的关键状态收口到 `RedesignStateCopyMatrix`。
矩阵只保存稳定状态 ID、Compose Resource key、承载面类型和是否阻塞流程，不保存最终中文字符串。
资源文案集中放在 `composeApp/src/commonMain/composeResources/values/strings.xml` 的 `rm14_state_*`
条目中，后续页面映射可按状态 ID 复用。

同步完成：

- `RedesignStateCopyId` 覆盖首次进入、首页加载、搜索空态、模型加载失败、模型价格失败、上传状态、Prompt/参数阻塞、余额/会员、生成确认和生成状态、复制、复用、保存、过期、历史空态、网络错误、钱包明细、扣费和退款状态。
- `RedesignStateCopySurface` 固定 `ScreenEmpty`、`InlineNotice`、`Snackbar`、`Dialog`、`BottomSheet`、`Toast` 和 `CardAction` 七类承载面。
- `RedesignStateCopyMatrixContractTest` 锁住完整状态集合、资源键存在和关键承载面边界：价格确认与复用参数为 Bottom Sheet，复制反馈为 Snackbar，历史/钱包空态为 ScreenEmpty，失败退款提示为 Inline Notice。
- `ui-copy-hardcoded-baseline.txt` 继续保持无正数基线；本批没有把最终中文文案写入 Kotlin 状态字段、Domain 或 Data。

### 2026-06-22 History Presentation 错误/提示语义

已把 `TaskHistoryUiState.actionMessage/error` 从最终中文 `String?` 改为
`TaskHistoryActionMessage?` 和 `TaskHistoryPresentationError?` 稳定语义；
`TaskHistoryFilter.label` 已删除，筛选标签继续由 composeApp Compose Resources 映射。
`TaskHistoryScreen` 不再通过 `String.contains("TOKEN"/"401")` 判断鉴权错误，而是直接读取
`TaskHistoryPresentationError.AuthRequired`。

同步完成：

- `TaskHistoryStateHolderTest` 改为断言稳定语义，并补充无复用参数、无重试参数、详情失败和鉴权失败分支。
- `composeApp` 增加 History 操作提示和错误资源。
- `ui-copy-hardcoded-baseline.txt` 移除 `TaskHistoryStateHolder.kt` 的 10 个硬编码 UI 文案基线。
- `feature-presentation-thresholds.txt` 将 History UI 壳基线同步到 1142 行；增长仅来自资源映射，状态机职责未回流。

### 2026-06-22 QuickCreate 生成阻塞与上传错误语义

已把 `QuickCreateRuntimeUiText` 从最终中文文案端口改为稳定语义类型，并把
`QuickCreateUiState.error`、`QuickCreateUiState.feePreviewError` 和
`MediaReference.errorMessage` 改为 `QuickCreateUiMessage?`。本批新增的生成阻塞、计费待确认、
重复生成、Prompt 校验和素材上传失败/阻塞/超时提示由 composeApp Compose Resources 映射；
当时尚未进入本批的服务字段校验和错误码端口已在后续批次继续收口。

同步完成：

- `QuickCreateGenerationInteractor` 不再把生成阻塞写成中文 `String`。
- `QuickCreateMediaUploadCoordinator.awaitPendingUploads` 不再通过 `IllegalStateException.message` 传递 UI 文案，改用 `QuickCreateMediaUploadException.uiMessage`。
- `QuickCreateRuntimeUiTextTest`、生成、计费、上传相关测试改为断言稳定语义或 legacy 包装。
- `composeApp` 增加 QuickCreate 运行时提示资源，并通过 `QuickCreateUiMessageText` 集中映射。
- `ui-copy-hardcoded-baseline.txt` 移除 `QuickCreateRuntimeUiText.kt` 的 8 个硬编码 UI 文案基线。

### 2026-06-22 QuickCreate 服务字段校验语义化

已把 `QuickCreationServiceSchema.validateFields/validateUploads` 从最终中文 `String?`
改为 `QuickCreationServiceValidationIssue?`。服务端字段标题继续作为运行时数据携带，
必填、选项无效、最小字符数和最大文件数由稳定 issue 表达；`QuickCreateGenerationRequestFactory`
将其包装为 `QuickCreateGenerationBlockReason.ServiceValidation`，`QuickCreateGenerationInteractor`
再映射到 `QuickCreateRuntimeUiText`，最终文案由 composeApp 的
`quick_create_runtime_service_*` Compose Resources 格式串生成。

同步完成：

- `QuickCreationServiceSchemaTest` 改为断言稳定校验 issue，不再断言 Domain 拼接中文句子。
- `QuickCreateGenerationRequestFactoryTest` 覆盖服务字段校验 issue 穿过生成请求构建器。
- `QuickCreateUiMessageText` 增加服务字段校验资源映射；服务端字段标题仍作为运行时参数插入格式串。
- `feature/quickcreate/domain` 与相关 Presentation 目标文件静态扫描确认无中文字符串字面量残留。

### 2026-06-22 QuickCreate 错误码端口语义化

已把 `QuickCreateErrorMessages` 从最终中文文案端口改为 `QuickCreatePresentationError`
稳定错误语义。历史、项目、灵感、计费预览和任务轮询失败不再写入
`QuickCreateUiMessage.LegacyText`，而是写入 `QuickCreateUiMessage.PresentationErrorText`；
composeApp 通过 `quick_create_error_*` Compose Resources 映射最终文案。任务失败状态区也从
`QuickCreateTaskStatusText.Custom("...")` 改为 `QuickCreateTaskStatusText.Error(error)`。

同步完成：

- `QuickCreateErrorMessagesTest` 改为断言错误码到稳定语义，以及远端摘要不会透传。
- History、Project、Inspiration、Billing 和 TaskPolling 相关测试改为断言稳定错误语义。
- `composeApp` 增加 QuickCreate 错误资源，并复用 `QuickCreatePresentationError.asQuickCreateErrorText()`。
- `ui-copy-hardcoded-baseline.txt` 移除 `QuickCreateErrorMessages.kt` 的 28 个硬编码 UI 文案基线。
- `LongTermGovernancePlugin` 不再要求 baseline 文档保留已完成治理的旧文件名。

### 2026-06-22 QuickCreate 本地兼容模型名语义化

已把 `QuickCreateEditorUiModels` 中迁移期本地兼容图片/视频模型名从最终中文
`displayName` 改为 `ImageModelLabel` 和 `VideoModelLabel` 稳定语义；composeApp 通过
`quick_create_image_model_*` 与 `quick_create_video_model_*` Compose Resources 映射最终文案。
英文第三方商品名继续作为运行时文本展示，不进入中文 UI 文案资源化基线。

同步完成：

- `QuickCreateEditorUiModelsTest` 覆盖本地图片/视频模型 label 语义。
- `QuickCreateCompactComposerContent` 和 `QuickCreateParamsSheetContent` 不再读取本地模型中文 `displayName`。
- `ui-copy-hardcoded-baseline.txt` 移除最后 5 个正数匹配；当前 baseline 无正数文件。
- `LongTermGovernancePlugin` 的文档锚点改为要求 baseline 显式声明当前无正数基线。

### 2026-06-22 composeApp 静态 UI 文案资源化

已把当前清单中列出的通用搜索框、QuickCreate 顶栏、媒体卡和上传字段固定文案迁入
composeApp Compose Resources。该批次只处理纯 UI 静态文案和格式串，不触碰 Presentation 错误语义、
运行时服务端内容或上传状态机。

同步完成：

- `AppSearchBar` 默认占位、搜索和清除无障碍描述改为 `app_search_bar_*` 资源。
- `QuickCreateScreen` 顶栏返回、客服、静音无障碍描述改为 `quick_create_top_bar_*` 资源。
- `MediaChipCard` 的素材类型、上传状态、失败 fallback 和移除无障碍描述改为 `quick_create_media_*` 资源。
- `QuickCreateMediaToolbarRow` 与 `QuickCreateServiceUploadFieldPicker` 的素材入口、数量格式、上传计数和默认提示改为 `quick_create_upload_*` 资源。
- 目标文件静态扫描确认没有中文字符串字面量残留。

### 2026-06-22 Plaza fallback 内容目录语义化

已把 `PlazaFallbackCatalog` 的本地降级标签、卡片简介、作者和媒体类型从最终展示文本改为稳定语义：
`PlazaUiState.fallbackTagLabels` 标记内置标签文案，`PlazaUiState.fallbackCreationTexts` 标记内置卡片文案。
服务端返回的标签名、卡片简介、作者和媒体类型继续作为运行时内容展示，不纳入静态 UI 文案资源化。

同步完成：

- `PlazaStateHolder` 的本地 fallback 目录不再保存最终展示文案，只保存内容 ID、排序指标和资源化语义。
- `PlazaScreen` 对 fallback 语义使用 Compose Resources 映射最终文案，对远端内容继续读取运行时字段。
- `PlazaStateHolderTest` 覆盖成功加载时 fallback 语义为空，以及失败降级时写入稳定语义。
- `ui-copy-hardcoded-baseline.txt` 保持无正数基线；Plaza Presentation 目标文件静态扫描确认无中文字符串字面量残留。

### 2026-06-22 Core/Data 文案载体语义化

已把 Core 权限说明和 Auth/Task/Model Data API Key 缺失错误从最终中文文案或异常 message 语义改为稳定语义。
权限说明使用 `PermissionTextKey`，API Key 缺失使用 `MissingCredentialException(MissingCredential.ApiKey)`；
最终用户可见文案由 composeApp 或 Presentation 层后续按语义映射。

同步完成：

- `core:storage` 的 `Permission` 不再保存 `description`/`requiredFor` 中文文案，改为 `descriptionKey`/`requiredForKey`。
- `PermissionBottomSheet` 使用 `permission_*` Compose Resources 映射权限说明，按钮和设置引导仍复用原资源。
- `feature:auth:data`、`feature:task:data` 与 `feature:model:data` 的 API Key 缺失不再通过中文 `IllegalStateException` 或功能内异常 message 表达，统一使用 `core:common` 稳定凭据异常。
- `feature:auth:data` 的登录响应缺 data、缺 access token、刷新失败和登录后用户资料缺 data 改为 `AuthError.EmptyLoginResponse`、`AuthError.MissingAccessToken`、`AuthError.TokenRefreshFailed` 和 `AuthError.EmptyUserResponse`；账户状态、用户资料、公开详情和关注接口失败改为 `UserRepositoryException(UserRepositoryIssue.*)`，不再依赖英文异常 message 表达 Auth Data 结构错误。
- `feature:task:data` 的 WebApp 任务示例、提交、输出、上传和历史接口失败改为 `WebAppTaskException(WebAppTaskIssue.*)`，不再通过 `TASK_*_FAILED_CODE_*` 或 `Empty response data` 异常 message 表达上层语义。
- `feature:community:data` 的 Plaza 标签、创作分页、短片分类和短片分页接口失败改为 `PlazaRepositoryException(PlazaRepositoryIssue.*)`，不再通过 `PLAZA_*_FAILED_CODE_*` 异常 message 表达上层语义。
- `feature:model:data` 的媒体上传响应缺少远端 URL 时返回 `ModelInvocationException(ModelInvocationIssue.MediaUploadEmptyUrl)`，目录接口失败、详情缺 data 和 endpoint 回源缺路由返回 `ModelCatalogException(ModelCatalogIssue.*)`，不再把英文异常 message 作为上层语义。
- `PermissionTest`、`AuthRepositoryImplTest`、`UserRepositoryImplTest`、`WebAppTaskRepositoryImplTest`、`PlazaRepositoryImplTest`、`ModelCatalogRepositoryImplTest` 和 `ModelInvocationRepositoryImplTest` 覆盖稳定 key/异常类型。

### 2026-06-22 AppDetail SWITCH 空选项 fallback 收口

已复核 `AppDetailScreen` 的字段类型关键字、选项 fallback 和参数占位。
参数占位、布尔开关和下拉占位此前已迁入 Compose Resources；本批移除 `SWITCH`
字段缺少服务端 options 时展示的 `选项A`/`选项B` 假选项，改为回退到已有布尔开关控件。
这样不会把无意义本地值提交给工作流，也避免把假选项资源化成新的固定 UI 文案。

同步完成：

- `InputNodeField` 的 `SWITCH` 分支仅在服务端提供 options 时展示分段选择器，否则使用 `BooleanSwitch`。
- `label.contains("上传视频"/"上传音频"/"上传图片")` 保留为服务端字段识别启发式；该字符串不直接展示给用户。
- 目标文件静态扫描确认 `选项A`/`选项B` 不再残留。

### 2026-06-22 AppDetail 字段渲染边界拆分

已把 AppDetail 输入节点的纯渲染规则从 `composeApp` UI 壳迁入
`feature:detail:presentation` 的结构化 UI model。字段类型到控件的分派、服务端选项解析消费、
媒体类型启发式、默认值优先级和连续图片上传聚合规则由 `AppDetailInputPresentation.kt` 集中维护；
`AppDetailScreen` 只负责 Compose 控件、资源占位、权限和平台媒体选择器映射。

同步完成：

- 新增 `AppDetailInputControl`、`AppDetailInputFieldUiModel` 和 `AppDetailInputRowUiModel`，
  避免 UI 壳直接解析 `InputNode.fieldType`。
- `SWITCH` 有服务端 options 时映射为分段选择器；无 options 时稳定降级为布尔开关。
- 上传图片/视频/音频关键字仍是服务端字段启发式，但已从 Compose 页面迁到 Presentation 规则，
  不作为固定 UI 文案展示。
- `AppDetailInputPresentationTest` 覆盖控件分派、媒体推断、连续图片分组、编辑值优先级和标题 fallback。

### 2026-06-22 QuickCreate LegacyText 兼容通道移除

已全量盘点 `QuickCreateUiMessage.LegacyText` 的生产写入点。当前生产代码只剩类型定义、
`String.asLegacyQuickCreateUiMessage()` 包装函数和 composeApp 展示映射，没有业务状态机继续写入。
本批删除 legacy 页面消息通道，QuickCreate 页面错误槽只能保存
`QuickCreateUiMessage.RuntimeText` 或 `QuickCreateUiMessage.PresentationErrorText`。

同步完成：

- `QuickCreateUiMessage` 不再包含最终展示文案载体。
- `QuickCreateUiMessageText` 删除 legacy 分支，只映射稳定运行时提示和稳定 Presentation 错误语义。
- `QuickCreateInspirationStateHolderTest` 使用稳定错误语义构造旧错误初始态，避免测试继续依赖 legacy API。
- `QuickCreateUiState` 和 `MediaReference.errorMessage` 注释同步为禁止保存最终中文文案或远端异常摘要。

### 2026-06-22 Login SMS Captcha Web 容器文案资源化

已全量盘点登录短信图形验证码的用户可见文案来源：`LoginScreen` 弹窗调用点、
`SmsCaptchaDialog` Android/iOS actual、`SmsCaptchaHtml` TAC 包装层和 `SmsCaptchaHtmlTest`。
本批将弹窗标题、关闭按钮、加载/重试/图片失败/脚本失败/超时、TAC 标题和成功失败提示
统一收口到 `SmsCaptchaCopy`，由 composeApp Compose Resources 构造后传入平台 Web 容器。
HTML 只负责 HTML/JavaScript 转义和插入 TAC 配置，不再硬编码最终中文 UI 文案。

同步完成：

- `SmsCaptchaDialog` expect/actual 增加 `SmsCaptchaCopy`，Android WebView 与 iOS WKWebView 只渲染调用方传入文案。
- `SmsCaptchaHtml` 增加 HTML 与 JavaScript 字符串转义，继续保留 token bridge、scheme 兜底、脚本超时、图片 watchdog 和旧 script 清理。
- `strings.xml` 增加 `login_sms_captcha_*` 资源，`LoginScreen` 负责从 Compose Resources 构造 copy。
- `SmsCaptchaHtmlTest` 改为使用英文测试 copy，避免测试继续锁定生产中文硬编码。
- `LongTermGovernancePlugin` 的 SMS captcha guard 改为检查资源文件持有生产文案、HTML 消费 copy 字段，防止治理门禁把中文文案推回 Kotlin 源码。
- `feature-presentation-thresholds.txt` 同步 Login UI 壳体行数基线；增长仅来自资源导入和 copy 映射，登录状态机仍归 `feature:auth:presentation`。

### 2026-06-22 QuickCreate Domain 旧模型文案字段清理

已复核旧版 `ImageModel`/`VideoModel` 的生产调用链：Data 层只读取 `modelKey`
进行旧 API 路由，Presentation 和 composeApp 使用 `ImageModelLabel`/`VideoModelLabel`
以及 Compose Resources 映射本地模型名，不再读取 Domain 枚举的展示名或说明。
本批删除 Domain 枚举上的 `displayName` 和 `description` 字段，使其只保存稳定模型标识和视频能力开关。

同步完成：

- `QuickCreationModels.kt` 不再保存旧版本地中文模型名称或模型说明。
- `LongTermGovernancePlugin` 新增 `checkQuickCreateDomainModelTextGuard`，防止旧 Domain 枚举重新定义 `displayName`/`description` 文案字段。
- 生产调用链扫描确认 `feature:quickcreate:data` 只使用 `modelKey` 和枚举常量做路由，UI 侧仍通过 Presentation 语义和资源映射展示模型名。

### 2026-06-22 QuickCreate 任务状态原文透传通道移除

已复核 `QuickCreateTaskStatusText.Custom` 的生产调用链。当前生产代码没有状态机继续写入该类型，
它只剩类型定义和 `QuickCreateResultContent` 中原样展示 `value` 的 UI 分支。本批删除该透传通道，
任务状态区只能展示稳定状态键或 `QuickCreatePresentationError` 映射后的资源文案。
服务端未知任务失败摘要继续降级为稳定错误语义，不允许直接进入用户界面。

同步完成：

- `QuickCreateTaskStatusText` 删除 `Custom(value)`，KDoc 改为明确禁止远端未知摘要原样透传。
- `QuickCreateResultContent` 删除 `Custom` 渲染分支。
- `LongTermGovernancePlugin` 新增 `checkQuickCreateTaskStatusTextGuard`，防止任务状态文案模型和结果区映射恢复 `Custom`。

### 2026-06-28 History 费用和输出数量改用服务端字段

已复核 `TaskHistoryUiState`、`TaskHistoryEntry` 和 `TaskHistoryScreen` 的费用/输出数量展示链路。
此前 `composeApp` 和 Presentation 曾通过任务标题、状态和固定资源推断费用档位、输出数量、
运行中百分比与时间信息，导致历史卡片出现非服务端真实数据。本批改为由
`feature:task:presentation` 保留 `costAmount`、`costCurrency` 和实际 `outputs.size`，
`TaskHistoryScreen` 只负责把服务端字段格式化为 Compose 展示。

同步完成：

- `TaskHistoryEntry` 改为保留服务端费用金额、币种和实际输出数量，字段注释明确不得保存最终文案。
- `TaskHistoryScreen` 删除标题关键字判断、金额硬编码、固定进度和固定日期时间资源。
- `TaskHistoryStateHolderTest` 覆盖成功、运行中和失败态的服务端费用与输出数量透传。
- `LongTermGovernancePlugin` 更新 `checkTaskHistoryPresentationTextGuard`，防止 History 恢复标题启发式或固定历史参数资源。

## 剩余问题清单

### A. Presentation 状态仍保存最终中文提示

优先级：高。处理目标是把状态字段从最终中文 `String` 改为稳定语义，由 composeApp 使用 Compose Resources 映射。

| 位置 | 问题 | 受影响调用点 |
|---|---|---|
| 无 | 无 | 当前清单内已知 Presentation 最终中文提示通道已收口；后续新增状态字段需先建模为稳定语义。 |

### B. composeApp 仍有静态硬编码可见文案

优先级：中。处理目标是迁入 Compose Resources；这些不应和 Presentation 错误语义混在同一批。

| 位置 | 示例 | 分类 |
|---|---|---|
| 无 | 无 | 当前本清单内已知 composeApp 静态中文 UI 文案已收口；后续新增命中需先分类登记。 |

### C. Core/Data 中存在用户提示或异常 message

优先级：中。处理目标是确认这些字符串是否会越过 Presentation 直接展示；不能简单按 UI 资源化处理。

| 位置 | 当前内容 | 分类 |
|---|---|---|
| `feature/audio/domain` | `AudioTaskResult.errorMessage` 保存服务端返回失败说明。 | 只读复核未发现 Presentation/UI 生产调用链展示原文；`convertTextToAudio()` 失败流已映射为稳定错误码，本批排除。 |
| `feature/model/domain` | `ModelInvocationTask.errorMessage` 与 `ApiModelField.description` 保存 OpenAPI/服务端运行时内容。 | 只读复核未发现 Presentation/UI 生产调用链展示原文；请求构建只读取字段 key、可见性和值，本批排除。 |

### D. 排除项

以下扫描命中目前不进入用户可见硬编码修复批：

- `composeApp/.../RhPreviewData.kt` 中的中文为 Preview/mock 数据。
- `feature/*/commonTest` 和 `feature/*/data/src/commonTest` 中的中文为测试 fixture 或契约样本，除非对应生产契约被修改。
- `QuickCreateDto.kt` 中的 `@SerialName("errorMessage")` 是协议字段。
- `QuickCreateRepositoryImpl.kt`、`QuickCreateApi.kt` 的 `response.message` 出现在日志或协议处理，不等同于 UI 文案；日志脱敏另走安全审计。

## 批量落地顺序

1. **后续 Presentation 文案端口复核批次**  
   继续从本清单剩余项出发，优先复核仍可能展示服务端原文的生产调用链；
   新批次开始前先全量盘点写入点，再按语义一次性覆盖调用点、测试和治理文档。

## 验证闭环

每个批次至少执行：

- 对应 feature presentation 单元测试。
- 受影响 composeApp Android Kotlin 编译或 Android debug 构建。
- `checkLongTermGovernance`。
- 针对本批次的静态扫描，确认目标文件不再新增中文硬编码或裸 `Throwable.message` 展示。

跨 `commonMain` 状态类型或资源映射的批次，还应执行 iOS Kotlin 编译；真实 iOS 运行证据按当前用户口径暂不作为中期批次门禁。
