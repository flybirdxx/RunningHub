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
