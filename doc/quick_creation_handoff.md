# 快捷创作重构交接文档

更新时间：2026-06-18
负责人：Codex
分支：`feature/kmp-refactoring`

## 当前实现边界

本轮已经把快捷创作从“纯旧 OpenAPI 硬编码提交”推进到“图片 G-2.0 默认链路使用 Web quick-creation v2”，并把携带服务端 quick-creation ID 的视频请求接入同一套 v2 状态机。当前边界如下：

- `all-power-image-g2`：默认仍可使用抓包确认的 `bindingId=2046586338670891013`、`skuId=2046514150500524034`、`categoryId=IMAGE`。
- `/api/qc/v2/models`：已接 DTO、mapper、repository 和 ScreenModel 状态。真实响应 `data` 是 catalog 对象，模型数组位于 `data.categories[categoryId]`，不是顶层数组；分组名优先使用 `groupName`。`IMAGE/VIDEO` 服务端模型会在页面初始化时加载。
- 图片服务端模型：加载后默认选中首个可用模型，Tune 高级页可切换；图片 v2 提交会优先携带选中模型的 `categoryId/bindingId/skuId`。
- 服务端字段：`fields/options/defaultValue/maxUploadCount/maxUploadSize/multipleInputs/skuInputExtraJson` 已解析到 domain model；字段默认值会初始化到 UI state，Tune 高级页可点选基础 options，也可输入文本/数值字段；图片/视频/音频上传字段会按字段类型把已上传素材 URL 写入对应服务端 `paramKey` 的数组参数；图片 v2 和携带服务端 ID 的视频 v2 提交会把当前字段值写入 `params`，未知字段会被过滤。
- 视频 v2：当 `VideoGenerationRequest` 携带 `quickCreationBindingId` 和 `quickCreationSkuId` 时，repository 会走 `fee-preview -> prepare -> commit -> list`；`QuickCreationV2Defaults.videoCreateRequest` 会生成抓包确认的 Seedance2.0 多模态参数，包括 `ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、`creationMode=multimodal`、`creationSubModeId=1`、`creationSubModeKey=MULTIMODAL_REFERENCE` 和参考素材 URL 数组。
- 提交流程：`fee-preview -> prepare -> commit -> list`。
- `commit` 请求体：严格使用 `prepareToken + createRequest` 嵌套结构。
- 任务轮询：使用 `/task/quick-creation/list`，不再依赖 `/api/output/taskHistory`。
- 历史/详情/取消数据层：`QuickCreateRepository` 已公开 `listQuickCreationHistory(page,size)`、`getQuickCreationHistoryDetail(outputId)` 和 `cancelQuickCreationTask(taskId)`；domain 模型会保留任务状态、分类、模型 ID、扣费金额、`apiRequestParams` 标量参数、输出 URL/预览图/尺寸/过期信息。详情请求按抓包结论使用 `outputId`；取消接口按 Chrome DevTools 确认使用 `/task/quick-creation/cancel`，请求体为 URL 编码后的 `taskId`。
- 项目列表：已公开 `listQuickCreationProjects(page,size)`，使用 `/task/quick-creation/project/list`，请求体为 `{"page":1,"size":20}`，响应按 `records/size/current/total/pages/hasNext/hasPrevious/nextCursor` 映射为 domain 分页模型；`QuickCreateScreenModel` 初始化会加载项目列表，历史区顶部会展示项目横向列表。
- 项目任务：已公开 `listQuickCreationProjectTasks(projectId,page,size)`，使用 `/task/quick-creation/project/tasks`。Chrome DevTools 使用临时项目验证请求体为 `{"projectId":"...","page":1,"size":10}`，响应分页字段为 `records/size/current/total/pages/hasNext/hasPrevious/nextCursor`，其中分页数值可能以字符串返回；移动端 DTO 已兼容 `list` 和 `records` 两种任务分页结构。创作页项目横向条已可点击，选中项目后中间列表切到项目内任务，点击“最近创作”可清除筛选。
- 项目置顶：已公开 `pinQuickCreationProject(projectId,pinned)`，使用 `/task/quick-creation/project/pin`。Chrome DevTools 使用临时项目验证请求体字段为 `{"projectId":"...","pinned":true|false}`；`pin` 字段会返回 `code=301,msg=不能为null`。项目 chip 上的图钉可切换置顶/取消置顶，请求中显示小 loading，成功后更新本地项目 state。
- 项目创建/重命名/删除/详情：已公开 `createQuickCreationProject(name)`、`renameQuickCreationProject(projectId,name)`、`deleteQuickCreationProject(projectId)` 和 `getQuickCreationProjectDetail(projectId)`，分别使用 `/task/quick-creation/project/create|rename|delete|detail`。Chrome DevTools 使用临时项目验证请求体分别为 `name`、`projectId/name`、`projectId`；创建响应返回 `projectId/name/projectType`，详情响应返回 `projectId/name/coverUrl/projectType/isSystem/pinned/pinnedAt/status/firstPrompt/lastGenerateAt/createTime/updateTime`，重命名和删除按成功 envelope 处理。服务端会截断过长项目名称。历史区项目标题右侧可新建项目，项目 chip 更多菜单可查看详情、重命名和删除；删除当前筛选项目后会回到最近创作。项目详情弹窗会显示封面、任务数、置顶状态、创建时间和更新时间，毫秒时间戳会格式化为 `yyyy-MM-dd HH:mm`。
- 历史 UI：`QuickCreateScreenModel` 初始化会加载最近 10 条 quick-creation 历史，生成成功后会刷新历史；创作页中间区域在没有当前任务/结果时展示最近创作，支持图片/视频预览、状态、分类和扣费金额摘要；列表底部可加载更多历史页并去重追加；当历史项状态不是 `SUCCESS/FAILED/ERROR/CANCELED` 等终态时，会每 5 秒刷新当前已加载范围，并显示取消入口；点击项目筛选时同一列表会加载项目内任务；点击历史项会按 `outputId` 加载详情并展示详情弹窗。
- 其它图片模型和未携带服务端 quick-creation ID 的视频请求：暂时保留旧 `openapi/v2` 兼容逻辑。
- 页面结构：按移动端截图理解为顶部轻量标题/模式区，中间大面积可滚动 RecyclerView 内容区，底部固定模型参数和提示词输入区；当前已落地顶部“创作/灵感”、中间滚动区、底部固定输入区，底部输入区会显示当前服务端模型摘要。模拟器已验证底部摘要显示真实 `全能图片G-2.0-文生图-官方版` 和 `4 个参数`，不再回落到“未获取到服务端模型”。
- 灵感页：已接真实 `tags/templates` 列表和 `template/detail`；真实详情请求体为 `{"templateId":"..."}`，响应里的 `snapshot.presetParams` 会用于“制作同款”，当前可回填分类、服务端模型、prompt、比例/分辨率/时长/开关参数和远端素材。

## 关键文件

- `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2Dto.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/QuickCreateApi.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/QuickCreateRepository.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationV2Defaults.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationModelMapper.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateUiState.kt`
- `doc/quick_creation_feature_plan.md`
- `doc/quick_creation_progress.md`

## 后续开发顺序

1. 真机验证图片 G-2.0 文生图：确认登录态、价格、扣费、任务状态和输出预览。
2. 补齐服务端字段动态表单矩阵：当前基础 options、文本/数值字段和图片/视频/音频上传字段已能渲染或进入请求结构；后续需要覆盖条件字段和复杂 `skuInputExtraJson`；当前本地枚举仍是 fallback。
3. 增强“制作同款”：当前已可回填基础参数和远端素材；后续补图片模板、复杂多输入模板、条件字段和 `skuInputExtraJson` 的完整映射。
4. 真实验证 Seedance2.0 视频 v2：基础请求构造和状态机已接入，下一步需要在用户明确授权后做 App 内真实视频扣费任务，确认价格、余额变化、任务状态和视频输出展示。此前抓包模板预估价格为 9.60 元，不在既有 0.76 元授权范围内。
5. 增强历史 UI：当前已展示最近创作，支持加载更多、非终态任务定时刷新、取消任务和详情弹窗；下一步做 App 内真实取消失败/成功响应验证。
6. App 内复测项目管理流程：`project/list`、`project/tasks`、`project/pin`、`project/create/rename/delete/detail` 已通过 Chrome DevTools 登录态临时项目验证并进入 ScreenModel/历史区 UI；模拟器已验证打开创作页、项目列表/最近创作渲染、项目操作菜单和项目详情弹窗。下一步在用户确认可改动数据后，验证项目创建、筛选、置顶、重命名和删除完整流程。

## 验证命令

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:assembleDebug
```

本轮三项均已通过。`assembleDebug` 的输出中仍有既有 Kotlin warning，以及 native strip 对部分库的提示，但没有阻断打包。

## 云端推送约束

当前远端是：

```text
origin https://github.com/flybirdxx/RunningHub.git
```

推送前只应纳入本任务文件，避免把既有无关修改混入提交：

- 不要自动纳入 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt`，该文件在本轮开始前已是修改状态。
- 不要自动纳入 `output/`。
- 若需要阶段性交替推送，建议按“代码提交”和“文档提交”交替：
  - 代码提交：shared/composeApp/gradle 测试相关文件。
  - 文档提交：`doc/quick_creation_*.md`。

推荐命令：

```powershell
git add shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2Dto.kt `
  shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/QuickCreateApi.kt `
  shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationV2Defaults.kt `
  shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImpl.kt `
  shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt `
  shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2DtoTest.kt `
  shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt `
  composeApp/build.gradle.kts `
  composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt `
  composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt `
  composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateUiState.kt `
  composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt `
  gradle/libs.versions.toml

git commit -m "feat(quickcreate): wire image quick creation v2 flow"
git push origin feature/kmp-refactoring
```

文档提交：

```powershell
git add doc/quick_creation_feature_plan.md doc/quick_creation_progress.md doc/quick_creation_handoff.md
git commit -m "docs(quickcreate): add progress and handoff notes"
git push origin feature/kmp-refactoring
```
## 2026-06-18 追加交接：App 端成功任务展示验证

本轮没有修改代码，只补充了模拟器验证。当前模拟器 `emulator-5554` 上的 `com.runninghub.app` 已能进入“创作”页并展示真实服务端模型与成功历史任务。

验证步骤：

```powershell
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_app_current_before_submit.xml
adb -s emulator-5554 shell input tap 489 2739
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_app_create_before_generation.xml
adb -s emulator-5554 exec-out screencap -p > output\quickcreate_app_create_before_generation.png
adb -s emulator-5554 shell input tap 640 1110
adb -s emulator-5554 exec-out uiautomator dump /dev/tty > output\quickcreate_app_history_detail_success.xml
adb -s emulator-5554 exec-out screencap -p > output\quickcreate_app_history_detail_success.png
```

当前证据结论：

- 创作页底部模型摘要显示 `全能图片G-2.0-文生图-官方版` / `全能图片G-2.0-官方版 · 4 个参数`。
- 最近创作列表中有真实成功任务，状态为 `IMAGE · SUCCESS · PNG`，费用显示 `0.8 CNY`。
- 详情弹窗显示 `IMAGE · SUCCESS · PNG · 2048x1152`、同一 prompt、费用 `0.8 CNY` 和输出预览区域。

扣费边界：

- 用户此前只明确授权 `0.76 元`继续验证。
- 当前 App 已能读取并展示真实成功扣费任务；本轮没有再次点击“生成”，避免发生第二次扣费。
- 若后续必须证明“移动端 App 自身点击生成后完成 prepare/commit/轮询/详情展示”的完整链路，需要先取得新的明确扣费授权，再提交一次低成本图片任务。

## 2026-06-18 追加交接：现金金额显示精度

问题：App 历史列表和详情弹窗曾把 `0.76 CNY` 显示为 `0.8 CNY`，会和 Web 抓包、`fee-preview`、`commit` 响应里的真实金额不一致。

处理：

- 新增 `com.runninghub.app.util.formatCashAmount(value: Double)`，用于现金金额展示，固定保留两位小数。
- `QuickCreateScreen.kt` 中快捷创作历史列表和详情弹窗的 `cashAmount` 展示已切换到 `formatCashAmount`。
- 保留原 `formatOneDecimal` 给文件大小、估算价格等旧调用点，避免改变其它 UI 语义。

回归测试：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.util.NumberFormatTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
```

代码提交：`ac9d40b fix(quickcreate): preserve cash amount precision`。

## 2026-06-18 追加交接：生成按钮金额格式

在历史列表和详情弹窗修复后，又检查到底部生成按钮仍使用一位小数 formatter。现已改为：

```kotlin
"¥${formatCashAmount(cost)}"
```

这样生成前看到的价格、任务历史里的扣费金额、详情弹窗里的扣费金额都使用同一套现金金额格式，避免 `0.76` 被显示成 `0.8`。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.util.NumberFormatTest"
```

代码提交：`363048f fix(quickcreate): format send button cost as cash`。

## 2026-06-18 追加交接：服务端模型 pricing 元数据

真实 `/api/qc/v2/models` 模型响应里的每个可提交 model 会带 `pricing` 对象。此前移动端只保留字段定义和模型 ID，没有把 `pricing` 带入 domain；这会阻断后续把生成按钮价格改为服务端驱动。

已落地：

- DTO：`QuickCreationPricingDto`
- Domain：`QuickCreationServicePricing`
- Mapper：`QuickCreationModelMapper` 已映射 `pricingMode/settlementMode/paidPriceKind/discountPercent/isFree/freeRemaining/isTimeFree/promoType`，并用 raw JSON 字符串保留 `flatPrice/dimensionPricing`。

验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.remote.dto.QuickCreationModelDtoTest" --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
```

代码提交：`be11bf9 fix(quickcreate): keep service model pricing metadata`。

后续建议：在 repository 增加只做 `fee-preview` 的公开方法，ScreenModel 根据当前模型、prompt、字段参数和上传 URL 组装 createRequest 后刷新价格，并对输入变化做 debounce；刷新失败时不要允许用户误以为本地估算价就是最终扣费价。

## 2026-06-18 追加交接：图片 fee-preview repository 能力

代码提交 `4cd380a fix(quickcreate): expose image fee preview` 已推送到 `feature/kmp-refactoring`。本次只暴露服务端价格预览能力，不会触发扣费。

变更边界：

- `QuickCreateRepository.previewImageQuickCreationFee(ImageGenerationRequest)` 已可供 UI 层调用。
- 返回模型为 `QuickCreationFeePreview`，包含 `passed/free/settlementMode/requiredRhAmount/requiredCashAmount/userCashBalance/insufficientType/cashCurrency`。
- `QuickCreateRepositoryImpl` 内部使用 `QuickCreationV2Defaults.imageG2CreateRequest(request)` 构造与真实提交一致的 v2 `createRequest`，然后只请求 `/task/quick-creation/fee-preview`。
- 新测试 `QuickCreateRepositoryImplFeePreviewTest` 使用 MockEngine 验证 endpoint、`0.76 CNY`、余额和结算模式映射。
- `QuickCreateScreenModelTest.FakeQuickCreateRepository` 已补齐新接口；后续给 ScreenModel 接实时价格时可以在这个 Fake 上增加调用次数和返回值控制。

已运行验证：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreateRepositoryImplFeePreviewTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest
```

下一步建议先做 ScreenModel 的 RED 测试：当图片 prompt、选中模型或服务端字段参数变化并满足可预览条件时，ScreenModel 调用 `previewImageQuickCreationFee`，将 `requiredCashAmount=0.76` 写入 UI state，并让底部生成按钮显示服务端金额。实现时需要 debounce 或任务取消，避免每个字符都打服务端；失败态必须和本地估算区分，不能把 `estimatedCost` 伪装成最终扣费价。

## 2026-06-18 追加交接：ScreenModel 图片实时价格刷新

代码提交 `650e13d fix(quickcreate): refresh image price from fee preview` 已推送到 `feature/kmp-refactoring`。

当前行为：

- 图片 prompt 变化后，ScreenModel 会等待 500ms debounce，再用当前图片配置、服务端模型 ID、服务端字段参数和已上传图片 URL 调用 `previewImageQuickCreationFee`。
- 以下图片配置变化也会触发刷新：本地图片模型、服务端图片模型、服务端字段参数、比例、分辨率、质量、数量、seed、图片素材上传完成、图片素材移除。
- fee-preview 成功后，`requiredCashAmount` 会写回 `uiState.estimatedCost`，因此底部生成按钮会显示服务端金额；`free=true` 时写回 0。
- `feePreviewLoading/feePreviewError` 已进入 `QuickCreateUiState`，但 UI 文案还没有消费它们。
- 图片生成提交和 fee-preview 复用 `buildImageGenerationRequest`，减少“预览价格参数”和“实际提交参数”不一致的风险。
- 顺手修复了草稿自动保存：不再依赖 `DraftData` 的生成 serializer，改为显式 `JsonObject` 读写，避免输入 500ms 后协程因 serializer 缺失失败。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image prompt refreshes server fee preview into estimated cost"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
```

后续建议：

- 在底部输入区消费 `feePreviewLoading/feePreviewError`，显示“价格确认中/价格待确认”，并考虑在价格预览失败时禁用生成或要求用户确认。
- 继续补视频 fee-preview 的 UI 价格刷新；当前本轮只覆盖图片链路。
- 下一次 App 真机/模拟器验证时，不需要额外扣费就能观察输入 prompt 后按钮金额是否从本地估算刷新为服务端 `0.76`；真正点击生成仍需新的扣费授权。

## 2026-06-18 追加交接：底部按钮消费 fee-preview 状态

代码提交 `29d2ee2 fix(quickcreate): show fee preview status on send button` 已推送到 `feature/kmp-refactoring`。

当前行为：

- 图片 tab 下，底部生成按钮会根据 fee-preview 状态显示：
  - `价格确认中`：`feePreviewLoading=true`，按钮临时禁用。
  - `价格待确认`：`feePreviewError != null`，不再把旧的本地估算金额伪装成最终扣费价。
  - `¥0.76`：服务端预览成功且有现金金额。
  - `生成`：金额为 0 或尚无可展示金额。
- 文案选择逻辑集中在 `QuickCreateBillingUiText.kt`，单测 `QuickCreateBillingUiTextTest` 覆盖优先级。
- `QuickCreateScreen.kt` 的 `BottomPromptPanel -> SendButton` 已透传图片 fee-preview 状态；视频 tab 暂不使用这两个状态。

验证命令：

```powershell
.\gradlew.bat --stop
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :composeApp:assembleDebug
```

注意：本轮曾并行运行 `testDebugUnitTest` 与 `assembleDebug`，导致 Kotlin incremental cache 报 `Storage ... already registered` 和缓存文件 MD5 缺失。停止 Gradle daemon 后串行执行通过；后续验证不要并行跑会写 `composeApp/build/kotlin/compileDebugKotlinAndroid` 的 Gradle 任务。

后续建议：

- 用模拟器实际进入快捷创作页，输入图片 prompt，观察按钮从 `价格确认中` 切到服务端金额；这一步不需要点击生成，不会扣费。
- 补视频 fee-preview 刷新和按钮状态。

## 2026-06-18 追加交接：图片 fee-preview 按钮状态模拟器验证

本轮在 `emulator-5554` 上安装当前 `composeApp-debug.apk`，保留登录态，完成了不扣费 UI 验证。

验证路径：

1. 启动 `com.runninghub.app/.MainActivity`。
2. 从底部导航点击“创作”。
3. 在图片 tab 的 prompt 输入框输入 `green%20minimal%20icon`，截图显示按钮立即变为 `价格确认中`。
4. 等待约 4 秒，截图显示按钮变为 `生成`，没有回退到空 prompt 时的本地估算 `¥0.93`。
5. 清空并输入 `greenicon` 复测，得到同样状态流：`价格确认中 -> 生成`。

证据文件位于未跟踪目录 `output/`：

- `quickcreate_fee_preview_create_initial_pulled.png`
- `quickcreate_fee_preview_after_type_fast.png`
- `quickcreate_fee_preview_after_wait.png`
- `quickcreate_fee_preview_greenicon_fast.png`
- `quickcreate_fee_preview_greenicon_wait.png`

结论：

- UI 已真实消费 `feePreviewLoading`，输入 prompt 后按钮会显示 `价格确认中`。
- 本次服务端 fee-preview 结束后显示 `生成`，说明当前账号/模型/prompt 返回的是零金额或免费态；该状态没有继续展示旧本地估算价。
- 本次没有点击“生成”，因此没有触发 `prepare/commit`，也没有产生新扣费。

仍需后续覆盖：

- 找到一个服务端返回非零 `requiredCashAmount` 的图片预览场景，验证真实 App UI 显示 `¥x.xx`。
- 视频 tab 的 fee-preview 状态刷新与按钮状态仍未接入。

## 2026-06-18 追加交接：视频 fee-preview 刷新与按钮状态

代码提交 `374995b fix(quickcreate): refresh video price from fee preview` 已推送到 `feature/kmp-refactoring`。

当前行为：

- `QuickCreateRepository.previewVideoQuickCreationFee(VideoGenerationRequest)` 已公开，内部用 `QuickCreationV2Defaults.videoCreateRequest(request)` 构造 v2 `createRequest`，只调用 `/task/quick-creation/fee-preview`，不触发 `prepare/commit`。
- `QuickCreateScreenModel` 的价格刷新已从图片专用扩展为当前 tab 通用。视频 prompt、服务端视频模型、服务端字段、视频模型、比例、分辨率、时长、数量、seed、真实模式、生成音频、素材上传完成和素材移除都会触发 500ms debounce 后的服务端价格预览。
- 视频 fee-preview 和视频正式提交共用 `buildVideoGenerationRequest`，服务端模型 ID、字段 params、素材 list params、参考图/视频/音频 URL、`realistic/generateAudio` 等参数保持一致。
- 底部生成按钮在图片和视频 tab 都消费 `feePreviewLoading/feePreviewError`。视频价格确认期间按钮会显示 `价格确认中` 并临时禁用；预览失败时显示 `价格待确认`。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.video prompt refreshes server fee preview into estimated cost"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreateRepositoryImplFeePreviewTest"
.\gradlew.bat :composeApp:testDebugUnitTest
.\gradlew.bat :shared:testDebugUnitTest
git diff --check
```

仍需后续覆盖：

- 在真实 App UI 中切换到视频 tab，输入 prompt，观察按钮从 `价格确认中` 切到服务端金额或 `生成`；这一步只做 fee-preview，不需要扣费。
- 如果要点击视频 `生成` 验证完整 `prepare/commit/list/detail` 链路，需要用户重新授权。此前抓包模板的 Seedance2.0 视频预览金额约 `9.60 CNY`，不在既有 `0.76 CNY` 授权范围内。
- 继续保留工作区边界：不要把既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动或未跟踪 `output/` 证据目录混入后续提交。

## 2026-06-18 追加交接：价格预览不确定时拦截生成

代码提交 `075d749 fix(quickcreate): block generation during fee preview uncertainty` 已推送到 `feature/kmp-refactoring`。

当前行为：

- `QuickCreateScreenModel.generate()` 会先检查 `feePreviewLoading`。如果价格仍在确认中，直接保持/回到 `IDLE`，设置页面错误为 `价格确认中`，不调用正式生成。
- `QuickCreateScreenModel.generate()` 会检查 `feePreviewError`。如果价格预览失败，直接保持/回到 `IDLE`，设置页面错误为 `价格待确认`，不调用正式生成。
- 这个保护覆盖图片和视频 tab，是 ScreenModel 级防线；即使后续 UI 按钮 enabled 逻辑被误改，也不会绕过价格确认直接触发扣费链路。
- 旧的生成请求测试已按真实流程更新：输入 prompt 或上传素材后，先等待 500ms fee-preview debounce 完成，再断言正式生成请求体。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked while fee preview is loading" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked while fee preview is loading"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest
git diff --check
```

后续建议：

- 在模拟器上复测图片和视频 tab：输入 prompt 后立刻尝试点击生成，应只看到价格确认状态，不应触发任务提交。
- 如果后续要验证真实 `prepare/commit` 扣费链路，仍需要新的明确扣费授权。
- 继续避免把既有 `AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 目录混入提交。

## 2026-06-18 追加交接：模拟器不扣费复测

本轮没有代码改动，只在 `emulator-5554` 上安装最新 debug 包并复测快捷创作 UI。

已执行：

```powershell
.\gradlew.bat :composeApp:assembleDebug
adb -s emulator-5554 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
adb -s emulator-5554 shell am start -n com.runninghub.app/.MainActivity
```

验证结论：

- 创作页可正常打开，保留登录态，最近创作仍展示既有成功图片任务 `IMAGE · SUCCESS · PNG · 0.76 CNY`。
- 图片 tab 输入 `guardimage` 后，按钮最终显示 `生成`；点击后只出现 `余额不足或价格预览未通过`，没有进入任务提交、排队或运行态，未产生新扣费。
- 视频 tab 加载真实服务端模型 `Seedance2.0 · 12 个参数`；输入 `guardvideo` 后按钮显示 `¥6.00`，证明视频 tab 可到达非零价格展示状态。
- 本轮没有点击视频 `生成`，没有触发视频 `prepare/commit`。

证据文件位于未跟踪目录 `output/`：

- `quickcreate_submit_guard_create_initial.xml/png`
- `quickcreate_submit_guard_image_loading.xml/png`
- `quickcreate_submit_guard_image_after_tap.xml/png`
- `quickcreate_submit_guard_image_after_wait.xml/png`
- `quickcreate_submit_guard_video_initial.xml/png`
- `quickcreate_submit_guard_video_fast.xml/png`
- `quickcreate_submit_guard_video_wait.xml/png`

后续注意：

- 视频 `价格确认中` 文案是瞬时状态，本轮没有稳定截获；若要补强证据，可以通过更慢网络或测试开关注入延迟。
- 视频真实生成仍需要新的明确扣费授权。当前不要为了验证而点击 `¥6.00` 的生成按钮。
- `AuthRepositoryImpl.kt` 和 `output/` 仍不要纳入后续提交，除非确认属于当前任务。

## 2026-06-18 追加交接：服务字段 extra metadata

代码提交 `bcdf91a fix(quickcreate): parse service field extra metadata` 已推送到 `feature/kmp-refactoring`。

本轮解决的问题：
- 服务端 `/api/qc/v2/models` 的字段里，`skuInputExtraJson` 不再只作为 raw string 保留；现在会解析为 `QuickCreationServiceField.inputExtra`。
- 已结构化字段包括标题、英文标题、参数描述、英文描述、占位符、上传 accept 格式、文本长度限制、最大输入数量和列表值大小写敏感标记。
- Tune 高级参数区现在优先展示服务端 `title/paramDesc/placeholder/accept`，更贴近截图底部“模型参数和提示词输入区域”的真实配置。

验证过的命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 继续用测试优先补复杂 `skuInputExtraJson`：优先覆盖 `inputsChildList`、条件展示、字段联动和 extra json 内嵌 options 的真实样例。
- UI 侧可以把 `QuickCreationServiceFieldExtra.maxLength/minLength/maxInputCount` 接入输入限制和上传数量提示，但要注意不要和顶层 `maxUploadCount/maxUploadSize/multipleInputs` 冲突。
- 若后续要验证真实视频生成，必须先取得新的明确扣费授权；当前只允许做不触发 `prepare/commit` 的 fee-preview、列表和 UI 验证。
- 继续不要把既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录混入提交。

## 2026-06-18 追加交接：媒体字段 Tune 渲染

代码提交 `98d7e90 fix(quickcreate): render media service fields in tune panel` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt` 集中承载服务端字段 UI helper。
- `IMAGE/VIDEO/AUDIO/UPLOAD` 都会被识别为上传类字段；因此真实模型里的 `imageUrls/referenceVideo/referenceAudio` 等字段可以进入 Tune 高级参数区。
- 上传提示会优先显示 `skuInputExtraJson` 解析出的 `acceptFormats` 和 `maxInputCount`，再使用顶层上传限制。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 在这个 helper 上继续补 `inputsChildList`、条件字段和字段级校验，先写单测再接 Tune UI。
- 若要做真实设备 UI 复测，只需打开 Tune 高级页观察上传字段是否出现；不要点击会触发付费的生成按钮。

## 2026-06-18 追加交接：字段 visible 边界

代码提交 `526ab48 fix(quickcreate): respect service field visibility` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationFieldDto.visible` 已解析，缺省值为 `true`。
- `QuickCreationServiceField.visible` 会保留到 domain；不要在 mapper 阶段过滤 `visible=false` 字段，因为这类字段可能仍携带服务端提交所需默认参数。
- Tune UI helper 会过滤 `visible=false` 字段，所以隐藏字段不展示给用户，但 `QuickCreateScreenModel.defaultServiceParams()` 仍可从所有字段读取默认值并参与请求构造。

验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.remote.dto.QuickCreationModelDtoTest" --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 继续补条件字段和子输入时，遵守同一原则：显示规则归 UI helper/request state，提交必需的隐藏默认值不能在 mapper 层丢弃。
- 真实 App 复测可以只观察 Tune 高级页字段是否减少，不需要触发生成扣费。

## 2026-06-18 追加交接：文本字段长度限制

代码提交 `4047fbf fix(quickcreate): enforce service text field length` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldUiModel.constrainQuickCreationTextInput()` 会读取 `inputExtra.maxLength` 并截断 Tune 高级参数区文本字段输入。
- `quickCreationTextLimitCounter()` 会为有 `maxLength` 的字段提供 `当前长度/最大长度` 文案；Tune UI 已在文本输入框下方展示该计数。
- 这只影响 Tune 高级参数里的服务端字段，不影响底部主 prompt 输入框。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- `minLength` 仍未做提交前校验；后续可在同一个 helper 上补 `isTooShort` 或错误文案，再由 Tune UI 和 `generate()` 防线共同消费。
- 继续补 `inputsChildList` 和条件联动时，优先扩展 `QuickCreationServiceFieldUiModelTest`，再接 Compose UI。

## 2026-06-18 追加交接：文本字段提交前校验

代码提交 `5164dc9 fix(quickcreate): validate service text fields before submit` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldUiModel.quickCreationTextValidationError()` 已覆盖 required 非空和 `inputExtra.minLength`。
- `QuickCreateScreenModel.generate()` 会在进入正式提交协程前校验当前 tab 的服务端文本字段；失败时设置 `taskStatus=IDLE` 和字段错误，不调用 repository 的正式生成方法。
- 校验会用用户填写的 `serviceParams`，并回退服务端字段默认值；不要在 mapper 层过滤隐藏默认字段。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when required service text field is too short"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 用相同模式补上传字段 required/maxInputCount 校验，尤其是图生图、视频参考图/视频/音频字段。
- 再继续处理 `inputsChildList` 和条件联动；这两项需要先扩展字段 metadata/domain，再接 Tune UI。

## 2026-06-18 追加交接：媒体字段请求映射

代码提交 `1f0bad6 fix(quickcreate): include media fields in upload params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 请求构造层的 `uploadFields()` 已复用 `isQuickCreationUploadField()`，因此 `IMAGE/VIDEO/AUDIO/UPLOAD` 都会进入素材 URL 列表参数映射。
- 真实模型里 `fieldType=IMAGE,paramKey=imageUrls` 的字段，上传图片后会写入 `quickCreationListParams["imageUrls"]`。
- 旧的 `UPLOAD` 字段行为保持不变；完整 `QuickCreateScreenModelTest` 已通过。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to service image field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 上传字段提交前校验应继续复用 `isQuickCreationUploadField()`，避免 UI、请求映射、校验三套规则再次分叉。
- 需要覆盖 required/maxInputCount，尤其是图生图的 `imageUrls` 和视频模型的参考图/视频/音频字段。

## 2026-06-18 追加交接：上传字段提交前校验

代码提交 `233aa91 fix(quickcreate): validate service upload fields before submit` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldUiModel.quickCreationUploadValidationError(uploadedCount)` 已覆盖上传字段的 required 非空和最大文件数校验。
- 最大文件数优先级为 `inputExtra.maxInputCount` 高于顶层 `maxUploadCount`，这与 Tune 上传提示的展示规则一致。
- `QuickCreateScreenModel.generate()` 会先等待挂起上传完成，再校验当前服务端上传字段；失败时设置 `taskStatus=IDLE` 和字段错误，不调用 repository 的正式 `generateImage/generateVideo`。
- 校验只统计 `UploadStatus.DONE` 且 `remoteUrl` 非空的素材，避免本地待上传、失败上传或空 URL 被误当成有效提交参数。
- 上传字段范围继续复用 `IMAGE/VIDEO/AUDIO/UPLOAD` 的统一 helper，后续新增字段类型时应先扩展 helper，再同步测试 UI、请求映射和校验。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when required service image field has no upload"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 继续处理 `inputsChildList`、条件字段和复杂 `skuInputExtraJson`。这些能力应先补 domain/metadata 解析和 helper 单测，再接 Tune UI。
- 后续如需验证真实视频 `prepare/commit/list/detail`，仍必须先取得新的明确扣费授权；本轮没有触发任何真实生成或扣费。
- 继续不要把既有 `AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录混入后续提交。

## 2026-06-18 追加交接：inputsChildList 子输入解析

代码提交 `ef79c92 fix(quickcreate): parse service input child metadata` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldExtra.inputChildren` 已承载 `skuInputExtraJson` 中的子输入列表。
- 子输入 domain 类型为 `QuickCreationServiceFieldInputChild`，当前保留 `fieldKey/paramKey/fieldType/required/visible/defaultValue/title/paramDescription/placeholder/options/visibleWhen`。
- mapper 兼容 `inputsChildList/inputChildList/children`，并兼容子列表是 JSON array 或 JSON array 字符串的形态。
- 子输入 options 支持对象数组和 primitive 数组；简单条件 metadata 支持 `showWhen/visibleWhen/dependsOn`，输出为 `QuickCreationServiceFieldVisibilityCondition`。
- 这一步只把复杂字段从 raw JSON 解析到 domain，没有改变 Tune UI 展示，也没有改变正式请求参数构造。

验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest.maps service field input child list metadata"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
git diff --check
```

下一步建议：
- 先在 `QuickCreationServiceFieldUiModelTest` 定义子输入渲染规则：父字段何时显示子字段、子字段标题/占位/说明怎么回退、子字段是否复用文本/上传校验 helper。
- 再接 `TuneBottomSheet`：渲染当前父字段激活的 `inputChildren`，并把子字段值写回 `serviceParams`，参数 key 使用子字段 `paramKey`。
- 最后补 `QuickCreateScreenModel` 提交前校验和请求构造测试，确保子字段只在可见/激活时进入正式 params。
- 继续不要触发真实生成；视频完整 `prepare/commit` 仍需新的明确扣费授权。

## 2026-06-18 追加交接：激活子输入渲染与透传

代码提交 `ab64160 fix(quickcreate): render active service child fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldUiModel.quickCreationActiveInputChildren(params)` 是当前子输入显示规则入口。
- 无 `visibleWhen` 的子输入默认跟随父字段显示；有 `visibleWhen` 的子输入会按 `params` 中的父字段值或父字段默认值判断是否激活。
- `TuneBottomSheet` 的图片高级参数区会在父字段下缩进展示激活子输入。子输入当前支持三类基础形态：options、文本/数值输入、上传提示。
- 子输入写回 `serviceParams` 时使用自身 `paramKey`；`QuickCreateScreenModel.hasFieldParam()` 已允许子输入 `paramKey` 进入 `quickCreationParams`。
- 这解决了“metadata 已解析但 Tune 不能填、填了也提交不出去”的第一层问题。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active child inputs follow parent selection metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image submits declared child service field values"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 子输入校验仍要补：文本 required/minLength/maxLength、上传 required/maxInputCount、以及“未激活子输入不应触发校验”。
- 当前子输入值只要 paramKey 属于模型子字段就允许提交；后续可以把 `hasFieldParam()` 收紧为“当前激活的字段白名单”，但需要同时处理 fee-preview 和模板回填。
- 视频高级参数区仍未渲染服务端模型字段；要继续把图片端的服务端参数区抽成 image/video 共用。
- 继续不要触发真实生成或视频扣费；完整视频 `prepare/commit` 仍需新的明确授权。

## 2026-06-18 追加交接：激活子输入文本校验

代码提交 `56f0639 fix(quickcreate): validate active child text fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 子输入 domain `QuickCreationServiceFieldInputChild` 已保留 `maxLength/minLength`；mapper 会从子输入对象或子输入内嵌 `skuInputExtraJson` 中解析。
- `QuickCreationServiceFieldInputChild.quickCreationTextValidationError(value)` 已覆盖 required 非空和 `minLength` 最小长度。
- `QuickCreateScreenModel.validateServiceFields()` 会先校验顶层文本字段，再校验当前激活的子文本字段。
- 激活规则继续复用 `quickCreationActiveInputChildren(params)`；未激活子输入不会参与提交前校验，因此不会因为隐藏 required 字段误拦截生成。
- 本轮只处理文本/数值类子输入校验，没有处理上传类子输入校验，也没有触发真实生成。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.required child text validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child min length validation uses child metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active required child text field is empty" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.inactive required child text field does not block image generation"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
.\gradlew.bat :shared:testDebugUnitTest
git diff --check
```

下一步建议：
- 给 Tune 子输入文本框接 `maxLength` 截断和计数展示，行为应与顶层文本字段一致。
- 给上传类子输入补 required/maxInputCount 校验；需要确认子输入上传如何映射到当前素材入口，避免和顶层上传字段重复计数。
- 视频高级参数区仍未复用服务端模型字段 UI，下一步可以先抽 `ServiceFieldOptionsContent` 为 image/video 共用。
- 继续不要触发真实生成或视频扣费；完整视频 `prepare/commit` 仍需新的明确授权。

## 2026-06-18 追加交接：子输入 maxLength 截断与计数

代码提交 `ef7eab1 fix(quickcreate): enforce child text max length` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreationServiceFieldInputChild.constrainQuickCreationTextInput(value)` 会读取子输入自身的 `maxLength`，当限制存在且非负时截断输入。
- `QuickCreationServiceFieldInputChild.quickCreationTextLimitCounter(value)` 会返回 `当前长度/最大长度`，Tune 子输入文本框已展示该计数。
- `TuneBottomSheet.ServiceChildFieldInput` 写回子输入文本参数前会先应用截断结果，因此进入 `serviceParams` 和后续 `quickCreationParams` 的值不会超过当前已解析的子字段长度限制。
- 这一步只处理文本/数字类子输入，不改变顶层字段、上传字段、主 prompt 输入框或真实生成链路。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child text input is constrained by max length metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child text limit counter uses max length metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 继续补上传类子输入 required/maxInputCount 校验；需要先确认子输入上传入口如何和当前素材列表关联，避免把顶层素材数量误算到子字段上。
- 抽取图片端服务端字段渲染区，让视频高级参数区也能展示服务端模型字段和子输入。
- 继续避免触发真实生成或视频扣费；完整视频 `prepare/commit/list/detail` 仍需要用户重新给出明确扣费授权。
- 后续提交仍不要纳入既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪的 `output/` 证据目录。

## 2026-06-18 追加交接：子上传字段映射与校验

代码提交 `0a27fe4 fix(quickcreate): validate child upload fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 子输入 domain 已保留 `maxInputCount`；`QuickCreationModelMapper` 会从子输入对象自身或其内嵌 `skuInputExtraJson` 读取。
- `QuickCreationServiceFieldInputChild.quickCreationUploadValidationError(uploadedCount)` 会校验 required 和 `maxInputCount`。
- `QuickCreateScreenModel.quickCreationListParams()` 会同时输出顶层上传字段和当前激活的子上传字段。子上传字段媒体类型按 `fieldType/fieldKey/paramKey` 里的 IMAGE/VIDEO/AUDIO 判断；图片 tab 的泛型 `UPLOAD` 子字段仍可回退到 IMAGE。
- `validateServiceUploads()` 会先校验顶层上传字段，再校验 active child upload；带 `visibleWhen` 且未激活的子上传字段不会阻断生成。

重要边界：
- 当前实现仍复用页面现有素材列表作为上传来源，所以适合“一个模型字段消费当前参考图/视频/音频”的场景。
- 尚未支持每个子上传字段拥有独立素材槽。如果真实服务端模型同时存在多个独立图片子槽，后续需要扩展 UI state，让素材和 `paramKey` 绑定，而不是仅按媒体类型分组。
- 本轮没有触发真实 `prepare/commit`，也没有新增扣费。

验证命令：

```powershell
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest.maps service field input child list metadata"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.required child upload validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.child upload max count validation uses child metadata" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to active child upload field" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active required child image field has no upload"
.\gradlew.bat :shared:testDebugUnitTest --tests "com.runninghub.shared.data.repository.QuickCreationModelMapperTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 把图片端 `ServiceFieldOptionsContent` 抽成 image/video 共用，让视频高级参数区也能展示服务端模型字段和子输入。
- 如果抓包发现同一模型有多个独立上传子槽，先调整 `MediaReference` 或新增绑定结构，再把 Tune UI 的上传入口按 `paramKey` 分流。
- 继续不要把既有 `AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录混入后续提交。

## 2026-06-18 追加交接：隐藏子字段不再提交

代码提交 `954e2f8 fix(quickcreate): skip inactive child params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `updateImageServiceParam()` 和 `updateVideoServiceParam()` 仍允许写入模型声明过的子字段，方便 Tune UI、模板回填或父字段切换后保留临时输入。
- 正式组装 `quickCreationParams` 时会调用 `activeServiceParamKeys(serviceParams)`，只允许顶层字段和当前 `quickCreationActiveInputChildren(serviceParams)` 返回的子字段进入请求。
- 这意味着用户填写过某个条件子字段后，如果父字段切换导致该子字段隐藏，旧值不会再带入 `generateImage/generateVideo` 的正式参数。
- 该规则同时作用于图片和视频的 `quickCreationParams`；列表型上传参数已经在上一轮通过 `activeChildUploadFields()` 按激活子上传字段过滤。

验证命令：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.inactive child service field value is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 继续抽取服务端字段 UI，让视频 Tune 高级参数区也能消费同一套动态字段与子字段能力。
- 如果后续决定在父字段切换时主动清理隐藏子字段 UI state，需要同步评估模板回填和用户切回父选项时是否应保留历史输入。
- 继续不要触发真实生成或视频扣费；完整视频 `prepare/commit` 仍需要新的明确授权。

## 2026-06-18 追加交接：视频 Tune 服务端字段 UI

代码提交 `aea0b07 fix(quickcreate): show video service fields in tune panel` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `TuneBottomSheet` 现在有 `onVideoServiceModelSelected` 和 `onVideoServiceParamChange`，调用点在 `QuickCreateScreen` 中分别接到 `QuickCreateScreenModel.updateVideoServiceModel()` 和 `updateVideoServiceParam()`。
- 视频高级参数区会展示视频服务端模型列表、当前选中模型、模型字段数量，并可切换选中模型。
- 当选中视频服务端模型包含可渲染字段时，视频高级参数区会复用 `ServiceFieldOptionsContent` 展示 options、文本/数字输入、上传字段提示和 active child input。
- 视频高级参数区已改为 vertical scroll，服务端字段较多时仍能滚动访问原有真人模式、生成音频、时长和 Seed 控件。

验证命令：

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest"
git diff --check
```

下一步建议：
- 在模拟器或真机打开视频 tab 的 Tune 高级参数区，确认视频服务端模型列表和动态字段真实可见且不遮挡底部输入区。
- 继续把服务端模型列表 UI 从图片/视频重复代码中抽成共用 Composable，降低后续维护成本。
- 继续不要触发真实视频生成或扣费；完整视频 `prepare/commit` 仍需要新的明确授权。

## 2026-06-18 追加交接：视频 Tune 高级参数 UI 复测

本轮基于最新 debug APK 在 `emulator-5554` 做了真实 UI 复测，未触发生成和扣费。

复测路径：
- 构建并安装 `composeApp/build/outputs/apk/debug/composeApp-debug.apk`。
- 启动 `com.runninghub.app/.MainActivity`。
- 进入底部 `创作` tab，切换到视频 tab，默认视频模型显示 `Seedance2.0`，价格区显示 `¥6.00`。
- 点击视频模型行打开 `创作调优`，进入 `高级` tab。
- 高级页先展示视频 `服务端模型` 列表，包含 `Seedance2.0`、`Seedance2.0-首尾帧`、`Seedance2.0-Fast`、`全能视频X 1.5-图生视频-官方版`、`可灵...`、`PixVerse...`、`Vidu...` 等多屏模型。
- 在高级页继续向下滚动到底部后，字段区可见，UI 树确认出现 `是否返回视频尾帧图片`、`是（支持真人模式）`、`否`，同时原有视频高级项 `真人模式`、`生成音频`、`时长`、`Seed（留空为随机）` 仍可访问。

证据文件：
- `output/quickcreate_video_tune_advanced.xml`
- `output/quickcreate_video_tune_advanced_scrolled_10.xml`
- `output/quickcreate_video_tune_advanced_fields.png`

后续注意：
- 服务端模型列表过长，当前虽然可滚动到字段和原有视频高级项，但用户要找字段成本较高；建议下一步优化服务模型选择控件。
- 继续不要把既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录混入提交。
- 完整视频扣费链路仍未复测；只有用户再次明确授权扣费后，才能继续真实 `prepare/commit/list/detail`。

## 2026-06-18 追加交接：紧凑服务端模型选择器

代码提交 `fd77866 fix(quickcreate): compact service model picker` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `TuneBottomSheet` 中图片和视频高级参数区共用 `ServiceModelPickerContent`。
- 服务端模型不再以内联长列表占据高级页，而是显示当前模型摘要；点击摘要后通过下拉菜单选择其他模型。
- 选中模型的 `ServiceFieldOptionsContent` 紧跟选择器渲染，因此视频高级页打开后即可看到 `服务端参数` 和 `prompt` 等动态字段。
- 下拉菜单仍显示模型名、分组名、参数数量和当前选中勾选态；服务模型加载态和空态保留。
- 如果状态里没有 `selectedServiceModel`，选择器显示 `请选择服务端模型`，避免把第一项显示成已选但不渲染字段。

验证记录：
- `.\gradlew.bat :composeApp:compileDebugKotlinAndroid` 通过。
- `.\gradlew.bat :composeApp:assembleDebug` 通过，安装到 `emulator-5554` 后打开 `创作 -> 视频 -> Seedance2.0 -> 创作调优 -> 高级`。
- UI 树 `output/quickcreate_service_picker_advanced.xml` 显示 `服务端模型` 后紧跟 `服务端参数`、`prompt`、`视频生成提示词`。
- UI 树 `output/quickcreate_service_picker_dropdown.xml` 显示下拉菜单中的 `Seedance2.0`、`Seedance2.0-首尾帧`、`Seedance2.0-Fast`、`全能视频X 1.5-图生视频-官方版` 等模型。
- 截图证据为 `output/quickcreate_service_picker_dropdown.png`；`output/` 继续保持未跟踪，不纳入提交。

下一步建议：
- 继续处理“多个独立上传子槽”问题：如果一个模型同时要求多个图片/视频/音频上传字段，应建立素材列表与字段 `paramKey` 的绑定结构，而不是只按媒体类型复用全局素材。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：字段级上传素材绑定

代码提交 `76b6de3 fix(quickcreate): bind uploads to service fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `MediaReference.fieldParamKey` 表示素材是否绑定到某个服务端上传字段；为空时仍是底部全局参考素材。
- `pickImageReferenceForField/pickVideoReferenceForField/pickAudioReferenceForField` 用于字段级上传入口，旧的 `pickImageReference/pickVideoReference/pickAudioReference` 保持兼容。
- 请求组装 `quickCreationListParams` 时，字段级素材优先；没有字段级素材时才回退到同媒体类型的全局素材。
- 校验上传数量时也按字段级素材优先计数，避免两个图片子字段因为共享全局图片而误判通过。
- Tune 高级参数区现在会为上传字段显示专属选择按钮，例如图生图模型的 `imageUrls` 字段下显示 `选择图片`。

测试覆盖：
- 新增 `generate image maps field bound images to matching child upload fields`，覆盖两个 active child image 字段分别提交不同 URL。
- 已跑完整快捷创作 ScreenModel 和字段 helper 测试：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
```

真实 UI 复测：
- `.\gradlew.bat :composeApp:assembleDebug` 后安装到 `emulator-5554`。
- 路径：`创作 -> 图片 -> 创作调优 -> 高级 -> 全能图片G-2.0-图生图-官方版`。
- UI 树 `output/quickcreate_field_upload_image_i2i_scrolled.xml` 确认 `imageUrls` 字段显示 `参考图片（1-4张）`、限制提示和 `选择图片`。
- 截图证据为 `output/quickcreate_field_upload_image_i2i_scrolled.png`；`output/` 仍保持未跟踪。

下一步建议：
- 找一个真实同时包含多个独立上传字段的模型做端到端 UI 验证，确认每个字段按钮都能选素材并在请求体中分别落到各自 `paramKey`。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：字段级上传素材展示与移除

代码提交 `d5b18d6 fix(quickcreate): separate field upload chips` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 底部快捷创作面板调用 `quickCreationGlobalMediaReferences()`，只展示全局参考素材。
- Tune 字段上传区域调用 `quickCreationFieldMediaReferences(paramKey)`，只展示当前字段绑定的素材。
- 字段级上传素材会显示为 `MediaChipCard`，删除操作复用 `QuickCreateScreenModel.removeMediaReference()`。
- `ServiceUploadFieldPicker` 的上传数量和上传中数量都按字段 `paramKey` 计算，不再看同类型的其它字段素材。

验证记录：
- 新增 helper 测试覆盖“全局素材排除字段绑定素材”和“字段素材只匹配精确 `paramKey`”。
- 已跑：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :composeApp:assembleDebug
git diff --check
```

真实 UI 复测：
- 路径：`创作 -> 图片 -> 创作调优 -> 高级 -> 全能图片G-2.0-图生图-官方版`。
- UI 树 `output/quickcreate_field_upload_remove_i2i_scrolled.xml` 确认 `imageUrls` 字段仍显示 `参考图片（1-4张）` 和 `选择图片`。
- 截图证据为 `output/quickcreate_field_upload_remove_i2i_scrolled.png`，继续保持未跟踪。

下一步建议：
- 在具备可控素材的设备上实际为字段上传一张图片，确认素材卡片出现在字段区域且可删除，同时底部全局参考区不显示该字段素材。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：字段级素材不污染 legacy reference

代码提交 `00616cc fix(quickcreate): keep field uploads out of legacy refs` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 字段级素材仍按 `fieldParamKey` 写入 `quickCreationListParams`。
- 旧的 `referenceImageUri/referenceVideoUri/referenceAudioUri` 只消费底部全局参考素材。
- 因此图生图等服务端字段上传不会再把同一个 URL 同时提交到 `imageUrls` 和 `referenceImageUri`。
- fee-preview 和正式生成都复用 `buildImageGenerationRequest/buildVideoGenerationRequest`，所以二者参数口径一致。

验证记录：
- 先给字段级双图片测试补红灯断言：字段素材不能填充 `referenceImageUri`。
- 修复后已跑：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps field bound images to matching child upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 在可控素材设备上做一次真实字段级上传，检查字段卡片、底部全局卡片、fee-preview 请求体和正式提交请求体四者一致。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：非激活上传字段不阻塞提交等待

代码提交 `40844be fix(quickcreate): ignore inactive field uploads while waiting` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `quickCreationActiveUploadParamKeys(serviceParams)` 会根据当前服务端模型、顶层字段可见性和 active child 规则，计算仍然有效的上传字段 `paramKey` 集合。
- `quickCreationRelevantMediaReferences(activeFieldParamKeys)` 保留全局素材，并只保留绑定到当前 active 上传字段的字段级素材。
- `QuickCreateScreenModel.awaitPendingUploads()` 已改为使用上述有效素材列表；如果用户先为某个条件上传字段选择素材，随后切换父字段导致该上传字段隐藏，隐藏字段的上传中/处理中状态不会继续阻塞提交。
- 这次变更不改变正式请求体的字段级映射规则：字段级素材仍通过 `fieldParamKey` 进入 `quickCreationListParams`，全局素材仍用于兼容 legacy reference 字段。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 在可控素材设备上实际上传一张字段级图片，切换父字段使该上传字段隐藏，再确认生成按钮不会被隐藏字段上传状态卡住。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：隐藏顶层上传字段不参与提交

代码提交 `5c8a039 fix(quickcreate): skip hidden upload fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateScreenModel.uploadFields()` 已收紧为 `isQuickCreationServiceFieldRenderable() && isQuickCreationUploadField()`。
- 顶层服务端上传字段如果 `visible=false`，Tune UI 不渲染它，提交前上传校验也不再要求它，即使该字段 marked required。
- 绑定到隐藏顶层上传字段 `paramKey` 的字段级素材不会进入 `quickCreationListParams`。
- active child 上传字段逻辑不变，仍由 `quickCreationActiveInputChildren(serviceParams)` 控制。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden required service upload field does not block image generation" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service upload field media is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 后续如果发现隐藏顶层文本字段也会进入 `quickCreationParams`，应按同样原则收紧 `activeServiceParamKeys()` 和顶层文本校验，只让可渲染顶层字段或 active child 字段提交。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：隐藏顶层文本字段不参与提交

代码提交 `51710bf fix(quickcreate): skip hidden text fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 顶层服务端字段的默认参数、提交白名单、提交前文本校验现在都按 `visible=true` 过滤。
- `visible=false` 的顶层文本字段即使有 `required=true`，也不会阻断生成。
- `visible=false` 的顶层文本字段即使被模板或旧 UI state 写入 `serviceParams`，也不会进入 `quickCreationParams`。
- 可见但不一定可渲染的服务端字段仍可提交默认值；这保留了现有 LIST 字段默认值行为，避免误删服务端需要的可见默认参数。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden required service text field does not block image generation" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service text field value is not submitted" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service text field default value is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 若后续抓包发现服务端存在 `visible=false` 但必须提交默认值的内部字段，需要在 mapper 层区分“隐藏但必须提交”和“隐藏且前端不应提交”，不能再单靠 `visible` 承载两种语义。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：隐藏父字段子上传不参与提交

代码提交 `891ddc6 fix(quickcreate): skip hidden parent uploads` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 顶层父字段 `visible=false` 时，其 `inputChildren` 中的上传字段不会进入 active child upload 集合。
- 提交前上传等待、上传校验和 `quickCreationListParams` 均不会消费隐藏父字段下绑定的字段级素材。
- 可见父字段下的 active child 上传规则保持不变，仍由 `quickCreationActiveInputChildren(serviceParams)` 和 child 自身 `visible/visibleWhen` 控制。
- 该规则和上一轮隐藏顶层文本/上传字段过滤形成一致边界：隐藏父字段不应通过子字段间接影响提交。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active upload param keys include visible parent and active child upload fields" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden parent child upload field media is not submitted"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 如后续发现隐藏父字段下有服务端强制提交的内部子字段，需要在 mapper 层显式建模该语义，不应让 UI 隐藏字段默认进入用户提交路径。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：非提交参数不触发 fee preview

代码提交 `24a0920 fix(quickcreate): skip preview for inactive params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 服务端参数更新会先写入当前 tab 的 `serviceParams`，但只有该参数属于更新后的 active 提交白名单时才刷新价格预览。
- 这意味着隐藏字段、隐藏父字段子字段、当前未激活子字段等不会进入正式请求的参数变更，不会重复发起 fee-preview。
- 父字段切换仍会触发预览，因为父字段自身属于 active key；切换后 active child 集合变化会通过父字段变更刷新请求体。
- 该规则只控制预览调度，不改变正式请求体过滤规则。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service param update does not refresh image fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check
```

下一步建议：
- 如后续发现 active 子字段被动失活但没有父字段变更事件，需要检查 Tune UI 是否总是通过父字段更新触发调度。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才能继续真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 改动和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：隐藏字段媒体不触发 fee preview

代码提交 `0c8136e fix(quickcreate): skip fee refresh for hidden media` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 媒体上传状态变更、上传完成、移除媒体时，只有当前正式请求会消费的媒体才会触发费用预览刷新。
- 判断来源复用 `currentRelevantMediaReferences()`：底部全局素材始终相关；字段级素材只有绑定到当前 active 上传字段 `paramKey` 时才相关。
- 隐藏顶层上传字段、隐藏父字段下的子上传字段、当前未激活条件下的子上传字段，即使保留在 UI state 中，也不会因为上传进度或上传完成额外发起 fee-preview。
- 正式请求体过滤规则没有变化；这次只收紧 fee-preview 调度条件，避免同一有效请求被隐藏素材重复刷新价格。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.hidden service upload field media does not refresh image fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 可以继续补“隐藏/非活跃字段媒体移除不刷新 fee-preview”的显式测试；当前实现已按移除前相关性判断，但还没有单独回归用例。
- 真实设备上仍建议复测字段级媒体的选择、上传中、上传完成、移除四个 UI 状态，确认底部全局素材区和 Tune 字段素材区显示一致。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：上传完成前不触发 fee preview

代码提交 `bb9e8af fix(quickcreate): wait for uploaded media before fee refresh` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `scheduleFeePreviewForMediaReference(id)` 现在不仅检查媒体是否仍属于 `currentRelevantMediaReferences()`，还要求该 `MediaReference` 已 `UploadStatus.DONE` 且 `remoteUrl` 非空。
- 这与请求体组装保持一致：`buildImageGenerationRequest()`、`buildVideoGenerationRequest()` 和 `quickCreationListParams()` 都只消费已完成上传且有 URL 的素材。
- 上传进度更新不会再因为全局素材或 active 字段素材处于上传中而重复刷新价格；上传完成后才会按真实请求体刷新。
- 移除媒体时，只有移除前已经影响请求体的素材才刷新价格；尚未上传完成或隐藏/非活跃素材的移除不会产生额外 fee-preview。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.uploading global image media does not refresh image fee preview before remote url exists"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 继续补一个显式用例覆盖“移除未完成上传的全局素材不刷新 fee-preview”，当前实现已经按同一 helper 处理，但单独行为还没有命名测试。
- 真实设备复测时重点观察素材上传耗时较长的场景，确认上传中不会出现多余 fee-preview 请求，上传完成后才刷新。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：上传状态回写原始 tab

代码提交 `bb291e0 fix(quickcreate): keep uploads bound to original tab` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `addMediaReference()` 会在素材创建时捕获 `targetTab = _uiState.value.currentTab`，并把该 tab 传入上传任务。
- `uploadReference()` 及 `updateReferenceStatus()` 使用 `targetTab` 回写素材状态，不再读取任务完成时的 `state.currentTab`。
- 因此用户在图片上传中切到视频 tab，图片素材仍会完成并留在 `imageConfig.mediaReferences`；反向视频上传同理。
- fee-preview 调度仍由 `scheduleFeePreviewForMediaReference()` 判断当前相关素材和 `remoteUrl` 状态；后台 tab 上传完成不会错误写到当前 tab。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image upload completion updates image config after switching to video tab"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补视频方向的显式回归测试：视频 tab 选择视频/音频素材后切回图片 tab，上传完成后切回视频生成，请求体仍应带上对应 URL。
- 真实设备上复测跨 tab 上传和移除，尤其是 Tune 字段级上传与底部全局上传同时存在时的状态显示。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：素材删除跨 tab 生效

代码提交 `457d859 fix(quickcreate): remove media across tabs` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `removeMediaReference(id)` 会取消对应上传任务，并同时从图片配置和视频配置中按 id 删除素材。
- 这个逻辑让删除动作不再依赖当前 tab；即使用户点删除后马上切 tab，或者异步回调在另一个 tab 执行，原 tab 素材也不会残留进请求体。
- `shouldRefreshFeePreview` 仍使用删除前的 `currentRelevantMediaReferences()` 判断，避免后台 tab 素材删除错误刷新当前 tab 价格。
- 测试侧移除了“上传中不刷新 fee-preview”用例里的人工上传延迟；`StandardTestDispatcher` 已足够保证协程在切 tab 后才执行，测试更稳定。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.remove media reference removes image media after switching to video tab"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补视频方向删除测试：视频 tab 上传视频/音频素材后切到图片 tab 调用删除，再切回视频生成，应不再提交对应 reference URL。
- 真实设备上复测删除时 UI 卡片消失、底部全局素材区和 Tune 字段素材区各自过滤是否一致。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：全局素材 fallback 消除歧义

代码提交 `fddd9a3 fix(quickcreate): avoid ambiguous global upload fallback` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `uploadFieldCountByType(serviceParams, fallbackMediaType)` 会把顶层可渲染上传字段和当前 active child 上传字段一起纳入统计。
- `fallbackUrlsForSingleUploadField()` 只在同媒体类型活跃上传字段数量为 1 时返回底部全局素材 URL。
- 请求体组装和 required 上传校验共用这套规则，避免请求体和校验对全局素材 fallback 的理解不一致。
- 多个同类型字段时，只有字段级绑定素材会进入对应 `paramKey`；底部全局素材仍可用于 legacy `referenceImageUri/referenceVideoUri/referenceAudioUri`。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.global image media does not fill multiple service image fields" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image maps uploaded images to service image field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补 required 校验的显式回归：两个 required 图片上传字段加一个全局图片时，应阻止生成并提示缺少字段素材。
- 真实设备上复测多字段模型的底部全局素材、字段级素材和 fee-preview 请求体，确认没有同 URL 多参数污染。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：灵感模板 listParams 绑定字段

代码提交 `be77572 fix(quickcreate): bind template media to list params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `templateMediaReferences()` 遍历 `QuickCreateInspirationTemplateDetail.listParams` 时，会把 map key 写入每个模板素材的 `fieldParamKey`。
- 例如模板返回 `listParams = {"imageUrls": ["..."]}`，生成的 `MediaReference` 会绑定到 `imageUrls`，后续请求体组装进入 `quickCreationListParams["imageUrls"]`。
- 这和用户手动字段级上传保持一致，也避免多图片字段模型下模板素材被当成全局素材后因 fallback 歧义丢失。
- 旧的模板素材仍为 `UploadStatus.DONE` 且保留 `remoteUrl`，不需要重新上传。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps list params bound to service fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补视频模板字段素材回归：模板带 `referenceVideos/referenceAudios` 时，应绑定到对应字段而不是全局素材。
- 真机复测灵感模板进入创作页后，字段级素材卡片是否显示在 Tune 对应字段区域，而不是底部全局素材区。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：模板素材 id 按 listParams 字段区分

代码提交 `6c4f6cc fix(quickcreate): make template media ids field specific` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateInspirationTemplateDetail.templateMediaReferences()` 会把 `listParams` 的 key 同时用于 `MediaReference.fieldParamKey` 和内部素材 id 片段。
- 同一模板内多个同媒体类型字段，例如 `imageUrls` 与 `maskUrls`，即使都是第 0 张图片，也会得到不同 id。
- id 片段只保留 ASCII 字母和数字，其余字符替换为 `_`；空 key 片段回退为 `field`。
- 这次改动不改变请求体映射，只消除模板素材列表在 UI key、删除和上传状态更新中的 id 冲突风险。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps media ids unique per list param field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测一个灵感模板含多个同类型素材字段的场景，重点观察字段级卡片显示、删除目标和最终 `quickCreationListParams` 是否一致。
- 若后续发现服务端字段 key 含非 ASCII 且不同 key 规整后相同，需要在 id 里追加 map 顺序或 key hash；当前单测覆盖的是常见 API key 场景。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板素材 id 规整碰撞规则

代码提交 `e1d611d fix(quickcreate): prevent sanitized template media id collisions` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 模板素材 id 现在包含 `fieldIndex`，所以同一模板里两个字段 key 即使规整为相同字符串，也不会生成重复 id。
- `fieldIndex` 来自 `listParams.entries.flatMapIndexed` 的遍历顺序；它只用于客户端内部素材 id，不参与请求体字段名。
- `fieldParamKey` 继续保留原始服务端 key，例如 `image-urls` 不会被改写成 `image_urls`，最终提交仍按原 key 分组。
- 这个规则补上了上一轮交接里“规整后相同 key 需要追加顺序或 hash”的风险点，目前选择顺序索引，改动最小且不引入额外 hash 工具。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps media ids unique when field keys sanitize equally"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 如果未来服务端 `listParams` 顺序不稳定但需要跨刷新保留同一素材 id，可再把原始 key 的稳定 hash 加入 id；当前实现已满足单次模板应用内唯一性。
- 继续补视频模板字段级素材回归：`referenceVideos`、`referenceAudios` 应绑定到对应字段，而不是底部全局素材。
- 真机复测字段 key 含特殊字符的模板，重点看删除目标和最终请求体是否使用原始 `paramKey`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板素材字段绑定只匹配当前模型声明字段

代码提交 `9f1013a fix(quickcreate): keep undeclared template media global` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `applyImageTemplateDetail()` 和 `applyVideoTemplateDetail()` 会先构造 `serviceParams = selectedModel.defaultServiceParams() + detail.params`。
- 模板素材绑定字段前，会调用 `selectedModel.quickCreationActiveUploadParamKeys(serviceParams)` 得到当前模型真正可接收的上传字段集合。
- `listParams` key 命中 active 上传字段时，素材写入 `fieldParamKey` 并走字段级 `quickCreationListParams`。
- `listParams` key 未命中当前模型字段时，素材保持全局；例如视频模板返回 `imageUrls`，但视频模型没有该字段时，图片会进入 `referenceImageUri`，不会被丢弃。
- 这条规则把“灵感模板字段素材绑定”和“legacy 全局参考素材”分开，避免模板 detail 返回的非模型字段 listParams 被错误吞掉。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template keeps undeclared image list params as global reference"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补一个显式视频字段级模板素材测试：模板返回 `referenceVideos/referenceAudios` 且当前视频模型声明这些字段时，应进入 `quickCreationListParams`，而不是 legacy 全局 URL。
- 真机复测 Tune 字段区和底部全局素材区在同一个视频模板里混合出现时的显示、删除和最终请求体。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：inactive child 模板素材丢弃规则

代码提交 `c59466b fix(quickcreate): ignore inactive template upload media` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `templateMediaReferences()` 现在接收 `activeFieldParamKeys` 和 `declaredFieldParamKeys` 两个集合。
- `key in activeFieldParamKeys`：生成字段级素材，写入 `fieldParamKey`。
- `key in declaredFieldParamKeys` 但不 active：直接跳过该模板素材，不生成全局素材。
- 未声明 key：保留全局素材，兼容 legacy `referenceImageUri/referenceVideoUri/referenceAudioUri`。
- `declaredFieldParamKeys` 覆盖顶层上传字段和 `inputChildren` 上传字段，但不受 `visibleWhen` 当前选中值影响；这正好用于识别“声明过但当前 inactive”的 child upload。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template does not submit inactive child upload media as global reference"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补 active child 模板素材正向回归：模板 params 激活 child upload 且 `listParams` 命中 child key 时，应进入 child 的 `quickCreationListParams`。
- 补视频模板 `referenceVideos/referenceAudios` 的字段级正向回归，作为交接里提到的显式覆盖。
- 真机复测条件字段切换后，模板素材卡片显示、删除和最终请求体是否跟当前 active 字段一致。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板 listParams key 别名映射

代码提交 `0cc985d fix(quickcreate): map template field keys to upload params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `templateMediaReferences()` 现在接收 active/declared 上传字段别名表，而不是单纯的 `paramKey` 集合。
- 每个上传字段会注册两个别名：`fieldKey -> paramKey` 和 `paramKey -> paramKey`；子上传字段同样处理。
- 模板 detail 如果返回 `listParams` key 为 `fieldKey`，客户端会把素材绑定到 canonical `paramKey`，请求体不会出现 fieldKey 版本的错误字段名。
- declared-but-inactive 判断也走别名表；所以 inactive child 如果以 `fieldKey` 出现在模板 listParams 中，也会被丢弃而不是回退到全局素材。
- 未声明 key 仍保持全局素材，用于兼容 legacy 参考图/视频/音频字段。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template maps list param field key to upload param key"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补 active child 模板素材正向回归：模板 params 激活 child upload，`listParams` 用 child 的 `fieldKey` 或 `paramKey` 都应进入 canonical child `paramKey`。
- 真机复测真实模板返回字段 key 形式，重点确认 UI 字段槽位、删除和 prepare 请求体字段名。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板 params fieldKey 归一化

代码提交 `e2cf988 fix(quickcreate): map template params to service param keys` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `applyImageTemplateDetail()` / `applyVideoTemplateDetail()` 不再直接使用 `detail.params` 覆盖服务字段默认值。
- 现在先调用 `selectedModel.canonicalTemplateParams(detail.params)`，把已知服务字段的 `fieldKey` 映射到 canonical `paramKey`。
- `serviceParams = defaultServiceParams() + templateParams`，所以模板 fieldKey 值可以正确覆盖默认 paramKey 值。
- active child 上传字段计算、最终 `quickCreationParams`、字段级 `quickCreationListParams` 都基于 canonical `paramKey`。
- 未知 key 保留原样，继续服务于模板样式、比例、分辨率等非服务字段解析。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template maps param field key before resolving active child upload media"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 补视频模板 `referenceAudios` 正向回归，确认 fieldKey/paramKey 两种 key 都能进入 canonical audio 参数。
- 继续审查模板 `params` 里 unknown key 是否会进入 `quickCreationParams`；如果真实接口要求只提交服务字段，需要把非服务模板配置和服务字段参数进一步拆开。
- 真机复测带条件 child upload 的灵感模板：父字段由模板 params 激活后，素材卡片应出现在对应 child 槽位，并按 canonical `paramKey` 提交。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板 params canonical key 优先级

代码提交 `7c92113 fix(quickcreate): prefer canonical template params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `canonicalTemplateParams()` 分两步处理模板 params。
- 第一步把所有已知 `fieldKey`/`paramKey` 归一化到 canonical `paramKey`，未知 key 保留原名。
- 第二步把原始 key 已经是 canonical `paramKey` 的值再覆盖一次。
- 因此如果服务端同时返回 `creationMode` 和 `mode`，最终以 `creationMode` 的值为准，避免别名覆盖正式字段。
- 这条规则会影响模板 params 激活 child upload、Tune 默认显示和最终 `quickCreationParams`，但不影响 `listParams` 素材绑定别名规则。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template keeps canonical param value over field key alias"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 继续审查模板 detail 的 `params` 是否应拆分为 UI 配置字段和服务字段，避免非服务字段在未来接口变化时误进提交参数。
- 补视频模板 `referenceAudios` 的字段级正向回归，覆盖音频素材的 fieldKey/paramKey 别名路径。
- 真机复测字段别名冲突模板，重点看 Tune 默认值、child 激活状态和最终 prepare 请求字段。
- 后续提交继续避开既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：visibleWhen 条件字段别名解析

代码提交 `6a20c50 fix(quickcreate): resolve visible conditions by field aliases` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 条件字段判断现在先构造只用于 UI/校验/归属计算的 params 视图：`quickCreationParamsWithFieldAliases(serviceParams)`。
- 这个视图会为每个服务字段和 input child 建立 `fieldKey <-> paramKey` 的值别名，并在没有显式值时使用非空默认值；它不会写回 `imageServiceParams` / `videoServiceParams`，也不会把 fieldKey 提交给后端。
- 因此当真实接口返回 `visibleWhen.fieldKey = "creationMode"`，但客户端状态只保存 `creation_mode` 这类 canonical `paramKey` 时，active child 仍能被正确渲染、校验、绑定模板素材并进入 `quickCreationListParams`。
- `TuneBottomSheet` 已在图片和视频服务字段渲染前使用同一 alias 视图，避免出现“提交路径认为 child active，但 UI 不显示”或反向不一致。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.active upload param keys resolve sibling field key conditions from param key values"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/TuneBottomSheet.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

下一步建议：
- 用真实登录态抓一个带 `visibleWhen` 且 `fieldKey != paramKey` 的模板，核对应用模板后的 Tune UI 子字段、素材卡片位置和最终 prepare 请求体。
- 继续审查 `detail.params` 里的 unknown key 是否应该进入 `quickCreationParams`；如果后端未来收紧 schema，可能需要把模板 UI 配置字段和服务字段进一步拆开。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：active child 默认值提交规则

代码提交 `0affee6 fix(quickcreate): submit active child defaults` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 顶层服务字段默认值和 active input child 默认值都会作为 `quickCreationParams` 的默认基线。
- `defaultServiceParams(activeParams)` 会用“顶层默认值 + 当前有效参数”判断哪些 child 当前 active，因此模板或用户选择激活的 child 也能带上自身 `defaultValue`。
- 请求体仍保持显式值优先：先 `putAll(model.defaultServiceParams(serviceParams))`，再把当前 active 且非空的 `serviceParams` 覆盖进去。
- 模板应用阶段使用 `selectedModel.defaultServiceParams(templateParams) + templateParams` 初始化状态，避免模板激活 child 后 UI/校验看不到 child 默认值。
- inactive child 的默认值不会进入请求体，因为 child 默认值只从 `quickCreationActiveInputChildren()` 结果里收集。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image submits active child service field defaults"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template submits defaults for child activated by template params"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 继续审查视频侧 child 默认值，特别是音频/视频参考字段以外的数值型参数，确认真实模型字段名、默认值和 `visibleWhen` 条件组合。
- 真机复测模板激活 child 后的 Tune 默认值显示和 fee-preview 请求体。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：inactive child 默认值不参与条件判断

代码提交 `55190bc fix(quickcreate): avoid inactive child default activation` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `quickCreationParamsWithFieldAliases(params)` 仍会为顶层字段使用非空默认值建立 `fieldKey/paramKey` 别名，用于父字段默认选项驱动 child 显示。
- 对 input child，则只在 `params` 已包含该 child 的 `fieldKey` 或 `paramKey` 时建立别名，不再直接读取 child.defaultValue。
- active child 默认值提交不依赖 alias helper 注入；由 `defaultServiceParams(activeParams)` 在确认 child active 后补齐。
- 因此 inactive child 的默认值不会影响 sibling `visibleWhen`，但已经 active 的 child 默认值仍会进入 UI 状态和最终 `quickCreationParams`。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.inactive child defaults do not activate sibling upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

下一步建议：
- 审查是否需要固定点方式补齐多级 active child 默认值：只有真实模型存在 child 默认值激活另一个 child 时再扩展，避免现在过度推断协议。
- 真机复测带复杂 `visibleWhen` 的模板，确认 inactive 素材字段不会显示、不会触发校验、不会进入 `quickCreationListParams`。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：stale inactive child 参数过滤

代码提交 `37338bf fix(quickcreate): ignore stale inactive child params` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `quickCreationParamsWithFieldAliases(params)` 不再是原始 params 的简单别名扩展，而是专门用于条件判断和 UI active 计算的 filtered params 视图。
- 顶层可见字段会纳入视图，使用显式值优先、非空默认值兜底。
- input child 只有在当前视图下被判定 active 后，才会把自身显式值或非空默认值加入视图。
- 通过固定点循环支持 active child 值继续激活 sibling child；但 inactive child 的 stale 显式值不会进入视图。
- 这条规则同时影响 Tune UI 渲染、active 参数过滤、上传字段归属、模板素材 active alias 和校验路径，因为这些入口都复用该条件视图。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest.inactive child explicit values do not activate sibling upload fields"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreationServiceFieldUiModelTest.kt
```

下一步建议：
- 继续审查切换父字段时是否需要主动清理 inactive child 的媒体引用；当前请求体会过滤 inactive 字段，但 UI 状态仍保留引用以便用户切回。
- 真机复测复杂 `visibleWhen` 模型：先激活并填写 child，再切回父字段默认分支，确认 sibling 上传字段不显示、不校验、不提交。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：media id tab/category 隔离

代码提交 `4b9034f fix(quickcreate): isolate media ids by tab` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 手动上传素材 id 格式从 `${type}_${timestamp}` 改为 `${currentTab}_${type}_${timestamp}`。
- 模板素材 id 格式加入 `categoryId` 片段：`template_${categoryId}_${templateId}_${fieldIndex}_${key}_${mediaType}_${index}`。
- `categoryId` 和 listParams key 都走同一个 ASCII 规整函数，空值回退到 `unknown`。
- `removeMediaReference(id)` 仍保持跨 image/video config 过滤同 id 的实现；现在依靠 id 生成规则确保不同 tab 的不同素材不会撞 id。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.removing image template media does not remove video template media with same template id"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：图片模板和视频模板都带同名 `listParams` 时，分别删除两个 tab 的素材卡片，确认 UI 和请求体互不影响。
- 继续观察历史草稿/恢复逻辑是否会持久化旧格式 media id；当前 draft 只保存 prompt/tab，暂未发现兼容风险。
- 完整视频扣费链路仍未复测；只有用户再次明确授权后才可触发真实 `prepare/commit/list/detail`。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：模板素材媒体类型来自服务字段

代码提交 `630609b fix(quickcreate): infer template media type from service fields` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `templateMediaReferences()` 现在接收 active 上传字段别名表，别名值包含 canonical `paramKey` 和服务字段推断出的 `QuickCreateMediaType`。
- 模板 `listParams` key 命中 active 上传字段别名时，生成的 `MediaReference.fieldParamKey` 仍写 canonical `paramKey`，`MediaReference.type` 优先使用服务字段声明推断出的媒体类型。
- 因此真实模板如果返回通用 key，例如 `reference`，但当前模型声明 `fieldKey=reference,paramKey=referenceVideos,fieldType=VIDEO_UPLOAD`，客户端会把它渲染为视频字段素材，而不是按 key 文本默认成图片素材。
- 未声明 key 仍走 legacy 全局素材路径，并继续按 key 文本推断图片/视频/音频类型；declared-but-inactive key 的丢弃规则不变。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template infers generic list param media type from service field"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测一个通用 `listParams` key 绑定视频/音频上传字段的模板，确认 Tune 字段区卡片类型、删除行为和最终 `quickCreationListParams` 一致。
- 继续审查模板 detail 的 unknown `listParams` key 是否存在“字段声明缺失但后端仍要求字段级提交”的例外；当前规则会把未声明 key 当 legacy 全局素材处理。
- 完整视频扣费链路本轮未复测；本轮没有点击真实生成、没有新增扣费。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：恢复草稿触发价格预览

代码提交 `1814fc7 fix(quickcreate): refresh fee preview after draft restore` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `restoreDraft()` 会先恢复 `imagePrompt`、`videoPrompt` 和 `currentTab`，再清除持久化草稿。
- 清除草稿后会调用 `scheduleFeePreview()`，让恢复后的当前 tab 请求体重新进入服务端 fee-preview debounce 流程。
- 如果恢复后的当前 tab 没有有效 prompt，`scheduleFeePreview()` 会沿用既有逻辑清空预览状态，不会发起无效请求。
- 这避免用户恢复草稿后底部按钮价格停留在旧状态，尤其是从空输入或另一个 tab 恢复到已有 prompt 时。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft refreshes fee preview for restored image prompt"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测恢复视频草稿：当前 tab 为视频且存在 videoPrompt 时，按钮应先进入价格确认中，再更新为服务端金额或明确失败态。
- 后续如果草稿扩展到服务端字段参数或素材引用，也需要在 restore 后复用同一调度点刷新 fee-preview。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：草稿恢复同步当前 tab

代码提交 `941c7b1 fix(quickcreate): restore draft tab before fee preview` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `restoreDraft()` 会根据草稿中的 `currentTab` 恢复当前 tab：值为 `VIDEO` 时恢复视频 tab，其余情况恢复图片 tab。
- 这修复了“用户当前在视频 tab，恢复图片草稿后仍停留视频 tab”的状态错配。
- tab 恢复发生在 `scheduleFeePreview()` 之前，所以 fee-preview 会按恢复后的当前 tab 构造请求体。
- 例如恢复 `currentTab=IMAGE,imagePrompt=...` 的草稿时，只会发起图片 fee-preview，不会错误走视频 fee-preview。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore image draft switches back from video tab before fee preview"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测两个方向：从视频 tab 恢复图片草稿、从图片 tab 恢复视频草稿，确认顶部 tab、prompt、底部价格预览状态同步。
- 如果后续草稿持久化扩展到模型、服务端字段或素材引用，恢复顺序仍应保持“先恢复 tab 与请求体状态，再调度 fee-preview”。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：坏草稿清理内存状态

代码提交 `a4ba123 fix(quickcreate): clear stale invalid draft state` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `checkForDraft()` 读取到非空持久化草稿后会尝试 `parseDraftData(raw)`。
- 解析成功时写入 `draftData` 并设置 `hasDraft=true`。
- 解析失败时现在会同时设置 `draftData=null`、`hasDraft=false`，再调用 `settingsRepository.clearQuickCreateDraft()` 清理坏草稿。
- 因此同一个 ScreenModel 曾经加载过有效草稿，也不会在后续遇到损坏草稿时继续保留旧内存草稿并被 `restoreDraft()` 误恢复。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft clears stale in memory draft when stored draft is invalid"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 如果后续把草稿恢复入口接到真实 UI，需要把 `hasDraft/draftData` 从非响应式属性迁移到 `QuickCreateUiState`，否则 UI 对草稿状态变化的感知仍可能不稳定。
- 真机复测旧版本/损坏草稿存在时，页面不应展示可恢复入口，也不应恢复旧 prompt。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：空草稿清理内存状态

代码提交 `c10c3ea fix(quickcreate): clear stale missing draft state` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `checkForDraft()` 读取到空字符串或 null 草稿时，会把 `draftData` 置空并设置 `hasDraft=false`。
- 这避免同一个 ScreenModel 曾经加载过有效草稿后，在持久化草稿已被清空时继续保留旧内存草稿。
- 损坏 JSON 和空草稿现在都遵循同一原则：不可用的持久化草稿不能留下可恢复的内存状态。
- `restoreDraft()` 因 `draftData=null` 会直接返回，不会修改 prompt/tab，也不会触发 fee-preview。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft clears stale in memory draft when stored draft is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测用户丢弃草稿、成功恢复后清空、旧版本缺失草稿等路径，恢复入口不应展示旧内容。
- 如果后续把草稿恢复入口接到真实 UI，仍建议把 `hasDraft/draftData` 迁移到 `QuickCreateUiState`，让入口展示随检查结果响应式刷新。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：草稿状态已响应式化

代码提交 `70a98ad fix(quickcreate): expose draft state in ui state` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateUiState.draftData` 是草稿状态的唯一来源，`QuickCreateUiState.hasDraft` 根据 `draftData != null` 派生。
- `QuickCreateScreenModel.hasDraft` 和 `QuickCreateScreenModel.draftData` 仍可读，但只是代理到当前 `uiState`，不再维护第二份普通 mutable 状态。
- `checkForDraft()` 加载有效草稿时会更新 `uiState.draftData`；空草稿、损坏草稿、恢复草稿和丢弃草稿都会把它清空。
- 后续接入恢复入口时，Compose 只需要收集 `uiState.hasDraft`，不需要读取 ScreenModel 的非响应式属性。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.checkForDraft exposes saved draft through ui state"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateUiState.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 在真实快捷创作页面顶部或底部输入区附近接入恢复/丢弃草稿入口，并以 `uiState.hasDraft` 控制显示。
- 真机复测：进入页面检查草稿、点击恢复、点击丢弃、恢复后再次输入，入口显示和底部 fee-preview 状态应同步。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：初始化自动加载草稿状态

代码提交 `46f354c fix(quickcreate): load draft state on init` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateScreenModel.init` 会先调用 `checkForDraft()`，再加载模型、历史和项目数据。
- 页面模型创建后，已有持久化草稿会进入 `uiState.draftData`，`uiState.hasDraft` 会变为 `true`。
- 后续恢复草稿入口不需要额外在 Composable 中主动调用 `checkForDraft()` 才能拿到初始状态。
- 手动 `checkForDraft()` 仍保留，适合未来页面恢复前台或用户显式刷新草稿状态时复用。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.init exposes saved draft through ui state"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 接入真实恢复/丢弃草稿入口时，以 `uiState.hasDraft` 控制展示，以 `restoreDraft()` 和 `discardDraft()` 处理动作。
- 真机复测进入页面即展示已有草稿提示，点击恢复后入口消失、prompt/tab/fee-preview 同步更新。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：草稿恢复/丢弃入口

代码提交 `79afee0 feat(quickcreate): add draft resume entry` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `BottomPromptPanel` 会在 `uiState.hasDraft=true` 且任务未处于 `SUBMITTING/QUEUING/RUNNING` 时显示草稿提示条。
- 提示条位于底部输入区的 Tab 选择器上方，文案来自 `DraftData.resumeSummaryText()`，格式为 `上次草稿 · 图片/视频 · N 字`。
- “恢复”按钮调用 `restoreDraft()`；恢复后会按草稿的 `currentTab` 切回对应 tab、恢复 prompt、清空草稿并重新调度 fee-preview。
- “丢弃”按钮调用 `discardDraft()`；只清空草稿状态和持久化草稿，不改当前输入。
- 预览调用点已补齐空回调，保持 Compose preview 编译路径一致。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.draft resume summary describes tab and prompt length"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测草稿提示条在窄屏、键盘弹起、任务运行中、恢复后、丢弃后五条路径的显示和交互。
- 如果提示条在 320dp 宽度下按钮拥挤，可把“丢弃”改为图标按钮或二级菜单；当前实现优先保持动作直观。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：空内容草稿过滤

代码提交 `00c01cc fix(quickcreate): ignore empty saved drafts` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 草稿是否可恢复由 `DraftData.hasPromptContent` 决定：`imagePrompt` 或 `videoPrompt` 任一非空才有效。
- `checkForDraft()` 读取到空内容草稿 JSON 时，会设置 `uiState.draftData=null`，并清除持久化草稿。
- 因此 `BottomPromptPanel` 的草稿提示条不会因为空 JSON 草稿而显示“0 字”入口。
- 损坏草稿、缺失草稿、空内容草稿现在都遵循同一类清理原则：不可恢复的数据不会进入 UI 状态。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.init ignores saved draft without prompt content"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测用户清空 prompt、切换 tab、退出再进入页面时，不应显示空草稿提示条。
- 后续如果草稿扩展到素材或服务参数，需要把 `hasPromptContent` 重命名为更通用的可恢复内容判断，并纳入素材/参数有效性。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：自动保存清理空草稿

代码提交 `537ec48 fix(quickcreate): avoid autosaving empty drafts` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `autoSaveDraft()` 构造 `DraftData` 后会先检查 `hasPromptContent`。
- 有图片或视频 prompt 时，继续调用 `settingsRepository.saveQuickCreateDraft(draft.toJsonString())`。
- 两个 prompt 都为空时，调用 `settingsRepository.clearQuickCreateDraft()`，不再保存空 JSON 草稿。
- 因此空草稿不会在写入阶段产生；如果来自旧版本或外部状态，也会在 `checkForDraft()` 读取阶段被清理。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.auto save clears draft when prompts become empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：输入 prompt 后等待保存、清空 prompt 后等待 debounce、退出再进入，草稿提示条不应出现。
- 如果后续草稿要保存素材或服务参数，自动保存的“有效草稿”规则需要扩展到这些字段，否则清空 prompt 可能误删仍有素材/参数价值的草稿。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：自动保存后隐藏旧草稿入口

代码提交 `73af161 fix(quickcreate): hide stale draft entry after autosave` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `autoSaveDraft()` 在 debounce 保存完成后，会调用 `setDraftData(null)`。
- 用户如果忽略旧草稿提示条并开始输入新内容，等待自动保存后，旧草稿入口会从 `uiState.hasDraft` 消失。
- 这避免“旧草稿入口仍显示，但持久化草稿已被新输入覆盖”的状态不一致。
- 恢复/丢弃按钮行为不变；只有用户直接编辑后，入口会被自动隐藏。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.auto save hides stale draft entry after prompt changes"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：存在旧草稿入口时直接输入新 prompt，等待 debounce 后提示条应自动消失。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：草稿恢复 tab 选择有内容的一侧

代码提交 `baf2e87 fix(quickcreate): restore draft tab with prompt` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 草稿入口摘要和恢复目标 tab 都由 `DraftData.restorableTab` 决定。
- 如果 `currentTab=VIDEO` 且 `videoPrompt` 非空，则恢复视频；如果视频 prompt 为空但图片 prompt 非空，则恢复图片。
- 如果 `currentTab=IMAGE` 且 `imagePrompt` 非空，则恢复图片；如果图片 prompt 为空但视频 prompt 非空，则恢复视频。
- `DraftData.resumeSummaryText()` 使用同一规则，因此提示条文案不会再出现“视频 · 0 字”但实际只有图片 prompt 的情况。
- `restoreDraft()` 使用 `restorableTab` 后再调用 `scheduleFeePreview()`，因此 fee-preview 会按恢复后真正有 prompt 的 tab 构造请求。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.draft resume summary falls back to image prompt when video tab has no prompt" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft falls back to image tab when video prompt is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：图片 prompt 非空、切到空视频 tab、退出再进入、恢复草稿，页面应回到图片 tab 并刷新图片价格。
- 如果草稿未来保存素材或参数，需要判断素材/参数是否也参与 `restorableTab`，目前只按 prompt 决定。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：草稿恢复替换 prompt 快照

代码提交 `e4cb9d1 fix(quickcreate): replace prompts on draft restore` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `restoreDraft()` 不再只写入非空 prompt。
- 恢复时会一次性设置 `imageConfig.prompt=draft.imagePrompt`、`videoConfig.prompt=draft.videoPrompt` 和 `currentTab=draft.restorableTab`。
- 因此草稿中的空 prompt 也会清空当前 UI 中对应 tab 的旧 prompt，恢复语义是“替换成草稿快照”，不是“把草稿非空字段叠加到当前页面”。
- 恢复后仍会 `clearDraft()` 并调用 `scheduleFeePreview()`，价格预览按恢复后的 tab 和 prompt 重新计算。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore video draft clears existing image prompt"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：图片 tab 有当前输入、恢复只有视频 prompt 的草稿后，切回图片 tab 应为空。
- 如果草稿后续扩展到素材和参数，恢复动作也应明确是替换快照还是合并当前状态，避免跨 tab 旧状态残留。
- 本轮没有触发真实生成或扣费；完整视频 `prepare/commit/list/detail` 仍需新的明确授权。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：恢复/丢弃取消待执行草稿保存

代码提交 `7cb0470 fix(quickcreate): cancel draft autosave on restore` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `clearDraft()` 会先取消 `draftSaveJob` 并置空，再清空 `uiState.draftData`，最后异步调用 `settingsRepository.clearQuickCreateDraft()`。
- 影响路径包括 `restoreDraft()` 和 `discardDraft()`。用户在看到旧草稿入口后如果先输入新内容、立刻恢复或丢弃旧草稿，之前输入触发的 500ms 自动保存不会再落盘。
- `restoreDraft()` 仍会完整替换图片/视频 prompt 快照、切到 `DraftData.restorableTab`，并重新调度 fee-preview。
- `discardDraft()` 只清理草稿入口和持久化草稿，不修改当前输入。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.restore draft cancels pending autosave"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：已有草稿入口时输入新 prompt，立即点恢复；等待 500ms 后不应再次出现草稿入口，重新进入页面也不应读到刚恢复后的草稿。
- 真机复测：已有草稿入口时输入新 prompt，立即点丢弃；等待 500ms 后不应重新写回持久化草稿。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：提交成功清理内存草稿入口

代码提交 `da5cd42 fix(quickcreate): clear draft entry on submit` 和文档提交 `019ab05 docs(quickcreate): record submit draft cleanup` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `handleTaskStatus()` 收到 `QuickCreateTaskStatus.Queuing` 时，会先调用 `clearDraft()`，再把任务状态更新为 `QUEUING`。
- `clearDraft()` 统一负责取消 `draftSaveJob`、清空 `uiState.draftData`、清理持久化草稿，因此恢复、丢弃、成功提交后的草稿清理语义一致。
- 这解决了“已有旧草稿入口，用户通过模板或其它非自动保存路径改了 prompt 并成功提交后，内存里仍保留旧草稿入口”的问题。
- 生成失败、价格未确认、字段校验失败等未进入 `Queuing` 的路径不会清理草稿，保留用户继续编辑和恢复的机会。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.successful submit clears in memory draft entry"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 远端已同步；后续继续保持代码提交和文档提交交替推送。
- 真机复测：有旧草稿入口时应用灵感模板并提交，进入排队、任务完成或失败后都不应再显示旧草稿入口。
- 真机复测：字段校验失败或价格待确认时不进入 `Queuing`，草稿入口/当前输入不应被误清理。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：失败任务替换旧运行文案

代码提交 `b546ac1 fix(quickcreate): replace failed task status text` 和文档提交 `3639727 docs(quickcreate): record failed status text fix` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateTaskStatus.Failed` 会把 `taskStatus` 设为 `FAILED`，同时把 `error` 和 `statusText` 都设为服务端失败消息。
- 这避免任务状态流为 `Running -> Failed` 时，UI 中间区域仍保留旧的“生成中...N%”状态文案。
- 测试 Fake repository 现在可配置 `imageTaskStatuses`，后续可以继续复用它覆盖更复杂的图片任务状态序列。
- `QuickCreateTaskStatus.Error` 仍保持当前语义：回到 `IDLE` 并通过 `error` 弹出错误；本轮未改变这条路径。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.failed image task replaces running status text"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 远端已同步；后续继续保持代码提交和文档提交交替推送。
- 真机复测：让任务进入运行态后失败，确认中间状态区显示失败消息，而不是旧进度。
- 后续可以进一步检查 `TaskStatusArea` 对 `FAILED` 状态是否仍应显示圆形进度条；本轮只修复文案残留。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：清空结果时同步清理状态文案

代码提交 `84cce5f fix(quickcreate): clear result status text` 和文档提交 `7d331d2 docs(quickcreate): record result status cleanup` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `clearResults()` 会清空 `results`、把 `taskStatus` 置回 `IDLE`，并把 `statusText` 置为 `null`。
- 因此用户清空成功结果后，ScreenModel 不会残留“生成完成”等旧状态文案。
- 这与失败态文案修复形成闭环：任务状态区展示的文案只跟随当前任务状态，清空结果后不继续保留上一次任务文案。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.clearing successful results clears status text"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 远端已同步；后续继续保持代码提交和文档提交交替推送。
- 真机复测：成功结果出现后点击清空，页面应回到历史/项目区域，状态文案不应残留。
- 后续可继续评估 `TaskStatusArea` 对 `FAILED` 状态是否还需要替换圆形进度图标；本轮只处理状态数据一致性。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：失败状态区不再显示进度圈

代码提交 `e8e722e fix(quickcreate): show failed task indicator` 和文档提交 `43e3ddc docs(quickcreate): record failed indicator fix` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `TaskStatusArea` 不再固定显示 `CircularProgressIndicator`，而是通过 `quickCreateTaskStatusDisplay()` 得到文案和指示器类型。
- `SUBMITTING`、`QUEUING`、`RUNNING` 显示进度圈；`SUCCESS` 显示成功图标；`FAILED` 显示错误图标。
- 失败状态优先展示 `ScreenModel` 写入的服务端失败消息，没有消息时回退为“生成失败”。
- 新增 `QuickCreateTaskStatusUiTest`，后续如果状态区 UI 语义变化，应先更新这个纯映射测试。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check
```

下一步建议：
- 远端推送完成后，继续按“代码提交、文档提交”节奏推进下一个可验证缺口。
- 真机复测：让任务进入运行态后失败，确认中间状态区显示错误图标和失败消息，不再有旋转进度圈。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：项目筛选内生成成功后刷新项目任务

代码提交 `18a8d0a fix(quickcreate): refresh selected project after generation` 和文档提交 `a4fec9e docs(quickcreate): record project refresh fix` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 用户处于项目筛选状态时，`selectedProjectId` 会保留在 `QuickCreateUiState`。
- 生成成功后不再固定调用最近创作列表，而是通过 `refreshCurrentHistoryArea()` 判断：无项目筛选刷新最近历史，有项目筛选刷新当前项目任务。
- `handleTaskStatus(Success)` 将历史刷新副作用移出 `StateFlow.update` lambda，避免状态更新 lambda 重试时重复发起历史/项目任务请求。
- `selectProject()` 和成功后的项目任务刷新复用 `loadSelectedProjectTasks()`；切换项目会清空旧列表，成功后的刷新只显示 loading 并保留项目上下文。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.successful generation refreshes selected project tasks"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check
```

下一步建议：
- 远端推送完成后，继续按“代码提交、文档提交”节奏推进下一个可验证缺口。
- 真机复测：在项目筛选页完成一次已授权的低成本图片生成，确认成功后列表仍为当前项目任务，而不是跳回最近创作数据。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：通用错误状态清理旧状态文案

代码提交 `23274aa fix(quickcreate): clear status text on task error` 和文档提交 `e52e27d docs(quickcreate): record task error status cleanup` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `QuickCreateTaskStatus.Error` 会把 `taskStatus` 置回 `IDLE`，同时清空 `statusText` 并设置 `error=status.message`。
- 这补齐了上一轮 `Failed` 和 `clearResults()` 的状态文案清理闭环：任务离开可见状态区后，ScreenModel 不再保留旧运行进度文案。
- `Failed` 仍保持失败态并显示服务端失败消息；`Error` 仍保持通用错误 toast/顶部错误语义。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.errored image task clears running status text"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check
```

下一步建议：
- 远端推送完成后，继续按“代码提交、文档提交”节奏推进下一个可验证缺口。
- 真机复测：模拟网络或接口通用错误时，页面不应在后续任务状态区重用旧“生成中...N%”文案。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。
## 2026-06-18 追加交接：素材上传超时不再继续正式提交

代码提交 `f2cc0e6 fix(quickcreate): block submit on upload timeout` 和文档提交 `9cb1c35 docs(quickcreate): record upload timeout guard` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `generate()` 进入正式图片/视频生成前会调用 `awaitPendingUploads()` 等待当前 tab 的相关素材上传完成。
- 等待窗口结束后，如果相关素材仍处于 `UPLOADING` 或 `PROCESSING`，会返回 `IDLE` 并设置 `error=素材上传超时: <文件名>`。
- 只有 pending 素材全部完成，或明确进入 `FAILED` 并被错误处理拦截后，才会继续后续流程；不再把长期卡住的上传当作成功等待结束。
- 这条防线覆盖全局素材和当前激活服务端上传字段素材，因为 `awaitPendingUploads()` 使用 `currentRelevantMediaReferences()`。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when upload stays pending past wait window"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check
```

下一步建议：
- 远端推送完成后，继续按“代码提交、文档提交”节奏推进下一个可验证缺口。
- 真机复测：模拟弱网或上传接口长时间无响应，确认点击生成不会触发正式 `prepare/commit`。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：拦截提交时清理旧状态文案

代码提交 `6a4e8db fix(quickcreate): clear status text on blocked submit` 和文档提交 `c5e1644 docs(quickcreate): record blocked submit status cleanup` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `generate()` 在价格确认中、价格预览失败、服务端字段校验失败、素材等待失败和上传字段校验失败时，会把 `taskStatus` 置回 `IDLE` 并清空 `statusText`。
- 这补齐了任务状态文案清理的另一类入口：不是服务端任务返回 `Failed/Error`，而是正式提交前就被本地状态或字段校验拦截。
- 价格预览失败仍不会提交新的 `generateImage()` / `generateVideo()` 请求；本轮只清理旧状态文案，不改变扣费和提交防线。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.fee preview failure clears previous task status text when generate is blocked"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 远端推送完成后，继续按“代码提交、文档提交”节奏推进下一个可验证缺口。
- 真机复测：保留上一轮成功结果，修改 prompt 让价格预览失败后点击生成，确认页面不会继续显示旧“生成完成”状态文案。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：必填 options 字段不再漏过校验

代码提交 `2ec3a66 fix(quickcreate): require selected service options` 和文档提交 `906e630 docs(quickcreate): record required option validation` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 顶层可见服务端字段只要带 `options` 且 `required=true`，在没有当前值也没有默认值时，会在 `generate()` 的服务端字段校验阶段被拦截。
- 拦截发生在素材等待和正式 `generateImage()` / `generateVideo()` 之前，因此不会进入 `prepare/commit`，也不会新增扣费。
- 文本字段、上传字段和已有默认值的 options 字段行为不变；本轮没有扩展子字段 options 校验，避免把未经红灯测试的行为混入提交。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when required service option field is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：找一个真实模型的必填下拉字段，确认未选择时不会进入正式提交；如果服务端总是带默认值，则用接口返回样例或 MockEngine 覆盖无默认值场景。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：active child options 字段不再漏过校验

代码提交 `8c6a448 fix(quickcreate): require active child options` 和文档提交 `2236193 docs(quickcreate): record child option validation` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `validateServiceFields()` 仍只遍历当前 active 的 child 字段；inactive child 不会参与校验，也不会阻止生成。
- 对 active child 字段，如果它带 `options` 且 `required=true`，没有当前值也没有默认值时，会在正式提交前拦截。
- 该拦截发生在素材等待和正式 `generateImage()` / `generateVideo()` 之前，因此不会进入 `prepare/commit`，也不会新增扣费。
- 文本 child 字段继续走原有 `quickCreationTextValidationError()`；上传 child 字段继续由 `validateServiceUploads()` 负责。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active required child option field is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：找一个父字段能激活子下拉字段的真实模型，确认子下拉必填但未选择时不会进入正式提交。
- 若真实模型总是带默认值，后续可继续用接口样例或 MockEngine 覆盖无默认值场景，并检查 Tune 面板是否应对 required child options 给出更明确视觉提示。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：顶层 options 非法值不再进入正式提交

代码提交 `5624496 fix(quickcreate): reject invalid service option values` 和文档提交 `189a119 docs(quickcreate): record option value validation` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 顶层可见服务端 options 字段如果存在非空值，该值必须在当前服务端 `options.value` 列表内。
- 非法值会在 `generate()` 的服务端字段校验阶段被拦截，发生在素材等待和正式 `generateImage()` / `generateVideo()` 之前。
- 该修复主要覆盖模板回填、旧草稿、状态恢复或服务端模型变更导致的过期选项值；正常 UI 选择仍应只产生合法值。
- 本轮只覆盖顶层 options 非法值；active child options 非法值已在后续提交 `bfa9a2e` 补齐。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when service option value is not allowed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：应用一个带过期顶层选项值的模板或草稿，确认不会进入正式提交。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：active child options 非法值不再进入正式提交

代码提交 `bfa9a2e fix(quickcreate): reject invalid child option values` 和文档提交 `43ed8cb docs(quickcreate): record child option value validation` 已推送到 `feature/kmp-refactoring`。

当前行为：
- `validateServiceFields()` 对 active child options 字段同时覆盖“必填为空”和“非空值不在 `options.value` 列表内”两类拦截。
- inactive child 仍不会参与校验；只有父字段当前值或默认值激活的子字段才会被检查。
- 非法值会在 `generate()` 的服务端字段校验阶段被拦截，发生在素材等待和正式 `generateImage()` / `generateVideo()` 之前，因此不会进入 `prepare/commit`，也不会新增扣费。
- 该修复主要覆盖模板回填、旧草稿、状态恢复或服务端模型变更导致的 active child 过期选项值；正常 UI 选择仍应只产生合法值。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when active child option value is not allowed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：应用一个带过期 active child 选项值的模板或草稿，确认不会进入正式提交。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：空 prompt 拦截不再残留提交状态文案

代码提交 `79f3da1 fix(quickcreate): clear status text on empty prompt` 和文档提交 `2609634 docs(quickcreate): record empty prompt status cleanup` 已推送到 `feature/kmp-refactoring`。

当前行为：
- 图片和视频生成在 prompt 为空或超过限制时，会把 `taskStatus` 置回 `IDLE`，同步清空 `statusText`，并设置 `error=请输入描述词`。
- 该拦截发生在正式 `generateImage()` / `generateVideo()` 请求发出前，因此不会进入 `prepare/commit`，也不会新增扣费。
- 这补齐了提交状态文案清理的 prompt 入口：此前 `generate()` 先写入“正在提交任务...”，再由图片/视频生成函数拦截空 prompt 时，可能留下旧提交文案。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.empty image prompt clears submitting status text when generate is blocked" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.empty video prompt clears submitting status text when generate is blocked"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：清空图片或视频 prompt 后点击生成，确认状态区不会残留“正在提交任务...”。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：已失败素材不再被静默忽略后提交

代码提交 `f0d32b1 fix(quickcreate): block submit when upload failed` 和文档提交 `4c5106c docs(quickcreate): record failed upload guard` 已在本地完成，等待网络恢复后推送到 `feature/kmp-refactoring`。

当前行为：
- `generate()` 进入正式图片/视频生成前会通过 `awaitPendingUploads()` 检查当前 tab 的相关素材。
- 如果相关素材已经是 `FAILED`，会直接返回 `IDLE` 并显示 `素材上传失败: <文件名>`，不会进入正式 `generateImage()` / `generateVideo()`。
- 这覆盖了可选参考素材上传失败的场景，避免用户以为带了参考图，实际提交并扣费一个不带参考素材的 prompt-only 任务。
- 已移除或不属于当前 active 字段的素材不在 `currentRelevantMediaReferences()` 范围内，不会被这条失败检查阻塞。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when selected upload already failed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：让参考图上传失败后点击生成，确认页面提示上传失败且不会进入正式提交。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：超长 prompt 显示长度超限原因

代码提交 `1cb8774 fix(quickcreate): explain over limit prompts` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 图片和视频 prompt 为空时仍提示 `请输入描述词`。
- 图片和视频 prompt 超过 `MAX_PROMPT_CHARS=500` 时，会在正式提交前返回 `IDLE`，清空 `statusText`，并提示 `描述词不能超过 500 个字符`。
- 该拦截发生在正式 `generateImage()` / `generateVideo()` 请求发出前，因此不会进入 `prepare/commit`，也不会新增扣费。

验证记录：

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.over limit image prompt shows length error when generate is blocked" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.over limit video prompt shows length error when generate is blocked"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：输入超过 500 字后点击生成，确认页面提示长度超限且不会进入正式提交。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：非法生成数量不再进入正式请求

代码提交 `40a0e04 fix(quickcreate): ignore unsupported output counts` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 图片生成数量只允许 `1/2/4`；视频生成数量只允许 `1/2`。
- `updateImageCount()` / `updateVideoCount()` 收到不支持的数量时直接返回，保留上一轮合法配置，不刷新价格预览。
- 最终 `ImageGenerationRequest.numImages` 和 `VideoGenerationRequest.numVideos` 只会来自合法数量，避免草稿恢复、外部入口或测试路径把 `0/99` 等值提交到正式生成链路。
- 正常 UI 枚举选择行为不变；本轮只是给公开 ScreenModel 更新入口补齐请求体防线。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image keeps previous count when unsupported image count is requested" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video keeps previous count when unsupported video count is requested"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：从草稿恢复、模板回填或异常状态恢复后切换生成数量，确认界面仍只显示合法数量且提交请求不带非法 `numImages/numVideos`。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：负 seed 不再进入正式请求

代码提交 `4923297 fix(quickcreate): drop negative generation seeds` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 图片和视频高级参数里的 seed 在 ScreenModel 层统一经过 `sanitizedSeed()`。
- 负 seed 会被转成 `null`，含义是随机 seed；合法范围内的 `0` 和正整数继续保留。
- 最终 `ImageGenerationRequest.seed` 和 `VideoGenerationRequest.seed` 不会携带负数，避免高级输入框、草稿恢复或外部入口把非法 seed 带入正式生成链路。
- 本轮只处理请求体安全边界，没有改 Tune 输入框的交互文案；如果产品需要更强提示，可以继续在 `SeedInput` 层限制输入或显示错误。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image clears negative seed before building request" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video clears negative seed before building request"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：在图片和视频高级参数里输入负 seed 后点击生成，确认请求不会带负 seed；必要时再补 UI 层非负数字输入限制。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：模型不支持参数不再进入正式请求

代码提交 `bdf27d1 fix(quickcreate): ignore unsupported model params` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 图片比例、分辨率、质量更新会先检查当前 `ImageModel` 支持集；视频比例、分辨率、时长更新会先检查当前 `VideoModel` 支持集。
- 不支持的值会被忽略，上一轮合法配置会保留，价格预览不会因为非法更新重新请求。
- 最终 `ImageGenerationRequest.aspectRatio/resolution/quality` 和 `VideoGenerationRequest.aspectRatio/resolution/duration` 不会携带当前模型不支持的值。
- 正常 Tune UI 选择行为不变；本轮是在 ScreenModel 公共入口补齐同样的请求体防线。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image keeps previous model params when unsupported image params are requested" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video keeps previous model params when unsupported video params are requested"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：切换到 `Seedream 4.0`、`Seedance2.0-Fast` 等支持范围较窄的模型，确认 Tune UI 与最终提交参数都不会出现不支持的比例、分辨率或时长。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：不支持的视频开关不再进入正式请求

代码提交 `3245edc fix(quickcreate): ignore unsupported video toggles` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- `toggleRealisticMode()` 只会在当前视频模型 `supportsRealistic=true` 时生效。
- `toggleGenerateAudio()` 只会在当前视频模型 `supportsGenerateAudio=true` 时生效。
- 对不支持的模型，开关调用会直接返回，保留 `realisticMode=false` / `generateAudio=false`，也不会刷新价格预览。
- 最终 `VideoGenerationRequest.realistic` 和 `VideoGenerationRequest.generateAudio` 不会携带当前模型不支持的能力。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video ignores realistic toggle when model does not support it" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video ignores audio toggle when model does not support it"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：切换到不支持生成音频或真人模式的视频模型后点击对应按钮，确认请求不会携带不支持字段；如需更好体验，再在 `TuneBottomSheet` 根据模型能力禁用或隐藏按钮。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：模板回填不再绕过模型能力过滤

代码提交 `6db525a fix(quickcreate): filter unsupported template params` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- `applyImageTemplateDetail()` 会先解析模板里的图片比例、分辨率、质量，再按当前 `imageConfig.model` 的支持集过滤。
- `applyVideoTemplateDetail()` 会先解析模板里的视频比例、分辨率、时长，再按当前 `videoConfig.model` 的支持集过滤。
- 视频模板布尔参数 `generateAudio` 和 `realPersonMode` 会再经过当前视频模型能力判断，不支持时最终仍为 `false`。
- 这条防线补齐了模板/灵感路径，不再只依赖用户手动更新函数的模型能力校验。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration image template ignores unsupported model params" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.apply inspiration video template ignores unsupported model params and toggles"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：使用真实灵感模板或抓包样例回填跨模型参数，确认应用模板后 UI 和最终提交都保留当前模型支持范围内的值。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：错分类服务模型不再覆盖当前选择

代码提交 `826a02a fix(quickcreate): reject cross-category service models` 已完成，待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 图片服务模型选择入口会在 `serviceImageModels` 中按 `bindingId/skuId` 查找匹配项，找不到就忽略。
- 视频服务模型选择入口会在 `serviceVideoModels` 中按 `bindingId/skuId` 查找匹配项，找不到就忽略。
- 命中时使用当前列表中的规范模型对象和默认参数，而不是直接信任外部传入对象。
- 最终图片/视频请求不会因为错分类模型对象进入公共入口而携带错误的 `quickCreationCategoryId/bindingId/skuId`。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image keeps selected image service model when video service model is requested" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video keeps selected video service model when image service model is requested"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：刷新服务模型列表、切换图片/视频 tab 后选择服务端模型，确认 UI 只会提交当前分类模型；如果列表重载后模型不再存在，应保留当前合法选择或回退默认选择。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：服务模型刷新使用当前列表对象

代码提交 `6f900a5 fix(quickcreate): canonicalize reloaded service models` 已完成，等待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- `loadServiceModels()` 重新拉取服务端模型列表后，会按 `bindingId/skuId` 在新图片列表或新视频列表中寻找旧选择对应项。
- 如果找到同身份模型，`selectedImageServiceModel` / `selectedVideoServiceModel` 会替换为新列表中的规范对象，而不是继续持有刷新前的旧对象。
- 同身份刷新保留当前 `imageServiceParams` / `videoServiceParams`，避免服务模型轮询或页面重进时覆盖用户已填写参数；身份变化或默认回退时才重新取默认参数。
- 这补齐了上一轮“拒绝错分类模型”的后续边界：不仅选择入口要信任当前列表，列表刷新后的选中对象也必须来自当前列表。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.service model reload keeps selected image model canonical when identity matches" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.service model reload keeps selected video model canonical when identity matches"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：服务模型接口刷新后，确认页面展示的模型名称、字段配置和最终请求体都来自最新接口列表对象。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：无效服务字段不触发价格预览

代码提交 `3a32f4e fix(quickcreate): skip preview for invalid service fields` 已完成，等待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- `scheduleFeePreview()` 仍统一走 `hasFeePreviewRequest()` 判定是否需要服务端价格预览。
- `hasFeePreviewRequest()` 现在会先确认当前图片/视频 tab 能构造正式请求，再复用 `validateCurrentServiceFields()` 和 `validateCurrentServiceUploads()`。
- 只要当前服务端动态字段缺少必填值、带非法 options 值、文本字段不满足长度限制，或 required 上传字段没有完成上传，就不会进入 fee-preview 防抖，也不会调用服务端预览接口。
- 这不会改变正式生成的拦截文案；`generate()` 仍会在用户点击生成时显示对应字段错误。本次只避免输入阶段先发无效 fee-preview。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image fee preview is skipped when required service option field is empty" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.video fee preview is skipped when required service option field is empty"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：找一个带必填下拉或必填上传字段的真实模型，先只输入 prompt，不补齐字段，确认按钮不会进入“价格确认中”并且不发 fee-preview。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：未就绪素材不触发价格预览

代码提交 `a95c7b8 fix(quickcreate): skip preview for unready uploads` 已完成，等待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- `hasFeePreviewRequest()` 在服务字段和上传字段校验通过后，还会检查当前 tab 的 relevant 素材状态。
- relevant 素材中只要有 `FAILED`、`UPLOADING` 或 `PROCESSING`，就不会进入 fee-preview 防抖，也不会调用服务端预览接口。
- relevant 范围由 `currentRelevantMediaReferences()` 决定：全局素材和当前 active 上传字段素材会影响预览；隐藏字段、inactive child 字段、非当前 tab 素材不影响预览。
- 这让 fee-preview 与正式生成的上传等待/失败阻断语义保持一致，避免失败参考图被静默忽略后预览 prompt-only 价格。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.failed image upload prevents image fee preview when prompt changes"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：让参考图上传失败或保持上传中，再输入 prompt，确认按钮不会进入“价格确认中”，也不会发出缺少素材 URL 的 fee-preview。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：上传失败后清理旧价格预览

代码提交 `49e7539 fix(quickcreate): clear preview when upload fails` 已完成，等待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 素材上传进入失败分支后，会触发 `scheduleFeePreviewForMediaReference(id)`。
- `scheduleFeePreviewForMediaReference()` 不再只响应 `DONE + remoteUrl` 的素材；只要该素材仍属于当前 relevant 素材范围，就会让 `scheduleFeePreview()` 重新评估当前状态。
- 由于 `hasFeePreviewRequest()` 已经阻止 `FAILED/UPLOADING/PROCESSING` 素材进入 fee-preview，所以失败后的重评估会清掉旧服务端预览状态，而不是发出新预览请求。
- 这解决了“先得到 prompt-only 服务端价格，再选择参考图但上传失败后，按钮仍显示旧服务端价格”的不一致。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.failed image upload clears previous image fee preview" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.failed image upload prevents image fee preview when prompt changes" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.uploading global image media does not refresh image fee preview before remote url exists"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：先输入 prompt 等服务端价格显示，再选择参考素材并让上传失败，确认按钮不会继续显示旧的 prompt-only 服务端金额。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：删除失败素材后恢复价格预览

代码提交 `a275c10 fix(quickcreate): restore preview after removing failed upload` 已完成，等待本文档提交后一并推送到 `feature/kmp-refactoring`。

当前行为：
- 删除素材前，`removeMediaReference()` 会判断该素材是否属于当前 relevant 素材集合。
- 只要删除的是当前 relevant 素材，无论它是 `DONE`、`FAILED`、`UPLOADING` 还是 `PROCESSING`，删除后都会调用 `scheduleFeePreview()` 重新评估当前价格预览状态。
- 因此失败素材存在时会阻止并清理 fee-preview；删除失败素材后，如果 prompt 和其他字段已经有效，会重新发起 prompt-only 或当前剩余素材对应的 fee-preview。
- 这和上一条“上传失败后清理旧价格预览”形成闭环：失败时不显示旧价，删除失败项后恢复可预览状态。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.removing failed image upload restores image fee preview" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.failed image upload clears previous image fee preview" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.remove media reference removes image media after switching to video tab"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：先让 prompt-only 服务端价格显示，再选择并失败上传参考素材，删除失败素材后确认按钮恢复到服务端预览价格。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：价格预览失败后回退本地估算价

当前行为：
- `scheduleFeePreview()` 在当前状态无法构造有效 fee-preview 请求时，会把 `estimatedCost` 回退到当前 tab 的本地估算价。
- 图片 fee-preview 请求失败时，状态会同时保留 `feePreviewError` 并把 `estimatedCost` 回退到当前图片配置估算价，避免继续展示上一轮服务端金额。
- 视频 fee-preview 请求失败时也通过 `applyFeePreviewError()` 使用同一套本地估算价回退逻辑。
- 点击生成时仍优先检查 `feePreviewError`，所以失败后不会因为有本地估算价而继续提交真实 `prepare/commit`。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image fee preview failure falls back from previous server amount to local estimate"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image fee preview failure falls back from previous server amount to local estimate" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.fee preview failure clears previous task status text when generate is blocked"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：断网或制造 fee-preview 失败后，确认按钮不再显示上一轮服务端金额，点击生成仍显示“价格待确认”。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：价格预览未通过时阻断提交

当前行为：
- 图片和视频 fee-preview 成功响应现在都统一进入 `applyFeePreview()`。
- `applyFeePreview()` 会继续写入服务端预览金额，但只要 `passed=false` 或 `insufficientType != null`，就会设置 `feePreviewError=余额不足或价格预览未通过`。
- `generate()` 在正式提交前会检查 `feePreviewError`，因此价格预览未通过时不会调用 `generateImage()` 或 `generateVideo()`，也不会进入真实 `prepare/commit`。
- 这补齐了 `doc/quick_creation_feature_plan.md` 中“`passed=false` 或 `insufficientType != null` 时禁用提交或提示充值”的约束。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview is not passed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：使用余额不足账号或可控 mock 响应，让 `/task/quick-creation/fee-preview` 返回未通过，确认按钮显示“价格待确认”，点击不会新增任务。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：价格预览未通过时展示具体原因

当前行为：
- `FEE_PREVIEW_NOT_PASSED_ERROR` 统一表示 `余额不足或价格预览未通过`。
- 当 `feePreviewError` 等于该值时，`generate()` 阻断后会把同一具体原因写入 `uiState.error`。
- 当 `feePreviewError` 是网络失败、接口异常或其它预览失败原因时，`generate()` 仍展示通用 `价格待确认`，避免把临时错误误导成余额不足。
- 图片和视频未通过预览的测试都覆盖了“不会生成 + 透出具体原因”。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview failed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview failed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：余额不足或预览未通过时，点击生成应显示 `余额不足或价格预览未通过`；断网或预览接口失败时仍显示 `价格待确认`。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：价格预览未通过时不保留服务端金额

当前行为：
- fee-preview 通过时，`applyFeePreview()` 继续把服务端金额写入 `estimatedCost`。
- fee-preview 未通过时，`estimatedCost` 回退到当前图片或视频配置的本地估算价，`feePreviewError` 保留 `余额不足或价格预览未通过`。
- 底部按钮仍因 `feePreviewError` 显示 `价格待确认`；点击生成时再透出具体未通过原因。
- 这样状态层不再同时保留“不可提交错误”和“服务端返回的不可用扣费金额”。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview is not passed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image prompt refreshes server fee preview into estimated cost" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.video prompt refreshes server fee preview into estimated cost" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image fee preview failure falls back from previous server amount to local estimate"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

下一步建议：
- 真机复测：余额不足或预览未通过时，观察按钮、错误提示和状态刷新，不应继续展示未通过预览返回的金额。
- 本轮没有触发真实生成或扣费；完整 `prepare/commit/list/detail` 仍需按后续授权单独验证。
- 后续提交继续避开已有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 证据目录。

## 2026-06-18 追加交接：旧价格预览响应防回写

当前行为：
- `QuickCreateScreenModel` 为 fee-preview 调度维护 `feePreviewRequestSeq`。
- 每次 `scheduleFeePreview()` 被触发时都会递增序号；旧 Job 即使取消失败或 repository 调用无视取消，返回后也必须通过序号校验才能回写 UI。
- 视频与图片 fee-preview 成功/失败路径都受序号保护；最新输入、最新模型参数、最新上传状态才拥有更新 `estimatedCost/feePreviewLoading/feePreviewError` 的权限。

覆盖的风险：
- 用户快速修改 prompt 或模型参数时，旧网络响应晚于新响应返回，导致按钮价格倒退到旧 prompt 的服务端价格。
- 非协作取消的 repository/网络层调用在真实设备和慢网下仍可能完成，单纯 `Job.cancel()` 不足以保证状态不会被旧响应覆盖。

回归测试：
- `stale image fee preview result does not overwrite latest prompt cost` 使用 `NonCancellable` 模拟旧图片 fee-preview 无视取消并延迟返回。
- 旧实现下测试红灯，最终价格会从新 prompt 的 `0.76` 被旧 prompt 的 `3.33` 覆盖；当前实现保持 `0.76`。

验证记录：
```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.stale image fee preview result does not overwrite latest prompt cost"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.stale image fee preview result does not overwrite latest prompt cost" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image prompt refreshes server fee preview into estimated cost" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.video prompt refreshes server fee preview into estimated cost" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.image fee preview failure falls back from previous server amount to local estimate" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate image is blocked when fee preview is not passed" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest.generate video is blocked when fee preview is not passed"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateScreenModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreationServiceFieldUiModelTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateBillingUiTextTest" --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateTaskStatusUiTest"
git diff --check -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt
```

调试备注：
- 完整快捷创作测试组首次运行时出现一次 `generate image maps field bound images to matching child upload fields` 失败；该用例单独运行通过，完整组重跑通过。当前证据指向既有顺序/调度敏感测试，不是本次 fee-preview 序号改动的稳定回归。

后续建议：
- 真机慢网或代理延迟下验证连续输入 prompt、切换模型参数、上传素材状态变化时，底部按钮价格不会被旧 fee-preview 响应回写。
- 本轮没有触发真实生成、`prepare/commit` 或新增扣费；完整端到端扣费仍需后续明确授权。
- 继续避免提交既有 `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/AuthRepositoryImpl.kt` 修改和未跟踪 `output/` 目录。
