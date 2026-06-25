# core:common Instructions

## Responsibility

本模块承载跨平台通用工具、错误语义、基础 Result/dispatcher/hash 等非业务能力。

## Allowed

- 纯 Kotlin 工具。
- 跨平台 expect/actual 基础能力，例如 hash、时间、调度器抽象。
- 不依赖具体业务的错误分类。

## Forbidden

- 任何 Feature 业务规则。
- API endpoint、认证协议、数据库 schema。
- Compose UI、Ktor Client、DataStore、SQLDelight。
- 平台类型泄漏到 commonMain。

## Rules

- 工具函数必须有明确边界，不创建无意义 `Util`/`Helper` 集合。
- 安全相关工具必须有测试向量。
- 兼容旧协议的算法，例如 MD5，必须注明协议来源、风险和替换条件。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
