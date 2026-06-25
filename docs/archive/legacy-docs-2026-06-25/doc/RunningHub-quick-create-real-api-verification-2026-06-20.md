# RunningHub 快捷创作真实 API 实装验证记录

日期：2026-06-20  
负责人：Codex  
关联 PRD：`doc/PRD-RunningHub-Quick-Create-Real-API-Implementation-2026-06-20.md`  
状态：实现与非扣费验证已通过；真实扣费生成验收待用户明确授权

## 范围

本记录覆盖 `composeApp` 新快捷创作界面接入 quick-creation v2 的当前实现状态，重点验证：

- 图片模型、字段、价格、历史数据来自 quick-creation API 主路径。
- 价格未确认、余额不足、字段校验失败时不会进入扣费提交。
- 历史抽屉展示真实最近生成文件，并能按 `outputId` 打开详情。

## 已验证证据

### 静态实现

- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreenModel.kt`
  - 主路径注入并使用 `QuickCreateRepository`。
  - 使用 `getModels(categoryId)` 加载图片模型。
  - 输入变更后 debounce 调用 `previewImageQuickCreationFee()`。
  - 生成前检查字段校验、上传状态、`feePreviewLoading`、`feePreviewError`、`feePreview.passed` 和 `insufficientType`。
  - 生成调用 `generateImage()`，成功或失败后刷新 `listQuickCreationHistory(page=1, size=12)`。
  - 历史详情调用 `getQuickCreationHistoryDetail(outputId)`。
  - 历史列表维护加载与错误状态，接口失败时抽屉展示错误和重试入口，不伪装为空历史。

- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImpl.kt`
  - quick-creation 图片请求带 `bindingId + skuId` 时走 V2 路径。
  - 提交流程为 `fee-preview -> prepare -> commit -> list`。
  - `commit` 请求体使用 `prepareToken + createRequest` 嵌套结构。
  - `prepareToken` 过期时会重新 prepare 后再 commit。
  - `code=412 TOKEN_INVALID` 通过既有 token refresh retry 处理。
  - 记录不含参数、不含 token/cookie、不含输出 URL 的高层调试日志：模型加载、fee-preview、prepare、commit、history list/detail、token refresh retry。

### 自动化验证

已通过的组合命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "*QuickCreationModelMapperTest" --tests "*QuickCreationV2DefaultsTest" --tests "*QuickCreateRepositoryImplFeePreviewTest" --tests "*QuickCreateRepositoryImplVideoV2Test" :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.create.CreateScreenModelTest" :shared:compileDebugKotlinAndroid :composeApp:assembleDebug
```

覆盖点：

- `/api/qc/v2/models` mapper 兼容 group + children。
- fee-preview 使用 `/task/quick-creation/fee-preview`。
- token invalid envelope 触发 refresh retry。
- 图片 quick-creation id 走 V2 submit flow，而不是旧模型枚举。
- fee-preview 不通过时只请求 fee-preview，不进入 prepare/commit。
- prepare token 过期时重新 prepare。
- create ScreenModel 在价格失败、余额不足、字段缺失时不调用 generate。
- 模型目录与 fee-preview 遇到 token 失效时展示登录提示，不展示 bearer token、cookie 值或其他敏感片段。
- 非终态历史每 5 秒刷新直到终态。
- 历史详情按 `outputId` 加载。
- 历史列表加载失败会暴露可重试错误，重试成功后恢复真实历史列表。
- quick-creation V2 链路日志仅记录阶段、结果码、状态和 taskId，不记录 prompt、token、cookie 或输出 URL。

### 模拟器验证

最新安装包在 Android 模拟器中验证：

- 输入 prompt 后真实 fee-preview 成功，页面显示 `¥0.76`，生成按钮启用。
  - 截图：`output/rh-fee-preview-after-upload-whitelist.png`
  - XML：`output/rh-fee-preview-after-upload-whitelist.xml`
- 点击历史图标后右侧抽屉展示真实最近生成文件。
  - 截图：`output/rh-create-history-drawer-real.png`
  - XML：`output/rh-create-history-drawer-real.xml`
  - 抽屉显示：`历史记录`、`最近生成的文件 · 3`、`FAST_WEBAPP_V2`、成功状态、尺寸、费用。
- 点击成功任务后详情面板展示真实输出信息。
  - 截图：`output/rh-create-history-detail-real.png`
  - XML：`output/rh-create-history-detail-real.xml`
  - 详情显示：`文件详情`、预览图、`IMAGE · SUCCESS · PNG · 1254x1254 · 有效期 12 天`、费用。
- 历史抽屉错误重试改动后重新安装 debug 包做渲染烟测。
  - 创建页截图：`output/rh-create-history-retry-smoke-create.png`
  - 抽屉截图：`output/rh-create-history-retry-smoke-drawer.png`
  - 抽屉 XML：`output/rh-create-history-retry-smoke-drawer.xml`
  - 抽屉显示：`历史记录`、`最近生成的文件 · 3`、真实 `FAST_WEBAPP_V2` 任务、成功状态、尺寸和费用。

## PRD 验收状态

| 验收项 | 状态 | 证据 |
|---|---|---|
| 首次进入快捷创作页时图片模型来自 `/api/qc/v2/models` | 已验证 | `CreateScreenModel.loadModels()` + mapper/unit tests + 模拟器页面真实模型 |
| 模型名称、字段默认值、字段 options 来自服务端 | 已验证 | `CreateScreenModelTest` 覆盖默认值、options fallback、动态字段 |
| 不再显示固定 `全能图片 V2`、固定 `¥0.045`、固定余额 `1,286` | 已验证 | `rg "1,286|0\\.045|全能图片 V2|CreationFallbackCatalog|ModelCatalogRepository|ModelInvocationRepository"` 未在 create 主路径命中 |
| 输入 prompt 后触发 fee-preview 并显示服务端价格 | 已验证 | 模拟器显示 `¥0.76`，log 显示 fee-preview success |
| 价格未确认、余额不足、字段校验失败时不会触发 commit | 已验证 | ScreenModel 测试 + shared flow 测试 |
| 点击生成后请求顺序为 `fee-preview -> prepare -> commit` | 自动化已验证；真实点击待授权 | `QuickCreateRepositoryImplVideoV2Test` 验证路径顺序 |
| `commit` 请求体包含 `prepareToken` 与嵌套 `createRequest` | 已验证 | Repository 实现 + V2 submit tests |
| 成功返回 `taskId` 后历史抽屉刷新并显示任务 | 自动化已验证；真实新任务待授权 | ScreenModel submit 测试；模拟器历史抽屉显示已有真实任务 |
| 成功任务最终展示真实输出文件 | 已验证已有真实历史输出 | 历史详情截图/XML |
| 接口失败有重试入口 | 已验证 | 模型目录错误面板可重试；历史抽屉加载失败显示错误和重试入口 |
| token/cookie/userId 不泄露，UI 显示登录或重试提示 | 已验证主要路径 | fee-preview log redaction、token refresh tests、CreateScreenModel token invalid UI tests、V2 高层链路日志审计 |

## 剩余授权边界

尚未在本轮触发新的真实扣费生成。PRD 的最终真实扣费验收需要用户明确授权后执行：

1. 使用低成本图片模型。
2. 输入短 prompt。
3. 点击生成，触发一次真实 `fee-preview -> prepare -> commit`。
4. 记录 taskId、最终状态、费用、输出尺寸和截图证据。

未授权前不得自动点击“生成”，也不得用页面加载、fee-preview 或历史刷新触发扣费动作。
