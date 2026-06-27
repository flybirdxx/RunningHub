# feature_community_presentation

## 模块概述

`:feature:community:presentation` 提供社区聚合页与 Plaza 页状态容器，负责分类、分页、加载、错误和刷新语义。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:community:presentation` |
| 路径 | `feature/community/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.community.presentation` |
| 完整扫描基线源文件数 | 4 |
| 内部依赖 | `:feature:community:domain` |
| 外部依赖 | coroutines, test coroutines |

## 关键源码

- `CommunityStateHolder.kt`：社区页入口状态。
- `PlazaStateHolder.kt`：Plaza 分类、列表和分页状态。

## 约束

- 只依赖 Community Domain，不依赖 Data。
- 远端错误统一成稳定错误语义。
- UI 文案由应用壳资源映射。
