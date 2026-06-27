# feature_discovery_data

## 模块概述

`:feature:discovery:data` 实现 WebApp 目录远端接口、分类 DTO 和仓库映射，用于发现页和搜索页的数据源。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:discovery:data` |
| 路径 | `feature/discovery/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.discovery.data` |
| 完整扫描基线源文件数 | 10 |
| 内部依赖 | `:core:model`, `:core:network`, `:feature:discovery:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/DiscoveryDataModule.kt`：Koin binding。
- `remote/api/WebAppCatalogApi.kt`：目录 endpoint。
- `remote/dto/WebAppCatalogDto.kt`、`CatalogTagDto.kt`、`DiscoveryBaseResponseDto.kt`：远端 DTO。
- `remote/dto/WebAppCatalogMappers.kt`：DTO-to-Domain 映射。
- `repository/WebAppCatalogRepositoryImpl.kt`：仓库实现。

## 约束

- API 路径、分页和 tag 映射由契约测试固定。
- Data 层不得依赖 Presentation 或 Compose。
