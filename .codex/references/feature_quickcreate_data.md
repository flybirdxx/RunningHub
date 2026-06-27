# feature_quickcreate_data

## 模块概述

`:feature:quickcreate:data` 实现 QuickCreate Web/API v2 endpoint、DTO、模型映射、生成、费用预览、历史、项目、灵感、上传、草稿和缓存。它是 QuickCreate Domain 仓库契约的运行期实现。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:quickcreate:data` |
| 路径 | `feature/quickcreate/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.quickcreate.data` |
| 完整扫描基线源文件数 | 23 |
| 内部依赖 | `:core:network`, `:core:storage`, `:core:model`, `:feature:auth:domain`, `:feature:model:domain`, `:feature:quickcreate:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/QuickCreateDataModule.kt`：Koin binding。
- `remote/api/QuickCreateApi.kt`：Web 快捷创作和 OpenAPI v2 endpoint。
- `remote/dto/QuickCreateDto.kt`、`QuickCreationV2Dto.kt`：远端 DTO。
- `repository/QuickCreateRepositoryImpl.kt`：核心仓库实现。
- `repository/QuickCreationModelMapper.kt`：模型目录映射。
- `repository/QuickCreationV2Defaults.kt`：v2 默认值。
- `repository/QuickCreateDraftRepositoryImpl.kt`：草稿持久化。
- `repository/QuickCreateModelSelectionRepositoryImpl.kt`：模型选择缓存。
- `repository/QuickCreateDataDebugLog.kt` + platform actual：调试日志边界。

## 约束

- API Key 只通过 Authorization header 或远端契约需要的字段发送，不能日志化。
- 快捷创作生成、上传和轮询必须可取消。
- 模型目录缓存和 DTO 兼容必须有测试覆盖。
- Data 不直接持有 UI 最终文案。
