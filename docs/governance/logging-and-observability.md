# Logging and Observability Governance

## 目标

建立结构化日志、脱敏和崩溃监控规则，避免调试信息变成长期安全风险。

## 规则

- 不记录 Token、Cookie、API Key、密码、完整 Authorization header、验证码、媒体文件名或可能带签名/凭据的 URL。
- 用户 ID、任务 ID 和请求 ID 可以记录，但应优先使用稳定 ID，不记录完整请求体。
- 网络错误日志只记录 endpoint 类别、状态码、业务错误码和可重试性。
- 崩溃监控接入前必须完成隐私评审，明确采集字段、保存周期和关闭方式。
- Release 构建不得启用详细网络 body 日志。

## 脱敏口径

敏感值只允许保留前后少量字符用于排错，例如 `abcd...wxyz`；无法确认敏感级别时按敏感处理。

## 自动门禁

`checkLongTermGovernance` 会扫描生产 Kotlin 源码中的 `Log.`、`println(`、`Napier.`、`Logger.`
和项目内自定义 `debug(` 调用行。如果日志语句同时包含 Token、Cookie、Authorization、API Key、
password、密码、URL 或媒体文件名等敏感语义，任务会失败。需要认证或上传排障时，应记录脱敏摘要、
状态码、请求类别、是否存在媒体位置或可重试性，不记录原始值。

同一门禁也会校验认证头主机判断保持精确白名单匹配。`Authorization` 和 `Cookie` 只能自动附加到
`ApiEnvironment.trustedAuthHosts` 中登记的主机，不能使用 `contains("runninghub.cn")` 或后缀匹配。
