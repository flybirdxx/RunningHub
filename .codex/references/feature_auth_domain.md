# feature_auth_domain

## 模块概述

`:feature:auth:domain` 定义登录、会话、用户资料、余额和凭据资料相关领域契约。它是 Auth Data、Auth Presentation 和应用壳共享的稳定边界。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:auth:domain` |
| 路径 | `feature/auth/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.auth.domain` |
| 完整扫描基线源文件数 | 7 |
| 内部依赖 | `:core:model` |
| 外部依赖 | coroutines, `kotlin("test")` |

## 关键源码

- `AuthRepository.kt`：登录、短信验证码、登出等认证契约。
- `UserRepository.kt`：用户资料和账号状态契约。
- `ProfileCredentialRepository.kt`：资料页凭据访问契约。
- `BalanceSnapshotRepository.kt`：余额快照契约。
- `GetLastKnownBalanceUseCase.kt`：余额缓存读取用例。
- `SessionManager.kt`：会话状态协调。

## 约束

- Domain 不依赖 Ktor、Koin、DataStore、Compose 或平台 SDK。
- `SessionManager` 生产构造必须通过 DI 提供恢复仓库，不允许裸构造。
- 对外暴露稳定错误语义，远端 message 不能直接进入 UI。
