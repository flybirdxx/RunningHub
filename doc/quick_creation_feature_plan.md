# 快捷创作功能开发计划

创建日期：2026-06-18  
适用模块：`shared`、`composeApp`  
状态：待开发

## 1. 背景

现有 KMP 客户端已经有 `quickcreate` UI、`QuickCreateRepository`、`QuickCreateApi` 等基础代码，但当前实现主要走旧的 `openapi/v2/*` 接口，并在客户端硬编码模型、参数、价格估算和任务轮询逻辑。

2026-06-18 通过已登录的 RunningHub Web 快捷创作页抓包验证，Web 端实际使用的是 `task/quick-creation/*` 与 `api/qc/v2/*` 这一套新接口。后续移动端快捷创作应迁移为服务端驱动：分类、模型、字段、默认值、价格预览、提交确认和任务结果都以接口返回为准。

## 2. 目标

实现移动端“快捷创作”功能，首期覆盖 Web 端核心体验：

- 图片创作：文本生成图片、图片参考生成/编辑图片。
- 视频创作：文本/图片/视频/音频参考生成视频，首期可先支持 Seedance2.0 默认链路。
- 灵感发现：标签、模板列表、模板详情、“制作同款”。
- 任务提交：价格预览、prepare 确认、commit 扣费提交、任务列表轮询、结果展示。
- 登录态：未登录拦截，已登录携带 `Authorization`、cookie、语言头请求。
- 结果管理基础：展示输出图/视频、预览图、尺寸、过期时间、任务状态。

## 3. 非目标

首期不做以下内容，除非后续单独排期：

- Agent 技能创作的完整聊天式工作流。
- 项目管理的完整交互，包括置顶、重命名、删除、项目内任务。
- 批量下载、发布、删除。
- 数字人。接口分类存在 `DIGITAL_HUMAN`，但当前模型列表为空。
- 自研价格估算。价格必须以 `fee-preview` 或 `prepare.feePreview` 为准。

## 4. 关键抓包结论

### 4.1 分类接口

```text
POST /api/qc/v2/categories
body: {}
```

返回分类：

- `IMAGE`：图片创作
- `VIDEO`：视频创作
- `DIGITAL_HUMAN`：数字人，当前模型为空
- `MOTION_IMITATE`：动作模仿
- `EDIT_ENHANCE`：编辑与增强

移动端首期 UI 可以继续只暴露 `IMAGE`、`VIDEO` 两个主入口，但数据层应支持全部分类。

### 4.2 模型接口

```text
POST /api/qc/v2/models
body: { "categoryIds": ["IMAGE"] }
body: { "categoryIds": ["VIDEO"] }
```

模型返回是服务端驱动结构：

- 顶层可能是 `type = group`，其 `children` 才是真正可提交模型。
- 也可能是 `type = model`，例如动作模仿分类。
- 每个模型带 `bindingId`、`skuId`、`name`、`fields`、`pricing`、`supportsT2i`、`supportsI2i`。
- `fields` 描述表单字段：`fieldKey`、`mappedApiParamKey`、`fieldType`、`required`、`defaultValue`、`options`、`maxUploadCount`、`maxUploadSize`、`multipleInputs`、`skuInputExtraJson`。

首期不要继续扩展客户端硬编码枚举，建议新增通用模型结构：

```kotlin
data class QuickCreationModelEntry(
    val categoryKey: String,
    val groupName: String?,
    val bindingId: String?,
    val skuId: String?,
    val name: String,
    val entryKind: String?,
    val fields: List<QuickCreationField>,
    val children: List<QuickCreationModelEntry>
)
```

### 4.3 价格预览

```text
POST /task/quick-creation/fee-preview
```

图片 G-2.0 文生图示例：

```json
{
  "bindingId": "2046586338670891013",
  "categoryId": "IMAGE",
  "params": {
    "aspectRatio": "16:9",
    "resolution": "2k",
    "quality": "medium"
  },
  "skuId": "2046514150500524034"
}
```

返回关键字段：

```json
{
  "passed": true,
  "free": false,
  "settlementMode": "cash_only",
  "requiredRhAmount": 0,
  "requiredCashAmount": 0.76,
  "userCashBalance": 157.136,
  "insufficientType": null,
  "cashCurrency": "CNY"
}
```

说明：

- `fee-preview` 不包含 prompt 也能预览价格。
- 价格不应由客户端按模型、分辨率、质量推导。
- `passed = false` 或 `insufficientType != null` 时，提交按钮应禁用或提示充值。

### 4.4 prepare

```text
POST /task/quick-creation/prepare
```

真实请求体：

```json
{
  "bindingId": "2046586338670891013",
  "categoryId": "IMAGE",
  "skuId": "2046514150500524034",
  "params": {
    "prompt": "测试生成一张极简风格的绿色圆形图标",
    "aspectRatio": "16:9",
    "resolution": "2k",
    "quality": "medium"
  }
}
```

真实响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "prepareToken": "xxxx",
    "expireAtMillis": "1781713572845",
    "ttlSeconds": 120,
    "skuId": "2046514150500524034",
    "feePreview": {
      "passed": true,
      "requiredCashAmount": 0.76,
      "userCashBalance": 157.136,
      "cashCurrency": "CNY"
    }
  }
}
```

说明：

- `prepareToken` 约 120 秒有效。
- UI 应在 prepare 成功后展示确认扣费信息，或在用户点击生成时自动 prepare 后立即 commit。
- 若 prepare 失败，不进入 commit。

### 4.5 commit

```text
POST /task/quick-creation/commit
```

真实结构不是平铺参数，而是 `prepareToken + createRequest`：

```json
{
  "prepareToken": "97b44d8ff8d846ffa3cece953ec981cd",
  "createRequest": {
    "bindingId": "2046586338670891013",
    "categoryId": "IMAGE",
    "skuId": "2046514150500524034",
    "params": {
      "prompt": "测试生成一张极简风格的绿色圆形图标，纯白背景，中心是 RunningHub 风格的绿色圆形符号",
      "aspectRatio": "16:9",
      "resolution": "2k",
      "quality": "medium"
    }
  }
}
```

错误验证：

- 如果直接平铺 `prepareToken` 和字段，接口返回 `code = 301`、`msg = createRequest不能为空`。

成功响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "taskId": "2067282648442818561",
    "skuId": "2046514150500524034",
    "taskStatus": "QUEUED",
    "isFree": false,
    "rhAmount": 0,
    "cashAmount": 0.76,
    "feePreview": {
      "requiredCashAmount": 0.76,
      "userCashBalance": 156.376,
      "cashCurrency": "CNY"
    },
    "outputWidth": null,
    "outputHeight": null,
    "projectId": null
  }
}
```

说明：

- commit 会真实扣费。
- 返回 `taskStatus = QUEUED` 后进入任务列表轮询。

### 4.6 任务列表和结果

快捷创作历史应使用：

```text
POST /task/quick-creation/list
body: { "page": 1, "size": 10 }
```

不要依赖：

```text
POST /api/output/taskHistory
```

本次验证中，任务完成后 `/api/output/taskHistory` 仍返回空。

成功任务核心结构：

```json
{
  "taskId": "2067282648442818561",
  "taskStatus": "SUCCESS",
  "taskCostTime": "66",
  "taskType": "FAST_WEBAPP_V2",
  "skuId": "2046514150500524034",
  "bindingId": "2046586338670891013",
  "bindingCategoryId": "IMAGE",
  "apiRequestParams": "{\"prompt\":\"...\",\"quality\":\"medium\",\"resolution\":\"2k\",\"aspectRatio\":\"16:9\"}",
  "prepayRecord": {
    "settlementMode": "cash_only",
    "rhAmount": 0,
    "cashAmount": 0.76,
    "cashCurrency": "CNY",
    "isFree": 0,
    "status": "PREPAID"
  },
  "outputList": [
    {
      "id": "2067282926235770882",
      "outputName": "f6b12596-cd33-4601-b7f7-994674e79700.png",
      "outputType": "png",
      "fileUrl": "https://...",
      "filePreviewUrl": "https://...",
      "outputSize": "2048x1152",
      "auditStatus": 1,
      "expireTime": "2026-07-02 00:27:12",
      "expireDays": "13"
    }
  ]
}
```

### 4.7 任务详情

```text
POST /task/quick-creation/detail
```

验证结果：

- 只传 `{ "taskId": "..." }` 会返回 `outputId不能为空`。
- 传 `{ "outputId": "..." }` 成功。
- 传 `{ "taskId": "...", "outputId": "..." }` 也成功。

推荐实现：

```json
{ "outputId": "2067282926235770882" }
```

### 4.8 灵感发现

标签：

```text
POST /task/quick-creation/inspiration/tags
body: {}
```

当前标签：

- 热门
- 人像
- 电商
- 舞蹈
- 创意

模板列表：

```text
POST /task/quick-creation/inspiration/templates
body: { "page": 1, "size": 20 }
```

模板字段：

- `templateId`
- `nameCn`
- `nameAi`
- `coverUrl`
- `categoryId`
- `videoUrl`
- `tagHot`
- `tagNew`
- `sortOrder`

模板详情：

```text
POST /task/quick-creation/inspiration/template/detail
body: { "templateId": "2066785582547582978" }
```

2026-06-18 已补抓模板详情。响应关键字段：

- 顶层：`templateId`、`nameCn/nameAi`、`coverUrl`、`categoryId`、`videoUrl`。
- 模型：`bindingId`、`skuId`、`bindingCnName/bindingAiName`。
- 参数：`snapshot.presetParams`，包含 `prompt/promptAi`、`ratio/aspectRatio`、`resolution`、`duration/videoDuration`、`generateAudio`、`realPersonMode`、`imageUrls` 等。
- 兜底：`apiRequestParamsRaw` 是 JSON 字符串，可在 `snapshot.presetParams` 缺失时二次解析。

移动端“制作同款”应依赖模板详情填充模型、字段和默认参数，而不是只用列表字段。

### 4.9 项目接口

已确认接口存在：

```text
POST /task/quick-creation/project/list
POST /task/quick-creation/project/create
POST /task/quick-creation/project/rename
POST /task/quick-creation/project/delete
POST /task/quick-creation/project/pin
POST /task/quick-creation/project/detail
POST /task/quick-creation/project/tasks
```

本次账号项目列表为空。项目功能放到二期。

## 5. 技术方案

### 5.1 数据层

新增或重构 `shared` 数据层：

- `QuickCreationV2Api`
  - `getCategories()`
  - `getModels(categoryIds)`
  - `getCreationModes(categoryId)`
  - `getInspirationTags()`
  - `getInspirationTemplates(page, size, tagId?)`
  - `getInspirationTemplateDetail(templateId)`
  - `feePreview(request)`
  - `prepare(createRequest)`
  - `commit(prepareToken, createRequest)`
  - `listTasks(page, size)`
  - `getTaskDetail(outputId)`
  - `cancel(taskId)`

- `QuickCreationRepository`
  - 对外暴露领域模型，不泄漏 DTO。
  - 将服务端字段定义映射为动态表单模型。
  - 维护提交状态流：`Idle -> Preparing -> Confirming -> Committing -> Queued -> Running -> Success/Failed`。

建议保留旧 `QuickCreateApi` 作为兼容层，新增 v2 路线后逐步替换 UI 调用。

### 5.2 DTO 重点

必须支持动态 JSON 参数：

```kotlin
@Serializable
data class QuickCreationCreateRequestDto(
    val bindingId: String,
    val categoryId: String,
    val skuId: String,
    val params: Map<String, JsonElement>
)

@Serializable
data class QuickCreationCommitRequestDto(
    val prepareToken: String,
    val createRequest: QuickCreationCreateRequestDto
)
```

字段值类型处理：

- `STRING` -> `JsonPrimitive(String)`
- `INT` -> `JsonPrimitive(Int)`
- `FLOAT` -> `JsonPrimitive(Double)`
- `BOOLEAN` -> `JsonPrimitive(Boolean)`
- `LIST` -> 单选用 `String/Boolean/Number`，多选用 `JsonArray`
- `IMAGE/VIDEO/AUDIO` -> 上传后 URL 或 URL 数组
- `COMPLEX` / `multipleInputs = true` -> `JsonArray(JsonObject)`

### 5.3 UI 层

现有 `composeApp/.../quickcreate` 可继续作为基础，但需要改为服务端驱动：

- 顶部模式：`Agent`、`图片`、`视频`。首期 Agent 可展示占位或技能入口，不提交。
- 主输入区：根据当前模型字段渲染 prompt、上传组件、选择项、开关、数值输入。
- 参数底部弹窗：使用服务端 `fields.options` 和默认值，不再用本地枚举。
- 灵感发现：瀑布流/网格卡片，支持视频角标、时长、New 标识、“制作同款”。
- 生成按钮：展示 `feePreview.requiredCashAmount`，未通过价格预览时禁用。
- 结果区：根据 `outputType` 渲染图片或视频，展示尺寸、过期时间、状态。

### 5.4 认证和 Header

请求需使用当前登录态：

- `Authorization: Bearer <Rh-Accesstoken>`
- `user-language: zh_CN`
- cookie 由 Ktor 客户端统一处理，或沿用现有认证拦截器。
- 团队上下文若存在，Web 会带 `X-Team-Id`；移动端后续如支持团队，需要同步。

不要在日志、文档、异常中输出 token、cookie、用户 ID 全量值。

## 6. 分阶段计划

### Phase 0：契约落地

1. 新增 v2 DTO 和 API 封装。
2. 用抓包样例写序列化/反序列化单元测试。
3. 明确旧 `QuickCreateRepository` 与新 v2 repository 的并存边界。

验收：

- `categories/models/fee-preview/prepare/commit/list/detail` DTO 能解析真实样例。
- 测试覆盖 `commit` 必须包裹 `createRequest`。

### Phase 1：图片 MVP

1. 图片分类模型列表：加载 `IMAGE` 模型组和子模型。
2. 支持 G-2.0 文生图字段：`prompt`、`aspectRatio`、`resolution`、`quality`。
3. 接入 `fee-preview -> prepare -> commit -> list`。
4. 轮询直到 `SUCCESS/FAILED`，展示 `outputList`。
5. 结果点击进入详情，详情按 `outputId` 请求。

验收：

- 登录账号可生成一张图片。
- 生成按钮展示真实价格。
- 扣费后余额变化来自响应，不做本地推算。
- 任务成功后显示预览图、尺寸、过期时间。

### Phase 2：图片参考和上传

1. 根据 `IMAGE` 字段支持单图/多图上传。
2. 上传成功后将远端 URL 写入 `params`。
3. 支持 `imageUrls`、`imageUrl` 两种字段形态。
4. 支持 `maxUploadCount`、`maxUploadSize` 校验。

验收：

- 图生图模型可提交并返回结果。
- 上传失败、超数量、超大小有明确错误状态。

### Phase 3：视频 MVP

1. 加载 `VIDEO` 模型与 `creation-modes`。
2. 首期支持 Seedance2.0 默认模型字段：
   - `prompt`
   - `resolution`
   - `duration`
   - `imageUrls`
   - `videoUrls`
   - `audioUrls`
   - `generateAudio`
   - `ratio/aspectRatio`
   - `realPersonMode`
3. 支持视频/音频上传。
4. 使用同一 `prepare/commit/list/detail` 状态机。

执行进展（2026-06-18）：

- 当视频请求携带服务端 `quickCreationBindingId/quickCreationSkuId` 时，repository 已复用 `fee-preview -> prepare -> commit -> list` 状态机。
- Seedance2.0 多模态请求构造已覆盖抓包确认的 `ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、`creationMode`、`creationSubModeId`、`creationSubModeKey` 和参考素材 URL 数组。
- 已用 `QuickCreationV2DefaultsTest` 与 `QuickCreateRepositoryImplVideoV2Test` 覆盖请求构造、v2 路由和视频输出解析；真实 App 端视频扣费任务仍待单独授权后验证。

验收：

- 文生视频或多模态参考视频至少一种链路可完整跑通。
- `outputType` 为视频时可播放预览。

### Phase 4：灵感模板

1. 标签列表和模板分页。
2. 模板卡片展示图片/视频预览。
3. 抓取并接入模板详情。
4. “制作同款”将模板详情映射到当前模型、上传槽位和参数。

验收：

- 点击模板能进入已填充状态。
- 图片模板和视频模板能切换正确分类。

### Phase 5：历史和项目

1. 快捷创作历史使用 `/task/quick-creation/list`。
2. 详情使用 `outputId`。
3. 接入取消任务。
4. 项目列表、创建、重命名、删除、置顶另行实现。

执行进展（2026-06-18）：

- `QuickCreateRepository` 已新增历史分页和详情方法，数据源使用 `/task/quick-creation/list` 与 `/task/quick-creation/detail`。
- domain 历史模型已覆盖任务状态、分类、模型 ID、扣费金额、`apiRequestParams` 标量参数、输出 URL/预览图/尺寸/过期信息。
- 已用 `QuickCreateRepositoryImplHistoryTest` 覆盖历史分页映射和按 `outputId` 获取详情。
- 创作页已在中间内容区展示最近创作，生成成功后刷新历史；点击历史项会按 `outputId` 加载详情并展示详情弹窗；分页加载、运行中刷新和取消任务仍待接入。

验收：

- 历史列表能展示运行中和成功任务。
- 运行中任务刷新后能继续轮询。

## 7. 风险和注意事项

- `taskName`、`name`、`apiRequestParams` 在抓包输出中出现乱码，可能是服务端返回编码或抓包工具展示问题。移动端应按 UTF-8 正常解析并在真机验证。
- `detail` 依赖 `outputId`，任务运行中没有输出时不能调用详情，只能通过 list 轮询。
- `apiRequestParams` 是 JSON 字符串，不是对象，需要二次解析。
- `prepareToken` 有时效，用户确认扣费前若超时，需要重新 prepare。
- `fee-preview` 和 `prepare.feePreview` 都可作为价格来源，最终扣费以 commit 响应为准。
- 不要把旧 `/api/output/taskHistory` 当作快捷创作历史来源。
- 动态字段会不断变化，UI 渲染层必须能容忍未知 `fieldType`，至少降级为只读/不支持提示。

## 8. 验证矩阵

| 场景 | 验证方式 | 通过标准 |
| --- | --- | --- |
| 未登录进入快捷创作 | 手动退出登录后打开页面 | 生成、价格预览、历史需要登录提示 |
| 图片文生图 | 真机提交 G-2.0 文生图 | 产生 `taskId`，最终 `SUCCESS`，显示图片 |
| prepare 超时 | 等待超过 `ttlSeconds` 后 commit | 失败后重新 prepare，可再次提交 |
| 余额不足 | 使用不足余额账号或 mock 响应 | UI 禁用提交并提示充值 |
| 任务运行中 | 提交后立即返回列表 | 显示 `QUEUED/RUNNING`，继续轮询 |
| 任务成功 | 轮询 list | `outputList` 非空，详情按 `outputId` 成功 |
| 图生图上传 | 上传 1 张图片后提交 | URL 写入 `imageUrl/imageUrls` 参数 |
| 视频结果 | 提交视频任务 | 输出按视频组件展示 |
| 模板制作同款 | 点击模板 | 分类、模型、字段默认值填充正确 |

## 9. 后续待补抓

后续开发前建议再补抓以下接口：

- 图片图生图的真实 `prepare/commit` 请求体。
- Seedance2.0 视频提交的真实 `prepare/commit` 请求体。
- 上传接口在 Web 快捷创作 v2 中的真实路径、响应结构和鉴权方式。
- 任务失败、取消任务、余额不足、敏感词拦截的响应结构。
