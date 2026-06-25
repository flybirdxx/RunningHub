# core:network Instructions

## Responsibility

本模块承载 Ktor 客户端配置、API 环境、认证头注入、Token 刷新、网络活动计数和网络错误映射。

## Boundaries

- 可以依赖 `core:storage` 的凭据端口，但不得依赖具体 Feature 或 composeApp。
- 不得直接导航、弹窗或生成最终 UI 文案。
- 不得引入 Feature Data 实现。
- 不得把 Token、Cookie、API Key、验证码 token 或请求体写入日志。

## Auth Rules

- Authorization/Cookie 只能发送到 `ApiEnvironment.trustedAuthHosts` 中的精确主机。
- 禁止用 `contains`、后缀匹配或通配符判断可信主机。
- Refresh Client 与 Main Client 必须隔离。
- Token 刷新必须使用 Mutex 去重。
- 401 后最多重试原请求一次。
- 非幂等请求重试前必须确认请求体可重放且不会产生重复副作用。
- Logout 与 refresh 并发必须有测试。

## Tests

修改认证、刷新、环境或错误映射时，必须覆盖：

- trusted host 正/反例；
- token 刷新成功、失败、并发去重；
- 重试仍 401；
- logout 期间迟到 refresh 结果不写回凭据；
- 日志脱敏。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
