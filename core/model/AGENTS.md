# core:model Instructions

## Responsibility

本模块只承载跨功能共享的纯业务模型、值对象、枚举和稳定状态类型。

## Allowed

- Kotlin 标准库。
- 无平台依赖的业务模型和值对象。
- 稳定错误码、状态枚举和跨 Feature 可复用的数据结构。

## Forbidden

- Compose、Ktor、DataStore、SQLDelight、Koin。
- Android/iOS/JVM 专属类型。
- DTO、Entity、Request、Response 等数据层命名。
- API endpoint、Header、Cookie、Token 或序列化细节。
- 用户最终展示文案。

## Model Rules

- 金额、时间、尺寸、ID、URL 等字段必须在 KDoc 中说明单位、格式和空值语义。
- 如果同类 String 在多个地方重复出现，应优先提取值对象。
- 不得为了复用把某个 Feature 私有模型过早提升到 core:model。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
