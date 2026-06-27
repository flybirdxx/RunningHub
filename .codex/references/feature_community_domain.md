# feature_community_domain

## 模块概述

`:feature:community:domain` 定义 Plaza/社区内容的领域模型和仓库契约。该模块为社区页、Plaza 页和数据实现共享分类、作品、短片等稳定结构。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:community:domain` |
| 路径 | `feature/community/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.community.domain` |
| 完整扫描基线源文件数 | 3 |
| 内部依赖 | 无 |
| 外部依赖 | `kotlin("test")` |

## 关键源码

- `Plaza.kt`：社区/Plaza 内容、分类、媒体和分页领域模型。
- `PlazaRepository.kt`：社区内容仓库契约。

## 约束

- 不引入远端 DTO 或 UI 文案。
- 字段变化必须同步 Data mapper 和 Presentation 状态测试。
