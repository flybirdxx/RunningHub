# feature:task Instructions

## Responsibility

本 Feature 负责：WebApp 任务运行、上传、输出轮询、统一历史与任务历史。

## Boundaries

- 业务契约放入 `domain`。
- API、DTO、Mapper、Repository 实现放入 `data`。
- UiState、Action、StateHolder、Interactor、ScreenModel 门面放入 `presentation`，如果该层存在。
- 不得导入 `com.runninghub.shared.*`。
- 不得从 Presentation 直接使用 Data 实现。
- 不得把服务端 `msg` 直接作为最终 UI 文案。

## Feature-specific Notes

- 修改本 Feature 时，先检查是否已有 Domain 契约可以复用。
- 如果需要跨 Feature 调用，优先在 composeApp 组合层增加适配器，而不是让两个 Feature Data 互相依赖。
- 新增接口时必须补 API/DTO/Mapper/Repository 测试。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
