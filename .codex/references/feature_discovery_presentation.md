# feature_discovery_presentation

## 模块概述

`:feature:discovery:presentation` 提供发现页、搜索页状态和目录错误消息语义，供 `composeApp` 渲染 Discovery/Search UI。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:discovery:presentation` |
| 路径 | `feature/discovery/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.discovery.presentation` |
| 完整扫描基线源文件数 | 5 |
| 内部依赖 | `:core:model`, `:feature:discovery:domain` |
| 外部依赖 | coroutines, test coroutines |

## 关键源码

- `DiscoveryStateHolder.kt`：发现首页状态。
- `SearchStateHolder.kt`：搜索页状态。
- `CatalogErrorMessages.kt`：目录错误语义映射。

## 约束

- 不触达 Data 实现或 Ktor DTO。
- 搜索/发现 UI 事件必须通过 StateHolder 进入 Domain 契约。
