# core_network

## 模块概述

`:core:network` 提供 RunningHub 网络环境、Ktor client 默认配置、认证头与 token refresh 支撑。Feature Data 模块复用该层访问远端 API。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:core:network` |
| 路径 | `core/network` |
| 类型 | library |
| 命名空间 | `com.runninghub.core.network` |
| 完整扫描基线源文件数 | 14 |
| 内部依赖 | `:core:common`, `:core:storage` |
| 外部依赖 | Ktor client, kotlinx.serialization, coroutines |

## 关键源码

- `RunningHubApiEnvironment.kt`：可注入 API 环境与 base URL 配置。
- `RunningHubHttpClientDefaults.kt`：Ktor client 默认配置。
- `NetworkActivityTracker.kt`：运行期网络活动观察。
- `NetworkErrorMapper.kt`：网络异常到稳定错误语义的映射。
- `auth/AuthHeaderProvider.kt`：认证头提供者。
- `auth/RunningHubAuthInterceptors.kt`：认证拦截器。
- `auth/TokenRefresher.kt`：token 刷新流程。

## 约束

- API base URL 必须由平台 runtime module 注入，不得在 Data 层写死。
- token refresh 响应使用 DTO 解析，禁止正则提取敏感字段。
- 不输出 Token、Cookie、Authorization 或请求体到日志。
- 修改认证或 client 默认配置后运行相关 core/network 测试和架构边界检查。
