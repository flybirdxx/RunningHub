# feature:auth:presentation Instructions

## Responsibility

本模块承载 `auth` 的 Presentation 契约和可复用状态逻辑，例如 UiState、Action、StateHolder、Interactor、Coordinator 或 ScreenModel 门面。

## Allowed

- 当前 Feature Domain。
- `core:model`、`core:common`。
- Kotlin coroutines、StateFlow、跨平台 Presentation 辅助类型。
- Compose 仅限确实属于 Presentation UI 模块且已在 build.gradle 声明。

## Forbidden

- Ktor API、DTO、DataStore、SQLDelight、Feature Data 实现。
- Android/iOS 平台类型进入 commonMain UiState。
- 直接读取 Token、Cookie、API Key。
- 直接使用服务端 `msg` 作为最终用户文案。
- 无取消条件的轮询、上传或自动保存任务。

## Feature Rules

- 登录页不直接替换根导航；登录成功由 SessionManager 驱动 App 根入口。
- 图形验证码弹窗只保存 UI 开关，不保存验证码 token。

## State Rules

- UiState 必须不可变并逐字段中文说明。
- Loading、Error、分页、选中项、旧响应隔离都必须有明确语义。
- 所有 Job 必须说明启动条件、取消条件和生命周期。
- 页面销毁或不可见时必须释放轮询和上传。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
