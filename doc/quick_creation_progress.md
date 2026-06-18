# 快捷创作重构开发进度表

更新时间：2026-06-18
分支：`feature/kmp-refactoring`
依据：Web 抓包结果、移动端截图结构、`doc/quick_creation_feature_plan.md`

## 总体状态

| 阶段 | 范围 | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- | --- |
| Phase 0 | v2 契约落地 | 已完成 | 100% | 已补 `prepare/commit/list/detail` 等 DTO，并用抓包样例做序列化/反序列化测试。 |
| Phase 1 | 图片 G-2.0 MVP | 部分完成 | 70% | 默认 `all-power-image-g2` 已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；图片提交会优先携带当前服务端模型的 `categoryId/bindingId/skuId`；真实端到端扣费生成尚未在 App 内复测。 |
| Phase 2 | 页面结构重构 | 部分完成 | 80% | 页面已按移动端截图边界调整为顶部轻量“创作/灵感”、中间可滚动内容区、底部固定模型参数和提示词输入面板；底部输入区已显示服务端模型摘要，Tune 高级页可切换服务端图片模型。 |
| Phase 3 | 灵感接口接入 | 部分完成 | 82% | 已接 tags/templates 列表和真实 template/detail；UI 使用真实 state 渲染，点击模板可“制作同款”并回填分类、服务端模型、prompt、基础参数和远端素材。 |
| Phase 3A | 服务端模型字段接入 | 部分完成 | 82% | 已接 `/api/qc/v2/models` 真实 catalog 响应结构，按 `data.categories[categoryId]` 读取模型并兼容 `groupName` 分组名；服务端 `defaultValue/options` 会初始化字段值，Tune 高级页可选择基础 options，文本/数值字段可输入，图片/视频/音频上传字段会按服务端字段映射已上传素材 URL 列表；图片 v2 提交会把字段值写入 `params`，视频请求结构已携带服务端模型 ID 和字段参数。 |
| Phase 4 | 视频 v2 链路 | 部分完成 | 45% | 当视频请求携带服务端 `bindingId/skuId` 时已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；Seedance2.0 多模态参数已按抓包结构生成，真实 App 端视频扣费生成尚未复测。 |
| Phase 5 | 历史/项目 | 部分完成 | 99% | `list/detail/cancel/project/list/project/tasks/project/pin/project/create/project/rename/project/delete/project/detail` DTO/API 已可解析，domain repository 已公开历史分页、按 `outputId` 获取详情、按 `taskId` 取消任务、项目列表分页、项目任务分页、项目置顶切换、项目创建/重命名/删除和项目详情；创作页中间区域已展示最近创作，可打开详情弹窗，已接入加载更多分页，会对非终态历史任务定时刷新，并可取消非终态任务；历史区顶部已展示项目筛选条和新建按钮，点击项目会加载项目内任务并可切回最近创作，项目 chip 可置顶、查看详情、重命名和删除；项目管理端点已通过 Chrome DevTools 使用登录态临时项目验证，仍缺 App 内端到端复测。 |

## 本轮已完成

| 类型 | 文件 | 内容 |
| --- | --- | --- |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2DtoTest.kt` | 覆盖 nested `commit` 请求、`prepare` 响应、任务列表输出结构。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定图片 G-2.0 默认 `bindingId`、`skuId`、`categoryId` 和参数映射。 |
| 测试 | `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt` | 增加创作/灵感模式切换测试、服务端模型加载默认选中测试、字段默认值初始化测试，以及图片/视频生成请求携带服务端模型 ID、字段 params 和上传列表 params 的回归测试。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 覆盖服务端字段 params、列表 params 与 prompt/比例/分辨率/质量的合并和覆盖规则。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定视频 v2 默认请求结构：服务端 ID、`ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、多模态 `creationMode/creationSubMode*` 和 `imageUrls`。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImplVideoV2Test.kt` | 覆盖视频请求在携带 quick-creation ID 时走 `fee-preview -> prepare -> commit -> list`，并从 `outputList` 产出视频结果。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImplHistoryTest.kt` | 覆盖 `/task/quick-creation/list` 历史分页映射、扣费字段、`apiRequestParams` 解析、输出尺寸解析、`/task/quick-creation/detail` 按 `outputId` 获取详情、`/task/quick-creation/cancel` 使用 URL 编码后的 `taskId` 请求体、`/task/quick-creation/project/list` 项目分页映射和请求体、`/task/quick-creation/project/tasks` 按真实 `projectId/page/size` 请求并解析 `records/current` 分页、`/task/quick-creation/project/pin` 按真实 `projectId/pinned` 提交置顶状态，以及 `project/create/rename/delete/detail` 的请求体和项目映射。 |
| 测试 | `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt` | 覆盖 ScreenModel 初始化加载 quick-creation 历史和项目列表、选择项目后加载项目内任务、清除项目筛选后回到最近创作、加载更多追加下一页、非终态历史任务定时刷新到终态、取消历史任务后刷新列表、选择 output 后按 `outputId` 加载详情并写入 UI state、切换项目置顶时调用 repository 并更新本地项目 state、创建/重命名/删除项目时更新项目列表并在删除当前筛选项目后回到最近创作，以及按 `projectId` 加载项目详情写入 UI state。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationModelDtoTest.kt` / `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationModelMapperTest.kt` / `QuickCreateRepositoryImplHistoryTest.kt` | 覆盖 `/api/qc/v2/models` 真实 `data.categoryMeta/categories` catalog 结构、分组、模型、字段、options/defaultValue 解析和 domain flatten 映射。 |
| 数据层 | `QuickCreationV2Dto.kt` | 新增 Web quick-creation v2 DTO。 |
| 数据层 | `QuickCreateApi.kt` | 新增 `categories/models/fee-preview/prepare/commit/list/detail/inspiration` 端点封装。 |
| 数据层 | `QuickCreateRepositoryImpl.kt` | 默认图片 G-2.0 改走 v2 提交和列表轮询，视频请求携带服务端 quick-creation ID 时也走 v2 提交和列表轮询；旧模型保留回退。 |
| 数据层 | `QuickCreationV2Defaults.kt` | 新增视频 v2 `createRequest` 构造，支持字段 params、列表 params、参考图/视频/音频 URL 和抓包确认的多模态参数。 |
| 数据层 | `QuickCreateRepository.kt` / `QuickCreateRepositoryImpl.kt` | 新增 `QuickCreationHistoryPage/Item/Output`、`QuickCreationProjectPage/Project` domain 模型和 `listQuickCreationHistory/getQuickCreationHistoryDetail/cancelQuickCreationTask/listQuickCreationProjects/listQuickCreationProjectTasks/createQuickCreationProject/renameQuickCreationProject/deleteQuickCreationProject/pinQuickCreationProject/getQuickCreationProjectDetail` 方法，后续 UI 可直接消费 quick-creation v2 历史数据、取消非终态任务、读取项目列表、按项目读取任务分页并管理项目。 |
| 网络 | `SharedModule.kt` | RunningHub 请求补 `user-language: zh_CN`，并在存在 Cookie 时自动携带。 |
| UI | `QuickCreateUiState.kt` / `QuickCreateScreenModel.kt` | 新增 `QuickCreateMode` 和 `switchMode`。 |
| UI | `QuickCreateScreen.kt` | 改为顶部模式切换、中间内容区、底部固定输入区；灵感页使用真实 tags/templates state 渲染。 |
| 灵感 | `QuickCreateRepository.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 接入真实 tags/templates/template detail 数据并替换灵感区占位内容；`template/detail` 请求体为 `{"templateId":"..."}`，响应中的 `snapshot.presetParams` 会映射到制作同款状态。 |
| 服务端模型 | `QuickCreationModelMapper.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 加载 IMAGE/VIDEO 服务端模型，默认选中首个可用模型，并在图片 v2 提交中使用选中模型的 `categoryId/bindingId/skuId`。 |
| 服务端字段 | `QuickCreateScreenModel.kt` / `QuickCreationV2Defaults.kt` / `QuickCreationModelMapper.kt` | 服务端字段默认值进入 UI state，用户更新后的字段值覆盖默认值，未知字段会被过滤；上传限制、multipleInputs 和 `skuInputExtraJson` 已保留到 domain model；图片/视频/音频上传字段会按字段类型把已上传素材 URL 写入对应服务端 `paramKey` 的数组参数。 |
| UI | `QuickCreateScreen.kt` / `TuneBottomSheet.kt` | 底部输入区展示当前服务端模型摘要，Tune 高级页展示并切换服务端图片模型，基础 options 字段可点选，文本/数值字段可输入，上传字段显示服务端限制摘要，保留本地模型作为兼容 fallback。 |
| UI | `QuickCreateScreen.kt` | 灵感模板卡片已接入“制作同款”，点击后加载模板详情并切回创作页。 |
| UI | `QuickCreateUiState.kt` / `QuickCreateScreenModel.kt` / `QuickCreateScreen.kt` | 新增历史加载状态、分页 state、历史列表 state、取消中 state、详情 state、项目列表 state、项目任务筛选 state、项目置顶中 state、项目变更中 state 和项目详情 state；创作页在无当前结果/任务时展示最近创作，支持图片/视频预览、状态、分类和扣费金额摘要；历史区顶部展示项目横向筛选条和新建按钮，点击项目会加载项目内任务，点击“最近创作”会清除筛选，点击项目 chip 图钉会调用置顶/取消置顶，更多菜单可打开详情、重命名/删除项目；历史列表可加载更多并去重追加；非终态历史任务会每 5 秒刷新当前已加载范围并显示取消入口；点击历史项按 `outputId` 加载详情弹窗；生成成功后刷新历史。 |

## 已验证

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `./gradlew.bat :shared:testDebugUnitTest` | 通过 | 存在既有 Kotlin warning，未新增失败。 |
| `./gradlew.bat :composeApp:testDebugUnitTest` | 通过 | 存在既有 Profile/MediaChip warning。 |
| `./gradlew.bat :composeApp:assembleDebug` | 通过 | Debug APK 打包成功；native strip 提示为既有库处理信息。 |
| `adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk` + UI tree/screenshot | 通过 | App 可启动到“创作”页；已验证项目列表、最近创作、项目操作菜单、项目详情弹窗和真实服务端模型摘要。修复了长项目名挤压项目操作按钮的问题，详情弹窗时间已从毫秒值格式化为可读日期时间；底部模型摘要已显示 `全能图片G-2.0-文生图-官方版` 和 `4 个参数`。 |

## 未完成与风险

| 风险 | 影响 | 下一步 |
| --- | --- | --- |
| App 内尚未实测 v2 真实扣费生成 | 不能宣称图片 MVP 端到端完成 | 用已登录账号在真机/模拟器提交一次低成本图片任务，确认余额、任务结果和输出展示。 |
| 服务端字段尚未完全覆盖所有 fieldType | 基础 options、文本/数值字段和图片/视频/音频上传字段已进入请求结构，但条件字段和复杂 `skuInputExtraJson` 仍未完整动态化 | 下一步覆盖条件字段和复杂 `skuInputExtraJson`，并用更多服务端模型验证字段映射。 |
| 模板详情和制作同款仍需增强 | 已能按真实 `template/detail` 回填分类、服务端模型、prompt、基础参数和远端素材，但复杂模板字段和后续视频 v2 提交消费仍需继续验证 | 继续补图片模板、复杂多输入模板和视频 v2 提交流程的端到端测试。 |
| 历史能力仍不完整 | 已能在创作页展示最近创作、项目横向筛选、项目内任务、项目置顶切换、项目创建/重命名/删除/详情、加载更多历史、定时刷新非终态任务、取消非终态任务并打开详情弹窗；项目管理端点已用 Chrome DevTools 临时项目验证，App 内已验证项目列表、菜单和详情只读流程 | 下一步在用户确认可改动数据后，验证项目创建、筛选、置顶、重命名和删除完整流程。 |
| 视频 v2 仍缺少真实扣费复测 | 当前仅用抓包结构和 MockEngine 验证请求构造、路由和结果解析；不能宣称视频端到端完成 | 需要用户明确授权后，用低成本视频模型在 App 内提交一次真实任务；此前抓包的 Seedance2.0 模板预估价格为 9.60 元，不在既有 0.76 元授权范围内。 |
| `AuthRepositoryImpl.kt` 有既有未提交修改 | 本轮未审查，可能影响登录态 | 后续提交时不要误包含，除非确认是本任务需要。 |

## 2026-06-18 App 端补充验证

本轮使用 `adb -s emulator-5554` 重新拉起 `com.runninghub.app/.MainActivity`，从底部导航进入“创作”页，并用 UI tree 而不是截图坐标定位关键控件。验证结果：

- 创作页顶部结构符合当前截图理解：顶部“创作/灵感”切换，中间历史/项目内容区，底部固定模型参数与 prompt 输入区。
- 底部模型摘要已加载真实服务端模型：`全能图片G-2.0-文生图-官方版`，副标题为 `全能图片G-2.0-官方版 · 4 个参数`。
- 最近创作列表能显示真实成功任务：prompt 为“测试生成一张极简风格的绿色圆形图标，纯白背景，中心是 RunningHub 风格的绿色圆形符号”，状态为 `IMAGE · SUCCESS · PNG`，费用显示 `0.8 CNY`。
- 点击最近创作项后，详情弹窗能显示输出预览区域、prompt、`IMAGE · SUCCESS · PNG · 2048x1152` 和 `0.8 CNY`。
- 为避免超过用户已授权的 0.76 元扣费范围，本轮没有再次点击“生成”提交新的真实扣费任务；App 端新增验证范围限定为“读取并展示已成功扣费任务的历史和详情”。后续若要验证“从 App 再次发起 prepare/commit 并扣费”，需要用户重新确认一次扣费授权。

本轮证据文件保存在未跟踪目录 `output/`，包括：

- `output/quickcreate_app_create_before_generation.xml`
- `output/quickcreate_app_create_before_generation.png`
- `output/quickcreate_app_history_detail_success.xml`
- `output/quickcreate_app_history_detail_success.png`

## 2026-06-18 费用显示精度修复

模拟器验证发现最近创作与详情弹窗把真实 `0.76 CNY` 显示成 `0.8 CNY`，原因是历史 UI 复用了通用 `formatOneDecimal`。本轮已新增现金金额专用格式化：

- `composeApp/src/commonMain/kotlin/com/runninghub/app/util/NumberFormat.kt` 新增 `formatCashAmount`，现金金额固定显示两位小数。
- `QuickCreateScreen.kt` 历史列表和详情弹窗费用展示改为使用 `formatCashAmount`。
- `composeApp/src/commonTest/kotlin/com/runninghub/app/util/NumberFormatTest.kt` 覆盖 `0.76 -> 0.76`、`9.6 -> 9.60`、`1.0 -> 1.00`。

已验证：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.util.NumberFormatTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
```

代码提交：`ac9d40b fix(quickcreate): preserve cash amount precision`。

## 2026-06-18 生成按钮费用精度补齐

同类审计发现底部“生成”按钮仍使用 `formatOneDecimal(cost)`，当估算价或服务端价格为 `0.76` 时会显示为 `¥0.8`。本轮已将 `SendButton` 的费用文本改为 `¥${formatCashAmount(cost)}`，与历史列表、详情弹窗保持一致。

已验证：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.util.NumberFormatTest"
```

代码提交：`363048f fix(quickcreate): format send button cost as cash`。

## 2026-06-18 服务端模型 pricing 元数据保留

审计发现 `/api/qc/v2/models` 真实响应中的 `pricing` 元数据此前没有进入 DTO/domain，导致后续无法基于服务端模型的计费模式做更准确的价格展示或实时 `fee-preview` UI。真实响应字段包括 `pricingMode`、`settlementMode`、`paidPriceKind`、`flatPrice`、`dimensionPricing`、`discountPercent`、`isFree`、`freeRemaining`、`isTimeFree` 和 `promoType`。

本轮已完成：

- `QuickCreationModelDto` 新增 `pricing: QuickCreationPricingDto?`。
- `QuickCreationServiceModel` 新增 `pricing: QuickCreationServicePricing?`。
- `QuickCreationModelMapper` 将服务端 pricing 元数据映射到 domain，并保留 `flatPrice/dimensionPricing` 原始 JSON 字符串。
- 新增/补充 DTO 与 mapper 单测，锁定 pricing 解析和映射。

已验证：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.remote.dto.QuickCreationModelDtoTest" --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
```

代码提交：`be11bf9 fix(quickcreate): keep service model pricing metadata`。

仍未完成：底部生成按钮的价格来源仍是当前 UI 配置的本地 `estimatedCost`；完整改为服务端实时价格需要新增 repository 级 `fee-preview` 方法、ScreenModel 价格刷新状态和请求节流。

## 2026-06-18 repository 级图片 fee-preview 能力

本轮已把 Web quick-creation v2 的 `/task/quick-creation/fee-preview` 从内部提交流程中抽出为 repository 公开能力，作为后续“底部生成按钮价格来源改为服务端实时价格”的基础。

- `QuickCreateRepository` 新增 `previewImageQuickCreationFee(request)`。
- 新增 domain 模型 `QuickCreationFeePreview`，保留 `passed/free/settlementMode/requiredRhAmount/requiredCashAmount/userCashBalance/insufficientType/cashCurrency`。
- `QuickCreateRepositoryImpl` 复用 `QuickCreationV2Defaults.imageG2CreateRequest(request)` 构造真实 v2 `createRequest`，只调用 `fee-preview`，不触发 `prepare/commit`，不会扣费。
- 新增 `QuickCreateRepositoryImplFeePreviewTest`，用 MockEngine 锁定请求路径为 `QuickCreateApi.QC_FEE_PREVIEW`，并验证真实抓包中的 `0.76 CNY`、余额和结算模式字段能进入 domain。
- `QuickCreateScreenModelTest.FakeQuickCreateRepository` 已补齐新接口，避免 repository contract 扩展破坏现有 Compose 测试。

已验证：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreateRepositoryImplFeePreviewTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest
```

代码提交：`4cd380a fix(quickcreate): expose image fee preview`。

仍未完成：ScreenModel 还没有把底部按钮价格刷新改成服务端 fee-preview。下一步应在 UI state 中增加价格预览加载/失败/最新值状态，基于当前图片模型、prompt、服务端字段参数和上传 URL debounce 调用 `previewImageQuickCreationFee`，成功后用 `requiredCashAmount` 替换本地 `estimatedCost` 展示；失败时显示“价格待确认”或保留明确的不可用状态，避免把本地估算误认为最终扣费。

## 2026-06-18 ScreenModel 接入图片实时 fee-preview

本轮已把图片创作底部按钮价格从纯本地估算推进为服务端 fee-preview 驱动：

- `QuickCreateUiState` 新增 `feePreviewLoading/feePreviewError`。
- `QuickCreateScreenModel` 在图片 prompt、图片模型、服务端模型、服务端字段参数、比例、分辨率、质量、数量、seed、图片素材上传完成和移除素材后调度价格刷新。
- 刷新使用 500ms debounce，调用 `QuickCreateRepository.previewImageQuickCreationFee`，成功后用 `requiredCashAmount` 写回 `estimatedCost`；免费任务写回 0，现金金额缺失时回退 `requiredRhAmount`。
- 图片提交与 fee-preview 现在复用同一个 `buildImageGenerationRequest`，避免预览价格与实际提交参数不一致。
- 修复 `autoSaveDraft()` 依赖生成 serializer 的问题，改为显式 `JsonObject` 读写 `currentTab/imagePrompt/videoPrompt`，避免输入 500ms 后触发 `DraftData` serializer 异常。
- 新增 `QuickCreateScreenModelTest.image prompt refreshes server fee preview into estimated cost`，锁定 prompt 变化后会用当前服务端模型 ID 请求 fee-preview，并把 `0.76` 写回 UI 价格。

已验证：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image prompt refreshes server fee preview into estimated cost"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
```

代码提交：`650e13d fix(quickcreate): refresh image price from fee preview`。

仍未完成：UI 还没有显式展示 `feePreviewLoading/feePreviewError` 文案，当前按钮金额会在预览成功后更新；后续应在底部输入区增加“价格确认中/价格待确认”的轻量状态，避免弱网或服务端失败时用户误解当前金额。

## 2026-06-18 底部按钮展示 fee-preview 状态

本轮已把图片 fee-preview 的 loading/error 状态接到底部生成按钮：

- 新增 `quickCreateSendButtonLabel`，统一输出 `价格确认中`、`价格待确认`、`¥0.76` 或 `生成`。
- `BottomPromptPanel` 在图片 tab 下把 `feePreviewLoading/feePreviewError` 传入 `SendButton`。
- 图片价格确认中时，生成按钮临时禁用，避免用户在服务端价格尚未确认时提交。
- fee-preview 失败时按钮显示 `价格待确认`，不再继续显示可能被误认为最终扣费价的旧金额。
- 新增 `QuickCreateBillingUiTextTest` 覆盖按钮文案优先级。

已验证：

```powershell
.\gradlew.bat --stop
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:assembleDebug
```

并行运行 `testDebugUnitTest` 和 `assembleDebug` 曾触发 Kotlin incremental cache 竞争，表现为 `Storage ... already registered`。停止 Gradle daemon 后串行重跑两条命令均通过。后续在同一工作区做验证时，避免并行跑会写同一 Kotlin cache 的 Gradle 任务。

代码提交：`29d2ee2 fix(quickcreate): show fee preview status on send button`。

仍未完成：视频 tab 仍未接入服务端 fee-preview 价格刷新；真实 App UI 还需要在模拟器上观察输入 prompt 后按钮状态从“价格确认中”更新为服务端金额。

## 2026-06-18 模拟器验证图片 fee-preview 按钮状态

本轮使用 `emulator-5554` 对快捷创作图片 tab 做了不扣费 UI 验证：

- 构建并覆盖安装 `composeApp-debug.apk`，保留登录态和本地数据。
- 从底部导航进入“创作”页，页面符合截图结构：顶部“创作 | 灵感”，中间项目/最近创作滚动区，底部固定模型参数与 prompt 输入区。
- 初始空 prompt 时，按钮显示本地估算 `¥0.93`。
- 输入 `green%20minimal%20icon` 后，按钮立即显示 `价格确认中`。
- 等待约 4 秒后，按钮变为 `生成`，没有回到旧的本地估算价。
- 再清空并输入 `greenicon` 复测，按钮同样先显示 `价格确认中`，约 4 秒后变为 `生成`。
- 未点击生成按钮，未触发 `prepare/commit`，不会产生新扣费。

当前结论：底部按钮已经真实消费 `feePreviewLoading` 状态；本次账号/模型/prompt 的 fee-preview 最终返回零金额或免费态，因此最终按钮文案为 `生成`，没有出现 `¥0.76`。这验证了“不会把旧本地估算价伪装成最终价”，但尚未验证“服务端返回非零现金金额时按钮显示该金额”的真实 App UI 场景。

证据文件保存在未跟踪目录 `output/`：

- `quickcreate_fee_preview_create_initial_pulled.png`
- `quickcreate_fee_preview_after_type_fast.png`
- `quickcreate_fee_preview_after_wait.png`
- `quickcreate_fee_preview_greenicon_fast.png`
- `quickcreate_fee_preview_greenicon_wait.png`

已验证命令：

```powershell
.\gradlew.bat :composeApp:assembleDebug
adb -s emulator-5554 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
```

仍未完成：真实 App UI 中非零 fee-preview 金额显示仍需要用一个服务端返回非零金额的 prompt/model 状态验证；视频 tab fee-preview 仍未接入。
## 2026-06-18 视频 fee-preview 接入底部生成按钮

本轮把视频 tab 的底部价格刷新也接入服务端 `fee-preview`，代码提交 `374995b fix(quickcreate): refresh video price from fee preview` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateRepository` 新增 `previewVideoQuickCreationFee(VideoGenerationRequest)`，`QuickCreateRepositoryImpl` 复用 `QuickCreationV2Defaults.videoCreateRequest(request)` 调用 `/task/quick-creation/fee-preview`，只做价格预览，不触发 `prepare/commit`，不会扣费。
- `QuickCreateScreenModel` 将原图片专用的价格刷新扩展为当前 tab 通用调度；视频 prompt、服务端模型、服务端字段、视频模型、比例、分辨率、时长、数量、seed、真实模式、生成音频、素材上传完成和素材移除都会触发 500ms debounce 后的 fee-preview。
- 视频预览和正式提交复用同一个 `buildVideoGenerationRequest`，避免预览参数和提交参数不一致。
- `QuickCreateScreen` 底部生成按钮现在在图片和视频 tab 都消费 `feePreviewLoading/feePreviewError`，视频也会显示“价格确认中/价格待确认”，并在价格确认中临时禁用按钮。
- 新增 repository 和 ScreenModel 测试，锁定视频 fee-preview endpoint、`9.60 CNY` 映射、视频服务端模型 ID 和 prompt 透传。

已验证：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.video prompt refreshes server fee preview into estimated cost"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreateRepositoryImplFeePreviewTest"
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :shared:testDebugUnitTest
git diff --check
```

仍未完成：
- 本轮未在真实 App UI 中切到视频 tab 验证“价格确认中 -> ¥9.60/生成”的真实显示链路。
- 未点击视频“生成”，没有触发视频 `prepare/commit`，也没有发生新扣费。此前抓包里的 Seedance2.0 视频预览金额约为 `9.60 CNY`，超出用户此前授权的 `0.76 CNY` 范围；后续如果要做真实视频提交，必须先重新取得明确扣费授权。
- `AuthRepositoryImpl.kt` 仍有既有未提交改动，`output/` 仍为未跟踪证据目录；本轮提交未包含它们。

## 2026-06-18 价格预览不确定时禁止提交

本轮补强了快捷创作生成入口的扣费安全边界，代码提交 `075d749 fix(quickcreate): block generation during fee preview uncertainty` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateScreenModel.generate()` 在 `feePreviewLoading=true` 时直接回到 `IDLE` 并提示 `价格确认中`，不会进入图片或视频正式生成请求。
- `QuickCreateScreenModel.generate()` 在 `feePreviewError != null` 时直接回到 `IDLE` 并提示 `价格待确认`，不会把本地估算价或旧状态当成可提交依据。
- 图片和视频两条链路都加了回归测试，覆盖“价格确认中”和“价格待确认”时不会调用 repository 的正式 `generateImage/generateVideo`。
- 原有生成请求测试已调整为先等待 500ms fee-preview debounce 完成，再执行正式生成，更贴近真实 UI 的提交前置条件。

已验证：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked while fee preview is loading" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked while fee preview is loading"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
git diff --check
```

仍未完成：
- 真机/模拟器还需要复测图片和视频 tab 在真实 UI 中的“价格确认中/价格待确认”禁提交体验。
- 本轮没有点击“生成”，没有触发新的 `prepare/commit`，也没有发生扣费。
- `AuthRepositoryImpl.kt` 和 `output/` 仍保持未纳入提交状态。

## 2026-06-18 模拟器复测 fee-preview 禁提交边界

本轮在 `emulator-5554` 上安装最新 `composeApp-debug.apk`，保留登录态，做了不扣费 UI 复测。验证命令：

```powershell
.\gradlew.bat :composeApp:assembleDebug
adb -s emulator-5554 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
```

验证结果：
- 图片 tab 初始按钮仍显示本地估算 `¥0.93`；输入 `guardimage` 后，最终按钮回到 `生成`，点击按钮只出现 `余额不足或价格预览未通过`，没有出现新任务提交、排队或运行状态。
- 图片侧本次点击发生在 fee-preview 未通过/余额不足状态下，未触发新的 `prepare/commit` 扣费任务；最近创作列表仍只显示既有 `IMAGE · SUCCESS · PNG · 0.76 CNY` 历史项。
- 视频 tab 能加载真实服务端模型 `Seedance2.0 · 12 个参数`；输入 `guardvideo` 后最终按钮显示 `¥6.00`。本轮没有点击视频生成按钮，因此没有触发视频扣费。
- 未能稳定截获视频按钮的瞬时 `价格确认中` 文案；当前证据证明视频 tab 真实模型、prompt 输入和非零价格显示可达，但不证明视频 loading 文案在真实 UI 中可见。

证据文件保存在未跟踪目录 `output/`：
- `quickcreate_submit_guard_create_initial.xml/png`
- `quickcreate_submit_guard_image_loading.xml/png`
- `quickcreate_submit_guard_image_after_tap.xml/png`
- `quickcreate_submit_guard_image_after_wait.xml/png`
- `quickcreate_submit_guard_video_initial.xml/png`
- `quickcreate_submit_guard_video_fast.xml/png`
- `quickcreate_submit_guard_video_wait.xml/png`

仍未完成：
- 视频真实生成的 `prepare/commit/list/detail` 端到端链路仍未验证；需要用户重新授权扣费后才能点击生成。
- 视频 `价格确认中` 的瞬时 UI 文案还缺少稳定截图证据。
- `AuthRepositoryImpl.kt` 和 `output/` 仍保持未纳入提交状态。

## 2026-06-18 服务字段 extra metadata 结构化解析

本轮围绕截图底部“模型参数和提示词输入区域”的动态化继续收窄服务端字段矩阵，代码提交 `bcdf91a fix(quickcreate): parse service field extra metadata` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceField` 新增 `inputExtra: QuickCreationServiceFieldExtra?`，在继续保留原始 `inputExtraJson` 的同时，结构化承载 `title/titleEn/paramDesc/paramDescEn/placeholder/accept/maxLength/minLength/maxInputCount/ignoreListValueCaseSensitive`。
- `QuickCreationModelMapper` 兼容服务端真实形态：`skuInputExtraJson` 可以是 JSON 对象，也可以是内容为 JSON 对象的字符串；`accept` 兼容 JSON 数组字符串和逗号分隔字符串。
- `TuneBottomSheet` 的服务端高级参数区开始使用 `inputExtra.title` 作为字段标题、`paramDesc` 作为说明、`placeholder` 作为文本输入占位符，并在上传字段提示中展示 `accept` 格式。
- 新增 mapper 回归测试，锁定 extra metadata 的解析行为，避免后续动态表单退回只显示 `fieldKey/paramKey`。

已验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 条件字段、字段联动、`inputsChildList` 等复杂 `skuInputExtraJson` 结构尚未渲染为完整动态表单。
- 本轮没有点击真实“生成”，没有触发新的 `prepare/commit`，也没有产生新扣费。
- `AuthRepositoryImpl.kt` 仍是既有未提交改动，`output/` 仍是未跟踪证据目录，本轮代码和文档提交均不纳入它们。

## 2026-06-18 媒体字段高级参数渲染修复

代码提交 `98d7e90 fix(quickcreate): render media service fields in tune panel` 已推送到 `feature/kmp-refactoring`。

已完成：
- 抽出 `QuickCreationServiceFieldUiModel.kt`，把服务端字段的文本输入、上传字段、标题、占位符、上传提示和可渲染判断集中为可单测 helper。
- 修复 Tune 高级参数区只识别 `UPLOAD` 的问题；真实服务端常见的 `fieldType=IMAGE/VIDEO/AUDIO` 现在也会作为上传类字段渲染，不再被 `.filter { ... }` 过滤掉。
- 上传提示优先使用 `inputExtra.acceptFormats` 和 `inputExtra.maxInputCount`，再回退到顶层 `maxUploadCount/maxUploadSize`，避免图生图字段把服务端“参考图片 1-4 张”的限制显示成更宽泛的顶层数量。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 当前只修复字段是否可见和提示文案；真正的多输入子项 `inputsChildList`、条件字段联动和字段级校验仍需继续补。
- 本轮未做真实生成，也没有新增扣费。

## 2026-06-18 服务字段 visible 元信息接入

代码提交 `526ab48 fix(quickcreate): respect service field visibility` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationFieldDto` 新增 `visible` 解析，默认 `true`，兼容旧响应和测试夹具。
- `QuickCreationServiceField` 保留 `visible` 到 domain model；mapper 不丢弃 `visible=false` 字段，因此隐藏字段的默认值仍可由 `defaultServiceParams()` 带入提交参数，避免破坏服务端必需的隐藏参数。
- Tune 参数区的 `isQuickCreationServiceFieldRenderable()` 会在 `visible=false` 时返回 false，只影响 UI 展示，不影响请求构造。
- 新增 DTO、mapper、UI helper 回归测试，锁定“隐藏字段不显示，但默认值仍保留”的边界。

已验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.remote.dto.QuickCreationModelDtoTest" --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- `visible` 只是显示层开关；条件字段、字段联动和 `inputsChildList` 子输入仍未完整动态化。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也未产生扣费。

## 2026-06-18 文本字段 maxLength 接入 Tune 输入

代码提交 `4047fbf fix(quickcreate): enforce service text field length` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldUiModel.kt` 新增 `constrainQuickCreationTextInput()` 和 `quickCreationTextLimitCounter()`，消费已解析的 `QuickCreationServiceFieldExtra.maxLength`。
- Tune 高级参数区的文本/数值字段输入会先按服务端 `maxLength` 截断，再写回字段参数，避免超出服务端限制的动态参数进入请求体。
- 有 `maxLength` 的文本字段会在输入框下方显示 `当前长度/最大长度` 计数，便于用户理解参数限制。
- 新增 UI helper 回归测试，覆盖截断和计数器行为。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- `minLength` 当前只保留在 domain 中，尚未用于提交前校验或错误提示。
- 条件字段、字段联动和 `inputsChildList` 子输入仍未完整动态化。
- 本轮未触发真实生成或扣费。

## 2026-06-18 文本字段 required/minLength 提交前校验

代码提交 `5164dc9 fix(quickcreate): validate service text fields before submit` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldUiModel.kt` 新增 `quickCreationTextValidationError()`，对服务端文本字段执行 `required` 非空和 `inputExtra.minLength` 最小长度校验。
- `QuickCreateScreenModel.generate()` 在 fee-preview 状态检查之后、正式提交协程启动之前校验当前 tab 的服务端文本字段；校验失败时回到 `IDLE` 并展示字段级错误，不调用 `generateImage/generateVideo`。
- 校验使用 `serviceParams` 覆盖值并回退 `defaultServiceParams()`，因此隐藏字段默认值仍可通过校验，用户输入不足会被本地拦截。
- 新增 helper 和 ScreenModel 回归测试，覆盖 required/minLength 错误文案和“参数不足不提交正式生成请求”。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when required service text field is too short"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 上传字段 required/maxInputCount、列表字段条件联动和 `inputsChildList` 子输入仍未完整校验。
- 本轮未触发真实生成或扣费。

## 2026-06-18 媒体字段请求参数映射修复

代码提交 `1f0bad6 fix(quickcreate): include media fields in upload params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateScreenModel.uploadFields()` 不再只识别 `fieldType` 包含 `UPLOAD` 的字段，改为复用 `isQuickCreationUploadField()`，覆盖真实服务端常见的 `IMAGE/VIDEO/AUDIO/UPLOAD`。
- 修复了 Tune UI 能展示 `IMAGE` 上传字段，但请求构造阶段忽略该字段、导致上传素材 URL 未写入 `quickCreationListParams` 的不一致。
- 新增回归测试：服务端字段 `fieldType=IMAGE,paramKey=imageUrls` 时，上传图片后生成请求会把远端素材 URL 写入 `quickCreationListParams["imageUrls"]`。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to service image field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 上传字段 required/maxInputCount 的提交前校验仍需继续补齐。
- 本轮未触发真实生成或扣费。

## 2026-06-18 上传字段 required/maxInputCount 提交前校验

代码提交 `233aa91 fix(quickcreate): validate service upload fields before submit` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldUiModel.kt` 新增 `quickCreationUploadValidationError()`，对上传类字段执行 `required` 非空和最大文件数校验。
- 最大文件数优先使用 `inputExtra.maxInputCount`，再回退顶层 `maxUploadCount`，与 Tune 上传提示保持同一优先级。
- `QuickCreateScreenModel.generate()` 在等待挂起上传完成后、正式调用 `generateImage/generateVideo` 前校验当前 tab 的服务端上传字段；校验失败会回到 `IDLE` 并展示字段错误，不触发正式生成请求。
- 上传字段识别继续复用 `isQuickCreationUploadField()` / `uploadFields()`，覆盖真实服务端常见的 `IMAGE/VIDEO/AUDIO/UPLOAD`，避免 UI 展示、请求映射和提交前校验规则分叉。
- 新增 helper 和 ScreenModel 回归测试，覆盖必填上传为空、`inputExtra.maxInputCount` 优先级，以及必填服务端图片字段为空时不提交图片生成请求。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when required service image field has no upload"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 列表字段条件联动、`inputsChildList` 子输入和更复杂的 `skuInputExtraJson` 动态表单仍未完整覆盖。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也未产生扣费。

## 2026-06-18 inputsChildList 子输入元数据解析

代码提交 `ef79c92 fix(quickcreate): parse service input child metadata` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldExtra` 新增 `inputChildren`，开始结构化承载复杂 `skuInputExtraJson.inputsChildList`。
- 新增 `QuickCreationServiceFieldInputChild` 和 `QuickCreationServiceFieldVisibilityCondition` domain 模型，保留子字段 `fieldKey/paramKey/fieldType/required/visible/defaultValue/title/paramDescription/placeholder/options/visibleWhen`。
- `QuickCreationModelMapper` 支持 `inputsChildList/inputChildList/children` 三种子列表键；子列表既可为 JSON array，也可为 JSON array 字符串。
- 子字段 options 支持对象数组和 primitive 数组；简单可见条件支持 `showWhen/visibleWhen/dependsOn`，并解析 `fieldKey` 与 `values/value`。
- 新增 mapper 回归测试，锁定子输入字段、选项和条件可见 metadata 不再只停留在 raw JSON。

已验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest.maps service field input child list metadata"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
git diff --check
```

仍未完成：
- 子输入 metadata 目前已进入 domain，但 Tune UI 尚未渲染 `inputChildren`，正式请求构造也尚未消费子字段值。
- 条件联动目前只保留简单 `visibleWhen` metadata，尚未在 UI 中按父字段值动态显示/隐藏。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也未产生扣费。

## 2026-06-18 激活子输入 Tune 渲染和提交参数透传

代码提交 `ab64160 fix(quickcreate): render active service child fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldUiModel.kt` 新增子输入 helper，支持判断 `QuickCreationServiceFieldInputChild` 是否可渲染、标题/占位符回退，以及按父字段当前值筛选激活子输入。
- `quickCreationActiveInputChildren(params)` 支持无条件子输入默认跟随父字段显示；带 `visibleWhen` 的子输入会按 `params` 或父字段默认值判断是否显示。
- `TuneBottomSheet` 服务端参数区现在会在父字段下缩进渲染当前激活的子输入；子输入支持 options、文本/数值输入和上传提示。
- `QuickCreateScreenModel.hasFieldParam()` 已把子输入 `paramKey` 纳入服务端参数白名单；用户在 Tune 中填写的子字段值不会再被正式提交参数过滤掉。
- 新增 helper 和 ScreenModel 回归测试，覆盖父字段激活条件和子字段值进入 `quickCreationParams`。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active child inputs follow parent selection metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image submits declared child service field values"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 子输入 required/minLength/maxLength/maxInputCount 的字段级校验还没有完整接入提交前防线。
- 当前 Tune UI 只在图片高级参数区渲染服务端模型和子输入；视频高级参数区仍需单独接服务端模型参数 UI。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也未产生扣费。

## 2026-06-18 激活子输入文本提交前校验

代码提交 `56f0639 fix(quickcreate): validate active child text fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldInputChild` 新增 `maxLength/minLength`，mapper 会从子输入 JSON 或子输入内嵌 `skuInputExtraJson` 解析文本长度限制。
- `QuickCreationServiceFieldUiModel.kt` 新增子输入文本校验 helper，覆盖 `required` 非空和 `minLength` 最小长度错误文案。
- `QuickCreateScreenModel.validateServiceFields()` 现在会校验当前激活的子文本字段；校验失败时回到 `IDLE`，不进入正式 `generateImage/generateVideo`。
- 未激活的 required 子输入不会阻止生成，避免条件字段在不可见状态下误拦截。
- 新增 mapper、helper 和 ScreenModel 回归测试，覆盖子输入长度 metadata、子输入校验文案、激活子输入拦截和未激活子输入放行。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.required child text validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child min length validation uses child metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active required child text field is empty" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.inactive required child text field does not block image generation"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest
git diff --check
```

仍未完成：
- 子输入 `maxLength` 目前已解析到 domain，但 Tune 子输入输入框尚未按 `maxLength` 截断或显示计数。
- 上传类子输入 required/maxInputCount 仍未接入提交前防线。
- 当前 Tune UI 仍只在图片高级参数区渲染服务端模型和子输入；视频高级参数区仍需单独接服务端模型参数 UI。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也未产生扣费。

## 2026-06-18 子输入 maxLength 截断与计数

代码提交 `ef7eab1 fix(quickcreate): enforce child text max length` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldInputChild` 现在复用与顶层文本字段一致的输入长度处理方式：当子输入携带 `maxLength` 时，Tune 子输入文本框会先截断到最大长度，再写回 `serviceParams`。
- 子输入文本框会在输入框下方展示 `当前长度/最大长度` 计数，避免用户在复杂 `inputsChildList` 字段里输入超过服务端限制的参数。
- 新增 UI helper 回归测试，覆盖子输入截断和计数器行为。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child text input is constrained by max length metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child text limit counter uses max length metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 上传类子输入的 required/maxInputCount 校验还没有接入提交前防线。
- 视频高级参数区仍未复用图片端的服务端模型字段 UI。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费；视频真实端到端仍需要新的明确扣费授权。

## 2026-06-18 子上传字段映射与提交前校验

代码提交 `0a27fe4 fix(quickcreate): validate child upload fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreationServiceFieldInputChild` 新增 `maxInputCount`，mapper 会从子输入对象或子输入内嵌 `skuInputExtraJson.maxInputCount` 解析该限制。
- `QuickCreationServiceFieldUiModel.kt` 新增子上传字段校验 helper，支持 active child upload 的 required 非空和 `maxInputCount` 最大数量校验。
- `QuickCreateScreenModel` 的 `quickCreationListParams` 现在会把当前激活的子上传字段写入请求列表参数；字段媒体类型按子字段 `fieldType/fieldKey/paramKey` 中的 IMAGE/VIDEO/AUDIO 标记识别。
- 提交前上传校验现在会先校验顶层上传字段，再校验当前激活的子上传字段；未激活子上传字段不会阻断生成。

已验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest.maps service field input child list metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.required child upload validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child upload max count validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to active child upload field" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active required child image field has no upload"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 当前子上传字段仍复用页面已有素材入口，尚未区分“顶层上传”和“某个子字段专属上传入口”；复杂模型如果需要多个独立上传槽，仍需继续扩展 UI 状态结构。
- 视频高级参数区仍未复用图片端服务端模型字段 UI。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 隐藏子字段参数提交过滤

代码提交 `954e2f8 fix(quickcreate): skip inactive child params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateScreenModel` 的正式 `quickCreationParams` 组装不再使用“所有声明过的子字段”作为白名单，而是改为“顶层字段 + 当前激活子字段”。
- 当用户曾经填写过条件子字段，随后父字段切换导致该子字段隐藏时，隐藏子字段的旧值不会再进入图片或视频正式提交参数。
- 保留 `updateImageServiceParam/updateVideoServiceParam` 对声明子字段的写入能力，避免 Tune UI 或模板回填阶段因为字段暂未激活而丢失用户输入；过滤只发生在正式请求组装阶段。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.inactive child service field value is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 视频高级参数区仍未复用图片端服务端模型字段 UI。
- 多个独立上传子槽仍需要后续扩展素材与 `paramKey` 的绑定结构。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 视频 Tune 服务端字段渲染

代码提交 `aea0b07 fix(quickcreate): show video service fields in tune panel` 已推送到 `feature/kmp-refactoring`。

已完成：
- `TuneBottomSheet` 新增视频服务端模型选择和服务端参数变更回调，并在 `QuickCreateScreen` 中接入 `updateVideoServiceModel/updateVideoServiceParam`。
- 视频高级参数区现在会展示 `serviceVideoModels`、当前选中的 `selectedVideoServiceModel`，并复用已有 `ServiceFieldOptionsContent` 渲染服务端字段、子输入、文本长度限制和上传提示。
- 视频高级参数区改为可滚动，避免服务端字段、子输入和原有真人模式/生成音频/时长/Seed 控件同时出现时被底部裁切。

已验证命令：

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

仍未完成：
- 本轮只完成 UI 接线和编译/单测验证，尚未在真实设备上打开视频 Tune 高级参数区做视觉截图复测。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。
- 多个独立上传子槽仍需要后续扩展素材与 `paramKey` 的绑定结构。

## 2026-06-18 视频 Tune 高级参数真实 UI 复测

本记录补充 `aea0b07 fix(quickcreate): show video service fields in tune panel` 的模拟器视觉复测结果。

已完成：
- 使用最新 `composeApp-debug.apk` 安装并启动 `emulator-5554`，进入底部 `创作` tab，再切到视频模型 `Seedance2.0`。
- 打开 `创作调优` 弹层并切换到 `高级` 页，真实 UI 树显示 `服务端模型`、`Seedance2.0`、`Seedance2.0-首尾帧`、`Seedance2.0-Fast` 等视频服务端模型。
- 在高级页内连续滚动后，确认服务模型列表底部可以访问字段区，UI 树显示 `是否返回视频尾帧图片`、`是（支持真人模式）`、`否`、`真人模式`、`生成音频`、`时长`、`5秒`、`10秒`、`Seed（留空为随机）`。
- 保存证据文件到本地 `output/`：`quickcreate_video_tune_advanced.xml`、`quickcreate_video_tune_advanced_scrolled_10.xml`、`quickcreate_video_tune_advanced_fields.png`。`output/` 仍为未跟踪证据目录，不纳入提交。

已验证命令：

```powershell
.\gradlew.bat :composeApp:assembleDebug
adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_video_tune_advanced.xml
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_video_tune_advanced_scrolled_10.xml
adb -s emulator-5554 shell screencap -p /sdcard/quickcreate_video_tune_advanced_fields.png
adb -s emulator-5554 pull /sdcard/quickcreate_video_tune_advanced_fields.png output\quickcreate_video_tune_advanced_fields.png
```

仍未完成：
- 高级页服务端模型列表较长，字段区需要向下滚动多屏才能看到；后续可考虑将服务模型选择器收敛为下拉/横向选择/折叠区，降低查找字段成本。
- 本轮只做 UI 打开、滚动和字段可见性复测，没有点击真实生成，没有触发新的 `prepare/commit`，也没有新增扣费。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。

## 2026-06-18 紧凑服务端模型选择器

代码提交 `fd77866 fix(quickcreate): compact service model picker` 已推送到 `feature/kmp-refactoring`。

已完成：
- 将图片/视频 Tune 高级参数区重复的服务端模型卡片列表抽成共用 `ServiceModelPickerContent`。
- 服务端模型入口从“全量纵向列表”改为“当前模型摘要 + 下拉菜单”，选中模型的字段区现在紧跟在选择器下方。
- 保留加载态、空态、当前选中态、模型分组名和参数数量提示；下拉菜单中仍可切换服务端模型。
- 修正未选中服务模型时的显示语义：不再把第一项伪装成已选，而是显示 `请选择服务端模型`。

已验证命令：

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:assembleDebug
adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_service_picker_advanced.xml
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_service_picker_dropdown.xml
adb -s emulator-5554 shell screencap -p /sdcard/quickcreate_service_picker_dropdown.png
adb -s emulator-5554 pull /sdcard/quickcreate_service_picker_dropdown.png output\quickcreate_service_picker_dropdown.png
git diff --check -- composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\quickcreate\TuneBottomSheet.kt
```

UI 复测结果：
- 视频 `Seedance2.0` 的高级页现在直接显示 `服务端模型`、`Seedance2.0`、`服务端参数`、`prompt`、`视频生成提示词`，不需要先滚过多屏服务端模型。
- 点击模型摘要后，下拉菜单显示 `Seedance2.0`、`Seedance2.0-首尾帧`、`Seedance2.0-Fast`、`全能视频X 1.5-图生视频-官方版` 等模型，切换入口仍可用。
- 本轮未点击真实生成，未触发新的 `prepare/commit`，也没有新增扣费。

仍未完成：
- 多个独立上传子槽仍需要后续扩展素材与 `paramKey` 的绑定结构。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。

## 2026-06-18 字段级上传素材绑定

代码提交 `76b6de3 fix(quickcreate): bind uploads to service fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `MediaReference` 新增 `fieldParamKey`，可以区分全局参考素材和某个服务端字段/子字段的专属素材。
- `QuickCreateScreenModel` 新增 `pickImageReferenceForField/pickVideoReferenceForField/pickAudioReferenceForField`，系统 picker 返回的 URI 可绑定到指定 `paramKey`。
- `quickCreationListParams` 组装时优先使用匹配字段 `paramKey` 的素材；如果没有字段级素材，继续兼容旧的全局素材入口。
- 上传校验同样优先按字段 `paramKey` 计数，避免多个同媒体类型子槽互相借用素材数量。
- `QuickCreationServiceFieldUiModel` 抽出服务端上传字段的媒体类型识别 helper，供 ScreenModel 和 Tune UI 共用。
- Tune 高级参数区的服务端上传字段现在显示专属上传按钮和字段级上传计数，点击后按 IMAGE/VIDEO/AUDIO 打开对应系统 picker，并把结果绑定回字段 `paramKey`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps field bound images to matching child upload fields"
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:assembleDebug
git diff --check
```

UI 复测结果：
- 安装最新 APK 到 `emulator-5554` 后进入 `创作 -> 图片 -> 创作调优 -> 高级`。
- 切换服务端模型为 `全能图片G-2.0-图生图-官方版`，滚动到 `imageUrls` 字段，UI 树显示 `参考图片（1-4张）`、上传限制提示和 `选择图片` 专属按钮。
- 证据文件保存在 `output/quickcreate_field_upload_image_i2i.xml`、`output/quickcreate_field_upload_image_i2i_scrolled.xml`、`output/quickcreate_field_upload_image_i2i_scrolled.png`，不纳入 Git。
- 本轮没有点击真实生成，没有触发新的 `prepare/commit`，也没有新增扣费。

仍未完成：
- 真实多独立上传槽模型还需要在后续有明确样例或授权时做完整端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。

## 2026-06-18 字段级上传素材展示与移除

代码提交 `d5b18d6 fix(quickcreate): separate field upload chips` 已推送到 `feature/kmp-refactoring`。

已完成：
- 新增 `quickCreationGlobalMediaReferences()` 和 `quickCreationFieldMediaReferences(paramKey)` helper，并用测试锁定全局素材与字段素材的过滤规则。
- 底部快捷创作面板现在只展示 `fieldParamKey` 为空的全局参考素材，避免字段专属上传混入全局参考区。
- Tune 高级参数区的上传字段会在字段自身区域展示已绑定素材卡片，并复用 `MediaChipCard` 支持移除。
- 字段上传按钮、上传计数和素材卡片都按字段 `paramKey` 过滤，便于多个独立上传槽各自管理素材。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.global media references exclude field bound uploads" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.field media references match only exact param key"
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:assembleDebug
git diff --check
```

UI 复测结果：
- 最新 APK 安装到 `emulator-5554` 后，进入 `创作 -> 图片 -> 创作调优 -> 高级`，切换服务端模型到 `全能图片G-2.0-图生图-官方版`。
- 滚动到 `imageUrls` 字段，UI 树仍显示 `参考图片（1-4张）`、上传限制和 `选择图片` 专属按钮。
- 证据文件保存在 `output/quickcreate_field_upload_remove_i2i_scrolled.xml` 和 `output/quickcreate_field_upload_remove_i2i_scrolled.png`，不纳入 Git。
- 本轮没有选择真实素材、没有点击生成、没有触发新的 `prepare/commit`，也没有新增扣费。

仍未完成：
- 字段级素材卡片的真实移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。

## 2026-06-18 非激活上传字段等待过滤
代码提交 `40844be fix(quickcreate): ignore inactive field uploads while waiting` 已推送到 `feature/kmp-refactoring`。

已完成：
- 新增 `quickCreationActiveUploadParamKeys(serviceParams)`，按当前服务端模型、顶层字段可见性和 active child 规则计算当前真正有效的上传字段 `paramKey`。
- 新增 `quickCreationRelevantMediaReferences(activeFieldParamKeys)`，提交前等待上传时只保留全局素材和当前激活字段绑定素材。
- `QuickCreateScreenModel.awaitPendingUploads()` 现在不再等待隐藏/未激活字段绑定的上传任务，避免用户切换创作模式后，被旧字段的上传状态拖住提交。
- 该过滤只影响提交前等待逻辑；请求参数组装仍沿用已实现的字段级 `quickCreationListParams` 和 legacy reference 隔离规则。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实选择/移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 字段级素材不再污染旧参考字段

代码提交 `00616cc fix(quickcreate): keep field uploads out of legacy refs` 已推送到 `feature/kmp-refactoring`。

已完成：
- `buildImageGenerationRequest()` 现在只用全局素材填充旧的 `referenceImageUri`。
- `buildVideoGenerationRequest()` 现在只用全局素材填充旧的 `referenceImageUri/referenceVideoUri/referenceAudioUri`。
- 字段级素材继续只进入 `quickCreationListParams`，不会同时作为 legacy reference URI 重复提交。
- 在字段级双图片上传回归测试中新增断言：只有 `firstImages/secondImages` 列表参数有 URL，`referenceImageUri` 必须为 `null`。

已验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps field bound images to matching child upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权。

## 2026-06-18 隐藏顶层上传字段过滤

代码提交 `5c8a039 fix(quickcreate): skip hidden upload fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateScreenModel.uploadFields()` 现在只返回可渲染且属于上传类型的顶层服务端字段。
- `visible=false` 的顶层上传字段不再参与 `quickCreationListParams` 组装，字段级绑定到隐藏字段的素材不会被提交。
- `visible=false` 且 required 的顶层上传字段不再参与提交前上传校验，避免服务端隐藏字段误阻断图片生成。
- 新增两个回归测试覆盖隐藏必填上传字段不阻断生成、隐藏上传字段素材不提交。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden required service upload field does not block image generation" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service upload field media is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实选择/移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 隐藏顶层文本字段过滤

代码提交 `51710bf fix(quickcreate): skip hidden text fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `defaultServiceParams()` 现在只读取 `visible=true` 的顶层服务端字段默认值，隐藏字段默认值不会被静默带入正式请求。
- `activeServiceParamKeys(serviceParams)` 现在只允许 `visible=true` 的顶层字段及其 active child 进入提交白名单。
- `validateServiceFields()` 现在只校验 `visible=true` 的顶层文本字段；隐藏 required 文本字段不会误阻断生成。
- 新增三个回归测试覆盖隐藏 required 文本字段不阻断、隐藏手填值不提交、隐藏默认值不提交。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden required service text field does not block image generation" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service text field value is not submitted" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service text field default value is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实选择/移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 隐藏父字段子上传过滤

代码提交 `891ddc6 fix(quickcreate): skip hidden parent uploads` 已推送到 `feature/kmp-refactoring`。

已完成：
- `activeChildUploadFields(serviceParams)` 现在只从 `visible=true` 的父字段下收集 active child 上传字段。
- `quickCreationActiveUploadParamKeys(serviceParams)` 同步过滤隐藏父字段，提交前等待上传时不会把隐藏父字段下的子上传素材视为有效素材。
- 隐藏父字段下的字段级上传素材不会进入 `quickCreationListParams`，也不会参与提交前上传校验或等待。
- 新增回归测试覆盖隐藏父字段子上传 key 不进入 active 集合、隐藏父字段子上传素材不提交。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active upload param keys include visible parent and active child upload fields" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden parent child upload field media is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实选择/移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 非提交参数不刷新价格预览

代码提交 `24a0920 fix(quickcreate): skip preview for inactive params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `updateImageServiceParam()` 和 `updateVideoServiceParam()` 仍允许写入模型声明过的参数，保留模板回填和临时 UI state 能力。
- 写入后只有当 `paramKey` 属于更新后 `activeServiceParamKeys(nextParams)` 时才触发 `scheduleFeePreview()`。
- 隐藏字段、非激活子字段等不会进入正式请求的参数变更，不再额外刷新价格预览，避免对同一请求体重复发起 fee-preview。
- 新增回归测试覆盖 prompt 已预览后更新隐藏服务端参数不会再次请求图片 fee-preview。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service param update does not refresh image fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

仍未完成：
- 字段级素材卡片的真实选择/移除流程仍需在可控测试素材或真机相册环境中做手动端到端验证。
- 视频真实 `prepare/commit/list/detail` 端到端扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 隐藏字段媒体不刷新费用预览

代码提交 `0c8136e fix(quickcreate): skip fee refresh for hidden media` 已推送到 `feature/kmp-refactoring`。

已完成：
- `uploadReference()` 上传完成后不再无条件调用 `scheduleFeePreview()`，而是先确认该媒体仍属于当前正式请求会消费的相关素材。
- `updateReferenceStatus()` 上传进度变化同样复用相关素材判断，隐藏字段或非活跃字段绑定的媒体不会额外触发 fee-preview。
- `removeMediaReference()` 会在移除前判断被删媒体是否属于当前相关素材；只有全局素材或活跃字段素材被移除时才刷新预估费用。
- 新增回归测试覆盖：prompt 已经完成一次图片 fee-preview 后，给 `visible=false` 的上传字段选择媒体，不应再次请求图片 fee-preview。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service upload field media does not refresh image fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 字段级素材选择/移除的真实设备端到端流程仍需在可控测试素材或真机相册环境中复测。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 上传未完成媒体不刷新费用预览

代码提交 `bb9e8af fix(quickcreate): wait for uploaded media before fee refresh` 已推送到 `feature/kmp-refactoring`。

已完成：
- 媒体相关 fee-preview 调度现在要求素材同时满足“属于当前相关素材”和“已上传完成且存在非空 `remoteUrl`”。
- 全局素材或字段级素材在 `UPLOADING/PROCESSING` 阶段不会刷新费用预览，避免请求体尚未包含素材 URL 时重复发起同一份 fee-preview。
- `removeMediaReference()` 也按移除前素材是否真正影响请求体判断；移除尚未上传完成的素材不会触发无效预览。
- 测试仓库新增 `uploadDelayMillis`，用于稳定模拟上传耗时并覆盖“上传中不刷新”的红灯场景。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.uploading global image media does not refresh image fee preview before remote url exists"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测全局/字段级素材的上传中、上传完成、移除状态与 fee-preview 网络请求是否一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 上传任务按创建时 tab 回写

代码提交 `bb291e0 fix(quickcreate): keep uploads bound to original tab` 已推送到 `feature/kmp-refactoring`。

已完成：
- `addMediaReference()` 现在在创建素材时记录 `targetTab`，后续上传任务不再依赖实时 `currentTab` 决定回写位置。
- `uploadReference()`、上传进度更新、上传成功和上传失败都按创建时 tab 更新对应的 `imageConfig` 或 `videoConfig`。
- 修复用户在图片素材上传中切到视频 tab 后，图片素材完成状态无法写回图片配置的问题。
- 新增回归测试覆盖：图片 tab 选择素材后立即切到视频 tab，上传完成后切回图片生成，请求体仍应包含 `referenceImages` URL。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image upload completion updates image config after switching to video tab"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测跨 tab 上传：图片上传中切到视频、视频上传中切到图片、字段级素材上传中切 tab。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 素材删除按 id 跨 tab 生效

代码提交 `457d859 fix(quickcreate): remove media across tabs` 已推送到 `feature/kmp-refactoring`。

已完成：
- `removeMediaReference(id)` 不再只删除当前 tab 的素材，而是同时从 `imageConfig.mediaReferences` 和 `videoConfig.mediaReferences` 中按 id 过滤。
- 修复删除回调到达时用户已切到另一个 tab，原 tab 素材残留并继续进入正式请求体的问题。
- fee-preview 刷新策略保持不变：只有删除前当前 tab 的相关且已上传素材会立即刷新；后台 tab 删除会在用户切回时由 `switchTab()` 触发新的预览。
- 稳定化了“上传中不刷新 fee-preview”的测试，移除不必要的上传延迟，避免受 `Dispatchers.IO` 调度影响。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.remove media reference removes image media after switching to video tab"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测跨 tab 删除：图片素材、视频素材、音频素材、字段级素材的删除回调是否都能清掉正确配置。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 全局素材只回填唯一同类型上传字段

代码提交 `fddd9a3 fix(quickcreate): avoid ambiguous global upload fallback` 已推送到 `feature/kmp-refactoring`。

已完成：
- `quickCreationListParams()` 现在会统计当前模型中活跃上传字段按媒体类型的数量。
- 底部全局素材只在某个媒体类型恰好对应一个活跃上传字段时作为 fallback 回填；如果同类型字段有多个，则必须使用字段级绑定素材。
- `validateServiceUploads()` 使用同一规则，避免一个全局素材让多个 required 同类型上传字段误判通过。
- 保留单上传字段兼容：只有一个图片上传字段时，底部全局图片仍可回填到该字段。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.global image media does not fill multiple service image fields" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to service image field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测多上传字段模型：底部全局素材不应同时填入多个字段，字段级上传应正确落到各自 `paramKey`。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 灵感模板素材保留字段绑定

代码提交 `be77572 fix(quickcreate): bind template media to list params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateInspirationTemplateDetail.templateMediaReferences()` 现在会把 `listParams` 的 key 写入 `MediaReference.fieldParamKey`。
- 灵感模板里的字段级素材不再被当作底部全局素材处理，能沿用字段级 `quickCreationListParams` 提交路径。
- 修复多同类型上传字段模型下，模板 `listParams["imageUrls"]` 因全局 fallback 歧义被丢弃的问题。
- 新增回归测试覆盖：图片模板提供 `imageUrls`，模型同时有 `imageUrls/maskUrls` 两个图片字段，生成请求只提交 `imageUrls`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps list params bound to service fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测灵感模板带字段素材的场景，确认 Tune 字段区域显示和最终请求体一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。

## 2026-06-18 灵感模板素材 id 按字段区分

代码提交 `6c4f6cc fix(quickcreate): make template media ids field specific` 已推送到 `feature/kmp-refactoring`。

已完成：
- `templateMediaReferences()` 生成模板素材 id 时加入 `listParams` 字段 key，例如同一模板内 `imageUrls[0]` 和 `maskUrls[0]` 不再共用 `template_tpl-image_IMAGE_0`。
- 字段 key 会先规整为 ASCII id 片段，避免空格、符号或路径分隔符进入内部素材 id。
- 保持上一轮字段绑定规则不变：模板素材仍写入 `MediaReference.fieldParamKey`，请求体继续按字段级 `quickCreationListParams[paramKey]` 提交。
- 新增回归测试覆盖同媒体类型多字段模板素材 id 唯一性，降低 UI key、删除、状态更新按 id 处理时的歧义风险。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps media ids unique per list param field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真实设备上仍需复测多字段灵感模板：同类型多个模板素材卡片是否能在 Tune 对应字段区域独立显示、删除和提交。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 灵感模板素材 id 规整碰撞收口

代码提交 `e1d611d fix(quickcreate): prevent sanitized template media id collisions` 已推送到 `feature/kmp-refactoring`。

已完成：
- `templateMediaReferences()` 现在把 `listParams` 字段顺序写入模板素材 id，格式包含 `templateId + fieldIndex + sanitizedKey + mediaType + itemIndex`。
- 即使服务端字段 key 例如 `image-urls` 与 `image_urls` 在 ASCII 规整后都变成 `image_urls`，同一模板内的素材 id 仍保持唯一。
- 字段级绑定不变：`MediaReference.fieldParamKey` 仍保留服务端原始 key，请求体继续按原始 `paramKey` 进入 `quickCreationListParams`。
- 新增回归测试覆盖规整后 key 碰撞的场景，避免后续 UI key、删除、状态更新依赖 id 时再次出现歧义。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps media ids unique when field keys sanitize equally"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测服务端真实模板字段 key 中包含符号、下划线或非 ASCII 字符时，Tune 字段卡片显示、删除和最终请求体是否保持一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 视频模板未声明 listParams 保持全局参考

代码提交 `9f1013a fix(quickcreate): keep undeclared template media global` 已推送到 `feature/kmp-refactoring`。

已完成：
- 应用灵感模板时，会先用选中的服务模型和模板 `params` 计算 active 上传字段 key。
- `templateMediaReferences()` 只在 `listParams` key 命中当前模型 active 上传字段时写入 `MediaReference.fieldParamKey`。
- 未被当前模型声明的模板素材不再被错误绑定为字段素材，而是保留为空 `fieldParamKey` 的全局素材。
- 修复视频模板 `listParams["imageUrls"]` 在当前视频模型没有 `imageUrls` 字段时，从生成请求中丢失的问题；现在会作为 `referenceImageUri` 提交。
- 新增回归测试覆盖：应用视频灵感模板后直接生成，未声明的图片模板素材进入 legacy 全局参考图，且不污染 `quickCreationListParams["imageUrls"]`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template keeps undeclared image list params as global reference"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测视频灵感模板里图片参考、视频参考、音频参考三类素材：声明字段的进入 Tune 字段区，未声明字段的进入底部全局参考并进入 legacy 请求字段。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 inactive child 模板素材不再回退为全局素材

代码提交 `c59466b fix(quickcreate): ignore inactive template upload media` 已推送到 `feature/kmp-refactoring`。

已完成：
- 模板素材分类现在区分三类 key：当前 active 上传字段、当前模型声明但未激活的上传字段、当前模型未声明字段。
- active 字段继续写入 `MediaReference.fieldParamKey`，后续进入字段级 `quickCreationListParams`。
- 当前模型未声明字段继续保持全局素材，用于兼容视频模板 `imageUrls` 这类 legacy 参考图场景。
- 当前模型声明但未激活的上传字段会被忽略，不再回退为全局素材，避免 inactive child upload 的模板 URL 被误提交到 `referenceImageUri`。
- 新增回归测试覆盖：图片模板返回 `childImages`，但 `creationMode=text` 未激活该 child 时，生成请求既不带 `referenceImageUri`，也不带 `quickCreationListParams["childImages"]`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template does not submit inactive child upload media as global reference"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测带条件子上传字段的灵感模板：切换父字段后，素材是否只在 active child 区域显示和提交。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 模板 listParams 支持 fieldKey 映射到 paramKey

代码提交 `0cc985d fix(quickcreate): map template field keys to upload params` 已推送到 `feature/kmp-refactoring`。

已完成：
- 模板素材匹配上传字段时，不再只按 `paramKey` 精确匹配；现在为当前模型的上传字段建立别名表：`fieldKey -> paramKey` 和 `paramKey -> paramKey`。
- active 上传字段命中任一别名时，`MediaReference.fieldParamKey` 写入 canonical `paramKey`，后续请求体仍按服务端要求的 `quickCreationListParams[paramKey]` 提交。
- declared 但 inactive 的上传字段也使用同一类别名表判断，避免 `fieldKey` 形式绕过 inactive child 丢弃规则。
- 新增回归测试覆盖视频模板 `listParams["referenceVideo"]`，当前模型字段为 `fieldKey=referenceVideo,paramKey=referenceVideos` 时，生成请求进入 `quickCreationListParams["referenceVideos"]`，且不进入 legacy `referenceVideoUri`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template maps list param field key to upload param key"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测服务端模板实际返回 `fieldKey` 或 `paramKey` 两种 key 时，Tune 字段卡片显示和最终请求体是否一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 模板 params 支持 fieldKey 归一化

代码提交 `e2cf988 fix(quickcreate): map template params to service param keys` 已推送到 `feature/kmp-refactoring`。

已完成：
- 应用灵感模板时，`detail.params` 会先通过当前服务模型的字段别名表归一化：`fieldKey -> paramKey`，`paramKey -> paramKey`。
- 归一化后的 `templateParams` 再覆盖默认字段值，避免父字段默认值压过模板返回的 fieldKey 值。
- active child 计算现在基于 canonical `serviceParams`，模板用父字段 fieldKey 激活 child upload 时，child 素材可以正确进入 canonical child `paramKey`。
- 未知 params key 仍保留原样，不影响 `ratio/aspectRatio/resolution/duration` 等非服务字段解析。
- 新增回归测试覆盖：父字段 `fieldKey=mode,paramKey=creationMode`，模板 `params["mode"]="imageReference"` 且 `listParams["childImage"]` 时，请求体进入 `quickCreationParams["creationMode"]` 和 `quickCreationListParams["childImages"]`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template maps param field key before resolving active child upload media"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测模板 params/listParams 混用 fieldKey 与 paramKey 的真实返回，确认 Tune 展示和最终请求体字段名一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 模板 params canonical key 优先

代码提交 `7c92113 fix(quickcreate): prefer canonical template params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `canonicalTemplateParams()` 现在先归一化全部模板 params，再二次覆盖原本就是 canonical `paramKey` 的值。
- 当模板同时返回 `paramKey` 和对应 `fieldKey` 时，canonical `paramKey` 优先，避免别名覆盖正式参数。
- 保持未知 key 透传；只有已知服务字段别名参与 canonical 覆盖。
- 新增回归测试覆盖：模板同时返回 `creationMode=imageReference` 和 `mode=text` 时，最终 `quickCreationParams["creationMode"]` 保留 `imageReference`，并继续激活 child upload 提交 `quickCreationListParams["childImages"]`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps canonical param value over field key alias"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测模板 params 同时包含 fieldKey/paramKey 的真实返回，确认最终请求体使用 canonical paramKey 且值不被别名覆盖。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 条件字段 visibleWhen 支持 fieldKey/paramKey 别名

代码提交 `6a20c50 fix(quickcreate): resolve visible conditions by field aliases` 已推送到 `feature/kmp-refactoring`。

已完成：
- 新增 `QuickCreationServiceModel?.quickCreationParamsWithFieldAliases()`，在条件判断视图里把同一服务字段的 `fieldKey`、`paramKey` 和非空默认值对齐为同一个值。
- `quickCreationActiveUploadParamKeys()` 现在用别名化 params 解析 active child upload，避免 `visibleWhen.fieldKey` 引用 sibling 字段时，因为当前状态只保存 canonical `paramKey` 而漏激活。
- `QuickCreateScreenModel` 的 active 参数过滤、文本校验、上传校验、模板素材 active alias 解析都改为基于别名化 params。
- `TuneBottomSheet` 的图片/视频服务字段渲染入口也使用别名化 params，保证 UI 显示、校验、模板素材归属和最终提交路径一致。
- 新增回归测试覆盖：`fieldKey=creationMode,paramKey=creation_mode` 的 sibling 条件字段，在状态只有 `creation_mode=imageReference` 时仍能激活 `reference_images` 上传字段。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active upload param keys resolve sibling field key conditions from param key values"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/TuneBottomSheet.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

仍未完成：
- 真机仍需复测真实模板中 `visibleWhen.fieldKey` 指向 sibling 字段且字段名与提交 `paramKey` 不一致的场景，重点确认 Tune 子字段显示、模板素材字段卡片和 prepare 请求体一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 active child 默认参数进入请求体

代码提交 `0affee6 fix(quickcreate): submit active child defaults` 已推送到 `feature/kmp-refactoring`。

已完成：
- `defaultServiceParams()` 现在会补齐 active input child 的非空 `defaultValue`，不再只提交顶层服务字段默认值。
- 默认值计算支持传入当前有效参数；当灵感模板 `params` 把父字段从默认 `text` 切到 `imageReference` 时，被模板激活的 child 默认值也会进入 `quickCreationParams`。
- 请求体构造时先写入“基于当前参数计算出的默认值”，再用用户/模板显式 `serviceParams` 覆盖，保持显式值优先。
- 模板应用后的 `imageServiceParams` / `videoServiceParams` 也使用同一规则初始化，降低 UI、校验和 prepare 请求体之间的默认值差异。
- 新增回归测试覆盖两类场景：模型默认父字段激活 child 默认值；模板参数激活 child 默认值。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image submits active child service field defaults"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template submits defaults for child activated by template params"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机需复测真实服务模型里带默认值的 child 参数，例如参考强度、权重、开关等，确认用户不手动修改时 prepare 请求体仍包含服务端期望的默认值。
- 仍需抓真实模板里“模板 params 激活 child”的返回，核对 Tune UI 默认显示、fee-preview 请求体和最终 prepare 请求体一致。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 inactive child 默认值不再污染 visibleWhen

代码提交 `55190bc fix(quickcreate): avoid inactive child default activation` 已推送到 `feature/kmp-refactoring`。

已完成：
- `quickCreationParamsWithFieldAliases()` 不再为 input child 无条件注入 `defaultValue`；child 只有在 `params` 已显式包含 `fieldKey` 或 `paramKey` 时才建立别名。
- 这避免了 inactive child 的默认值被 sibling 的 `visibleWhen.fieldKey` 读到，从而误激活另一个 child 上传字段。
- active child 默认值提交仍由 `QuickCreateScreenModel.defaultServiceParams(activeParams)` 负责，保持上一轮“active child 默认参数进入请求体”的行为。
- 新增回归测试覆盖：`referenceStrength` child 因父字段默认 `text` 未激活时，其默认值 `0.65` 不会让依赖 `referenceStrength=0.65` 的 `derivedImages` 上传 child 变 active。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.inactive child defaults do not activate sibling upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

仍未完成：
- 真机需复测复杂 inputChildren 链：child 默认值、child visibleWhen、上传 child 同时存在时，Tune 显示和 prepare 请求体是否只跟随当前 active 字段。
- 如果真实服务端存在“active child 的默认值继续激活另一个 child”的链式条件，当前 `defaultServiceParams(activeParams)` 可以提交已 active child 默认值，但还需要用真实模型确认是否存在更深层级联。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 stale inactive child 参数不再污染 visibleWhen

代码提交 `37338bf fix(quickcreate): ignore stale inactive child params` 已推送到 `feature/kmp-refactoring`。

已完成：
- `quickCreationParamsWithFieldAliases()` 现在返回的是条件判断视图，不再原样透传所有 `params`。
- 条件视图先加入可见顶层字段的显式值或默认值，再通过固定点方式逐轮加入当前 active child 的显式值或默认值。
- 之前用户在 child active 时填过的参数，如果父字段切换后该 child 已 inactive，则该 stale child 值不会再被 sibling `visibleWhen` 读取。
- 保留链式 active child 能力：只有 child 当前 active 且有值时，才会继续影响依赖它的 sibling 条件。
- 新增回归测试覆盖：`creationMode=text` 时，即使 `serviceParams` 里残留 `referenceStrength=0.65`，依赖该值的 `derivedImages` 上传 child 也不会变 active。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.inactive child explicit values do not activate sibling upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

仍未完成：
- 真机需复测切换父字段后，之前填过的 child 文本/上传素材不会在 UI、fee-preview 或 prepare 请求体里继续影响 inactive 分支。
- 如果真实模型存在多层 sibling 条件链，需要用真实字段确认固定点收敛后的 active child 展示顺序和请求体符合后端预期。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 素材 id 按 tab/category 隔离

代码提交 `4b9034f fix(quickcreate): isolate media ids by tab` 已推送到 `feature/kmp-refactoring`。

已完成：
- 手动上传素材 id 现在包含当前 tab：`IMAGE_IMAGE_...`、`VIDEO_AUDIO_...` 这类格式，降低同毫秒上传时跨 tab 撞 id 风险。
- 灵感模板素材 id 现在包含模板 `categoryId`，同一个 `templateId` 分别应用到图片和视频 tab 时，不再生成相同素材 id。
- 保留原有删除行为：`removeMediaReference(id)` 仍按 id 同时过滤两个配置，用于兼容旧测试里“切到另一个 tab 后删除指定 id”的调用方式；这次通过 id 隔离避免误删另一个 tab 的不同素材。
- 新增回归测试覆盖：同一个 `shared-template` 分别应用图片和视频模板，删除图片模板素材后，视频模板素材仍保留。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.removing image template media does not remove video template media with same template id"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机需复测图片/视频 tab 同时存在模板素材时，删除当前 tab 的素材卡片不会影响另一个 tab 的素材卡片和最终请求体。
- 如果后续发现同一 tab 内模板素材跨刷新需要稳定复用 id，可再引入 key hash；当前规则优先保证删除目标隔离和单次状态唯一性。
- 完整视频 `prepare/commit/list/detail` 扣费链路仍需要新的明确授权；本轮没有触发真实生成、`prepare/commit` 或新增扣费。
## 2026-06-18 模板素材媒体类型优先服务字段元数据

代码提交 `630609b fix(quickcreate): infer template media type from service fields` 已推送到 `feature/kmp-refactoring`。

已完成：
- `templateMediaReferences()` 解析模板 `listParams` 时，若 key 命中当前 active 上传字段别名，素材类型优先使用服务字段声明的 `fieldType/fieldKey/paramKey` 推断结果。
- 活动上传字段别名从单纯的 `paramKey` 扩展为 `paramKey + mediaType`，保留 fieldKey/paramKey 到 canonical paramKey 的映射，同时让通用 key 例如 `reference` 能按 `VIDEO_UPLOAD` 渲染为视频素材。
- 未命中服务字段的 legacy 全局素材仍按 key 文本推断图片/视频/音频类型，不改变既有 `referenceImageUri/referenceVideoUri/referenceAudioUri` 兼容路径。
- 新增回归测试覆盖：视频模板 `listParams["reference"]` 命中服务字段 `fieldKey=reference,paramKey=referenceVideos,fieldType=VIDEO_UPLOAD` 时，生成的模板素材类型为 `VIDEO`，并绑定到 `referenceVideos`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template infers generic list param media type from service field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测通用 `listParams` key 绑定到视频/音频字段时，Tune 字段素材卡片、删除动作和最终 `quickCreationListParams` 是否都按服务字段媒体类型展示和提交。
- 完整视频 `prepare/commit/list/detail` 扣费链路本轮未触发；本轮没有点击真实生成、没有新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 草稿恢复后刷新 fee-preview

代码提交 `1814fc7 fix(quickcreate): refresh fee preview after draft restore` 已推送到 `feature/kmp-refactoring`。

已完成：
- `restoreDraft()` 在恢复图片/视频 prompt 和当前 tab 后会重新调用 `scheduleFeePreview()`。
- 这样用户从草稿恢复上次输入后，底部价格会按恢复后的请求体重新走服务端 fee-preview，而不是停留在旧 tab 或空 prompt 的本地估算状态。
- `scheduleFeePreview()` 仍保留原有防线：没有有效 prompt 时不会发请求，只会清理价格预览状态。
- 新增回归测试覆盖：保存图片草稿、加载并恢复后等待 debounce，应发起一次图片 fee-preview，请求 prompt 为草稿内容。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft refreshes fee preview for restored image prompt"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测恢复视频草稿后的按钮状态，确认从“价格确认中”更新为服务端金额或明确失败态。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 草稿恢复同步 currentTab

代码提交 `941c7b1 fix(quickcreate): restore draft tab before fee preview` 已推送到 `feature/kmp-refactoring`。

已完成：
- `restoreDraft()` 现在始终按草稿里的 `currentTab` 恢复当前 tab：`VIDEO` 切到视频，其余值按图片处理。
- 修复用户当前停留在视频 tab 时恢复图片草稿，页面仍留在视频 tab，导致图片 prompt 不进入图片 fee-preview 的问题。
- 恢复 tab 后继续调用上一轮已接入的 `scheduleFeePreview()`，因此价格预览会基于恢复后的真实当前 tab 和 prompt 构造请求体。
- 新增回归测试覆盖：先切到视频 tab，再恢复 `currentTab=IMAGE` 的图片草稿，应切回 IMAGE，只发起图片 fee-preview，不发起视频 fee-preview。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore image draft switches back from video tab before fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测从视频 tab 恢复图片草稿、从图片 tab 恢复视频草稿两条 UI 路径，重点确认顶部 tab、底部按钮价格状态和 prompt 一致。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 坏草稿不会保留旧内存状态

代码提交 `a4ba123 fix(quickcreate): clear stale invalid draft state` 已推送到 `feature/kmp-refactoring`。

已完成：
- `checkForDraft()` 解析持久化草稿失败时，现在会同步清掉内存里的 `draftData` 和 `hasDraft`。
- 修复同一个 `QuickCreateScreenModel` 先加载过有效草稿、后续持久化草稿损坏时，内存仍保留旧草稿并可能被 `restoreDraft()` 恢复的问题。
- 解析失败仍会调用 `settingsRepository.clearQuickCreateDraft()` 清理持久化坏数据。
- 新增回归测试覆盖：先加载有效图片草稿，再把存储改成坏 JSON，重新检查后调用恢复，不应恢复旧 prompt，也不应触发 fee-preview。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft clears stale in memory draft when stored draft is invalid"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测损坏草稿或旧版本草稿存在时，入口 UI 不应展示可恢复旧内容；当前 `hasDraft/draftData` 仍是非响应式字段，后续若接入 UI 入口需要一起迁移到 `QuickCreateUiState`。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 空草稿不会保留旧内存状态

代码提交 `c10c3ea fix(quickcreate): clear stale missing draft state` 已推送到 `feature/kmp-refactoring`。

已完成：
- `checkForDraft()` 在持久化草稿为空或缺失时也会同步清掉内存里的 `draftData` 和 `hasDraft`。
- 修复同一个 `QuickCreateScreenModel` 先加载过有效草稿、后续草稿被清空时，内存仍保留旧草稿并可能被 `restoreDraft()` 恢复的问题。
- 这与上一轮损坏 JSON 草稿清理规则保持一致：持久化草稿不可用时，内存状态也不可恢复。
- 新增回归测试覆盖：先加载有效图片草稿，再清空存储，重新检查后调用恢复，不应恢复旧 prompt，也不应触发 fee-preview。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft clears stale in memory draft when stored draft is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需复测草稿被用户丢弃、成功恢复后清空、旧版本草稿缺失等路径下，恢复入口 UI 不应展示旧内容。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 草稿状态进入 QuickCreateUiState

代码提交 `70a98ad fix(quickcreate): expose draft state in ui state` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateUiState` 新增 `draftData`，并通过派生属性 `hasDraft` 暴露是否存在可恢复草稿。
- `QuickCreateScreenModel.hasDraft/draftData` 改为只读 getter，读取同一份 `uiState` 草稿数据，保留现有调用兼容性。
- `checkForDraft()`、损坏/空草稿清理、`restoreDraft()` 和 `discardDraft()` 现在都通过 `setDraftData()` 更新 `uiState`，后续 UI 恢复入口可以稳定收集状态变化。
- 新增回归测试覆盖：加载有效草稿后，`model.uiState.value.hasDraft` 应变为 `true`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft exposes saved draft through ui state"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateUiState.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需把恢复草稿入口接入真实快捷创作页面，并验证 `uiState.hasDraft` 变化能驱动入口显示/隐藏。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 初始化时加载草稿状态

代码提交 `46f354c fix(quickcreate): load draft state on init` 已推送到 `feature/kmp-refactoring`。

已完成：
- `QuickCreateScreenModel` 初始化时现在会调用 `checkForDraft()`，创建页面模型后即可把已有持久化草稿同步到 `uiState.draftData`。
- 这补齐了上一轮响应式草稿状态的入口：后续 UI 只要收集 `uiState.hasDraft`，进入页面后就能拿到初始草稿存在性。
- 保留手动 `checkForDraft()` 方法，后续如果需要在页面恢复前台或用户显式刷新时重新检查，仍可复用。
- 新增回归测试覆盖：构造 `QuickCreateScreenModel` 后不手动调用 `checkForDraft()`，`uiState.hasDraft` 也应为 `true`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.init exposes saved draft through ui state"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需接入并验证恢复/丢弃草稿 UI 入口，确认页面首屏出现时能正确显示已有草稿提示。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 接入草稿恢复/丢弃入口

代码提交 `79afee0 feat(quickcreate): add draft resume entry` 已推送到 `feature/kmp-refactoring`。

已完成：
- `BottomPromptPanel` 在 `uiState.hasDraft=true` 且当前没有任务运行时，会在 Tab 区上方显示草稿提示条。
- 草稿提示条展示 `上次草稿 · 图片/视频 · N 字` 摘要，并提供“恢复”和“丢弃”两个动作。
- “恢复”连接 `QuickCreateScreenModel.restoreDraft()`，会恢复 prompt/tab、清空草稿状态并刷新 fee-preview；“丢弃”连接 `discardDraft()`，会清空持久化草稿和 `uiState.draftData`。
- 新增 `DraftData.resumeSummaryText()`，集中生成草稿入口文案，避免 UI 层散落拼接逻辑。
- 新增回归测试覆盖：视频草稿摘要应展示视频类型和当前 tab 对应 prompt 字数。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.draft resume summary describes tab and prompt length"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证首屏草稿提示条在不同屏宽、键盘弹起、任务运行中和恢复/丢弃后的显示隐藏是否符合预期。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 空内容草稿不显示恢复入口

代码提交 `00c01cc fix(quickcreate): ignore empty saved drafts` 已推送到 `feature/kmp-refactoring`。

已完成：
- 新增 `DraftData.hasPromptContent` 判断，只有图片或视频 prompt 至少一个非空时才认为草稿可恢复。
- `checkForDraft()` 读取到两个 prompt 都为空的草稿 JSON 时，会清空 `uiState.draftData` 并调用 `settingsRepository.clearQuickCreateDraft()`。
- 这避免用户清空输入或历史遗留空草稿时，底部输入区显示没有实际内容的“上次草稿 · 0 字”入口。
- 新增回归测试覆盖：初始化读取空内容草稿后，`uiState.hasDraft=false`，持久化草稿也应被清除。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.init ignores saved draft without prompt content"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证用户手动清空 prompt 后再次进入页面，不应出现空内容草稿入口。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 自动保存不落空草稿

代码提交 `537ec48 fix(quickcreate): avoid autosaving empty drafts` 已推送到 `feature/kmp-refactoring`。

已完成：
- `autoSaveDraft()` 现在复用 `DraftData.hasPromptContent` 判断，只有图片或视频 prompt 至少一个非空时才保存草稿 JSON。
- 当用户把图片/视频 prompt 都清空时，自动保存 debounce 到期后会调用 `settingsRepository.clearQuickCreateDraft()`，不再写入空草稿。
- 这让“读取阶段过滤空草稿”和“写入阶段不产生空草稿”形成闭环，减少下一次进入页面时的清理负担。
- 新增回归测试覆盖：先输入并自动保存草稿，再清空 prompt，持久化草稿应被删除。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.auto save clears draft when prompts become empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证清空图片和视频 prompt 后退出再进入，草稿入口不会短暂闪现。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 自动保存后隐藏过期草稿入口

代码提交 `73af161 fix(quickcreate): hide stale draft entry after autosave` 已推送到 `feature/kmp-refactoring`。

已完成：
- `autoSaveDraft()` 在保存新草稿或清理空草稿后，会同步调用 `setDraftData(null)` 清掉当前 UI 中的旧草稿入口状态。
- 修复用户看到旧草稿提示条但选择直接输入新内容时，旧 `uiState.draftData` 仍保留，导致底部继续显示已过期草稿入口的问题。
- 新增回归测试覆盖：已有旧草稿入口时输入新 prompt，等待自动保存 debounce 后，`uiState.hasDraft` 应变为 `false`。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.auto save hides stale draft entry after prompt changes"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证已有旧草稿入口时直接输入新内容，等待自动保存后草稿提示条应消失。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 草稿恢复优先切到有内容的 tab

代码提交 `baf2e87 fix(quickcreate): restore draft tab with prompt` 已推送到 `feature/kmp-refactoring`。

已完成：
- 新增 `DraftData.restorableTab`，用于统一决定草稿入口摘要和恢复后的目标 tab。
- 规则为：优先使用草稿 `currentTab` 对应的非空 prompt；如果当前 tab 的 prompt 为空，则回退到另一个有内容的 tab。
- 修复 `currentTab=VIDEO` 但 `videoPrompt` 为空、`imagePrompt` 有内容时，入口显示“视频 · 0 字”并恢复到空视频 tab 的问题。
- `restoreDraft()` 现在使用 `draft.restorableTab`，因此恢复后会停在实际有 prompt 的 tab，并触发对应 tab 的 fee-preview。
- 新增回归测试覆盖摘要文案回退和恢复后 fee-preview 走图片请求。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.draft resume summary falls back to image prompt when video tab has no prompt" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft falls back to image tab when video prompt is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证从图片输入切到空视频 tab 后退出再进入，草稿入口应显示图片字数，恢复后停在图片 tab。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 草稿恢复完整替换 prompt 快照

代码提交 `e4cb9d1 fix(quickcreate): replace prompts on draft restore` 已推送到 `feature/kmp-refactoring`。

已完成：
- `restoreDraft()` 现在一次性用草稿里的 `imagePrompt` 和 `videoPrompt` 替换当前 UI 两侧 prompt。
- 修复只写入非空 prompt 导致的问题：恢复一个只有视频 prompt 的草稿时，图片 tab 里恢复前的旧 prompt 不会再残留。
- 恢复 tab 仍使用上一轮的 `DraftData.restorableTab`，因此恢复后会停在真正有内容的 tab。
- 新增回归测试覆盖：当前页面已有图片 prompt，恢复只有视频 prompt 的草稿后，图片 prompt 应为空、视频 prompt 应为草稿内容。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore video draft clears existing image prompt"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证恢复视频草稿后，再切回图片 tab 时不会看到恢复前旧图片 prompt。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 恢复草稿取消待执行自动保存

代码提交 `7cb0470 fix(quickcreate): cancel draft autosave on restore` 已推送到 `feature/kmp-refactoring`。

已完成：
- `clearDraft()` 现在会取消 `draftSaveJob` 并置空，避免恢复或丢弃草稿后，恢复前输入触发的 pending auto-save 再次写回持久化草稿。
- 修复用户输入后立即恢复草稿时，500ms debounce 到期后又把恢复后的内容重新保存为草稿，导致后续草稿入口可能再次出现的问题。
- `restoreDraft()` 和 `discardDraft()` 都复用 `clearDraft()`，因此两条路径都会取消待执行自动保存。
- 新增回归测试覆盖：触发自动保存防抖后立即恢复草稿，再推进 500ms，`SettingsRepository.getQuickCreateDraft()` 应保持为空。

TDD 与验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft cancels pending autosave"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

仍未完成：
- 真机仍需验证输入后立即恢复或丢弃草稿，等待 debounce 时间后草稿提示条不会重新出现。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
