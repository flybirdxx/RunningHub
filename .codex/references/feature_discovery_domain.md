# feature_discovery_domain

## 模块概述

`:feature:discovery:domain` 定义 WebApp 发现页的查询条件、错误语义和目录仓库契约，是 Discovery Data 与 Presentation 的稳定边界。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:discovery:domain` |
| 路径 | `feature/discovery/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.discovery.domain` |
| 完整扫描基线源文件数 | 2 |
| 内部依赖 | `:core:model` |
| 外部依赖 | coroutines, `kotlin("test")` |

## 关键源码

- `CatalogQuery.kt`：目录查询参数和 `CatalogError`。
- `WebAppCatalogRepository.kt`：WebApp 目录仓库契约。

## 约束

- 查询和错误语义必须平台无关。
- 不保存 UI 文案或远端 DTO。
