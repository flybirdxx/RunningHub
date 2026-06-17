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
- 历史/详情数据层：`QuickCreateRepository` 已公开 `listQuickCreationHistory(page,size)` 和 `getQuickCreationHistoryDetail(outputId)`；domain 模型会保留任务状态、分类、模型 ID、扣费金额、`apiRequestParams` 标量参数、输出 URL/预览图/尺寸/过期信息。详情请求按抓包结论使用 `outputId`。
- 其它图片模型和未携带服务端 quick-creation ID 的视频请求：暂时保留旧 `openapi/v2` 兼容逻辑。
- 页面结构：顶部“创作/灵感”、中间滚动区、底部固定输入区已落地；底部输入区会显示当前服务端模型摘要。
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
5. 接入历史 UI：数据层已经能消费 `/task/quick-creation/list` 和按 `outputId` 获取详情，下一步在中间滚动区展示历史任务、运行中任务刷新、图片/视频输出预览和详情入口。
6. 再考虑项目管理接口：`project/list/create/rename/delete/pin/detail/tasks`。

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
