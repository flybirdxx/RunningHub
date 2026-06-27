# feature_quickcreate_presentation

## 模块概述

`:feature:quickcreate:presentation` 承载 QuickCreate 页面级状态、Coordinator、费用预览、生成、草稿、历史、项目、灵感、上传和字段 UI 模型。它是 `composeApp` QuickCreate UI 的主要状态来源。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:quickcreate:presentation` |
| 路径 | `feature/quickcreate/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.quickcreate.presentation` |
| 完整扫描基线源文件数 | 51 |
| 内部依赖 | `:feature:quickcreate:domain` |
| 外部依赖 | coroutines, kotlinx.datetime, test coroutines |

## 关键源码

- `QuickCreatePresentationStateHolder.kt`：页面聚合状态和 factory。
- `coordinator/QuickCreateCoordinator.kt`：多个状态容器与领域仓库的协调。
- `editor/QuickCreateEditorStateHolder.kt`、`QuickCreateEditorUiModels.kt`：编辑器状态。
- `billing/QuickCreateFeePreviewInteractor.kt`、`QuickCreateBillingUiText.kt`：费用预览和展示语义。
- `generation/QuickCreateGenerationInteractor.kt`、`QuickCreateGenerationRequestFactory.kt`：生成请求和提交流程。
- `history/QuickCreateHistoryStateHolder.kt`：快捷创作历史。
- `project/QuickCreateProjectStateHolder.kt`：项目状态。
- `inspiration/QuickCreateInspirationStateHolder.kt`：灵感模板状态。
- `upload/QuickCreateMediaUploadCoordinator.kt`：媒体上传协调。
- `result/QuickCreateTaskPollingController.kt`、`QuickCreateTaskStatusUi.kt`：任务状态轮询和展示语义。

## 约束

- `composeApp` 的 `QuickCreateScreenModel` 只做 Voyager facade，不回收这里的状态所有权。
- 远端错误必须降级为稳定 Presentation 语义。
- 任务状态区不得重新引入自定义原文透传。
- 修改生成/上传/轮询流程必须补取消路径测试。
