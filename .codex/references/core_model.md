# core_model

## 模块概述

`:core:model` 提供跨业务复用的纯领域数据模型，包括应用详情、分页、标签、任务、用户和 WebApp。该模块无内部模块依赖，是 Feature Domain 与应用壳共享模型的基础层。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:core:model` |
| 路径 | `core/model` |
| 类型 | library |
| 命名空间 | `com.runninghub.core.model` |
| 完整扫描基线源文件数 | 7 |
| 内部依赖 | 无 |
| 外部依赖 | `kotlin("test")` |

## 关键源码

- `AppDetail.kt`：应用详情领域模型。
- `PageData.kt`：分页响应的通用领域结构。
- `Tag.kt`：标签和分类模型。
- `Task.kt`：任务、执行状态和结果模型。
- `User.kt`：用户资料和账号信息模型。
- `WebApp.kt`：WebApp 列表、详情和能力描述模型。

## 约束

- 只保存平台无关数据结构，不依赖网络、存储、Compose 或平台 SDK。
- 新增字段必须考虑 DTO-to-Domain mapper 和相关契约测试。
- 不存储最终 UI 文案；展示文案应在 Presentation 或 Compose Resources 映射。
