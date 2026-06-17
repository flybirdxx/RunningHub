# 快捷创作重构交接文档

更新时间：2026-06-18
负责人：Codex
分支：`feature/kmp-refactoring`

## 当前实现边界

本轮已经把快捷创作从“纯旧 OpenAPI 硬编码提交”推进到“图片 G-2.0 默认链路使用 Web quick-creation v2”，并把携带服务端 quick-creation ID 的视频请求接入同一套 v2 状态机。当前边界如下：

- `all-power-image-g2`：默认仍可使用抓包确认的 `bindingId=2046586338670891013`、`skuId=2046514150500524034`、`categoryId=IMAGE`。
- `/api/qc/v2/models`：已接 DTO、mapper、repository 和 ScreenModel 状态，`IMAGE/VIDEO` 服务端模型会在页面初始化时加载。
- 图片服务端模型：加载后默认选中首个可用模型，Tune 高级页可切换；图片 v2 提交会优先携带选中模型的 `categoryId/bindingId/skuId`。
- 服务端字段：`fields/options/defaultValue/maxUploadCount/maxUploadSize/multipleInputs/skuInputExtraJson` 已解析到 domain model；字段默认值会初始化到 UI state，Tune 高级页可点选基础 options，也可输入文本/数值字段；图片/视频/音频上传字段会按字段类型把已上传素材 URL 写入对应服务端 `paramKey` 的数组参数；图片 v2 和携带服务端 ID 的视频 v2 提交会把当前字段值写入 `params`，未知字段会被过滤。
- 视频 v2：当 `VideoGenerationRequest` 携带 `quickCreationBindingId` 和 `quickCreationSkuId` 时，repository 会走 `fee-preview -> prepare -> commit -> list`；`QuickCreationV2Defaults.videoCreateRequest` 会生成抓包确认的 Seedance2.0 多模态参数，包括 `ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、`creationMode=multimodal`、`creationSubModeId=1`、`creationSubModeKey=MULTIMODAL_REFERENCE` 和参考素材 URL 数组。
- 提交流程：`fee-preview -> prepare -> commit -> list`。
- `commit` 请求体：严格使用 `prepareToken + createRequest` 嵌套结构。
- 任务轮询：使用 `/task/quick-creation/list`，不再依赖 `/api/output/taskHistory`。
- 历史/详情/取消数据层：`QuickCreateRepository` 已公开 `listQuickCreationHistory(page,size)`、`getQuickCreationHistoryDetail(outputId)` 和 `cancelQuickCreationTask(taskId)`；domain 模型会保留任务状态、分类、模型 ID、扣费金额、`apiRequestParams` 标量参数、输出 URL/预览图/尺寸/过期信息。详情请求按抓包结论使用 `outputId`；取消接口按 Chrome DevTools 确认使用 `/task/quick-creation/cancel`，请求体为 URL 编码后的 `taskId`。
- 项目列表：已公开 `listQuickCreationProjects(page,size)`，使用 `/task/quick-creation/project/list`，请求体为 `{"page":1,"size":20}`，响应按 `records/size/current/total/pages/hasNext/hasPrevious/nextCursor` 映射为 domain 分页模型；`QuickCreateScreenModel` 初始化会加载项目列表，历史区顶部会展示项目横向列表。当前账号抓包返回空列表。
- 项目任务：已公开 `listQuickCreationProjectTasks(projectId,page,size)`，使用 `/task/quick-creation/project/tasks`。Chrome DevTools 观察到空项目状态下 Web 请求体为 `{"page":1,"size":10}` 且服务端返回 `code=301,msg=不能为null`，前端包只确认端点函数 `quickCreationProjectTasksApi`；移动端暂按 `projectId/page/size` 请求体实现并复用历史分页模型。创作页项目横向条已可点击，选中项目后中间列表切到项目内任务，点击“最近创作”可清除筛选。后续拿到非空项目真实响应后需要校正请求体字段或响应结构。
- 项目置顶：已公开 `pinQuickCreationProject(projectId,pinned)`，使用 `/task/quick-creation/project/pin`。前端 bundle 确认了 `quickCreationProjectPinApi` 端点，项目列表 DTO 已确认存在 `pin/pinned` 字段；移动端暂按 `{"projectId":"...","pin":true|false}` 请求体实现。项目 chip 上的图钉可切换置顶/取消置顶，请求中显示小 loading，成功后更新本地项目 state。该请求体字段仍需用非空项目的真实交互抓包确认。
- 项目创建/重命名/删除/详情：已公开 `createQuickCreationProject(name)`、`renameQuickCreationProject(projectId,name)`、`deleteQuickCreationProject(projectId)` 和 `getQuickCreationProjectDetail(projectId)`，分别使用 `/task/quick-creation/project/create|rename|delete|detail`。前端 bundle 只确认端点函数存在，移动端暂按 `name`、`projectId/name`、`projectId` 请求体实现；创建和详情响应按 `QuickCreationProjectDto` 映射，重命名和删除按成功 envelope 处理。历史区项目标题右侧可新建项目，项目 chip 更多菜单可重命名和删除；删除当前筛选项目后会回到最近创作。项目详情目前只有数据层，独立详情 UI 尚未设计。
- 历史 UI：`QuickCreateScreenModel` 初始化会加载最近 10 条 quick-creation 历史，生成成功后会刷新历史；创作页中间区域在没有当前任务/结果时展示最近创作，支持图片/视频预览、状态、分类和扣费金额摘要；列表底部可加载更多历史页并去重追加；当历史项状态不是 `SUCCESS/FAILED/ERROR/CANCELED` 等终态时，会每 5 秒刷新当前已加载范围，并显示取消入口；点击项目筛选时同一列表会加载项目内任务；点击历史项会按 `outputId` 加载详情并展示详情弹窗。
- 其它图片模型和未携带服务端 quick-creation ID 的视频请求：暂时保留旧 `openapi/v2` 兼容逻辑。
- 页面结构：按移动端截图理解为顶部轻量标题/模式区，中间大面积可滚动 RecyclerView 内容区，底部固定模型参数和提示词输入区；当前已落地顶部“创作/灵感”、中间滚动区、底部固定输入区，底部输入区会显示当前服务端模型摘要。
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
6. 补项目详情 UI 与真实校验：`project/list`、`project/tasks`、`project/pin`、`project/create/rename/delete` 已进入 ScreenModel/历史区 UI，`project/detail` 已进入数据层；下一步补独立项目详情 UI，并用非空项目抓包校正项目任务结构和项目管理请求体字段。

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
