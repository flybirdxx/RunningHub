# feature_model_data

## 模块概述

`:feature:model:data` 实现标准模型目录、SKU/LLM 详情、字段映射、缓存和模型调用请求构造。它通过 Model Domain 接口向上暴露能力。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:model:data` |
| 路径 | `feature/model/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.model.data` |
| 完整扫描基线源文件数 | 16 |
| 内部依赖 | `:core:common`, `:core:network`, `:core:storage`, `:feature:model:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/ModelDataModule.kt`：Koin binding。
- `remote/api/ModelCatalogApi.kt`：SKU/LLM 模型目录 endpoint。
- `remote/dto/ApiModelDto.kt`、`BaseResponseDto.kt`：远端 DTO。
- `repository/ApiModelFieldMapper.kt`：字段映射。
- `repository/ModelCatalogRepositoryImpl.kt`：模型目录仓库实现。
- `repository/ModelInvocationRepositoryImpl.kt`：模型调用仓库实现。
- `repository/ModelInvocationRequestBuilder.kt`：调用请求构造。
- `repository/ModelEndpointRegistry.kt`：endpoint 注册。

## 约束

- DTO 字段兼容和缓存恢复必须有测试覆盖。
- 不把服务端价格/说明作为 Domain 最终展示文案直接透出。
