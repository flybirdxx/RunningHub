# PRD: RunningHub 快捷创作真实 API 实装

创建日期：2026-06-20  
负责人：Codex  
适用模块：`shared`、`composeApp`  
状态：待开发

## 1. Summary

快捷创作当前需要从“视觉稿还原 + 局部真实数据 + 局部占位”的状态，收敛到一条可真实使用的服务端驱动创作链路。用户在快捷创作页应能切换真实模型，看到真实字段、真实价格，提交真实图片生成任务，并在历史抽屉里看到最近生成文件和任务状态。

首期聚焦图片创作，尤其是 Web 抓包已验证过的 quick-creation v2 链路：

- 模型目录：`POST /api/qc/v2/models`
- 价格预览：`POST /task/quick-creation/fee-preview`
- 预提交：`POST /task/quick-creation/prepare`
- 扣费提交：`POST /task/quick-creation/commit`
- 历史列表：`POST /task/quick-creation/list`
- 历史详情：`POST /task/quick-creation/detail`

当前 `composeApp/.../ui/feature/create` 的新视觉界面应复用并迁移 `quickcreate` 既有真实能力，而不是继续沿用标准 API 模型目录、固定模型卡、固定余额、固定价格和本地 fallback 作为主路径。

## 2. Contacts

| 角色 | 负责人 | 职责 |
|---|---|---|
| Product | 用户 | 确认首期范围、扣费授权边界、UI 取舍 |
| Engineering | Codex | PRD、实现方案、KMP shared/Compose 改造、验证 |
| QA | Codex + 用户 | 模拟器验证、登录态验证、真实扣费前授权 |

## 3. Background

### 3.1 现状

仓库里已经存在两套相关实现：

1. `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create`
   - 当前更接近最新设计稿。
   - 顶部已把搜索按钮换成历史图标，并有右侧历史抽屉。
   - 仍存在多个硬编码占位：固定余额 `1,286`、固定模型卡、固定 `¥ 0.045`、固定上传区域、固定高级选项。
   - `CreateScreenModel` 当前走 `ModelCatalogRepository.listStandardModels()`、`ModelCatalogRepository.getStandardModelDetail()` 和 `ModelInvocationRepository.submitStandardModel()`，主链路是 `/api/sku/*` + `/openapi/v2...`，不是 Web 快捷创作使用的 quick-creation v2。

2. `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate`
   - 旧界面功能更完整，已经接入 `QuickCreateRepository`。
   - 支持服务端模型、服务端字段、fee-preview、prepare、commit、历史列表、历史详情、取消和项目部分能力。
   - 但 UI 与最新设计稿有差异，且存在较多本轮设计不需要的功能入口。

`shared` 层已有可复用的真实 API 能力：

- `QuickCreateApi` 已封装 `/api/qc/v2/*` 与 `/task/quick-creation/*`。
- `QuickCreateRepositoryImpl` 已有 `quickCreationRequestWithTokenRetry()`、图片/视频 fee-preview、prepare、commit、历史列表、详情和取消。
- `QuickCreationModelMapper` 已能把 `/api/qc/v2/models` 的 group/model 结构压平成领域模型。
- `GenerationHistoryRepositoryImpl` 已可把 quick-creation 历史规范化为通用历史模型。

### 3.2 已验证事实

根据 `doc/quick_creation_feature_plan.md` 和 `doc/quick_creation_handoff.md`：

- Web 端快捷创作实际使用 `task/quick-creation/*` 与 `api/qc/v2/*`。
- `/api/qc/v2/models` 返回 catalog 对象，模型数组位于 `data.categories[categoryId]`，顶层可能是 group，真正可提交模型在 `children`。
- `fee-preview` 是价格来源，客户端不能按模型、分辨率、质量自行推导最终扣费。
- `commit` 请求体必须是 `prepareToken + createRequest` 嵌套结构。
- 历史必须使用 `/task/quick-creation/list`，不能依赖 `/api/output/taskHistory`。
- 真实低成本图片链路曾验证过：G-2.0 文生图提交后返回 `QUEUED`，最终 `SUCCESS`，金额约 `0.76 CNY`。

## 4. Objective

### 4.1 产品目标

让用户在最新快捷创作界面内完成一次真实图片生成：

1. 进入页面后加载真实图片模型。
2. 可在“图片/视频/音频/LLM/全部”入口中切换类别；首期图片必须真实可用，其他类别允许显示空态或“暂未接入”。
3. 选择真实模型后，表单字段由服务端 `fields` 驱动。
4. 输入 prompt 和必要参数后，按钮显示真实 fee-preview 价格。
5. 点击生成后执行 `fee-preview -> prepare -> commit -> list/detail`。
6. 提交成功后，历史抽屉展示真实任务和输出文件。

### 4.2 工程目标

- `create` 新界面不再以 `CreationFallbackCatalog`、固定模型卡、固定价格作为生产主路径。
- `create` 新界面复用 `QuickCreateRepository` 的 quick-creation v2 能力。
- 价格、模型名、字段、输出、状态都来自 API 或明确的加载/错误/空状态。
- 真实扣费动作有清晰保护：价格未确认、余额不足、token 失效、字段校验失败时不得进入 `commit`。
- 实现后有单元测试、构建验证和模拟器 UI 验证。

### 4.3 非目标

- 不在本期重做 Profile。
- 不在 Profile 显示 API 管理、创作数据或 API 状态。
- 不实现完整项目管理，包括置顶、重命名、删除、项目内筛选；历史抽屉只展示最近生成文件。
- 不实现灵感模板、制作同款、批量下载、发布、删除。
- 不扩大到视频真实扣费，除非用户单独授权并接受更高费用。
- 不继续扩展客户端硬编码模型枚举作为主方案。

## 5. Market Segments

| 用户类型 | 当前痛点 | 本期价值 |
|---|---|---|
| 轻量创作者 | 想快速输入提示词生成图片，不关心 API 细节 | 进入页面即可选模型、看价格、生成、查看结果 |
| 设计/运营用户 | 需要反复生成素材并回看最近任务 | 历史抽屉集中展示最近生成文件，不占据主创作区 |
| 移动端用户 | Web 功能可用，但 App 里仍像 demo | App 与 Web quick-creation 链路一致，减少失败和参数不一致 |
| 内部开发/测试 | 现有 create 与 quickcreate 两套逻辑重复 | 统一到服务端驱动模型和状态机，减少维护面 |

## 6. Value Propositions

1. 真实可用：快捷创作不再只是设计稿静态还原，用户能真实生成图片。
2. 服务端驱动：模型、字段、默认值、价格、任务结果由 API 决定，减少客户端硬编码漂移。
3. 扣费透明：生成前展示 fee-preview，价格不确定时阻断提交。
4. 低干扰历史：最近文件放进右侧抽屉，主页面保持专注创作。
5. 可迭代：首期打通图片链路后，视频、音频、LLM 可复用同一套模型字段和提交状态机扩展。

## 7. Solution

### 7.1 UX Requirements

#### 顶部栏

- 保留 RunningHub 品牌、余额 pill、历史图标。
- 余额必须来自真实账户数据；首期如果没有账户余额接口，显示 `--` 或隐藏数值，不允许固定 `1,286`。
- 历史图标点击后从右侧弹出抽屉。

#### 分类切换

- 保留设计稿中的 `图片`、`视频`、`音频`、`LLM`、`全部`。
- 首期 `图片` 必须接真实 `IMAGE` 模型。
- `视频/音频/LLM/全部` 可以先加载真实分类/模型；如果未接入提交链路，显示明确空态或不可用态。
- 切换分类时重置当前选中模型、字段值、价格预览和错误。

#### 模型区

- 模型卡必须由 `QuickCreationServiceModel` 渲染。
- 展示真实模型名、分组名、推荐标记、类别、价格摘要或价格预览状态。
- 点击模型卡或模型入口可打开模型选择面板。
- 不再固定显示“全能图片 V2”“¥ 0.045 / 张”等占位内容。

#### 表单区

- Prompt 字段映射服务端 `prompt` 或 `promptAi`，底部主 prompt 与服务端字段要合并成同一个 `params`。
- 比例、分辨率、质量等参数由服务端 `fields.options/defaultValue` 驱动。
- 上传字段根据 `fieldType`、`maxUploadCount`、`maxUploadSize`、`multipleInputs` 渲染。
- 未识别字段不能静默丢失；首期可以在高级选项里降级为文本输入或显示“不支持此字段”。

#### 价格与生成按钮

- 输入变化后 debounce 调用 `fee-preview`。
- 按钮状态：
  - 字段不足：禁用，展示缺失字段。
  - 价格确认中：禁用，展示“价格确认中”。
  - 价格失败：禁用，展示“价格待确认”。
  - 余额不足：禁用，展示充值/余额不足提示。
  - 可生成：展示真实金额，例如 `生成 ¥0.76`。
- 点击生成后先执行 `prepare`，成功后立即 `commit`；如后续需要二次确认，可在本状态机上增加确认弹层。

#### 历史抽屉

- 抽屉标题为“最近生成”或“历史记录”。
- 数据源为 `/task/quick-creation/list`。
- 展示任务状态、模型名或 taskType、预览图、输出数量、尺寸、费用、创建/更新时间。
- 点击成功任务可按 `outputId` 调用 `/task/quick-creation/detail` 并预览文件。
- 非终态任务每 5 秒刷新已加载范围。
- 空态显示“暂无生成文件”，不展示假任务。

### 7.2 Data Model Requirements

需要让 `create` 新界面直接消费或适配以下领域模型：

- `QuickCreationServiceModel`
  - `categoryId`
  - `bindingId`
  - `skuId`
  - `name`
  - `groupName`
  - `fields`
  - `pricing`
- `QuickCreationServiceField`
  - `fieldKey`
  - `paramKey`
  - `fieldType`
  - `required`
  - `defaultValue`
  - `options`
  - `maxUploadCount`
  - `maxUploadSize`
  - `multipleInputs`
  - `inputChildren`
- `QuickCreationFeePreview`
  - `passed`
  - `free`
  - `requiredCashAmount`
  - `requiredRhAmount`
  - `userCashBalance`
  - `insufficientType`
  - `cashCurrency`
- `QuickCreationHistoryItem`
  - `taskId`
  - `status`
  - `categoryId`
  - `bindingId`
  - `skuId`
  - `cashAmount`
  - `outputs`

### 7.3 API Flow

#### 加载模型

```text
POST /api/qc/v2/models
body: { "categoryIds": ["IMAGE"] }
```

处理要求：

- 从 `data.categories.IMAGE` 取模型。
- 支持 group + children 与直接 model 两种结构。
- 优先用 `groupName` 作为分组名。
- 过滤缺少 `bindingId` 或 `skuId` 的不可提交项。
- 真实请求失败时显示错误/重试；生产环境不自动展示伪模型。

#### 价格预览

```text
POST /task/quick-creation/fee-preview
body: {
  "bindingId": "...",
  "categoryId": "IMAGE",
  "skuId": "...",
  "params": {
    "prompt": "...",
    "aspectRatio": "16:9",
    "resolution": "2k",
    "quality": "medium"
  }
}
```

处理要求：

- 与最终提交共用同一个 `createRequest` 构建器。
- 不把本地估算伪装成最终价格。
- 对 `code=412,msg=TOKEN_INVALID` 走一次 token refresh retry。

#### 提交生成

```text
POST /task/quick-creation/prepare
body: createRequest

POST /task/quick-creation/commit
body: {
  "prepareToken": "...",
  "createRequest": createRequest
}
```

处理要求：

- `prepare` 失败不得进入 `commit`。
- `prepareToken` 过期时重新 prepare。
- `commit` 成功后将 `taskId` 写入当前任务状态，并刷新历史抽屉。
- 所有扣费相关动作必须由用户点击触发，不能在页面加载或价格预览阶段触发。

#### 查询结果

```text
POST /task/quick-creation/list
body: { "page": 1, "size": 10 }

POST /task/quick-creation/detail
body: { "outputId": "..." }
```

处理要求：

- 列表用于轮询状态和历史抽屉。
- 详情必须带 `outputId`。
- 成功任务展示 `outputList` 中的真实文件 URL/预览图/尺寸/过期信息。

### 7.4 Technical Plan

#### Phase A: 数据层收敛

- 在 `CreateScreenModel` 中移除对 `ModelCatalogRepository` 和 `ModelInvocationRepository` 的主路径依赖。
- 注入 `QuickCreateRepository`，复用：
  - `getModels(categoryId)`
  - `previewImageQuickCreationFee(request)`
  - `generateImage(request)`
  - `listQuickCreationHistory(page,size)`
  - `getQuickCreationHistoryDetail(outputId)`
- 如现有 `ImageGenerationRequest` 不能表达所有服务端字段，新增一个通用 quick-creation submit request，避免把字段塞回旧枚举。

#### Phase B: UI 状态改造

新增或改造 `CreateUiState`：

- `selectedCategory`
- `serviceModels`
- `selectedServiceModel`
- `serviceParams`
- `uploadedMedia`
- `feePreviewLoading`
- `feePreviewError`
- `feePreview`
- `currentTaskStatus`
- `historyItems`
- `historyDrawerLoading`
- `historyDetail`

删除或降级：

- `CreationFallbackCatalog` 作为生产展示来源。
- 固定 `DesignModelHero()` 文案和固定价格。
- 固定余额。
- 主界面最近任务列表；最近任务放入抽屉。

#### Phase C: 动态字段渲染

字段类型首期覆盖：

- 文本：prompt、negative prompt、普通 string。
- 选择项：aspect ratio、resolution、quality。
- 数字：seed、count、duration 等。
- 上传：image/video/audio URL 列表，先复用已有 `MediaResolver` 和上传能力。
- 条件字段：根据 `inputChildren` 或 `visibleWhen` 仅提交当前 active 字段。

#### Phase D: 提交状态机

状态流：

```text
Idle
  -> Validating
  -> FeePreviewConfirmed
  -> Preparing
  -> Committing
  -> Queued/Running
  -> Success/Failed
```

关键约束：

- `feePreviewLoading=true` 时禁止生成。
- `feePreviewError != null` 时禁止生成。
- `feePreview.passed=false` 或 `insufficientType != null` 时禁止生成。
- 非终态任务进入历史轮询。

#### Phase E: 历史抽屉

- 使用 `historyItems` 渲染分组列表。
- 成功任务优先展示首个 output 缩略图。
- 运行中任务显示状态 pill 和轻量进度文案。
- 点击任务加载详情；详情可先用抽屉内展开，后续再做全屏预览。

### 7.5 Acceptance Criteria

#### 必须满足

- 首次进入快捷创作页时，图片模型来自 `/api/qc/v2/models`。
- 模型名称、字段默认值、字段 options 来自服务端。
- 页面不再显示固定 `全能图片 V2`、固定 `¥0.045`、固定余额 `1,286` 作为真实数据。
- 输入 prompt 后触发 `fee-preview`，按钮显示服务端价格或价格状态。
- 价格未确认、余额不足或字段校验失败时，点击生成不会触发 `commit`。
- 点击生成后请求顺序为 `fee-preview -> prepare -> commit`。
- `commit` 请求体包含 `prepareToken` 与嵌套 `createRequest`。
- 成功返回 `taskId` 后，历史抽屉刷新并显示该任务。
- 成功任务最终展示真实输出文件。
- 退出登录或 token 失效时，不泄露 token/cookie/userId；UI 显示登录或重试提示。

#### 应该满足

- 图片模型切换后字段和价格状态同步重置。
- 运行中任务每 5 秒刷新。
- 历史详情按 `outputId` 加载。
- 接口失败有重试入口。
- 空模型、空历史、空输出都有明确空态。

#### 暂不要求

- 视频真实扣费。
- 项目管理操作。
- 灵感模板和制作同款。
- 批量下载和发布。

### 7.6 Analytics and Observability

首期至少保留本地调试日志或事件点，但不得输出敏感信息：

- 模型加载成功/失败。
- fee-preview 成功/失败。
- prepare 成功/失败。
- commit 成功/失败。
- 历史刷新成功/失败。
- token refresh retry 是否发生。

日志禁止包含：

- access token
- refresh token
- cookie
- 完整用户 ID
- 完整输出私有 URL，如后续发现 URL 有权限语义，应仅记录域名和文件类型。

## 8. Release

### 8.1 Milestones

| 阶段 | 目标 | 验收 |
|---|---|---|
| M1 | `create` 页面接入真实 IMAGE 模型和字段 | 模型/字段来自 `/api/qc/v2/models`，无固定模型占位 |
| M2 | 接入 fee-preview 和按钮状态 | 输入 prompt 后显示真实价格或价格错误状态 |
| M3 | 接入图片生成状态机 | 可在授权后真实提交一张低成本图片任务 |
| M4 | 接入历史抽屉真实刷新和详情 | 抽屉展示真实任务，成功任务可查看输出 |
| M5 | 清理旧占位和回归验证 | 构建、单测、模拟器截图、真实链路验证通过 |

### 8.2 Verification Plan

单元测试：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "*QuickCreationModelMapperTest" --tests "*QuickCreationV2DefaultsTest" --tests "*QuickCreateRepositoryImplFeePreviewTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.create.CreateScreenModelTest"
```

构建：

```powershell
.\gradlew.bat :shared:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:assembleDebug
```

模拟器验证：

- 登录态进入快捷创作页。
- 图片模型列表展示真实模型。
- 切换模型后字段变化。
- 输入 prompt 后按钮进入价格确认，再显示真实金额或明确价格失败。
- 打开历史图标，右侧抽屉展示真实最近生成文件。

真实扣费验证：

- 必须先取得用户明确授权。
- 仅提交一张低成本图片任务。
- 记录 taskId、最终状态、费用、输出尺寸和截图证据。
- 不在 PRD 或日志中保存 token、cookie、完整私有 URL。

### 8.3 Risks

| 风险 | 影响 | 缓解 |
|---|---|---|
| 服务端字段结构继续变化 | 表单渲染失败或提交参数缺失 | 字段渲染容忍未知类型，保留 raw JSON 测试样例 |
| 价格预览与提交参数不一致 | 展示价格与扣费不一致 | fee-preview 与 commit 共用同一个 createRequest 构建器 |
| token 业务码不是 HTTP 401 | 登录态过期但 UI 不知道 | 对 quick-creation envelope 的 `code=412 TOKEN_INVALID` 做一次 refresh retry |
| 历史详情缺少 outputId | 详情加载失败 | 详情入口仅对有 outputId 的成功输出展示 |
| 误触发真实扣费 | 用户资金损失 | 只有点击生成才进入 prepare/commit；价格不确定时 ScreenModel 层阻断 |
| 新旧 create/quickcreate 代码重复 | 后续维护成本高 | 以 `QuickCreateRepository` 为唯一生产链路，逐步淘汰旧 UI 私有逻辑 |

### 8.4 Source References

- `doc/quick_creation_feature_plan.md`
- `doc/quick_creation_handoff.md`
- `doc/RunningHub-real-data-implementation-2026-06-19.md`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/QuickCreateApi.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationModelMapper.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationV2Defaults.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreen.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreenModel.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt`
