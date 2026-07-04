# feature_community_presentation

## 模块概述

`:feature:community:presentation` 提供 Plaza(广场，底栏「灵感」tab）页状态容器，负责分类、分页、加载、错误和刷新语义。原社区工具聚合页（`CommunityStateHolder`）为导航不可达死代码，已于 UI 重设计收尾批删除，本模块现仅托管 Plaza。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:community:presentation` |
| 路径 | `feature/community/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.community.presentation` |
| 内部依赖 | `:feature:community:domain` |
| 外部依赖 | coroutines, test coroutines |

## 关键源码

- `PlazaStateHolder.kt`：Plaza 分类、列表和分页状态。
- `PlazaReusePresentation.kt`：Plaza 作品卡与复用参数 UI 模型。

## 约束

- 只依赖 Community Domain，不依赖 Data。
- 远端错误统一成稳定错误语义。
- UI 文案由应用壳资源映射。
