# feature_task_data

## 模块概述

`:feature:task:data` 实现 WebApp 任务 API、任务历史、文件上传和任务 DTO 映射。它通过 Task Domain 接口向上暴露能力。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:task:data` |
| 路径 | `feature/task/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.task.data` |
| 完整扫描基线源文件数 | 8 |
| 内部依赖 | `:core:common`, `:core:model`, `:core:network`, `:core:storage`, `:feature:task:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/TaskDataModule.kt`：Koin binding。
- `remote/api/WebAppTaskApi.kt`：任务与上传 endpoint。
- `remote/dto/TaskBaseResponseDto.kt`、`WebAppTaskDto.kt`：远端 DTO。
- `remote/dto/WebAppTaskMappers.kt`：DTO-to-Domain 映射。
- `repository/WebAppTaskRepositoryImpl.kt`：仓库实现。

## 约束

- API Key 只用于远端调用，不得写入日志或 UI。
- 上传和任务轮询必须可取消并映射稳定错误。
- 变更远端路径必须更新契约测试。
