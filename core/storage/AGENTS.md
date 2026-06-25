# core:storage Instructions

## Responsibility

本模块承载存储端口、迁移期 Preferences 存储、权限状态、草稿、余额缓存和平台存储工厂。

## Boundaries

- commonMain 只能暴露跨平台接口和稳定模型。
- Android/iOS 具体路径、Keystore、Keychain、权限 API 放在平台 source set。
- 不依赖 Feature Data 或 composeApp。
- 不生成用户最终 UI 文案。

## Sensitive Data

敏感凭据包括：

- access token
- refresh token
- Cookie
- API Key
- 企业 API Key

这些凭据必须通过安全存储边界读写。迁移期兼容逻辑必须说明懒迁移规则和删除条件。
余额缓存、草稿和普通偏好不得误放入安全凭据接口。

## iOS Rules

iOS 权限和媒体选择不得固定返回授权、空实现或直接成功。需要保留真实系统授权、设置页跳转和可读取 URI。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
