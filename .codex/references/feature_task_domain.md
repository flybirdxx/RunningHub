# feature_task_domain

## 模块概述

`:feature:task:domain` 定义 WebApp 任务运行、任务历史和生成历史的领域模型与仓库契约。它为详情页、历史页和 Task Data 共享任务语义。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:task:domain` |
| 路径 | `feature/task/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.task.domain` |
| 完整扫描基线源文件数 | 5 |
| 内部依赖 | `:core:model` |
| 外部依赖 | `kotlin("test")` |

## 关键源码

- `GenerationHistory.kt`：生成历史领域模型。
- `GenerationHistoryRepository.kt`：生成历史仓库契约。
- `WebAppTaskRepository.kt`：任务运行和输出契约。
- `WebAppTaskHistoryRepository.kt`：任务历史契约。

## 约束

- 任务状态和错误保持领域语义，不绑定远端 DTO。
- API Key 语义只能作为领域需要的输入，不可日志化。
