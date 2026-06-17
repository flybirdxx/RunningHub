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
