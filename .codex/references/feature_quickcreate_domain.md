# feature_quickcreate_domain

## 模块概述

`:feature:quickcreate:domain` 定义快捷创作的模型目录、生成、费用预览、媒体上传、项目、灵感、草稿和选择仓库契约，是 QuickCreate 新架构的核心边界。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:quickcreate:domain` |
| 路径 | `feature/quickcreate/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.quickcreate.domain` |
| 完整扫描基线源文件数 | 14 |
| 内部依赖 | 无 |
| 外部依赖 | coroutines, `kotlin("test")` |

## 关键源码

- `QuickCreationServiceModel.kt`、`QuickCreationServiceSchema.kt`：服务模型和字段 schema。
- `QuickCreationModels.kt`：旧模型标识和能力。
- `QuickCreationGenerationRepository.kt`：生成契约。
- `QuickCreationFeePreviewRepository.kt`：费用预览契约。
- `QuickCreationMediaUploadRepository.kt`：媒体上传契约。
- `QuickCreationModelCatalogRepository.kt`：快捷创作模型目录契约。
- `QuickCreationProjectRepository.kt`：项目契约。
- `QuickCreationInspirationRepository.kt`：灵感模板契约。
- `QuickCreateDraftRepository.kt`、`QuickCreateModelSelectionRepository.kt`：本地草稿和模型选择契约。
- `QuickCreateRepositoryIssue.kt`：稳定问题语义。

## 约束

- Domain 不承载最终中文 UI 文案。
- 生成、上传、轮询和取消必须可表达稳定状态和错误。
- 不依赖 Data、Ktor、Compose 或平台 SDK。
