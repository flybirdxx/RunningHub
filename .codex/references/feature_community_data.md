# feature_community_data

## 模块概述

`:feature:community:data` 实现 Plaza/社区内容远端接口、DTO 和仓库映射，覆盖创作列表、标签树、短片分类和短片列表。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:community:data` |
| 路径 | `feature/community/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.community.data` |
| 完整扫描基线源文件数 | 9 |
| 内部依赖 | `:core:network`, `:feature:community:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/CommunityDataModule.kt`：Koin binding。
- `remote/api/PlazaApi.kt`：社区远端 endpoint。
- `remote/dto/CommunityBaseResponseDto.kt`：响应 envelope。
- `remote/dto/PlazaDto.kt`：Plaza DTO。
- `remote/dto/PlazaMappers.kt`：DTO-to-Domain 映射。
- `repository/PlazaRepositoryImpl.kt`：仓库实现。

## 约束

- Plaza endpoint、请求体和字段兼容由测试固定；变更 API 必须更新契约测试。
- 标签树压平、分页和媒体字段映射必须保持 Domain 稳定。
- 不向 Presentation 暴露 DTO。
