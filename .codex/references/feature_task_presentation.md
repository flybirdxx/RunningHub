# feature_task_presentation

## 模块概述

`:feature:task:presentation` 提供任务历史页面状态容器，负责历史列表、详情、取消、复用参数和稳定展示语义。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:task:presentation` |
| 路径 | `feature/task/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.task.presentation` |
| 完整扫描基线源文件数 | 2 |
| 内部依赖 | `:feature:task:domain` |
| 外部依赖 | coroutines, test coroutines |

## 关键源码

- `TaskHistoryStateHolder.kt`：历史列表、筛选、详情、取消任务和展示文本语义。

## 约束

- 历史页费用和输出数量语义集中在 Presentation，不允许 `composeApp` 根据运行时标题推断。
- 不透传 Repository 异常或服务端原始 message。
