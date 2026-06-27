# feature_auth_presentation

## 模块概述

`:feature:auth:presentation` 保存登录、资料页和创作者资料的状态容器。该层依赖 Auth/Discovery Domain，向 `composeApp` 提供可展示状态和用户动作语义。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:auth:presentation` |
| 路径 | `feature/auth/presentation` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.auth.presentation` |
| 完整扫描基线源文件数 | 6 |
| 内部依赖 | `:core:model`, `:feature:auth:domain`, `:feature:discovery:domain` |
| 外部依赖 | coroutines, test coroutines |

## 关键源码

- `login/LoginStateHolder.kt`：登录页状态和动作。
- `profile/ProfileStateHolder.kt`：资料页状态。
- `creator/CreatorProfileStateHolder.kt`：创作者资料状态。

## 约束

- 不依赖 Auth Data 实现或 Ktor DTO。
- 只输出稳定 UI 语义，不透传远端原始错误。
- Compose 资源文案由 `composeApp` 映射，Presentation 保持语义层。
