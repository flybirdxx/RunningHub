# Feature Modules Instructions

## Scope

适用于 `feature/` 下所有业务模块。

## Architecture

每个 Feature 遵守：

```text
presentation -> domain <- data
```

## Global Rules

- Feature 之间不得直接依赖对方 Data 实现。
- Domain 不依赖 Compose、Ktor、DataStore、SQLDelight、平台 SDK 或 `shared`。
- Data 不依赖 composeApp 或任何 Presentation 类型。
- Presentation 不依赖 Data 实现、Ktor、DataStore、SQLDelight 或 DTO。
- 新业务不得写入历史 `shared` 包或导入 `com.runninghub.shared.*`。
- 跨 Feature 协作通过 Domain 接口或 composeApp 组合层适配器完成。

## Module Creation

新增 Feature 时必须同时明确：

- Domain 契约；
- Data 实现是否需要；
- Presentation 是否独立成模块；
- DI 装配位置；
- 测试矩阵；
- 是否影响 Android/iOS 平台能力。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
