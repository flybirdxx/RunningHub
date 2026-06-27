# feature_detail_presentation

## 模块概述

`:feature:detail:presentation` 提供应用详情页输入展示与状态容器，连接 WebApp 详情、任务创建和页面展示所需的稳定状态。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:detail:presentation` |
| 路径 | `feature/detail/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.detail.presentation` |
| 完整扫描基线源文件数 | 4 |
| 内部依赖 | `:core:model`, `:feature:discovery:domain`, `:feature:task:domain` |
| 外部依赖 | coroutines, kotlinx.datetime, test coroutines |

## 关键源码

- `AppDetailInputPresentation.kt`：详情页输入字段的展示模型。
- `AppDetailStateHolder.kt`：详情页加载、输入和任务动作状态。

## 约束

- 详情页只能依赖 Domain 契约，不直接访问 Data。
- 输入展示语义与任务提交契约要同步测试。
