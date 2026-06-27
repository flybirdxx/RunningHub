# feature_model_domain

## 模块概述

`:feature:model:domain` 定义标准模型目录、模型调用、字段值和仓库契约。QuickCreate 和应用壳通过该层读取模型目录和发起标准模型调用。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:model:domain` |
| 路径 | `feature/model/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.model.domain` |
| 完整扫描基线源文件数 | 4 |
| 内部依赖 | 无 |
| 外部依赖 | `kotlin("test")` |

## 关键源码

- `ApiModel.kt`：模型目录领域模型。
- `ModelCatalogRepository.kt`：模型目录仓库契约。
- `ModelInvocation.kt`：模型调用字段值和请求语义。
- `ModelInvocationRepository.kt`：模型调用仓库契约。

## 约束

- 领域模型不保存最终 UI 展示文案。
- 字段值应保持平台无关，避免传递 JSON 字符串旁路。
