# feature_auth_data

## 模块概述

`:feature:auth:data` 实现 Auth Domain 的远端 API、DTO 映射、会话恢复、用户资料和余额仓库。它由平台 runtime module 装配，不应被 commonMain UI 直接依赖。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:auth:data` |
| 路径 | `feature/auth/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.auth.data` |
| 完整扫描基线源文件数 | 15 |
| 内部依赖 | `:core:common`, `:core:model`, `:core:network`, `:core:storage`, `:feature:auth:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/AuthDataModule.kt`：Koin bindings。
- `remote/api/AuthApi.kt`：用户中心 endpoint 和认证请求。
- `remote/dto/AuthBaseResponseDto.kt`、`AuthDto.kt`、`UserDto.kt`：远端 DTO。
- `remote/dto/AuthMappers.kt`：DTO-to-Domain 映射。
- `repository/AuthRepositoryImpl.kt`：认证仓库实现。
- `repository/UserRepositoryImpl.kt`：用户资料仓库实现。
- `repository/SessionRestoreRepositoryImpl.kt`：会话恢复支撑。
- `repository/ProfileCredentialRepositoryImpl.kt`、`BalanceSnapshotRepositoryImpl.kt`：资料与余额实现。

## 约束

- Authorization header 可以发送到远端，但不得进入日志或错误文案。
- API 路径和请求体变更必须补 Ktor MockEngine 契约测试。
- Data 实现只暴露 Domain 接口，不向 Presentation 暴露 DTO。
