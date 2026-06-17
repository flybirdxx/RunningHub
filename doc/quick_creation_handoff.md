# 快捷创作重构交接文档

更新时间：2026-06-18
负责人：Codex
分支：`feature/kmp-refactoring`

## 当前实现边界

本轮已经把快捷创作从“纯旧 OpenAPI 硬编码提交”推进到“图片 G-2.0 默认链路使用 Web quick-creation v2”。当前边界如下：

- `all-power-image-g2`：使用抓包确认的 `bindingId=2046586338670891013`、`skuId=2046514150500524034`、`categoryId=IMAGE`。
- 提交流程：`fee-preview -> prepare -> commit -> list`。
- `commit` 请求体：严格使用 `prepareToken + createRequest` 嵌套结构。
- 任务轮询：使用 `/task/quick-creation/list`，不再依赖 `/api/output/taskHistory`。
- 其它图片模型和视频模型：暂时保留旧 `openapi/v2` 兼容逻辑。
- 页面结构：顶部“创作/灵感”、中间滚动区、底部固定输入区已落地。
- 灵感页：当前是 UI 占位，尚未接真实模板接口。

## 关键文件

- `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2Dto.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/QuickCreateApi.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreationV2Defaults.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModel.kt`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateUiState.kt`
- `doc/quick_creation_feature_plan.md`
- `doc/quick_creation_progress.md`

## 后续开发顺序

1. 真机验证图片 G-2.0 文生图：确认登录态、价格、扣费、任务状态和输出预览。
2. 接入 `/api/qc/v2/models`：替换本地图片模型枚举的参数来源，先保留 UI 枚举作为 fallback。
3. 接入真实灵感接口：`tags/templates/template/detail`，把占位列表替换为服务端数据。
4. 抓包并接入 Seedance2.0 视频 v2：复用当前 `QuickCreationCreateRequestDto` 和 prepare/commit/list 状态机。
5. 做历史列表：直接消费 `/task/quick-creation/list`，详情使用 `outputId`。
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
